package io.restassured.internal.assertion;

import io.restassured.assertion.StreamVerifier;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.XmlConfig;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.common.assertion.Assertion;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.xml.HasXPath;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.restassured.config.MatcherConfig.ErrorDescriptionType.REST_ASSURED;
import static java.lang.String.format;

public class BodyMatcher {

    private static final String XPATH = "XPath";
    private Object key;
    private Matcher matcher;
    private ResponseParserRegistrar rpr;

    public Map<String, Object> validate(Response response, Object contentParser, RestAssuredConfig config) {
        boolean success = true;
        String errorMessage = "";

        contentParser = fallbackToResponseBodyIfContentParserIsNull(response, contentParser);
        if (key == null) {
            if (isXPathMatcher()) {
                XmlConfig xmlConfig = config.getXmlConfig();
                boolean namespaceAware = xmlConfig.isNamespaceAware();
                Map<String, Boolean> features = xmlConfig.features();

                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(namespaceAware);
                if (!features.isEmpty()) {
                    features.forEach((featureName, isEnabled) -> {
                        try {
                            factory.setFeature(featureName, isEnabled);
                        } catch (ParserConfigurationException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }


                Map<String, Object> properties = xmlConfig.properties();
                if (!properties.isEmpty()) {
                    properties.forEach(factory::setAttribute);
                }


                Element node;
                try {
                    node = factory.newDocumentBuilder().parse(new ByteArrayInputStream(response.asByteArray())).getDocumentElement();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (!matcher.matches(node)) {
                    success = false;
                    if (config.getMatcherConfig().hasErrorDescriptionType(REST_ASSURED)) {
                        errorMessage = String.format("Expected: %s\n  Actual: %s\n", StringUtils.trim(matcher.toString()), contentParser);
                    } else {
                        errorMessage = getDescription(matcher, contentParser);
                    }
                }

            } else if (!matcher.matches(response.asString())) {
                success = false;
                if (config.getMatcherConfig().hasErrorDescriptionType(REST_ASSURED)) {
                    errorMessage = "Response body doesn't match expectation.\nExpected: " + getMatcher() + "\n  Actual: " + contentParser + "\n";
                } else {
                    errorMessage = String.format("Response body doesn't match expectation.\n%s", getDescription(matcher, response.asString()));
                }

            }

        } else {
            Assertion assertion = (Assertion) StreamVerifier.newAssertion(response, key, rpr);
            Object result = null;
            if (contentParser != null) {
                if (contentParser instanceof String) {
                    // This happens for example when expecting JSON/XML assertion but response content is empty
                    boolean isEmpty = ((String) contentParser).isEmpty();
                    errorMessage = format("Cannot assert that path \"%s\" matches %s because the response body %s.", key, matcher, isEmpty ? "is empty" : "equal to \"$contentParser\"");
                    success = false;
                } else {
                    result = assertion.getResult(contentParser, config);
                }
            }

            if (success && !matcher.matches(result)) {
                success = false;
                if (config.getMatcherConfig().hasErrorDescriptionType(REST_ASSURED)) {
                    if (result instanceof Object[]) {
                        result = Arrays.stream((Object[]) result).map(Objects::toString).collect(Collectors.joining(","));
                    }
                    errorMessage = String.format("%s %s doesn't match.\nExpected: %s\n  Actual: %s\n", assertion.description(), key, removeQuotesIfString(matcher.toString()), removeQuotesIfString(new StringDescription().appendValue(result).toString()));
                } else {
                    errorMessage = String.format("%s %s doesn't match.\n%s", assertion.description(), key, getDescription(matcher, result));
                }

            }

        }

        LinkedHashMap<String, Object> map = new LinkedHashMap<>(2);
        map.put("success", success);
        map.put("errorMessage", errorMessage);
        return map;
    }

    private static String getDescription(Matcher matcher, Object actual) {
        Description description = new StringDescription();
        description.appendText("\nExpected: ").appendDescriptionOf(matcher).appendText("\n  Actual: ");
        matcher.describeMismatch(actual, description);
        return description.toString();
    }

    private static String removeQuotesIfString(String string) {
        if (StringUtils.startsWith(string, "\"") && StringUtils.endsWith(string, "\"")) {
            String start = StringUtils.removeStart(string, "\"");
            string = StringUtils.removeEnd(start, "\"");
        }

        return string;
    }

    public static Object fallbackToResponseBodyIfContentParserIsNull(Response response, Object contentParser) {
        if (contentParser == null) {
            return response.asString();
        }

        return contentParser;
    }

    private boolean isXPathMatcher() {
        Supplier<Boolean> isNestedMatcherContainingXPathMatcher = () -> {
            StringDescription description = new StringDescription();
            getMatcher().describeTo(description);
            return description.toString().contains(XPATH);
        };

        return matcher instanceof HasXPath || isNestedMatcherContainingXPathMatcher.get();
    }

    public boolean requiresTextParsing() {
        return key == null || isXPathMatcher();
    }

    public boolean requiresPathParsing() {
        return !requiresTextParsing();
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Matcher getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    public ResponseParserRegistrar getRpr() {
        return rpr;
    }

    public void setRpr(ResponseParserRegistrar rpr) {
        this.rpr = rpr;
    }
}

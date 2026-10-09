/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.restassured.internal;


import io.restassured.assertion.DetailedCookieAssertion;
import io.restassured.assertion.HeaderMatcher;
import io.restassured.assertion.ResponseTimeMatcher;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Cookies;
import io.restassured.http.ContentType;
import io.restassured.internal.MapCreator.ArgsAndValue;
import io.restassured.internal.MapCreator.CollisionStrategy;
import io.restassured.internal.assertion.BodyMatcher;
import io.restassured.internal.assertion.BodyMatcherGroup;
import io.restassured.internal.assertion.CookieMatcher;
import io.restassured.internal.http.HttpResponseDecorator;
import io.restassured.internal.http.HttpResponseHandler;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.util.MatcherErrorMessageBuilder;
import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.matcher.DetailedCookieMatcher;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.Argument;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSender;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseLogSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.restassured.http.ContentType.ANY;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.common.util.GroovyStyleToString.format;
import static io.restassured.internal.util.GroovyStringConversion.castToString;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.hamcrest.Matchers.equalTo;

public class ResponseSpecificationImpl implements FilterableResponseSpecification {

    private static final String EMPTY = "";
    private static final String DOT = ".";
    private static final String SAFE_REF = "?.";
    private static final String SAFE_INDEX = "?[";
    private static final String SPREAD_REF = "*.";
    private static final Pattern CONTENT_TYPE_WITHOUT_PARAMETERS = Pattern.compile("(^[\\w\\d_\\-]+/[\\w\\d_\\-]+)\\s*(?:;)");

    private Matcher<Integer> expectedStatusCode;
    private Matcher<String> expectedStatusLine;
    private BodyMatcherGroup bodyMatchers = new BodyMatcherGroup();
    private HamcrestAssertionClosure assertionClosure = new HamcrestAssertionClosure(this);
    private List<HeaderMatcher> headerAssertions = new ArrayList<>();
    // Instances of CookieMatcher and DetailedCookieAssertion
    private List<Object> cookieAssertions = new ArrayList<>();
    private RequestSpecification requestSpecification;
    // A String, a ContentType or a Matcher
    private Object contentType;
    private RestAssuredResponseImpl restAssuredResponse;
    private String bodyRootPath;
    private ResponseParserRegistrar rpr;
    private RestAssuredConfig config;
    private Response response;
    private SimpleEntry<Matcher<Long>, TimeUnit> expectedResponseTime;
    private LogDetail responseLogDetail;
    private boolean forceDisableEagerAssert = false;
    private String onFailMessage;

    private Object contentParser;
    private LogRepository logRepository;

    public ResponseSpecificationImpl(String bodyRootPath, ResponseSpecification defaultSpec, ResponseParserRegistrar rpr,
                                     RestAssuredConfig config, LogRepository logRepository) {
        this(bodyRootPath, defaultSpec, rpr, config, null, logRepository);
    }

    public ResponseSpecificationImpl(String bodyRootPath, ResponseSpecification defaultSpec, ResponseParserRegistrar rpr,
                                     RestAssuredConfig config, Response response, LogRepository logRepository) {
        Validate.notNull(config, "RestAssuredConfig cannot be null");
        this.config = config;
        this.response = response;
        rootPath(bodyRootPath);
        this.rpr = rpr;
        if (defaultSpec != null) {
            spec(defaultSpec);
        }
        this.logRepository = logRepository;
    }

    public ResponseSpecification body(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        throwIllegalStateExceptionIfRootPathIsNotDefined("specify arguments");
        return body("", arguments, matcher, additionalKeyMatcherPairs);
    }

    public Response validate(Response response) {
        assertionClosure.validate(response);
        return response;
    }

    public ResponseSpecification body(Matcher<?> matcher, Matcher<?>... additionalMatchers) {
        notNull(matcher, "matcher");
        validateResponseIfRequired(() -> {
            bodyMatchers.add(newBodyMatcher(null, matcher));
            if (additionalMatchers != null) {
                for (Matcher<?> hamcrestMatcher : additionalMatchers) {
                    bodyMatchers.add(newBodyMatcher(null, hamcrestMatcher));
                }
            }
        });
        return this;
    }

    public ResponseSpecification body(String key, Matcher<?> matcher, Object... additionalKeyMatcherPairs) {
        return body(key, Collections.emptyList(), matcher, additionalKeyMatcherPairs);
    }

    public ResponseSpecification time(Matcher<Long> matcher) {
        return time(matcher, TimeUnit.MILLISECONDS);
    }

    public ResponseSpecification time(Matcher<Long> matcher, TimeUnit timeUnit) {
        notNull(matcher, Matcher.class);
        notNull(timeUnit, TimeUnit.class);
        validateResponseIfRequired(() -> expectedResponseTime = new SimpleEntry<>(matcher, timeUnit));
        return this;
    }

    @SuppressWarnings("unchecked")
    public ResponseSpecification statusCode(Matcher<? super Integer> expectedStatusCode) {
        notNull(expectedStatusCode, "expectedStatusCode");
        validateResponseIfRequired(() -> this.expectedStatusCode = (Matcher<Integer>) expectedStatusCode);
        return this;
    }

    public ResponseSpecification statusCode(int expectedStatusCode) {
        return statusCode(equalTo(expectedStatusCode));
    }

    @SuppressWarnings("unchecked")
    public ResponseSpecification statusLine(Matcher<? super String> expectedStatusLine) {
        notNull(expectedStatusLine, "expectedStatusLine");
        validateResponseIfRequired(() -> this.expectedStatusLine = (Matcher<String>) expectedStatusLine);
        return this;
    }

    public ResponseSpecification headers(Map<String, ?> expectedHeaders) {
        notNull(expectedHeaders, "expectedHeaders");
        validateResponseIfRequired(() -> {
            // The keys need not be Strings, see MapCreator
            for (Map.Entry<?, ?> entry : expectedHeaders.entrySet()) {
                Object headerName = entry.getKey();
                Object matcher = entry.getValue();
                if (matcher instanceof List) {
                    for (Object value : (List<?>) matcher) {
                        headerAssertions.add(newHeaderMatcher(headerName, toMatcherOrEqualTo(value)));
                    }
                } else {
                    headerAssertions.add(newHeaderMatcher(headerName, toMatcherOrEqualTo(matcher)));
                }
            }
        });
        return this;
    }

    public ResponseSpecification headers(String firstExpectedHeaderName, Object firstExpectedHeaderValue, Object... expectedHeaders) {
        notNull(firstExpectedHeaderName, "firstExpectedHeaderName");
        notNull(firstExpectedHeaderValue, "firstExpectedHeaderValue");
        return headers(MapCreator.createMapFromParams(CollisionStrategy.MERGE, firstExpectedHeaderName, firstExpectedHeaderValue, expectedHeaders));
    }

    @Override
    public <T> ResponseSpecification header(String headerName, Function<String, T> mappingFunction, Matcher<? super T> expectedValueMatcher) {
        notNull(headerName, "Header name");
        notNull(mappingFunction, "Mapping function");
        notNull(expectedValueMatcher, "Hamcrest matcher");
        validateResponseIfRequired(() -> {
            HeaderMatcher headerMatcher = newHeaderMatcher(headerName, expectedValueMatcher);
            headerMatcher.setMappingFunction(mappingFunction);
            headerAssertions.add(headerMatcher);
        });
        return this;
    }

    public ResponseSpecification header(String headerName, Matcher<?> expectedValueMatcher) {
        notNull(headerName, "headerName");
        notNull(expectedValueMatcher, "expectedValueMatcher");
        validateResponseIfRequired(() -> headerAssertions.add(newHeaderMatcher(headerName, expectedValueMatcher)));
        return this;
    }

    public ResponseSpecification header(String headerName, String expectedValue) {
        return header(headerName, equalTo(expectedValue));
    }

    public ResponseSpecification header(String headerName, ResponseAwareMatcher<Response> responseAwareMatcher) {
        notNull(headerName, "headerName");
        notNull(responseAwareMatcher, "responseAwareMatcher");
        validateResponseIfRequired(() -> {
            HeaderMatcher headerMatcher = new HeaderMatcher();
            headerMatcher.setHeaderName(headerName);
            headerMatcher.setResponseAwareMatcher(responseAwareMatcher);
            headerAssertions.add(headerMatcher);
        });
        return this;
    }

    public ResponseSpecification cookies(Map<String, ?> expectedCookies) {
        notNull(expectedCookies, "expectedCookies");
        validateResponseIfRequired(() -> {
            // The keys need not be Strings, see MapCreator
            for (Map.Entry<?, ?> entry : expectedCookies.entrySet()) {
                Object cookieName = entry.getKey();
                Object matcher = entry.getValue();
                if (matcher instanceof List) {
                    for (Object value : (List<?>) matcher) {
                        cookieAssertions.add(newCookieMatcher(cookieName, toMatcherOrEqualTo(value)));
                    }
                } else {
                    cookieAssertions.add(newCookieMatcher(cookieName, toMatcherOrEqualTo(matcher)));
                }
            }
        });
        return this;
    }

    public ResponseSpecification cookies(String firstExpectedCookieName, Object firstExpectedCookieValue, Object... expectedCookieNameValuePairs) {
        notNull(firstExpectedCookieName, "firstExpectedCookieName");
        notNull(firstExpectedCookieValue, "firstExpectedCookieValue");
        notNull(expectedCookieNameValuePairs, "expectedCookieNameValuePairs");
        return cookies(MapCreator.createMapFromParams(CollisionStrategy.MERGE, firstExpectedCookieName, firstExpectedCookieValue, expectedCookieNameValuePairs));
    }

    public ResponseSpecification cookie(String cookieName, Matcher<?> expectedValueMatcher) {
        notNull(cookieName, "cookieName");
        notNull(expectedValueMatcher, "expectedValueMatcher");
        validateResponseIfRequired(() -> cookieAssertions.add(newCookieMatcher(cookieName, expectedValueMatcher)));
        return this;
    }

    public ResponseSpecification cookie(String cookieName, DetailedCookieMatcher detailedCookieMatcher) {
        notNull(cookieName, "cookieName");
        notNull(detailedCookieMatcher, "cookieMatcher");
        validateResponseIfRequired(() -> {
            DetailedCookieAssertion detailedCookieAssertion = new DetailedCookieAssertion();
            detailedCookieAssertion.setCookieName(cookieName);
            detailedCookieAssertion.setMatcher(detailedCookieMatcher);
            cookieAssertions.add(detailedCookieAssertion);
        });
        return this;
    }

    public ResponseSpecification cookie(String cookieName) {
        notNull(cookieName, "cookieName");
        return cookie(cookieName, Matchers.<String>anything());
    }

    public ResponseSpecification cookie(String cookieName, Object expectedValue) {
        return cookie(cookieName, equalTo(expectedValue));
    }

    public ResponseSpecification spec(ResponseSpecification responseSpecificationToMerge) {
        if (responseSpecificationToMerge != null && !(responseSpecificationToMerge instanceof ResponseSpecificationImpl)) {
            throw new IllegalArgumentException("Cannot merge a response specification of type " + responseSpecificationToMerge.getClass().getName()
                    + ", it must be of type " + ResponseSpecificationImpl.class.getName() + ".");
        }
        SpecificationMerger.merge(this, (ResponseSpecificationImpl) responseSpecificationToMerge);
        return this;
    }

    public ResponseSpecification specification(ResponseSpecification responseSpecificationToMerge) {
        return spec(responseSpecificationToMerge);
    }

    public ResponseSpecification statusLine(String expectedStatusLine) {
        return statusLine(equalTo(expectedStatusLine));
    }

    public ResponseSpecification body(String key, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
        notNull(key, "key");
        notNull(matcher, "matcher");

        validateResponseIfRequired(() -> {
            bodyMatchers.add(newBodyMatcher(applyArguments(mergeKeyWithRootPath(key), arguments), matcher));
            if (additionalKeyMatcherPairs != null && additionalKeyMatcherPairs.length > 0) {
                // The keys are Strings or, when only arguments are given, lists of arguments
                Map<?, ?> pairs = MapCreator.createMapFromObjects(CollisionStrategy.MERGE, additionalKeyMatcherPairs);
                for (Map.Entry<?, ?> pair : pairs.entrySet()) {
                    Object matchingKey = pair.getKey();
                    Object matchingValue = pair.getValue();
                    String keyWithRoot;
                    Object hamcrestMatcher;
                    if (matchingKey instanceof List) {
                        // If matching key is instance of list (we assume it's a list of arguments) then we should simply return the merged path,
                        // otherwise merge the current path with the supplied key
                        keyWithRoot = applyArguments(mergeKeyWithRootPath(""), toArguments(matchingKey));
                        hamcrestMatcher = matchingValue;
                    } else if (matchingValue instanceof ArgsAndValue) {
                        // A key that is not a String (for example a GString) is used in its string form
                        String mergedPath = mergeKeyWithRootPath(castToString(matchingKey));
                        keyWithRoot = applyArguments(mergedPath, ((ArgsAndValue) matchingValue).getArgs());
                        hamcrestMatcher = ((ArgsAndValue) matchingValue).getValue();
                    } else {
                        keyWithRoot = mergeKeyWithRootPath(castToString(matchingKey));
                        hamcrestMatcher = matchingValue;
                    }

                    if (hamcrestMatcher instanceof List) {
                        for (Object m : (List<?>) hamcrestMatcher) {
                            String keyToUse;
                            Object matcherToUse;
                            if (m instanceof ArgsAndValue) {
                                keyToUse = applyArguments(keyWithRoot, ((ArgsAndValue) m).getArgs());
                                matcherToUse = ((ArgsAndValue) m).getValue();
                            } else {
                                // Plain hamcrest matcher, what happens is that if a user has specified body("x", greaterThan(2), "x", lessThan(10)) then "x" will have a list of these hamcrest matchers
                                keyToUse = keyWithRoot;
                                matcherToUse = m;
                            }
                            bodyMatchers.add(newBodyMatcher(keyToUse, toMatcher(matcherToUse)));
                        }
                    } else {
                        bodyMatchers.add(newBodyMatcher(keyWithRoot, toMatcher(hamcrestMatcher)));
                    }
                }
            }
        });
        return this;
    }

    public ResponseLogSpecification log() {
        return new ResponseLogSpecificationImpl(this, logRepository);
    }

    public ResponseSpecificationImpl logDetail(LogDetail logDetail) {
        this.responseLogDetail = logDetail;
        return this;
    }

    public ResponseSpecification onFailMessage(String message) {
        this.onFailMessage = message;
        return this;
    }

    public LogDetail getLogDetail() {
        return responseLogDetail;
    }

    public RequestSender when() {
        return requestSpecification;
    }

    public ResponseSpecification response() {
        return this;
    }

    public RequestSpecification given() {
        return requestSpecification;
    }

    public ResponseSpecification that() {
        return this;
    }

    public RequestSpecification request() {
        return requestSpecification;
    }

    public ResponseSpecification parser(String contentType, Parser parser) {
        rpr.registerParser(contentType, parser);
        return this;
    }

    public ResponseSpecification and() {
        return this;
    }

    public RequestSpecification with() {
        return given();
    }

    public ResponseSpecification then() {
        return this;
    }

    public ResponseSpecification expect() {
        return this;
    }

    public ResponseSpecification rootPath(String rootPath) {
        return this.rootPath(rootPath, Collections.emptyList());
    }

    public ResponseSpecification noRootPath() {
        return rootPath("");
    }

    public ResponseSpecification appendRootPath(String pathToAppend) {
        return appendRootPath(pathToAppend, Collections.emptyList());
    }

    public ResponseSpecification appendRootPath(String pathToAppend, List<Argument> arguments) {
        notNull(pathToAppend, "Path to append to root path");
        notNull(arguments, "Arguments for path to append");
        String mergedPath = mergeKeyWithRootPath(pathToAppend);
        return rootPath(mergedPath, arguments);
    }

    public ResponseSpecification detachRootPath(String pathToDetach) {
        notNull(pathToDetach, "Path to detach from root path");
        throwIllegalStateExceptionIfRootPathIsNotDefined("detach path");
        pathToDetach = StringUtils.trim(pathToDetach);
        if (!bodyRootPath.endsWith(pathToDetach)) {
            throw new IllegalStateException("Cannot detach path '" + pathToDetach + "' since root path '" + bodyRootPath + "' doesn't end with '" + pathToDetach + "'.");
        }
        bodyRootPath = StringUtils.substringBeforeLast(bodyRootPath, pathToDetach);
        if (bodyRootPath.endsWith(".")) {
            bodyRootPath = bodyRootPath.substring(0, bodyRootPath.length() - 1);
        }
        return this;
    }

    public ResponseSpecification rootPath(String rootPath, List<Argument> arguments) {
        notNull(rootPath, "Root path");
        notNull(arguments, "Arguments");
        this.bodyRootPath = applyArguments(rootPath, arguments);
        return this;
    }

    public ResponseSpecification root(String rootPath, List<Argument> arguments) {
        return this.rootPath(rootPath, arguments);
    }

    public boolean hasBodyAssertionsDefined() {
        return bodyMatchers.containsMatchers();
    }

    public boolean hasAssertionsDefined() {
        return hasBodyAssertionsDefined() || !headerAssertions.isEmpty() ||
                !cookieAssertions.isEmpty() || expectedStatusCode != null || expectedStatusLine != null ||
                contentType != null || expectedResponseTime != null;
    }

    public ResponseSpecification defaultParser(Parser parser) {
        notNull(parser, "Parser");
        rpr.registerDefaultParser(parser);
        return this;
    }

    public ResponseSpecification contentType(ContentType contentType) {
        notNull(contentType, "contentType");
        validateResponseIfRequired(() -> this.contentType = contentType);
        return this;
    }

    public ResponseSpecification contentType(String contentType) {
        notNull(contentType, "contentType");
        validateResponseIfRequired(() -> this.contentType = contentType);
        return this;
    }

    public ResponseSpecification contentType(Matcher<? super String> contentType) {
        notNull(contentType, "contentType");
        validateResponseIfRequired(() -> this.contentType = contentType);
        return this;
    }

    public class HamcrestAssertionClosure {
        private final ResponseSpecification responseSpecification;

        public HamcrestAssertionClosure(ResponseSpecification responseSpecification) {
            this.responseSpecification = responseSpecification;
        }

        public Object call(Object response, Object content) {
            restAssuredResponse.parseResponse((HttpResponseDecorator) response, content, hasBodyAssertionsDefined(), rpr);
            return null;
        }

        public Object call(Object response) {
            return call(response, null);
        }

        /**
         * @return The expected content type (a String, a {@link ContentType} or a Matcher), or {@link ContentType#ANY}
         * if none or an empty String is expected.
         */
        public Object getResponseContentType() {
            boolean noContentType = contentType == null || (contentType instanceof String && ((String) contentType).isEmpty());
            return noContentType ? ANY : contentType;
        }

        private boolean requiresPathParsing() {
            return bodyMatchers.requiresPathParsing();
        }

        /**
         * @return A handler that parses the response into the REST Assured response.
         */
        public HttpResponseHandler getResponseHandler() {
            return this::call;
        }

        public void validate(Response response) {
            if (!hasAssertionsDefined()) {
                return;
            }
            List<Object> validations = new ArrayList<>();
            try {
                validations.addAll(validateStatusCodeAndStatusLine(response));
                validations.addAll(validateHeadersAndCookies(response));
                validations.addAll(validateContentType(response));
                validations.addAll(validateResponseTime(response));
                if (hasBodyAssertionsDefined()) {
                    RestAssuredConfig cfg = config == null ? new RestAssuredConfig() : config;
                    if (requiresPathParsing() && (!isEagerAssert() || contentParser == null)) {
                        contentParser = new ContentParser().parse(response, rpr, cfg, isEagerAssert());
                    }
                    validations.addAll(bodyMatchers.validate(response, contentParser, cfg));
                }
            } catch (Throwable e) {
                fireFailureListeners(response);
                throw e;
            }

            List<String> errorMessages = new ArrayList<>();
            for (Object validation : validations) {
                Map<?, ?> result = (Map<?, ?>) validation;
                if (!Boolean.TRUE.equals(result.get("success"))) {
                    errorMessages.add(format(result.get("errorMessage")));
                }
            }
            int numberOfErrors = errorMessages.size();
            if (numberOfErrors > 0) {
                fireFailureListeners(response);
                String errorMessage = String.join("\n", errorMessages);
                String s = numberOfErrors > 1 ? "s" : "";
                throw new AssertionError(numberOfErrors + " expectation" + s + " failed.\n" + errorMessage + getFormattedOnFailMessage());
            }
        }

        private String getFormattedOnFailMessage() {
            return StringUtils.isEmpty(onFailMessage) ? "" : "\nOn fail message: " + onFailMessage;
        }

        private void fireFailureListeners(Response response) {
            for (ResponseValidationFailureListener listener : config.getFailureConfig().getFailureListeners()) {
                listener.onFailure(requestSpecification, responseSpecification, response);
            }
        }

        private List<Map<String, Object>> validateContentType(Response response) {
            List<Map<String, Object>> errors = new ArrayList<>();
            if (contentType != null) {
                String actualContentType = response.getContentType();
                if (contentType instanceof Matcher) {
                    if (!((Matcher<?>) contentType).matches(actualContentType)) {
                        errors.add(error(String.format("Expected content-type %s doesn't match actual content-type \"%s\".\n", contentType, actualContentType)));
                    }
                } else if (contentType instanceof String) {
                    String normalizedExpectedContentType = normalizeContentType((String) contentType);
                    String normalizedActualContentType = normalizeContentType(actualContentType);
                    if (!StringUtils.startsWithIgnoreCase(normalizedActualContentType, normalizedExpectedContentType)) {
                        errors.add(error(String.format("Expected content-type \"%s\" doesn't match actual content-type \"%s\".\n", contentType, actualContentType)));
                    }
                } else {
                    ContentType expectedContentType = (ContentType) contentType;
                    java.util.regex.Matcher matcher = CONTENT_TYPE_WITHOUT_PARAMETERS.matcher(actualContentType == null ? "" : actualContentType);
                    String contentTypeToMatch;
                    if (matcher.find()) {
                        contentTypeToMatch = matcher.group(1);
                    } else {
                        contentTypeToMatch = actualContentType;
                    }
                    if (ContentType.fromContentType(contentTypeToMatch) != expectedContentType) {
                        errors.add(error(String.format("Expected content-type \"%s\" doesn't match actual content-type \"%s\".\n", expectedContentType.name(), actualContentType)));
                    }
                }
            }
            return errors;
        }

        private String normalizeContentType(String actualContentType) {
            return StringUtils.replaceEach(actualContentType, new String[]{" ", "\t"}, new String[]{"", ""});
        }

        private List<Map<String, Object>> validateStatusCodeAndStatusLine(Response response) {
            List<Map<String, Object>> errors = new ArrayList<>();
            if (expectedStatusCode != null) {
                int actualStatusCode = response.getStatusCode();
                if (!expectedStatusCode.matches(actualStatusCode)) {
                    String errorMessage = new MatcherErrorMessageBuilder<Integer, Matcher<Integer>>("status code")
                            .buildError(actualStatusCode, expectedStatusCode);
                    errors.add(error(errorMessage));
                }
            }

            if (expectedStatusLine != null) {
                String actualStatusLine = response.getStatusLine();
                if (!expectedStatusLine.matches(actualStatusLine)) {
                    String errorMessage = String.format("Expected status line %s doesn't match actual status line \"%s\".\n", expectedStatusLine.toString(), actualStatusLine);
                    errors.add(error(errorMessage));
                }
            }
            return errors;
        }

        private List<Map<String, Object>> validateResponseTime(Response response) {
            List<Map<String, Object>> validations = new ArrayList<>();
            if (expectedResponseTime != null) {
                ResponseTimeMatcher responseTimeMatcher = new ResponseTimeMatcher();
                responseTimeMatcher.setMatcher(expectedResponseTime.getKey());
                responseTimeMatcher.setTimeUnit(expectedResponseTime.getValue());
                validations.add(responseTimeMatcher.validate(response));
            }
            return validations;
        }

        private List<Map<String, Object>> validateHeadersAndCookies(Response response) {
            List<Map<String, Object>> validations = new ArrayList<>();
            for (HeaderMatcher matcher : headerAssertions) {
                validations.add(matcher.validateHeader(response));
            }

            for (Object matcher : cookieAssertions) {
                List<String> headerWithCookieList = response.getHeaders().getValues("Set-Cookie");
                Cookies responseCookies = response.getDetailedCookies();
                if (matcher instanceof DetailedCookieAssertion) {
                    validations.add(((DetailedCookieAssertion) matcher).validateCookies(headerWithCookieList, responseCookies));
                } else {
                    validations.add(((CookieMatcher) matcher).validateCookies(headerWithCookieList, responseCookies));
                }
            }
            return validations;
        }

        private Map<String, Object> error(String errorMessage) {
            Map<String, Object> error = new LinkedHashMap<>(2);
            error.put("success", false);
            error.put("errorMessage", errorMessage);
            return error;
        }
    }

    public Matcher<Integer> getStatusCode() {
        return expectedStatusCode;
    }

    public Matcher<String> getStatusLine() {
        return expectedStatusLine;
    }

    public boolean hasHeaderAssertions() {
        return !headerAssertions.isEmpty();
    }

    public boolean hasCookieAssertions() {
        return !cookieAssertions.isEmpty();
    }

    /**
     * @return The expected content type as a String, or {@link ContentType#ANY} if no content type is expected.
     */
    public String getResponseContentType() {
        // The Groovy version referred to itself here and failed with a StackOverflowError
        return assertionClosure.getResponseContentType().toString();
    }

    public String getRootPath() {
        return bodyRootPath;
    }

    public ResponseParserRegistrar getRpr() {
        return rpr;
    }

    HamcrestAssertionClosure getAssertionClosure() {
        return assertionClosure;
    }

    void setRestAssuredResponse(RestAssuredResponseImpl restAssuredResponse) {
        this.restAssuredResponse = restAssuredResponse;
    }

    public void setRpr(ResponseParserRegistrar rpr) {
        this.rpr = rpr;
    }

    public RestAssuredConfig getConfig() {
        return config;
    }

    public void setConfig(RestAssuredConfig config) {
        this.config = config;
    }

    public LogRepository getLogRepository() {
        return logRepository;
    }

    public void setLogRepository(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    // Accessors for SpecificationMerger and TestSpecificationImpl. The collections are the live ones.

    Object getExpectedContentType() {
        return contentType;
    }

    void setExpectedContentType(Object contentType) {
        this.contentType = contentType;
    }

    void setResponseLogDetail(LogDetail responseLogDetail) {
        this.responseLogDetail = responseLogDetail;
    }

    BodyMatcherGroup getBodyMatchers() {
        return bodyMatchers;
    }

    void setBodyRootPath(String bodyRootPath) {
        this.bodyRootPath = bodyRootPath;
    }

    List<Object> getCookieAssertions() {
        return cookieAssertions;
    }

    List<HeaderMatcher> getHeaderAssertions() {
        return headerAssertions;
    }

    void setExpectedStatusCode(Matcher<Integer> expectedStatusCode) {
        this.expectedStatusCode = expectedStatusCode;
    }

    void setExpectedStatusLine(Matcher<String> expectedStatusLine) {
        this.expectedStatusLine = expectedStatusLine;
    }

    SimpleEntry<Matcher<Long>, TimeUnit> getExpectedResponseTime() {
        return expectedResponseTime;
    }

    void setExpectedResponseTime(SimpleEntry<Matcher<Long>, TimeUnit> expectedResponseTime) {
        this.expectedResponseTime = expectedResponseTime;
    }

    void setRequestSpecification(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    private BodyMatcher newBodyMatcher(Object key, Matcher<?> matcher) {
        BodyMatcher bodyMatcher = new BodyMatcher();
        bodyMatcher.setKey(key);
        bodyMatcher.setMatcher(matcher);
        bodyMatcher.setRpr(rpr);
        return bodyMatcher;
    }

    private static HeaderMatcher newHeaderMatcher(Object headerName, Matcher<?> matcher) {
        HeaderMatcher headerMatcher = new HeaderMatcher();
        headerMatcher.setHeaderName(headerName);
        headerMatcher.setMatcher(matcher);
        return headerMatcher;
    }

    @SuppressWarnings("unchecked")
    private static CookieMatcher newCookieMatcher(Object cookieName, Matcher<?> matcher) {
        CookieMatcher cookieMatcher = new CookieMatcher();
        cookieMatcher.setCookieName(cookieName);
        cookieMatcher.setMatcher((Matcher<String>) matcher);
        return cookieMatcher;
    }

    private static Matcher<?> toMatcherOrEqualTo(Object value) {
        return value instanceof Matcher ? (Matcher<?>) value : equalTo(value);
    }

    /**
     * Fails like Groovy did when it assigned a value that is not a Matcher to {@code BodyMatcher.matcher}.
     */
    private static Matcher<?> toMatcher(Object value) {
        if (value == null || value instanceof Matcher) {
            return (Matcher<?>) value;
        }
        throw new ClassCastException("Cannot cast object '" + value + "' with class '" + value.getClass().getName() + "' to class '" + Matcher.class.getName() + "'");
    }

    @SuppressWarnings("unchecked")
    private static List<Argument> toArguments(Object key) {
        for (Object argument : (List<?>) key) {
            if (!(argument instanceof Argument)) {
                throw new IllegalArgumentException("The path of a body expectation must be a String or a list of " + Argument.class.getName() + ", was '" + format(key) + "'.");
            }
        }
        return (List<Argument>) key;
    }

    private String applyArguments(String path, List<Argument> arguments) {
        if (arguments != null && arguments.size() > 0) {
            int numberArgsOfAfterMerge = StringUtils.countMatches(path, "%s");
            if (numberArgsOfAfterMerge > arguments.size()) {
                arguments = new ArrayList<>(arguments);
                // The condition is evaluated against the growing list, so at most half of the missing arguments are added (kept from the Groovy version)
                for (int i = 0; i < (numberArgsOfAfterMerge - arguments.size()); i++) {
                    arguments.add(new Argument("%s"));
                }
            }
            Object[] args = new Object[arguments.size()];
            for (int i = 0; i < args.length; i++) {
                Object argument = ((List<?>) arguments).get(i);
                if (!(argument instanceof Argument)) {
                    throw new IllegalArgumentException("Path arguments must be instances of " + Argument.class.getName() + ", use withArgs(..) to create them. Was '" + format(arguments) + "'.");
                }
                args[i] = ((Argument) argument).getArgument();
            }
            path = String.format(path, args);
        }
        return path;
    }

    private String mergeKeyWithRootPath(String key) {
        if (bodyRootPath != null && !EMPTY.equals(bodyRootPath)) {
            if (bodyRootPath.endsWith(DOT) && key.startsWith(DOT)) {
                return bodyRootPath + substringAfter(key, DOT);
            } else if (!bodyRootPath.endsWith(DOT) && !key.startsWith(DOT)
                    && !key.startsWith(SAFE_REF) && !key.startsWith("[")
                    && !key.startsWith(SAFE_INDEX) && !key.startsWith(SPREAD_REF)) {
                return bodyRootPath + DOT + key;
            }
            return bodyRootPath + key;
        }
        return key;
    }

    public void throwIllegalStateExceptionIfRootPathIsNotDefined(String description) {
        String rootPath = getRootPath();
        if (rootPath == null || rootPath.isEmpty()) {
            throw new IllegalStateException("Cannot " + description + " when root path is empty");
        }
    }

    /**
     * Forcefully disable eager assert. This is useful for certain language extensions to allow for validation of multiple expectations in one go.
     */
    public Object forceDisableEagerAssert() {
        forceDisableEagerAssert = true;
        return this;
    }

    /**
     * Forcefully validate response expectations. This is useful for certain language extensions to allow for validation of multiple expectations in one go.
     *
     * @return {@code null}
     */
    public Object forceValidateResponse() {
        // We parse the response as a string here because we need to enforce it otherwise we cannot use "extract" after validations are completed
        response.asString();

        assertionClosure.validate(response);
        return null;
    }

    private boolean isEagerAssert() {
        return !forceDisableEagerAssert && response != null;
    }

    private void validateResponseIfRequired(Runnable addExpectation) {
        if (isEagerAssert()) {
            // Reset the body matchers before each validation to avoid testing multiple matchers on each invocation
            bodyMatchers.reset();
        }
        addExpectation.run();
        if (isEagerAssert()) {
            assertionClosure.validate(response);
        }
    }
}

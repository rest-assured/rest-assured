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
package io.restassured.internal.filter;

import io.restassured.authentication.FormAuthConfig;
import io.restassured.config.CsrfConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.filter.session.SessionFilter;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.csrf.CsrfData;
import io.restassured.internal.csrf.CsrfTokenFinder;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.spi.AuthFilter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM;
import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FormAuthFilter implements AuthFilter {

    private static final String FIND_INPUT_TAG_WITH_TYPE = "html.depthFirst().grep { it.name() == 'input' && it.@type == '%s' }.collect { it.@name }";
    private static final String FIND_INPUT_VALUE_OF_INPUT_TAG_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }";
    private static final String COUNT_INPUT_TAGS_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.size()";
    private static final String FIND_FORM_ACTION = "html.depthFirst().grep { it.name() == 'form' }.get(0).@action";
    private static final Set<Integer> REDIRECT_STATUS_CODES = Set.of(301, 302, 303, 307, 308);

    private Object userName;
    private Object password;
    private FormAuthConfig formAuthConfig;
    private SessionConfig sessionConfig;
    private CsrfConfig csrfConfig;

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        // A form action from FormAuthConfig, which is used as it is
        String formAction = null;
        // A form action read from the login page, resolved against the URL of the login page
        URI formActionUri = null;
        String userNameInputField;
        String passwordInputField;
        CsrfData csrfData = null;
        // The cookies set by the login page, and by the responses that redirected to it, by origin
        Map<String, Map<String, String>> cookiesFromLoginPage;
        List<SimpleEntry<String, String>> additionalInputFields = new ArrayList<>();

        if (formAuthConfig == null) {
            formAuthConfig = new FormAuthConfig();
        }

        if (formAuthConfig.requiresParsingOfLoginPage() || csrfConfig.isCsrfEnabled()) {
            LoginPage loginPage = fetchLoginPage(requestSpec, ctx);
            Response loginPageResponse = loginPage.response();
            cookiesFromLoginPage = loginPage.cookiesByOrigin();

            XmlPath html = new XmlPath(HTML, loginPageResponse.asString());

            if (formAuthConfig.hasFormAction()) {
                formAction = formAuthConfig.getFormAction();
            } else {
                String htmlFormAction = throwIfException(() -> html.getString(FIND_FORM_ACTION));
                formActionUri = resolveFormAction(loginPage.uri(), htmlFormAction);
                if (formActionUri == null) {
                    formAction = htmlFormAction;
                }
            }
            userNameInputField = formAuthConfig.hasUserInputTagName() ? formAuthConfig.getUserInputTagName() : throwIfException(() ->
                    html.getString(format(FIND_INPUT_TAG_WITH_TYPE, "text")));
            passwordInputField = formAuthConfig.hasPasswordInputTagName() ? formAuthConfig.getPasswordInputTagName() : throwIfException(() ->
                    html.getString(format(FIND_INPUT_TAG_WITH_TYPE, "password")));

            if (csrfConfig.isCsrfEnabled()) {
                csrfData = CsrfTokenFinder.findInHtml(csrfConfig, loginPageResponse);
            }

            if (formAuthConfig.hasAdditionalInputFieldNames()) {
                for (String name : formAuthConfig.getAdditionalInputFieldNames()) {
                    int numberOfInputFields = throwIfException(() -> html.getInt(format(COUNT_INPUT_TAGS_WITH_NAME, name)));
                    if (numberOfInputFields == 0) {
                        throw new IllegalArgumentException(format("Couldn't find the additional input field \"%s\" on the login page. " +
                                "Check the additional fields specified in FormAuthConfig.", name));
                    }
                    String value = throwIfException(() ->
                            html.getString(format(FIND_INPUT_VALUE_OF_INPUT_TAG_WITH_NAME, name)));
                    additionalInputFields.add(new SimpleEntry<>(name, value));
                }
            }

        } else {
            formAction = formAuthConfig.getFormAction();
            userNameInputField = formAuthConfig.getUserInputTagName();
            passwordInputField = formAuthConfig.getPasswordInputTagName();
            additionalInputFields = null;
            cookiesFromLoginPage = null;
        }

        RequestSpecification loginRequestSpec = given().auth().none().and().disableCsrf().and().formParams(userNameInputField, userName, passwordInputField, password);

        String loginUri;
        String loginOrigin;
        if (formActionUri != null) {
            loginUri = toLoginUri(formActionUri, loginRequestSpec);
            loginOrigin = originOf(formActionUri);
        } else {
            formAction = formAction != null && formAction.startsWith("/") ? formAction : "/" + formAction;
            URI uri = toURI(requestSpec.getURI());
            loginUri = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + formAction;
            loginOrigin = originOf(uri);
        }

        if (cookiesFromLoginPage != null && cookiesFromLoginPage.containsKey(loginOrigin)) {
            loginRequestSpec.cookies(cookiesFromLoginPage.get(loginOrigin));
        }

        if (csrfData != null) {
            if (csrfData.shouldSendTokenAs(FORM)) {
                loginRequestSpec.formParam(csrfData.inputFieldOrHeaderName, csrfData.token);
            } else {
                loginRequestSpec.header(csrfData.inputFieldOrHeaderName, csrfData.token);
            }
        }

        if (formAuthConfig.isLoggingEnabled()) {
            LogConfig logConfig = formAuthConfig.getLogConfig();
            LogDetail logDetail = formAuthConfig.getLogDetail();
            if (logDetail != LogDetail.STATUS) {
                loginRequestSpec.filter(new RequestLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream(), logConfig.shouldUrlEncodeRequestUri(), logConfig.blacklistedHeaders()));
            }

            if (logDetail != LogDetail.PARAMS) {
                loginRequestSpec.filter(new ResponseLoggingFilter(logDetail, logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream()));
            }
        }

        if (additionalInputFields != null) {
            for (SimpleEntry<String, String> field : additionalInputFields) {
                loginRequestSpec.formParam(field.getKey(), field.getValue());
            }
        }

        applySessionFilterFromOriginalRequestIfDefined(requestSpec, loginRequestSpec);
        final Response loggedInResponse = loginRequestSpec.post(loginUri);
        // Don't send the detailed cookies because they contain too many detail (such as Path which is a reserved token)
        requestSpec.cookies(loggedInResponse.cookies());
        return ctx.next(requestSpec, responseSpec);
    }

    public static void applySessionFilterFromOriginalRequestIfDefined(FilterableRequestSpecification requestSpec, RequestSpecification loginRequestSpec) {
        Filter sessionFilterInOriginalRequest = null;
        for (Filter filter : requestSpec.getDefinedFilters()) {
            if (filter instanceof SessionFilter) {
                sessionFilterInOriginalRequest = filter;
                break;
            }
        }
        if (sessionFilterInOriginalRequest != null) {
            loginRequestSpec.noFiltersOfType(SessionFilter.class);
            loginRequestSpec.filter(sessionFilterInOriginalRequest);
            String sessionIdName = requestSpec.getConfig().getSessionConfig().sessionIdName();
            RestAssuredConfig loginConfig = ((QueryableRequestSpecification) loginRequestSpec).getConfig();
            RestAssuredConfig cfg = loginConfig != null ? loginConfig : new RestAssuredConfig();
            loginRequestSpec.config(cfg.sessionConfig(SessionConfig.sessionConfig().sessionIdName(sessionIdName)));
        }
    }

    public Object getUserName() {
        return userName;
    }

    public void setUserName(Object userName) {
        this.userName = userName;
    }

    public Object getPassword() {
        return password;
    }

    public void setPassword(Object password) {
        this.password = password;
    }

    public FormAuthConfig getFormAuthConfig() {
        return formAuthConfig;
    }

    public void setFormAuthConfig(FormAuthConfig formAuthConfig) {
        this.formAuthConfig = formAuthConfig;
    }

    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(SessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    public CsrfConfig getCsrfConfig() {
        return csrfConfig;
    }

    public void setCsrfConfig(CsrfConfig csrfConfig) {
        this.csrfConfig = csrfConfig;
    }

    private static URI toURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    /**
     * Fetches the login page (the page of the request, or the CSRF token page) and follows its redirects by hand, like a browser,
     * to know the URL of the login page.
     */
    private LoginPage fetchLoginPage(FilterableRequestSpecification requestSpec, FilterContext ctx) {
        Response response;
        URI uri;
        if (csrfConfig.isCsrfEnabled()) {
            RequestSpecification csrfPageRequestSpec = given().auth().none().disableCsrf().redirects().follow(false).cookies(requestSpec.getCookies());
            response = csrfPageRequestSpec.get(csrfConfig.getCsrfTokenPath());
            uri = toURI(((FilterableRequestSpecification) csrfPageRequestSpec).getURI());
        } else {
            RequestSpecification loginPageRequestSpec = given().spec(requestSpec).auth().none().redirects().follow(false);
            if (requestSpec instanceof RequestSpecificationImpl) {
                // Send to the path of the request, with its path parameters, and not to the request URI (as ctx.send(..) does),
                // which already has the path parameters applied and is URL encoded
                response = loginPageRequestSpec.request(requestSpec.getMethod(), ((RequestSpecificationImpl) requestSpec).getPath());
            } else {
                response = ctx.send(loginPageRequestSpec);
            }
            uri = toURI(requestSpec.getURI());
        }

        // Like a browser, only send cookies to the origin that set them, and the cookies of the request only to its own origin
        String requestOrigin = originOf(toURI(requestSpec.getURI()));
        Map<String, Map<String, String>> cookiesByOrigin = new HashMap<>();
        cookiesByOrigin.computeIfAbsent(originOf(uri), origin -> new LinkedHashMap<>()).putAll(response.cookies());
        RestAssuredConfig config = requestSpec.getConfig() == null ? new RestAssuredConfig() : requestSpec.getConfig();
        int maxRedirects = config.getRedirectConfig().maxRedirects();
        int numberOfRedirects = 0;
        while (REDIRECT_STATUS_CODES.contains(response.statusCode())) {
            String location = response.getHeader("Location");
            if (location == null) {
                throw new IllegalArgumentException(format("The request for the login page was redirected (%d) without a Location header, " +
                        "so REST Assured couldn't follow the redirect to the login page.", response.statusCode()));
            }
            if (++numberOfRedirects > maxRedirects) {
                throw new IllegalArgumentException(format("The request for the login page was redirected more than %d times (the maximum number of redirects in RedirectConfig).", maxRedirects));
            }
            uri = resolve(uri, location);
            String origin = originOf(uri);
            // The URI is already URL encoded
            RequestSpecification redirectRequestSpec = given().auth().none().disableCsrf().redirects().follow(false).urlEncodingEnabled(false);
            if (origin.equals(requestOrigin)) {
                redirectRequestSpec.cookies(requestSpec.getCookies());
            }
            Map<String, String> cookiesOfOrigin = cookiesByOrigin.computeIfAbsent(origin, it -> new LinkedHashMap<>());
            if (!cookiesOfOrigin.isEmpty()) {
                redirectRequestSpec.cookies(cookiesOfOrigin);
            }
            response = redirectRequestSpec.get(uri.toString());
            cookiesOfOrigin.putAll(response.cookies());
        }
        return new LoginPage(uri, response, cookiesByOrigin);
    }

    /**
     * @return The form action resolved against the URL of the login page, like a browser does (so a form without action is posted
     * to the URL of the login page), or {@code null} if the form action isn't a valid URI
     */
    private static URI resolveFormAction(URI loginPageUri, String formAction) {
        if (StringUtils.isBlank(formAction)) {
            return loginPageUri;
        }
        try {
            return resolve(loginPageUri, formAction.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static URI resolve(URI base, String reference) {
        if (StringUtils.isEmpty(base.getRawPath())) {
            // URI.resolve(..) doesn't add a slash between the authority and a relative path
            base = toURI(base.getScheme() + "://" + base.getRawAuthority() + "/");
        }
        return base.resolve(reference);
    }

    /**
     * @return The URI to post the login form to. The form action is URL encoded, so its path is decoded and its query is added to
     * the request specification as query parameters, so that REST Assured URL encodes them once. A fragment is left out.
     */
    private static String toLoginUri(URI formActionUri, RequestSpecification loginRequestSpec) {
        if (formActionUri.getRawQuery() != null) {
            addQueryParams(loginRequestSpec, formActionUri.getRawQuery());
        }
        return formActionUri.getScheme() + "://" + formActionUri.getRawAuthority() + decodePathIfStructureIsKept(Objects.toString(formActionUri.getRawPath(), ""));
    }

    private static String decodePathIfStructureIsKept(String encodedPath) {
        // A decoded "/", "?" or "#" would change the structure of the URI
        for (String encodedReservedCharacter : new String[]{"%2F", "%3F", "%23"}) {
            if (StringUtils.containsIgnoreCase(encodedPath, encodedReservedCharacter)) {
                return encodedPath;
            }
        }
        String decodedPath;
        try {
            // A "+" in a path is not a space
            decodedPath = URLDecoder.decode(encodedPath.replace("+", "%2B"), UTF_8);
        } catch (IllegalArgumentException e) {
            return encodedPath;
        }
        // A decoded "{" or "}" would be taken as a path parameter placeholder
        return StringUtils.containsAny(decodedPath, '{', '}') ? encodedPath : decodedPath;
    }

    private static void addQueryParams(RequestSpecification requestSpec, String encodedQuery) {
        for (String nameAndValue : StringUtils.split(encodedQuery, '&')) {
            String name = urlDecode(StringUtils.substringBefore(nameAndValue, "="));
            if (nameAndValue.contains("=")) {
                requestSpec.queryParam(name, urlDecode(StringUtils.substringAfter(nameAndValue, "=")));
            } else {
                requestSpec.queryParam(name);
            }
        }
    }

    private static String urlDecode(String value) {
        try {
            // Like a browser, "+" is decoded as a space
            return URLDecoder.decode(value, UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private static String originOf(URI uri) {
        String scheme = StringUtils.lowerCase(uri.getScheme());
        int port = uri.getPort() != -1 ? uri.getPort() : "https".equals(scheme) ? 443 : 80;
        return scheme + "://" + StringUtils.lowerCase(uri.getHost()) + ":" + port;
    }

    private record LoginPage(URI uri, Response response, Map<String, Map<String, String>> cookiesByOrigin) {
    }

    private static <T> T throwIfException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse login page. Check for errors on the login page or specify FormAuthConfig.", e);
        }
    }
}

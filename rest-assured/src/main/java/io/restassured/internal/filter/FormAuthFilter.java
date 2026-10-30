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
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.internal.csrf.CsrfData;
import io.restassured.internal.csrf.CsrfTokenFinder;
import io.restassured.internal.http.exchange.Redirects;
import io.restassured.internal.support.HtmlDocument;
import io.restassured.internal.support.HtmlDocument.Element;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.spi.AuthFilter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class FormAuthFilter implements AuthFilter {

    private Object userName;
    private Object password;
    private FormAuthConfig formAuthConfig;
    private SessionConfig sessionConfig;
    private CsrfConfig csrfConfig;

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        // A form action from FormAuthConfig (or one that isn't a valid URI), which is used as it is
        String formAction = null;
        // A form action read from the login page, resolved against the URL of the login page
        URI formActionUri = null;
        String userNameInputField;
        String passwordInputField;
        CsrfData csrfData = null;
        Map<String, String> cookiesFromLoginPage;
        List<SimpleEntry<String, String>> additionalInputFields = new ArrayList<>();

        if (formAuthConfig == null) {
            formAuthConfig = new FormAuthConfig();
        }

        if (formAuthConfig.requiresParsingOfLoginPage() || csrfConfig.isCsrfEnabled()) {
            Response loginPageResponse;
            URI loginPageUri;
            if (csrfConfig.isCsrfEnabled()) {
                RequestSpecification csrfPageRequestSpec = requestWithSettingsOf(requestSpec).cookies(requestSpec.getCookies());
                loginPageResponse = csrfPageRequestSpec.get(csrfConfig.getCsrfTokenPath());
                loginPageUri = urlOf(loginPageResponse, ((FilterableRequestSpecification) csrfPageRequestSpec).getURI());
                cookiesFromLoginPage = loginPageResponse.cookies();
            } else {
                // Send to the path of the request, with its path parameters, and not to the request URI (as ctx.send(..) does),
                // which already has the path parameters applied and is URL encoded
                RequestSpecificationImpl request = (RequestSpecificationImpl) requestSpec;
                loginPageResponse = request.newRequestOfClient().spec(requestSpec).auth().none().request(requestSpec.getMethod(), request.getPath());
                loginPageUri = urlOf(loginPageResponse, requestSpec.getURI());
                cookiesFromLoginPage = loginPageResponse.cookies();
                if (loginPageResponse.statusCode() == 302) {
                    // This means that Rest Assured has not done a redirect automatically.
                    // This may happen if status code is 302 and method is not GET (see https://blog.jayway.com/2012/10/17/what-you-may-not-know-about-http-redirects/).
                    // Thus we follow the Location header explicitly.
                    String location = loginPageResponse.getHeader("Location");
                    if (location == null) {
                        throw new IllegalArgumentException("The request for the login page was redirected (302) without a Location header, " +
                                "so REST Assured couldn't follow the redirect to the login page.");
                    }
                    URI redirectedLoginPageUri = Redirects.resolve(loginPageUri, location);
                    RequestSpecification redirectedLoginPageRequestSpec = requestWithSettingsOf(requestSpec);
                    // The cookies belong to the host that set them, so they aren't sent to another host, as a browser wouldn't
                    if (isSameHost(loginPageUri, redirectedLoginPageUri)) {
                        redirectedLoginPageRequestSpec.cookies(cookiesFromLoginPage);
                    }
                    loginPageResponse = redirectedLoginPageRequestSpec.get(redirectedLoginPageUri);
                    loginPageUri = urlOf(loginPageResponse, ((FilterableRequestSpecification) redirectedLoginPageRequestSpec).getURI());
                }
            }

            HtmlDocument html = HtmlDocument.parse(loginPageResponse.asString());

            if (formAuthConfig.hasFormAction()) {
                formAction = formAuthConfig.getFormAction();
            } else {
                String htmlFormAction = throwIfException(() -> html.elements("form").findFirst()
                        .orElseThrow(() -> new IllegalStateException("The login page has no form.")).attribute("action"));
                formActionUri = resolveFormAction(loginPageUri, findBaseHref(html), htmlFormAction);
                if (formActionUri == null) {
                    formAction = htmlFormAction;
                }
            }
            userNameInputField = formAuthConfig.hasUserInputTagName() ? formAuthConfig.getUserInputTagName() : throwIfException(() ->
                    nameOfFirstInputOfType(html, "text"));
            passwordInputField = formAuthConfig.hasPasswordInputTagName() ? formAuthConfig.getPasswordInputTagName() : throwIfException(() ->
                    nameOfFirstInputOfType(html, "password"));

            if (csrfConfig.isCsrfEnabled()) {
                csrfData = CsrfTokenFinder.findInHtml(csrfConfig, html);
            }

            if (formAuthConfig.hasAdditionalInputFieldNames()) {
                for (String name : formAuthConfig.getAdditionalInputFieldNames()) {
                    Element input = html.inputsNamed(name).findFirst().orElseThrow(() -> new IllegalArgumentException(format(
                            "Couldn't find the additional input field \"%s\" on the login page. Check the additional fields specified in FormAuthConfig.", name)));
                    additionalInputFields.add(new SimpleEntry<>(name, input.attribute("value")));
                }
            }

        } else {
            formAction = formAuthConfig.getFormAction();
            userNameInputField = formAuthConfig.getUserInputTagName();
            passwordInputField = formAuthConfig.getPasswordInputTagName();
            additionalInputFields = null;
            cookiesFromLoginPage = null;
        }

        RequestSpecification loginRequestSpec = requestWithSettingsOf(requestSpec).formParams(userNameInputField, userName, passwordInputField, password);

        URI uri = toURI(requestSpec.getURI());
        String origin = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        String loginUri;
        if (formActionUri != null) {
            // The login form is always posted to the origin of the request, also when the form action is on another origin
            loginUri = origin + toPathAndQueryParams(formActionUri, loginRequestSpec);
        } else {
            formAction = formAction != null && formAction.startsWith("/") ? formAction : "/" + formAction;
            loginUri = origin + formAction;
        }

        if (cookiesFromLoginPage != null) {
            loginRequestSpec.cookies(cookiesFromLoginPage);
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

    /**
     * @return A request with the defaults of the client that sends the original request, which is sent like the original
     * request (with its config, so with its SSL settings and timeouts, through its proxy, and to its base URI, port and
     * base path) but carries nothing of it: no authentication, CSRF handling, filters, headers, parameters or body.
     * Unlike {@code given().spec(requestSpec)}, which copies all of those too.
     */
    static RequestSpecification requestWithSettingsOf(FilterableRequestSpecification requestSpec) {
        RequestSpecificationImpl request = (RequestSpecificationImpl) requestSpec;
        RequestSpecification spec = request.newRequestOfClient().auth().none().disableCsrf()
                .baseUri(request.getBaseUri()).port(request.getRequestPort()).basePath(request.getBasePath());
        RestAssuredConfig config = requestSpec.getConfig();
        if (config != null) {
            spec.config(config);
        }
        ProxySpecification proxy = requestSpec.getProxySpecification();
        if (proxy != null) {
            spec.proxy(proxy);
        }
        return spec;
    }

    /**
     * Cookies are scoped to the host that set them, not to its scheme or port (RFC 6265, section 5.1.3 and 8.5)
     */
    private static boolean isSameHost(URI uri, URI other) {
        return uri.getHost() != null && uri.getHost().equalsIgnoreCase(other.getHost());
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
     * @return The URL of the response, after the redirects that REST Assured followed, or <code>requestUri</code> if
     * the response doesn't know one
     */
    private static URI urlOf(Response response, String requestUri) {
        if (response instanceof RestAssuredResponseImpl) {
            URI effectiveUri = ((RestAssuredResponseImpl) response).getEffectiveUri();
            if (effectiveUri != null) {
                return effectiveUri;
            }
        }
        return toURI(requestUri);
    }

    private static String findBaseHref(HtmlDocument html) {
        return html.elements("base").map(base -> base.attribute("href")).filter(StringUtils::isNotBlank).findFirst().orElse(null);
    }

    /**
     * @return The name of the first input of the type on the login page, in document order
     */
    private static String nameOfFirstInputOfType(HtmlDocument html, String type) {
        return html.inputsOfType(type).findFirst()
                .orElseThrow(() -> new IllegalStateException("The login page has no input of type " + type + ".")).attribute("name");
    }

    /**
     * @return The form action resolved against the URL of the login page (or the URL of its base element), like a browser does,
     * so a form without action is posted to the URL of the login page, or {@code null} if the form action isn't a valid URI
     */
    private static URI resolveFormAction(URI loginPageUri, String baseHref, String formAction) {
        if (StringUtils.isBlank(formAction)) {
            return loginPageUri;
        }
        URI baseUri = loginPageUri;
        if (baseHref != null) {
            try {
                baseUri = Redirects.resolve(loginPageUri, baseHref.trim());
            } catch (IllegalArgumentException e) {
                // An invalid base URL is ignored
            }
        }
        try {
            return Redirects.resolve(baseUri, formAction.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return The path to post the login form to. The form action is URL encoded, so its path is decoded and its query is added to
     * the request specification as query parameters, so that REST Assured URL encodes them once. A fragment is left out.
     */
    private static String toPathAndQueryParams(URI formActionUri, RequestSpecification loginRequestSpec) {
        if (formActionUri.getRawQuery() != null) {
            addQueryParams(loginRequestSpec, formActionUri.getRawQuery());
        }
        return decodePathIfStructureIsKept(Objects.toString(formActionUri.getRawPath(), ""));
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
            // "+" is decoded as a space, as servlet containers do
            return URLDecoder.decode(value, UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private static <T> T throwIfException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse login page. Check for errors on the login page or specify FormAuthConfig.", e);
        }
    }
}

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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM;
import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML;
import static java.lang.String.format;

public class FormAuthFilter implements AuthFilter {

    private static final String FIND_INPUT_TAG_WITH_TYPE = "html.depthFirst().grep { it.name() == 'input' && it.@type == '%s' }.collect { it.@name }";
    private static final String FIND_INPUT_VALUE_OF_INPUT_TAG_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }";
    private static final String FIND_FORM_ACTION = "html.depthFirst().grep { it.name() == 'form' }.get(0).@action";

    private Object userName;
    private Object password;
    private FormAuthConfig formAuthConfig;
    private SessionConfig sessionConfig;
    private CsrfConfig csrfConfig;

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        String formAction;
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
            if (csrfConfig.isCsrfEnabled()) {
                loginPageResponse = given().auth().none().disableCsrf().cookies(requestSpec.getCookies()).get(csrfConfig.getCsrfTokenPath());
                cookiesFromLoginPage = loginPageResponse.cookies();
            } else {
                FilterableRequestSpecification loginPageRequestSpec = (FilterableRequestSpecification) given().spec(requestSpec).auth().none();
                // ctx.send(..) sends to the request URI, where the path parameters are already applied
                for (String pathParamName : loginPageRequestSpec.getPathParams().keySet()) {
                    loginPageRequestSpec.removePathParam(pathParamName);
                }
                loginPageResponse = ctx.send(loginPageRequestSpec);
                cookiesFromLoginPage = loginPageResponse.cookies();
                if (loginPageResponse.statusCode() == 302) {
                    // This means that Rest Assured has not done a redirect automatically.
                    // This may happen if status code is 302 and method is not GET (see https://blog.jayway.com/2012/10/17/what-you-may-not-know-about-http-redirects/).
                    // Thus we follow the Location header explicitly.
                    loginPageResponse = given().auth().none().cookies(cookiesFromLoginPage).get(loginPageResponse.getHeader("Location"));
                }
            }

            XmlPath html = new XmlPath(HTML, loginPageResponse.asString());

            if (formAuthConfig.hasFormAction()) {
                formAction = formAuthConfig.getFormAction();
            } else {
                String tempFormAction = throwIfException(() -> html.getString(FIND_FORM_ACTION));
                formAction = tempFormAction.startsWith("/") ? tempFormAction : "/" + tempFormAction;
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

        formAction = formAction != null && formAction.startsWith("/") ? formAction : "/" + formAction;

        RequestSpecification loginRequestSpec = given().auth().none().and().disableCsrf().and().formParams(userNameInputField, userName, passwordInputField, password);

        URI uri = toURI(requestSpec.getURI());
        String loginUri = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + formAction;

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

    public static void applySessionFilterFromOriginalRequestIfDefined(FilterableRequestSpecification requestSpec, RequestSpecification loginRequestSpec) {
        Filter sessionFilterInOriginalRequest = null;
        for (Filter filter : requestSpec.getDefinedFilters()) {
            if (filter != null && filter.getClass().isAssignableFrom(SessionFilter.class)) {
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

    private static <T> T throwIfException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse login page. Check for errors on the login page or specify FormAuthConfig.", e);
        }
    }
}

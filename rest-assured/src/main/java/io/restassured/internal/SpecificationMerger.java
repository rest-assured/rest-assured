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

import io.restassured.authentication.ExplicitNoAuthScheme;
import io.restassured.config.Config;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.Filter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.spi.AuthFilter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.ADD_CSRF_FILTER;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.ALLOW_CONTENT_TYPE;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.AUTHENTICATION_SCHEME;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.BASE_PATH;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.BODY;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.BASE_URI;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.PORT;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.PROXY;
import static io.restassured.internal.RequestSpecificationImpl.MergeableValue.URL_ENCODING_ENABLED;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

public class SpecificationMerger {

    /**
     * Merge this builder with settings from another specification. Note that the supplied specification
     * can overwrite data in the current specification. The following settings are overwritten:
     * <ul>
     *     <li>Content type</li>
     *     <li>Root path</
     *     <li>Status code</li>
     *     <li>Status line</li>
     *     <li>Fallback parser</li>
     *     <li>Expected response time</li>
     * </ul>
     * The following settings are merged:
     * <ul>
     *     <li>Response body expectations</li>
     *     <li>Cookies</li>
     *     <li>Headers</li>
     *     <li>Response parser settings</li>
     * </ul>
     */
    public static void merge(ResponseSpecificationImpl thisOne, ResponseSpecificationImpl with) {
        notNull(thisOne, "Specification to merge");
        notNull(with, "Specification to merge with");

        thisOne.setExpectedContentType(with.getExpectedContentType());
        thisOne.getRpr().setDefaultParser(with.getRpr().getDefaultParser());
        thisOne.getRpr().getAdditional().putAll(with.getRpr().getAdditional());
        thisOne.setResponseLogDetail(with.getLogDetail());
        thisOne.getBodyMatchers().add(with.getBodyMatchers());
        thisOne.setBodyRootPath(with.getRootPath());
        thisOne.getCookieAssertions().addAll(with.getCookieAssertions());
        thisOne.setExpectedStatusCode(with.getStatusCode());
        thisOne.setExpectedStatusLine(with.getStatusLine());
        thisOne.setExpectedResponseTime(with.getExpectedResponseTime());
        thisOne.getHeaderAssertions().addAll(with.getHeaderAssertions());
    }

    /**
     * Merges <code>with</code> into <code>thisOne</code>, see {@link io.restassured.specification.RequestSpecification#spec(io.restassured.specification.RequestSpecification)}
     * for what is overwritten and what is merged.
     */
    public static void merge(RequestSpecificationImpl thisOne, RequestSpecificationImpl with) {
        notNull(thisOne, "Specification to merge");
        notNull(with, "Specification to merge with");

        if (with.isExplicitlySet(PORT)) {
            thisOne.setRequestPort(with.getRequestPort());
        }
        if (with.isExplicitlySet(BASE_URI)) {
            thisOne.setBaseUri(with.getBaseUri());
        }
        if (with.isExplicitlySet(BASE_PATH)) {
            thisOne.setBasePath(with.getBasePath());
        }
        thisOne.getRequestParameters().putAll(with.getRequestParameters());
        thisOne.getQueryParameters().putAll(with.getQueryParams());
        thisOne.getFormParameters().putAll(with.getFormParams());
        // Includes the unnamed path parameters of "with" that have a placeholder
        thisOne.getNamedPathParameters().putAll(with.getPathParams());
        thisOne.getMultiParts().addAll(with.getMultiParts());
        if (with.isExplicitlySet(AUTHENTICATION_SCHEME)) {
            thisOne.setAuthenticationScheme(with.getAuthenticationScheme());
        }
        mergeSessionId(thisOne, with);
        thisOne.cookies(with.getCookies());
        if (with.isExplicitlySet(BODY)) {
            thisOne.setRequestBody(with.getBody());
        }
        mergeFilters(thisOne, with);
        if (with.isExplicitlySet(URL_ENCODING_ENABLED)) {
            thisOne.urlEncodingEnabled(with.isUrlEncodingEnabled());
        }
        if (with.isExplicitlySet(ALLOW_CONTENT_TYPE)) {
            thisOne.setAllowContentType(with.isAllowContentType());
        }
        if (with.isExplicitlySet(ADD_CSRF_FILTER)) {
            thisOne.setAddCsrfFilter(with.isAddCsrfFilter());
        }
        if (with.isExplicitlySet(PROXY)) {
            thisOne.setProxySpecification(with.getProxySpecification());
        }
        if (with.getMethod() != null) {
            thisOne.setMethod(with.getMethod());
        }
        // The unnamed path parameters belong to the path
        if (StringUtils.isNotEmpty(with.getPath())) {
            thisOne.setUnnamedPathParamsTuples(new ArrayList<>(with.getUnnamedPathParamsTuples()));
            thisOne.setPath(with.getPath());
        }

        mergeConfig(thisOne, with);
        // It's important that headers are merged after the configs are merged since HeaderConfig affects that way headers are merged.
        thisOne.headers(with.getHeaders());
    }

    private static void mergeConfig(RequestSpecificationImpl thisOne, RequestSpecificationImpl other) {
        RestAssuredConfig thisConfig = thisOne.restAssuredConfig();
        RestAssuredConfig otherConfig = other.restAssuredConfig();
        boolean thisIsUserConfigured = thisConfig.isUserConfigured();
        boolean otherIsUserConfigured = otherConfig.isUserConfigured();
        if (thisIsUserConfigured && otherIsUserConfigured) {
            // Use the config of the other specification for each config type that it has configured
            RestAssuredConfig newConfig = new RestAssuredConfig(
                    pick(thisConfig.getRedirectConfig(), otherConfig.getRedirectConfig()),
                    pick(thisConfig.getHttpClientConfig(), otherConfig.getHttpClientConfig()),
                    pick(thisConfig.getLogConfig(), otherConfig.getLogConfig()),
                    pick(thisConfig.getEncoderConfig(), otherConfig.getEncoderConfig()),
                    pick(thisConfig.getDecoderConfig(), otherConfig.getDecoderConfig()),
                    pick(thisConfig.getSessionConfig(), otherConfig.getSessionConfig()),
                    pick(thisConfig.getObjectMapperConfig(), otherConfig.getObjectMapperConfig()),
                    pick(thisConfig.getConnectionConfig(), otherConfig.getConnectionConfig()),
                    pick(thisConfig.getJsonConfig(), otherConfig.getJsonConfig()),
                    pick(thisConfig.getXmlConfig(), otherConfig.getXmlConfig()),
                    pick(thisConfig.getSSLConfig(), otherConfig.getSSLConfig()),
                    pick(thisConfig.getMatcherConfig(), otherConfig.getMatcherConfig()),
                    pick(thisConfig.getHeaderConfig(), otherConfig.getHeaderConfig()),
                    pick(thisConfig.getMultiPartConfig(), otherConfig.getMultiPartConfig()),
                    pick(thisConfig.getParamConfig(), otherConfig.getParamConfig()),
                    pick(thisConfig.getOAuthConfig(), otherConfig.getOAuthConfig()),
                    pick(thisConfig.getFailureConfig(), otherConfig.getFailureConfig()),
                    pick(thisConfig.getCsrfConfig(), otherConfig.getCsrfConfig()));
            thisOne.setRestAssuredConfig(newConfig);
        } else if (!thisIsUserConfigured && otherIsUserConfigured) {
            thisOne.setRestAssuredConfig(otherConfig);
        }
    }

    private static <T extends Config> T pick(T thisConfig, T otherConfig) {
        return otherConfig.isUserConfigured() ? otherConfig : thisConfig;
    }

    private static void mergeSessionId(RequestSpecificationImpl thisOne, RequestSpecificationImpl with) {
        RestAssuredConfig thisOneConfig = thisOne.getConfig();
        Cookies thisOneCookies = thisOne.getCookies();

        RestAssuredConfig otherConfig = with.getConfig();
        Cookies otherCookies = with.getCookies();

        String oldSessionIdName = SessionConfig.DEFAULT_SESSION_ID_NAME;
        if (thisOneConfig != null) {
            oldSessionIdName = thisOneConfig.getSessionConfig().sessionIdName();
        }

        boolean shouldRemoveSessionFromThis;
        if (otherConfig == null) {
            shouldRemoveSessionFromThis = otherCookies.hasCookieWithName(oldSessionIdName);
        } else {
            String otherSessionIdName = otherConfig.getSessionConfig().sessionIdName();
            shouldRemoveSessionFromThis = otherCookies.hasCookieWithName(otherSessionIdName);
        }

        if (shouldRemoveSessionFromThis) {
            List<Cookie> cookieList = new ArrayList<>();
            for (Cookie cookie : thisOneCookies) {
                if (!cookie.getName().equalsIgnoreCase(oldSessionIdName)) {
                    cookieList.add(cookie);
                }
            }
            thisOne.setCookies(new Cookies(cookieList));
        }
    }

    private static void mergeFilters(RequestSpecificationImpl thisOne, RequestSpecificationImpl with) {
        List<Filter> thisFilters = thisOne.getFilters();
        List<Filter> withFilters = with.getFilters();

        // Overwrite auth filters
        if ((containsAuthFilter(thisFilters) && containsAuthFilter(withFilters)) || with.getAuthenticationScheme() instanceof ExplicitNoAuthScheme) {
            thisFilters.removeIf(filter -> filter instanceof AuthFilter);
        }
        // Only add filters not already present
        List<Filter> toAdd = new ArrayList<>();
        for (Filter filter : withFilters) {
            if (!thisFilters.contains(filter)) {
                toAdd.add(filter);
            }
        }
        thisFilters.addAll(toAdd);
    }

    private static boolean containsAuthFilter(List<Filter> filters) {
        return filters.stream().anyMatch(filter -> filter instanceof AuthFilter);
    }
}

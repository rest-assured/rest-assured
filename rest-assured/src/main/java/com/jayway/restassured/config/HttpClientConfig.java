/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.config;

import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;
import static java.util.Arrays.asList;

/**
 * Configure the Apache HTTP Client parameters.
 * <p>Note that you can't configure the redirect settings from this config. Please use {@link RedirectConfig} for this purpose.</p>
 * <p/>
 * The following parameters are applied per default:
 * <table border=1>
 * <tr>
 * <th>Parameter name</th><th>Parameter value</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link ClientPNames#COOKIE_POLICY}</td><td>{@link CookiePolicy#IGNORE_COOKIES}</td><td>Don't automatically set response cookies in subsequent requests.</td>
 * </tr>
 * <tr>
 * <td>{@link CookieSpecPNames#DATE_PATTERNS}</td><td>[EEE, dd-MMM-yyyy HH:mm:ss z, EEE, dd MMM yyyy HH:mm:ss z]</td><td>Defines valid date patterns to be used for parsing non-standard
 * <code>expires</code> attribute.</td>
 * <p/>
 * </tr>
 * </table>
 * <p>
 * You can also specify a http client factory that is used to create the http client instances that REST Assured uses ({@link #httpClientFactory(com.jayway.restassured.config.HttpClientConfig.HttpClientFactory)}).
 * By default the {@link DefaultHttpClient} is used
 * </p>
 *
 * @see org.apache.http.client.params.ClientPNames
 * @see org.apache.http.client.params.CookiePolicy
 * @see org.apache.http.params.CoreProtocolPNames
 */
public class HttpClientConfig {

    private final Map<String, ?> httpClientParams;
    private final HttpMultipartMode httpMultipartMode;
    private final HttpClientFactory httpClientFactory;

    /**
     * Creates a new  HttpClientConfig instance with the <code>{@value ClientPNames#COOKIE_POLICY}</code> parameter set to <code>{@value CookiePolicy#IGNORE_COOKIES}</code>.
     */
    public HttpClientConfig() {
        this.httpClientFactory = defaultHttpClientFactory();


        this.httpClientParams = new HashMap<String, Object>() {
            {
                put(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
                put(CookieSpecPNames.DATE_PATTERNS, asList("EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss z"));
            }
        };
        this.httpMultipartMode = HttpMultipartMode.STRICT;
    }

    private HttpClientConfig(HttpClientFactory httpClientFactory, Map<String, ?> httpClientParams, HttpMultipartMode httpMultipartMode) {
        notNull(httpClientParams, "httpClientParams");
        notNull(httpMultipartMode, "httpMultipartMode");
        notNull(httpClientFactory, "Http Client factory");
        this.httpClientFactory = httpClientFactory;
        this.httpClientParams = new HashMap<String, Object>(httpClientParams);
        this.httpMultipartMode = httpMultipartMode;
    }

    /**
     * Creates a new  HttpClientConfig instance with the parameters defined by the <code>httpClientParams</code>.
     */
    public HttpClientConfig(Map<String, ?> httpClientParams) {
        this(defaultHttpClientFactory(), httpClientParams, HttpMultipartMode.STRICT);
    }

    /**
     * @return The configured parameters
     */
    public Map<String, ?> params() {
        return Collections.unmodifiableMap(httpClientParams);
    }

    /**
     * @return The same HttpClientConfig instance. Only here for syntactic sugar.
     */
    public HttpClientConfig and() {
        return this;
    }

    /**
     * Set a http client parameter.
     *
     * @param parameterName  The name of the parameter
     * @param parameterValue The value of the parameter (may be null)
     * @param <T>            The parameter type
     * @return An updated HttpClientConfig
     */
    public <T> HttpClientConfig setParam(String parameterName, T parameterValue) {
        notNull(parameterName, "Parameter name");
        final Map<String, Object> newParams = new HashMap<String, Object>(httpClientParams);
        newParams.put(parameterName, parameterValue);
        return new HttpClientConfig(newParams);
    }

    /**
     * Replaces the currently configured parameters with the ones supplied by <code>httpClientParams</code>. This method is the same as {@link #setParams(java.util.Map)}.
     *
     * @param httpClientParams The parameters to set.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig withParams(Map<String, ?> httpClientParams) {
        return new HttpClientConfig(httpClientParams);
    }

    /**
     * Replaces the currently configured parameters with the ones supplied by <code>httpClientParams</code>. This method is the same as {@link #withParams(java.util.Map)}.
     *
     * @param httpClientParams The parameters to set.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig setParams(Map<String, ?> httpClientParams) {
        return withParams(httpClientParams);
    }

    /**
     * Add the given parameters to an already configured number of parameters.
     *
     * @param httpClientParams The parameters.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig addParams(Map<String, ?> httpClientParams) {
        notNull(httpClientParams, "httpClientParams");
        final Map<String, Object> newParams = new HashMap<String, Object>(httpClientParams);
        newParams.putAll(httpClientParams);
        return new HttpClientConfig(newParams);
    }

    /**
     * Set the http client factory that Rest Assured should use when making request. For each request REST Assured will invoke the factory to get the a the HttpClient instance.
     *
     * @param httpClientFactory The http client factory to use.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig httpClientFactory(HttpClientFactory httpClientFactory) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode);
    }

    /**
     * @return The configured http client instance created by the factory that will be used when making a request.
     */
    public AbstractHttpClient httpClientInstance() {
        try {
            return httpClientFactory.createHttpClient();
        } catch (Exception e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    /**
     * Specify the HTTP Multipart mode when sending multi-part data.
     *
     * @param httpMultipartMode The multi-part mode to set.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig httpMultipartMode(HttpMultipartMode httpMultipartMode) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode);
    }

    /**
     * @return A static way to create a new HttpClientConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static HttpClientConfig httpClientConfig() {
        return new HttpClientConfig();
    }

    /**
     * @return The http multi-part mode.
     */
    public HttpMultipartMode httpMultipartMode() {
        return httpMultipartMode;
    }

    private static HttpClientFactory defaultHttpClientFactory() {
        return new HttpClientFactory() {
            @Override
            public AbstractHttpClient createHttpClient() {
                return new DefaultHttpClient();
            }
        };
    }

    public static abstract class HttpClientFactory {
        public abstract AbstractHttpClient createHttpClient() throws Exception;
    }
}
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

package io.restassured.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
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
 * You can also specify a http client factory that is used to create the http client instances that REST Assured uses ({@link #httpClientFactory(HttpClientConfig.HttpClientFactory)}).
 * By default the {@link DefaultHttpClient} is used. It's also possible to specify whether or not this instance should be reused in multiple requests. By default the http client instance is not reused.
 * </p>
 *
 * @see org.apache.http.client.params.ClientPNames
 * @see org.apache.http.client.params.CookiePolicy
 * @see org.apache.http.params.CoreProtocolPNames
 */
    public class HttpClientConfig implements Config {

    private static final boolean SHOULD_REUSE_HTTP_CLIENT_INSTANCE_BY_DEFAULT = false;
    private static final HttpClient NO_HTTP_CLIENT = null;

    private final boolean shouldReuseHttpClientInstance;
    private final Map<String, ?> httpClientParams;
    private final HttpMultipartMode httpMultipartMode;
    private final HttpClientFactory httpClientFactory;
    private final boolean isUserConfigured;
    private volatile HttpClient httpClient;

    /**
     * Creates a new  HttpClientConfig instance with the <code>{@value org.apache.http.client.params.ClientPNames#COOKIE_POLICY}</code> parameter set to <code>{@value org.apache.http.client.params.CookiePolicy#IGNORE_COOKIES}</code>.
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
        this.shouldReuseHttpClientInstance = SHOULD_REUSE_HTTP_CLIENT_INSTANCE_BY_DEFAULT;
        this.httpClient = null;
        this.isUserConfigured = false;
    }

    private HttpClientConfig(HttpClientFactory httpClientFactory, Map<String, ?> httpClientParams, HttpMultipartMode httpMultipartMode,
                             boolean shouldReuseHttpClientInstance, HttpClient abstractHttpClient, boolean isUserConfigured) {
        notNull(httpClientParams, "httpClientParams");
        notNull(httpMultipartMode, "httpMultipartMode");
        notNull(httpClientFactory, "Http Client factory");
        this.shouldReuseHttpClientInstance = shouldReuseHttpClientInstance;
        this.httpClientFactory = httpClientFactory;
        this.httpClientParams = new HashMap<String, Object>(httpClientParams);
        this.httpMultipartMode = httpMultipartMode;
        this.httpClient = abstractHttpClient;
        this.isUserConfigured = isUserConfigured;
    }

    /**
     * Creates a new  HttpClientConfig instance with the parameters defined by the <code>httpClientParams</code>.
     */
    public HttpClientConfig(Map<String, ?> httpClientParams) {
        this(defaultHttpClientFactory(), httpClientParams, HttpMultipartMode.STRICT, SHOULD_REUSE_HTTP_CLIENT_INSTANCE_BY_DEFAULT, NO_HTTP_CLIENT, true);
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
     * Instruct REST Assured to reuse the configured http client instance for multiple requests. By default REST Assured
     * will create a new {@link org.apache.http.client.HttpClient} instance for each request. Note that for this to work
     * the configuration must be defined statically, for example:
     * <p/>
     * <pre>
     * RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());
     * </pre>
     *
     * @return An updated HttpClientConfig
     * @see #httpClientFactory(HttpClientConfig.HttpClientFactory)
     */
    public HttpClientConfig reuseHttpClientInstance() {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, true, httpClient, true);
    }

    /**
     * Instruct REST Assured <i>not</i> to reuse the configured http client instance for multiple requests. This is the default behavior.
     *
     * @return An updated HttpClientConfig
     * @see #reuseHttpClientInstance()
     */
    public HttpClientConfig dontReuseHttpClientInstance() {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, false, NO_HTTP_CLIENT, true);
    }

    /**
     * If this method returns <code>true</code> then REST Assured will reuse the same {@link org.apache.http.client.HttpClient} instance created
     * by the {@link #httpClientInstance()} method for all requests. If <code>false</code> is returned then REST Assured creates a new instance for each request.
     * <p>
     * By default <code>false</code> is returned.
     * </p>
     * Note that for this to work the configuration must be defined statically, for example:
     * <pre>
     * RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());
     * </pre>
     *
     * @return <code>true</code> if the same HTTP Client instance should be reused between several requests, <code>false</code> otherwise.
     */
    public boolean isConfiguredToReuseTheSameHttpClientInstance() {
        return shouldReuseHttpClientInstance;
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
        return new HttpClientConfig(httpClientFactory, newParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true);
    }

    /**
     * Replaces the currently configured parameters with the ones supplied by <code>httpClientParams</code>. This method is the same as {@link #setParams(java.util.Map)}.
     *
     * @param httpClientParams The parameters to set.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig withParams(Map<String, ?> httpClientParams) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true);
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
        final Map<String, Object> newParams = new HashMap<String, Object>(this.httpClientParams);
        newParams.putAll(httpClientParams);
        return new HttpClientConfig(httpClientFactory, newParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true);
    }

    /**
     * Set the http client factory that Rest Assured should use when making request. For each request REST Assured will invoke the factory to get the a the HttpClient instance.
     *
     * @param httpClientFactory The http client factory to use.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig httpClientFactory(HttpClientFactory httpClientFactory) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, NO_HTTP_CLIENT, true);
    }

    /**
     * @return The configured http client that will create an {@link org.apache.http.client.HttpClient} instances that's used by REST Assured when making a request.
     */
    public HttpClient httpClientInstance() {
        if (isConfiguredToReuseTheSameHttpClientInstance()) {
            if (httpClient == NO_HTTP_CLIENT) {
                httpClient = httpClientFactory.createHttpClient();
            }
            return httpClient;
        }
        return httpClientFactory.createHttpClient();
    }

    /**
     * Specify the HTTP Multipart mode when sending multi-part data.
     *
     * @param httpMultipartMode The multi-part mode to set.
     * @return An updated HttpClientConfig
     */
    public HttpClientConfig httpMultipartMode(HttpMultipartMode httpMultipartMode) {
        return new HttpClientConfig(httpClientFactory, httpClientParams, httpMultipartMode, shouldReuseHttpClientInstance, httpClient, true);
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
            public HttpClient createHttpClient() {
                return new DefaultHttpClient();
            }
        };
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    /**
     * A factory for creating and configuring a custom http client instance that will be used by REST Assured.
     */
    public interface HttpClientFactory {
        /**
         * Create an instance of {@link HttpClient} that'll be used by REST Assured when making requests. By default
         * REST Assured creates a {@link DefaultHttpClient}.
         * <p>
         * <b>Important: Version 1.9.0 of REST Assured ONLY supports instances of {@link org.apache.http.impl.client.AbstractHttpClient}</b>. The API is
         * how ever prepared for future upgrades.
         * </p>
         *
         * @return An instance of {@link HttpClient}.
         */
        HttpClient createHttpClient();
    }
}
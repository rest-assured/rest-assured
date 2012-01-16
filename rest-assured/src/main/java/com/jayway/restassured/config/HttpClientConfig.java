/*
 * Copyright 2011 the original author or authors.
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

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * Configure the Apache HTTP Client parameters.
 * <p>Note that you can't configure the redirect settings from this config. Please use {@link RedirectConfig} for this purpose.</p>
 *
 * The following parameters are applied per default:
 * <table border=1>
 *     <tr>
 *         <th>Parameter name</th><th>Parameter value</th><th>Description</th>
 *     </tr>
 *     <tr>
 *         <td>{@link ClientPNames#COOKIE_POLICY}</td><td>{@link CookiePolicy#IGNORE_COOKIES}</td><td>Don't automatically set response cookies in subsequent requests</td>
 *     </tr>
 * </table>
 *
 * @see org.apache.http.client.params.ClientPNames
 * @see org.apache.http.client.params.CookiePolicy
 * @see org.apache.http.params.CoreProtocolPNames
 */
public class HttpClientConfig {

    private final Map<String, ?> httpClientParams;

    /**
     * Creates a new  HttpClientConfig instance with the <code>{@value ClientPNames#COOKIE_POLICY}</code> parameter set to <code>{@value CookiePolicy#IGNORE_COOKIES}</code>.
     */
    public HttpClientConfig() {
        this.httpClientParams = new HashMap<String, Object>() {
            {
                put(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
            }
        };
    }

    /**
     * Creates a new  HttpClientConfig instance with the parameters defined by the <code>httpClientParams</code>.
     */
    public HttpClientConfig(Map<String, ?> httpClientParams) {
        notNull(httpClientParams, "httpClientParams");
        this.httpClientParams = new HashMap<String, Object>(httpClientParams);
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
     * @param parameterName The name of the parameter
     * @param parameterValue The value of the parameter (may be null)
     * @param <T> The parameter type
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
     * @return A static way to create a new HttpClientConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static HttpClientConfig httpClientConfig() {
        return new HttpClientConfig();
    }
}
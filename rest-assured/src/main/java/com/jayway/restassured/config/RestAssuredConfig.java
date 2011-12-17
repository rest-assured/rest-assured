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

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * Main configuration for REST Assured that allows you to configure advanced redirection setting and HTTP Client parameters.
 * <p>
 * Usage example:
 * <pre>
 *  RestAssured.config = config().redirect(redirectConfig().followRedirects(false));
 * </pre>
 * </p>
 */
public class RestAssuredConfig {

    private final RedirectConfig redirectConfig;
    private final HttpClientConfig httpClientConfig;

    /**
     * Create a new RestAssuredConfiguration with the default {@link RedirectConfig} and a default {@link HttpClientConfig}.
     */
    public RestAssuredConfig() {
        this(new RedirectConfig(), new HttpClientConfig());
    }

    /**
     * Create a new RestAssuredConfiguration with the supplied {@link RedirectConfig} and {@link HttpClientConfig}.
     */
    public RestAssuredConfig(RedirectConfig redirectConfig, HttpClientConfig httpClientConfig) {
        notNull(redirectConfig, "Redirect Config");
        notNull(httpClientConfig, "HTTP Client Config");
        this.httpClientConfig = httpClientConfig;
        this.redirectConfig = redirectConfig;
    }

    /**
     * Set the redirect config.
     *
     * @param redirectConfig The {@link RedirectConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig redirect(RedirectConfig redirectConfig) {
        notNull(redirectConfig, "Redirect config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig);
    }

    /**
     * Set the HTTP Client config.
     *
     * @param httpClientConfig The {@link HttpClientConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig httpClient(HttpClientConfig httpClientConfig) {
        notNull(httpClientConfig, "HTTP Client Config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredConfig set() {
        return this;
    }

    /**
     * @return The RedirectConfig
     */
    public RedirectConfig getRedirectConfig() {
        return redirectConfig;
    }

    /**
     * @return The HttpClientConfig
     */
    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    /**
     * @return A static way to create a new RestAssuredConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static RestAssuredConfig newConfig() {
        return new RestAssuredConfig();
    }

    /**
     * @return A static way to create a new RestAssuredConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static RestAssuredConfig config() {
        return new RestAssuredConfig();
    }
}

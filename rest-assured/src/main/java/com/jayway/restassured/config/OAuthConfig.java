/*
 * Copyright 2016 the original author or authors.
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

/**
 * Let's you configure OAuth specific configuration
 */
public class OAuthConfig implements Config {

    private final boolean useEmptyOAuthToken;
    private final boolean isUserConfigured;

    /**
     * Create a new OAuthConfig that uses an OAuthToken
     */
    public OAuthConfig() {
        this(false, false);
    }

    private OAuthConfig(boolean useEmptyOAuthToken, boolean isUserConfigured) {
        this.useEmptyOAuthToken = useEmptyOAuthToken;
        this.isUserConfigured = isUserConfigured;
    }

    /**
     * Configure whether or not to use an empty oauth token for OAuth1
     *
     * @param useEmptyOAuthToken Whether or not to use an empty oauth token
     * @return A new instance of {@link OAuthConfig}
     */
    public OAuthConfig useEmptyOAuthToken(boolean useEmptyOAuthToken) {
        return new OAuthConfig(useEmptyOAuthToken, true);
    }

    /**
     * Returns whether or not an empty oauth token should be used for oauth1.
     *
     * @return A new instance of {@link OAuthConfig}
     */
    public boolean shouldUseEmptyOAuthToken() {
        return useEmptyOAuthToken;
    }


    /**
     * @return Whether or not this config is user configured.
     */
    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    /**
     * @return A static way to create a new OAuthConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static OAuthConfig oauthConfig() {
        return new OAuthConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same OAuthConfig instance.
     */
    public OAuthConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same OAuthConfig instance.
     */
    public OAuthConfig with() {
        return this;
    }
}

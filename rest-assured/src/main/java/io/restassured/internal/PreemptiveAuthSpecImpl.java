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

import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.authentication.PreemptiveOAuth2HeaderScheme;
import io.restassured.specification.PreemptiveAuthSpec;
import io.restassured.specification.RequestSpecification;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Specify a preemptive authentication scheme to use when sending a request.
 */
public class PreemptiveAuthSpecImpl implements PreemptiveAuthSpec {
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private final RequestSpecification requestBuilder;

    public PreemptiveAuthSpecImpl(RequestSpecification requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    /**
     * Use http basic authentication.
     *
     * @param username The user name.
     * @param password The password.
     * @return The request builder
     */
    @Override
    public RequestSpecification basic(String username, String password) {
        notNull(username, "userName");
        notNull(password, "password");

        PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
        scheme.setUserName(username);
        scheme.setPassword(password);
        removePreviousAuth().header(AUTHORIZATION_HEADER_NAME, scheme.generateAuthToken());
        return requestBuilder;
    }

    @Override
    public RequestSpecification oauth2(String accessToken) {
        notNull(accessToken, "accessToken");

        PreemptiveOAuth2HeaderScheme scheme = new PreemptiveOAuth2HeaderScheme();
        scheme.setAccessToken(accessToken);
        return removePreviousAuth().header(AUTHORIZATION_HEADER_NAME, scheme.generateAuthToken());
    }

    // Disable auth added by static configuration then specify the Authorization header
    private RequestSpecification removePreviousAuth() {
        return requestBuilder.auth().none();
    }
}

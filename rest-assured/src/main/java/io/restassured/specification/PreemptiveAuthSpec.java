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

package io.restassured.specification;

/**
 * Specify a preemptive authentication scheme to use when sending a request.
 */
public interface PreemptiveAuthSpec {
    /**
     * Use preemptive http basic authentication. This means that the authentication details are sent in the request
     * header regardless if the server has challenged for authentication or not.
     *
     * @param username The username.
     * @param password The password.
     * @return The Request specification
     */
    RequestSpecification basic(String username, String password);

    /**
     * OAuth2 sign the request. Note that this currently does not wait for a WWW-Authenticate challenge before sending the the OAuth header.
     * This assumes you've already generated an accessToken for the site you're targeting. The access token will be put in a header.
     *
     * @param accessToken The access token
     * @return The request io.restassured.specification
     */
    RequestSpecification oauth2(String accessToken);
}

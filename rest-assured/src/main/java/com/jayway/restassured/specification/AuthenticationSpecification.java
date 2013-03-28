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

package com.jayway.restassured.specification;

import com.jayway.restassured.authentication.FormAuthConfig;

/**
 * Specify an authentication scheme to use when sending a request.
 */
public interface AuthenticationSpecification {
    /**
     * Use http basic authentication.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The Request specification
     */
    RequestSpecification basic(String userName, String password);

    /**
     * Use http digest authentication.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The Request specification
     */
    RequestSpecification digest(String userName, String password);

    /**
     * Use form authentication. Rest Assured will try to parse the response
     * login page and determine and try find the action, username and password input
     * field automatically.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The Request specification
     */
    RequestSpecification form(String userName, String password);

    /**
     * Use form authentication with the supplied configuration.
     *
     * @param userName The user name.
     * @param password The password.
     * @param config The form authentication config
     * @return The authentication scheme
     */
    RequestSpecification form(String userName, String password, FormAuthConfig config);

    /**
     * Sets a certificate to be used for SSL authentication. See {@link Class#getResource(String)}
     * for how to get a URL from a resource on the classpath.
     *
     * @param certURL URL to a JKS keystore where the certificate is stored.
     * @param password  password to decrypt the keystore
     * @return Request specification
     */
    RequestSpecification certificate(String certURL, String password);

    /**
     * Excerpt from the HttpBuilder docs:<br>
     * OAuth sign the request. Note that this currently does not wait for a WWW-Authenticate challenge before sending the the OAuth header.
     * All requests to all domains will be signed for this instance.
     * This assumes you've already generated an accessToken and secretToken for the site you're targeting.
     * For More information on how to achieve this, see the <a href="http://code.google.com/p/oauth-signpost/wiki/GettingStarted#Using_Signpost">Signpost documentation</a>.
     *
     * @param consumerKey
     * @param consumerSecret
     * @param accessToken
     * @param secretToken
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken);

    /**
     * Returns the preemptive authentication view. This means that the authentication details are sent in the request
     * header regardless if the server has challenged for authentication or not.
     *
     * @return The preemptive authentication specification.
     */
    PreemptiveAuthSpec preemptive();

    /**
     * Explicitly state that you don't which to use any authentication in this request. This is useful only in cases where you've
     * specified a default authentication scheme and you wish to override it for a single request.
     * @return The Request specification
     */
    RequestSpecification none();
}

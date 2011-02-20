/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.jayway.restassured.internal

import com.jayway.restassured.authentication.BasicAuthScheme
import com.jayway.restassured.authentication.CertAuthScheme
import com.jayway.restassured.authentication.ExplicitNoAuthScheme
import com.jayway.restassured.authentication.OAuthScheme
import com.jayway.restassured.specification.AuthenticationSpecification
import com.jayway.restassured.specification.RequestSpecification
import static com.jayway.restassured.assertion.AssertParameter.notNull

/**
 * Specify an authentication scheme to use when sending a request.
 */
class AuthenticationSpecificationImpl implements AuthenticationSpecification {
  private RequestSpecification requestBuilder;

  AuthenticationSpecificationImpl(RequestSpecification requestBuilder) {
    this.requestBuilder = requestBuilder
  }

  /**
   * Use http basic authentication.
   *
   * @param userName The user name.
   * @param password The password.
   * @return The request builder
   */
  def RequestSpecification basic(String userName, String password) {
    notNull userName, "userName"
    notNull password, "password"

    requestBuilder.authenticationScheme = new BasicAuthScheme(userName: userName, password: password)
    return requestBuilder
  }

  /**
   * Use http digest authentication.
   *
   * @param userName The user name.
   * @param password The password.
   * @return The request builder
   */
  def RequestSpecification digest(String userName, String password) {
    notNull userName, "userName"
    notNull password, "password"

    requestBuilder.authenticationScheme = new BasicAuthScheme(userName: userName, password: password)
    return requestBuilder
  }

  /**
   * Sets a certificate to be used for SSL authentication. See {@link java.lang.Class#getResource(String)}
   * for how to get a URL from a resource on the classpath.
   *
   * @param certURL URL to a JKS keystore where the certificate is stored.
   * @param password  password to decrypt the keystore
   * @return The request com.jayway.restassured.specification
   */
  def RequestSpecification certificate(String certURL, String password) {
    notNull certURL, "certURL"
    notNull password, "password"

    requestBuilder.authenticationScheme = new CertAuthScheme(certURL: certURL, password: password)
    return requestBuilder
  }

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
  def RequestSpecification oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
    notNull consumerKey, "consumerKey"
    notNull consumerSecret, "consumerSecret"
    notNull accessToken, "accessToken"
    notNull secretToken, "secretToken"

    requestBuilder.authenticationScheme = new OAuthScheme(consumerKey: consumerKey, consumerSecret: consumerSecret, accessToken: accessToken, secretToken: secretToken)
    return requestBuilder
  }

  def RequestSpecification none() {
    requestBuilder.authenticationScheme = new ExplicitNoAuthScheme();
    return requestBuilder
  }
}

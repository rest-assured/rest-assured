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

package com.jayway.restassured.specification

import com.jayway.restassured.authentication.BasicAuthScheme
import com.jayway.restassured.authentication.CertAuthScheme
import com.jayway.restassured.authentication.OAuthScheme
import com.jayway.restassured.internal.RequestSpecificationImpl

class AuthenticationSpecification {
  private RequestSpecification requestBuilder;

  AuthenticationSpecification(RequestSpecification requestBuilder) {
    this.requestBuilder = requestBuilder
  }

  def RequestSpecification basic(String userName, String password) {
    requestBuilder.authenticationScheme = new BasicAuthScheme(userName: userName, password: password)
    return requestBuilder
  }

  def RequestSpecification certificate(String certURL, String password) {
    requestBuilder.authenticationScheme = new CertAuthScheme(certURL: certURL, password: password)
    return requestBuilder
  }

  def RequestSpecification oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
    requestBuilder.authenticationScheme = new OAuthScheme(consumerKey: consumerKey, consumerSecret: consumerSecret, accessToken: accessToken, secretToken: secretToken)
    return requestBuilder
  }
}

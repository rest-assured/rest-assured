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



package com.jayway.restassured.internal

import com.jayway.restassured.specification.PreemptiveAuthSpec
import com.jayway.restassured.specification.RequestSpecification
import static com.jayway.restassured.assertion.AssertParameter.notNull
import com.jayway.restassured.authentication.PreemptiveBasicAuthScheme

/**
 * Specify a preemptive authentication scheme to use when sending a request.
 */
class PreemptiveAuthSpecImpl implements PreemptiveAuthSpec {
  private static final String AUTHORIZATION_HEADER_NAME = "Authorization"
  private RequestSpecification requestBuilder;

  PreemptiveAuthSpecImpl(RequestSpecification requestBuilder) {
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

    requestBuilder.header(AUTHORIZATION_HEADER_NAME, new PreemptiveBasicAuthScheme(userName: userName, password: password).generateAuthToken())
    return requestBuilder
  }
}

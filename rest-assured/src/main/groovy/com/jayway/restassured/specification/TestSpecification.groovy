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

import com.jayway.restassured.assertion.AssertParameter

/**
 * A test specification contains a {@link ResponseSpecification} and a {@link RequestSpecification}. It's
 * mainly used when you have long specifications, e.g.
 * <pre>
 * RequestSpecification requestSpecification = with().parameters("firstName", "John", "lastName", "Doe");
 * ResponseSpecification responseSpecification = expect().body("greeting", equalTo("Greetings John Doe"));
 *  given(requestSpecification, responseSpecification).get("/greet");
 * </pre>
 *
 * This will perform a GET request to "/greet" and verify it according to the <code>responseSpecification</code>.
 */
class TestSpecification implements RequestSender {
  def final RequestSpecification requestSpecification
  def final ResponseSpecification responseSpecification


  TestSpecification(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
    AssertParameter.notNull requestSpecification, "requestSpecification"
    AssertParameter.notNull responseSpecification, "responseSpecification"

    this.requestSpecification = requestSpecification
    this.responseSpecification = responseSpecification;
    responseSpecification.requestSpecification = requestSpecification
    requestSpecification.responseSpecification = responseSpecification
  }

  /**
   * {@inheritDoc}
   */
  void get(String path) {
    requestSpecification.get path
  }

  /**
   * {@inheritDoc}
   */
  void post(String path) {
    requestSpecification.post path
  }

  /**
   * {@inheritDoc}
   */
  void put(String path) {
    requestSpecification.put path
  }

  /**
   * {@inheritDoc}
   */
  void delete(String path) {
    requestSpecification.delete path
  }

  /**
   * {@inheritDoc}
   */
  void head(String path) {
    requestSpecification.head path
  }
}

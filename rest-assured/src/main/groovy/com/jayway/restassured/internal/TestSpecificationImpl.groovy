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



package com.jayway.restassured.internal;


import com.jayway.restassured.assertion.AssertParameter
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.RequestSender
import com.jayway.restassured.specification.RequestSpecification
import com.jayway.restassured.specification.ResponseSpecification

/**
 * A test com.jayway.restassured.specification contains a {@link ResponseSpecification} and a {@link RequestSpecification}. It's
 * mainly used when you have long specifications, e.g.
 * <pre>
 * RequestSpecification requestSpecification = with().parameters("firstName", "John", "lastName", "Doe");
 * ResponseSpecification responseSpecification = expect().body("greeting", equalTo("Greetings John Doe"));
 *  given(requestSpecification, responseSpecification).get("/greet");
 * </pre>
 *
 * This will perform a GET request to "/greet" and verify it according to the <code>responseSpecification</code>.
 */
class TestSpecificationImpl implements RequestSender {
  def final RequestSpecification requestSpecification
  def final ResponseSpecification responseSpecification


  TestSpecificationImpl(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
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
  Response get(String path, Object...pathParams) {
    requestSpecification.get path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response post(String path, Object...pathParams) {
    requestSpecification.post path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response put(String path, Object...pathParams) {
    requestSpecification.put path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response delete(String path, Object...pathParams) {
    requestSpecification.delete path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response head(String path, Object...pathParams) {
    requestSpecification.head path, pathParams
  }

  Response get(String path, Map pathParams) {
    requestSpecification.get path, pathParams
  }

  Response post(String path, Map pathParams) {
    requestSpecification.post path, pathParams
  }

  Response put(String path, Map pathParams) {
    requestSpecification.put path, pathParams
  }

  Response delete(String path, Map pathParams) {
    requestSpecification.delete path, pathParams
  }

  Response head(String path, Map pathParams) {
    requestSpecification.head path, pathParams
  }

  def RequestSpecification getRequestSpecification() {
    requestSpecification
  }

  def ResponseSpecification getResponseSpecification() {
    responseSpecification
  }
}
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


package io.restassured.internal

import io.restassured.http.Method
import io.restassured.internal.common.assertion.AssertParameter
import io.restassured.response.Response
import io.restassured.specification.RequestSender
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification

import static AssertParameter.notNull

/**
 * A test io.restassured.specification contains a {@link ResponseSpecification} and a {@link RequestSpecification}. It's
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
  Response get(String path, Object... pathParams) {
    requestSpecification.get path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response post(String path, Object... pathParams) {
    requestSpecification.post path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response put(String path, Object... pathParams) {
    requestSpecification.put path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response delete(String path, Object... pathParams) {
    requestSpecification.delete path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response head(String path, Object... pathParams) {
    requestSpecification.head path, pathParams
  }

  /**
   * {@inheritDoc}
   */
  Response patch(String path, Object... pathParams) {
    requestSpecification.patch path, pathParams
  }

  Response options(String path, Object... pathParams) {
    requestSpecification.options path, pathParams
  }


  Response get(URI uri) {
    get(notNull(uri, "URI").toString())
  }


  Response post(URI uri) {
    post(notNull(uri, "URI").toString())
  }


  Response put(URI uri) {
    put(notNull(uri, "URI").toString())
  }


  Response delete(URI uri) {
    delete(notNull(uri, "URI").toString())
  }


  Response head(URI uri) {
    head(notNull(uri, "URI").toString())
  }


  Response patch(URI uri) {
    patch(notNull(uri, "URI").toString())
  }


  Response options(URI uri) {
    options(notNull(uri, "URI").toString())
  }

  def Response get(URL url) {
    get(notNull(url, "URL").toString())
  }

  def Response post(URL url) {
    post(notNull(url, "URL").toString())
  }

  def Response put(URL url) {
    put(notNull(url, "URL").toString())
  }

  def Response delete(URL url) {
    delete(notNull(url, "URL").toString())
  }

  def Response head(URL url) {
    head(notNull(url, "URL").toString())
  }

  def Response patch(URL url) {
    patch(notNull(url, "URL").toString())
  }

  def Response options(URL url) {
    options(notNull(url, "URL").toString())
  }

  def Response get() {
    get("")
  }

  def Response post() {
    post("")
  }

  def Response put() {
    put("")
  }

  def Response delete() {
    delete("")
  }

  def Response head() {
    head("")
  }

  def Response patch() {
    patch("")
  }

  def Response options() {
    options("")
  }

  Response request(Method method) {
    request(notNull(method, Method.class).toString())
  }

  Response request(String method) {
    request(method, "")
  }

  Response request(Method method, String path, Object... pathParams) {
    request(notNull(method, Method.class).toString(), path, pathParams)
  }

  Response request(String method, String path, Object... pathParams) {
    requestSpecification.request(method, path, pathParams)
  }

  Response request(Method method, URI uri) {
    requestSpecification.request(method, uri)
  }

  Response request(Method method, URL url) {
    requestSpecification.request(method, url)
  }

  Response request(String method, URI uri) {
    requestSpecification.request(method, uri)
  }

  Response request(String method, URL url) {
    requestSpecification.request(method, url)
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

  Response patch(String path, Map pathParams) {
    requestSpecification.patch path, pathParams
  }

  Response options(String path, Map pathParams) {
    requestSpecification.options path, pathParams
  }

  def RequestSpecification getRequestSpecification() {
    requestSpecification
  }

  def ResponseSpecification getResponseSpecification() {
    responseSpecification
  }
}
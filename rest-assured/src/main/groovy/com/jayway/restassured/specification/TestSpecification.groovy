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

/**
 *
 */
class TestSpecification implements RequestSender {
  def final RequestSpecification requestSpecification
  def final ResponseSpecification responseSpecification


  TestSpecification(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
    this.requestSpecification = requestSpecification
    this.responseSpecification = responseSpecification;
    responseSpecification.requestSpecification = requestSpecification
    requestSpecification.responseSpecification = responseSpecification
  }

  void get(String path) {
    requestSpecification.get path
  }

  void post(String path) {
    requestSpecification.post path
  }

  void put(String path) {
    requestSpecification.put path
  }

  void delete(String path) {
    requestSpecification.delete path
  }

  void head(String path) {
    requestSpecification.head path
  }
}

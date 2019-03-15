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

import io.restassured.specification.RedirectSpecification
import io.restassured.specification.RequestSpecification

import static org.apache.http.client.params.ClientPNames.*

class RedirectSpecificationImpl implements RedirectSpecification {
  private final RequestSpecification requestSpecification;
  private final Map httpClientParams

  def RedirectSpecificationImpl(RequestSpecification requestSpecification, Map httpClientParams) {
    this.requestSpecification = requestSpecification
    this.httpClientParams = httpClientParams
  }

  def RequestSpecification max(int maxNumberOfRedirect) {
    httpClientParams.put(MAX_REDIRECTS, maxNumberOfRedirect)
    requestSpecification
  }

  def RequestSpecification follow(boolean followRedirects) {
    httpClientParams.put(HANDLE_REDIRECTS, followRedirects)
    requestSpecification
  }

  def RequestSpecification allowCircular(boolean allowCircularRedirects) {
    httpClientParams.put(ALLOW_CIRCULAR_REDIRECTS, allowCircularRedirects)
    requestSpecification
  }

  def RequestSpecification rejectRelative(boolean rejectRelativeRedirects) {
    httpClientParams.put(REJECT_RELATIVE_REDIRECT, rejectRelativeRedirects)
    requestSpecification
  }
}

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

import com.jayway.restassured.filter.log.LogDetail
import com.jayway.restassured.filter.log.RequestLoggingFilter
import com.jayway.restassured.specification.RequestLogSpecification
import com.jayway.restassured.specification.RequestSpecification

class RequestLogSpecificationImpl extends LogSpecificationImpl implements RequestLogSpecification {
  private RequestSpecification requestSpecification

  RequestSpecification params() {
    logWith(LogDetail.PARAMS)
  }

  RequestSpecification parameters() {
    logWith(LogDetail.PARAMS)
  }

  RequestSpecification body() {
    body(shouldPrettyPrint(requestSpecification))
  }

  RequestSpecification body(boolean shouldPrettyPrint) {
    logWith(LogDetail.BODY, shouldPrettyPrint)
  }

  RequestSpecification all(boolean shouldPrettyPrint) {
    logWith(LogDetail.ALL, shouldPrettyPrint)
  }

  RequestSpecification everything(boolean shouldPrettyPrint) {
    all(shouldPrettyPrint)
  }

  RequestSpecification all() {
    all(shouldPrettyPrint(requestSpecification))
  }

  RequestSpecification everything() {
    all()
  }

  RequestSpecification headers() {
    logWith(LogDetail.HEADERS)
  }

  RequestSpecification cookies() {
    logWith(LogDetail.COOKIES)
  }

  private def logWith(LogDetail logDetail) {
    logWith(logDetail, shouldPrettyPrint(requestSpecification))
  }

  private def logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
    requestSpecification.filter(new RequestLoggingFilter(logDetail, getPrintStream(requestSpecification)))
    requestSpecification
  }
}

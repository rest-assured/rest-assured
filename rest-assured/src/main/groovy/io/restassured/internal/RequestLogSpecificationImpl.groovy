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

import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.internal.log.LogRepository
import io.restassured.specification.RequestLogSpecification
import io.restassured.specification.RequestSpecification

class RequestLogSpecificationImpl extends LogSpecificationImpl implements RequestLogSpecification {
  def RequestSpecification requestSpecification
  def LogRepository logRepository

  RequestSpecification params() {
    logWith(LogDetail.PARAMS)
  }

  RequestSpecification parameters() {
    logWith(LogDetail.PARAMS)
  }

  RequestSpecification uri() {
    logWith(LogDetail.URI)
  }

  RequestSpecification method() {
    logWith(LogDetail.METHOD)
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

  RequestSpecification ifValidationFails() {
    ifValidationFails(LogDetail.ALL)
  }


  RequestSpecification ifValidationFails(LogDetail logDetail) {
    ifValidationFails(logDetail, shouldPrettyPrint(requestSpecification));
  }

  RequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    logRepository.registerRequestLog(baos);
    logWith(logDetail, shouldPrettyPrint, ps)
  }

  private def logWith(LogDetail logDetail) {
    logWith(logDetail, shouldPrettyPrint(requestSpecification))
  }

  private def logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
    return logWith(logDetail, prettyPrintingEnabled, getPrintStream(requestSpecification));
  }

  private def logWith(LogDetail logDetail, boolean prettyPrintingEnabled, PrintStream printStream) {
    requestSpecification.filter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri(requestSpecification)))
    requestSpecification
  }
}

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
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.internal.log.LogRepository
import io.restassured.specification.ResponseLogSpecification
import io.restassured.specification.ResponseSpecification
import org.hamcrest.Matcher

import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.greaterThanOrEqualTo

class ResponseLogSpecificationImpl extends LogSpecificationImpl implements ResponseLogSpecification {
  def ResponseSpecification responseSpecification
  def LogRepository logRepository

  ResponseSpecification body() {
    body(shouldPrettyPrint())
  }

  ResponseSpecification body(boolean shouldPrettyPrint) {
    return logWith(LogDetail.BODY, shouldPrettyPrint)
  }

  ResponseSpecification all() {
    all(shouldPrettyPrint())
  }

  ResponseSpecification all(boolean shouldPrettyPrint) {
    return logWith(LogDetail.ALL, shouldPrettyPrint)
  }

  ResponseSpecification everything() {
    all()
  }

  ResponseSpecification everything(boolean shouldPrettyPrint) {
    all(shouldPrettyPrint);
  }

  ResponseSpecification headers() {
    logWith(LogDetail.HEADERS)
  }

  ResponseSpecification cookies() {
    logWith(LogDetail.COOKIES)
  }

  ResponseSpecification ifValidationFails() {
    ifValidationFails(LogDetail.ALL)
  }

  ResponseSpecification ifValidationFails(LogDetail logDetail) {
    ifValidationFails(logDetail, shouldPrettyPrint())
  }

  ResponseSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    logRepository.registerResponseLog(baos);
    logWith(logDetail, shouldPrettyPrint, ps)
  }

  ResponseSpecification status() {
    logWith(LogDetail.STATUS)
  }

  ResponseSpecification ifError() {
    logWith(new ResponseLoggingFilter(getPrintStream(), greaterThanOrEqualTo(400)))
  }

  ResponseSpecification ifStatusCodeIsEqualTo(int statusCode) {
    logWith(new ResponseLoggingFilter(getPrintStream(), equalTo(statusCode)))
  }

  ResponseSpecification ifStatusCodeMatches(Matcher<Integer> matcher) {
    logWith(new ResponseLoggingFilter(getPrintStream(), matcher))
  }

  private def logWith(LogDetail logDetail) {
    logWith(new ResponseLoggingFilter(logDetail, getPrintStream()))
  }

  private def logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
    logWith(logDetail, prettyPrintingEnabled, getPrintStream())
  }

  private def logWith(LogDetail logDetail, boolean prettyPrintingEnabled, PrintStream printStream) {
    logWith(new ResponseLoggingFilter(logDetail, prettyPrintingEnabled, printStream))
  }

  private def logWith(ResponseLoggingFilter filter) {
    responseSpecification.request().filter(filter)
    responseSpecification
  }

  private def getPrintStream() {
    super.getPrintStream(responseSpecification.request())
  }

  private def shouldPrettyPrint() {
    super.shouldPrettyPrint(responseSpecification.request())
  }
}
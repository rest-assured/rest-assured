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

import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.LogDetail
import io.restassured.http.ContentType
import io.restassured.internal.log.LogRepository
import io.restassured.matcher.DetailedCookieMatcher
import io.restassured.matcher.ResponseAwareMatcher
import io.restassured.parsing.Parser
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import io.restassured.response.ValidatableResponse
import io.restassured.response.ValidatableResponseLogSpec
import io.restassured.specification.Argument
import io.restassured.specification.ResponseSpecification
import org.hamcrest.Matcher

import java.util.concurrent.TimeUnit
import java.util.function.Function

class ValidatableResponseImpl extends ValidatableResponseOptionsImpl<ValidatableResponse, Response> implements ValidatableResponse {

  ValidatableResponseImpl(String contentType, ResponseParserRegistrar rpr, RestAssuredConfig config, Response response,
                          ExtractableResponse<Response> extractableResponse, LogRepository logRepository) {
    super(rpr, config, response, extractableResponse, logRepository)
  }

  Response originalResponse() {
    response
  }

  // These methods are only implemented because of errors when compiling from Maven
  @Override
  ValidatableResponse time(Matcher<Long> matcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.time(matcher)
  }

  @Override
  ValidatableResponse time(Matcher<Long> matcher, TimeUnit timeUnit) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.time(matcher, timeUnit)
  }

  @Override
  ValidatableResponse defaultParser(Parser parser) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.defaultParser(parser)
  }

  @Override
  ValidatableResponse parser(String contentType, Parser parser) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.parser(contentType, parser)
  }

  @Override
  ValidatableResponse spec(ResponseSpecification responseSpecification) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.spec(responseSpecification)
  }

  @Override
  ValidatableResponse assertThat() {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.assertThat()
  }

  @Override
  ValidatableResponse using() {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.using()
  }

  @Override
  ValidatableResponse and() {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.and()
  }

  @Override
  ValidatableResponse all() {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.all()
  }

  @Override
  ValidatableResponse body(List<Argument> arguments, ResponseAwareMatcher<Response> responseAwareMatcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.body(arguments, responseAwareMatcher)
  }

  @Override
  ValidatableResponse body(String key, List<Argument> arguments, ResponseAwareMatcher<Response> responseAwareMatcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.body(key, arguments, responseAwareMatcher)
  }

  @Override
  ValidatableResponse body(String key, ResponseAwareMatcher<Response> responseAwareMatcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.body(key, responseAwareMatcher)
  }

  @Override
  ValidatableResponse body(String path, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.body(path, arguments, matcher, additionalKeyMatcherPairs)
  }

  @Override
  ValidatableResponse body(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.body(arguments, matcher, additionalKeyMatcherPairs)
  }

  @Override
  ValidatableResponse statusCode(Matcher<? super Integer> expectedStatusCode) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.statusCode(expectedStatusCode)
  }

  @Override
  ValidatableResponse statusCode(int expectedStatusCode) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.statusCode(expectedStatusCode)
  }

  @Override
  ValidatableResponse statusLine(Matcher<? super String> expectedStatusLine) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.statusLine(expectedStatusLine)
  }

  @Override
  ValidatableResponse statusLine(String expectedStatusLine) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.statusLine(expectedStatusLine)
  }

  @Override
  ValidatableResponse headers(Map<String, ?> expectedHeaders) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.headers(expectedHeaders)
  }

  @Override
  ValidatableResponse headers(String firstExpectedHeaderName, Object firstExpectedHeaderValue, Object... expectedHeaders) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.headers(firstExpectedHeaderName, firstExpectedHeaderValue, expectedHeaders)
  }

  @Override
  ValidatableResponse header(String headerName, Matcher expectedValueMatcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.header(headerName, expectedValueMatcher)
  }

  @Override
  ValidatableResponse header(String headerName, ResponseAwareMatcher<Response> r) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.header(headerName, r)
  }

  @Override
  ValidatableResponse header(String headerName, String expectedValue) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.header(headerName, expectedValue)
  }

  @Override
  ValidatableResponse header(String headerName, Function f, Matcher matcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.header(headerName, f, matcher)
  }

  @Override
  ValidatableResponse cookies(Map<String, ?> expectedCookies) {
    return super.cookies(expectedCookies)
  }

  @Override
  ValidatableResponse cookie(String cookieName) {
    return super.cookie(cookieName)
  }

  @Override
  ValidatableResponse cookies(String firstExpectedCookieName, Object firstExpectedCookieValue, Object... expectedCookieNameValuePairs) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.cookies(firstExpectedCookieName, firstExpectedCookieValue, expectedCookieNameValuePairs)
  }

  @Override
  ValidatableResponse cookie(String cookieName, Matcher<?> expectedValueMatcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.cookie(cookieName, expectedValueMatcher)
  }

  @Override
  ValidatableResponse cookie(String cookieName, Object expectedValue) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.cookie(cookieName, expectedValue)
  }

  @Override
  ValidatableResponse cookie(String cookieName, DetailedCookieMatcher detailedCookieMatcher) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.cookie(cookieName, detailedCookieMatcher)
  }

  @Override
  ValidatableResponse rootPath(String rootPath) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.rootPath(rootPath)
  }

  @Override
  ValidatableResponse rootPath(String rootPath, List<Argument> arguments) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.rootPath(rootPath, arguments)
  }

  @Override
  ValidatableResponse root(String rootPath, List<Argument> arguments) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.root(rootPath, arguments)
  }

  @Override
  ValidatableResponse root(String rootPath) {
    //noinspection GroovyUncheckedAssignmentOfMemberOfRawType
    return super.root(rootPath)
  }

  @Override
  ValidatableResponse noRoot() {
    return super.noRoot()
  }

  @Override
  ValidatableResponse noRootPath() {
    return super.noRootPath()
  }

  @Override
  ValidatableResponse appendRootPath(String pathToAppend) {
    return super.appendRootPath(pathToAppend)
  }

  @Override
  ValidatableResponse appendRootPath(String pathToAppend, List<Argument> arguments) {
    return super.appendRootPath(pathToAppend, arguments)
  }

  @Override
  ValidatableResponse detachRootPath(String pathToDetach) {
    return super.detachRootPath(pathToDetach)
  }

  @Override
  ValidatableResponse contentType(ContentType contentType) {
    return super.contentType(contentType)
  }

  @Override
  ValidatableResponse contentType(String contentType) {
    return super.contentType(contentType)
  }

  @Override
  ValidatableResponse contentType(Matcher<? super String> contentType) {
    return super.contentType(contentType)
  }

  @Override
  ValidatableResponse body(Matcher<?> matcher, Matcher<?>... additionalMatchers) {
    return super.body(matcher, additionalMatchers)
  }

  @Override
  ValidatableResponse body(String path, Matcher<?> matcher, Object... additionalKeyMatcherPairs) {
    return super.body(path, matcher, additionalKeyMatcherPairs)
  }

  @Override
  ValidatableResponse content(String path, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
    return super.content(path, arguments, matcher, additionalKeyMatcherPairs)
  }

  @Override
  ExtractableResponse<Response> extract() {
    return super.extract()
  }

  @Override
  ValidatableResponseLogSpec log() {
    return super.log()
  }

  @Override
  ValidatableResponse status() {
    return super.status()
  }

  @Override
  ValidatableResponse ifError() {
    return super.ifError()
  }

  @Override
  ValidatableResponse ifStatusCodeIsEqualTo(int statusCode) {
    return super.ifStatusCodeIsEqualTo(statusCode)
  }

  @Override
  ValidatableResponse ifStatusCodeMatches(Matcher<Integer> matcher) {
    return super.ifStatusCodeMatches(matcher)
  }

  @Override
  ValidatableResponse body() {
    return super.body()
  }

  @Override
  ValidatableResponse body(boolean shouldPrettyPrint) {
    return super.body(shouldPrettyPrint)
  }

  @Override
  ValidatableResponse all(boolean shouldPrettyPrint) {
    return super.all(shouldPrettyPrint)
  }

  @Override
  ValidatableResponse everything() {
    return super.everything()
  }

  @Override
  ValidatableResponse everything(boolean shouldPrettyPrint) {
    return super.everything(shouldPrettyPrint)
  }

  @Override
  ValidatableResponse headers() {
    return super.headers()
  }

  @Override
  ValidatableResponse cookies() {
    return super.cookies()
  }

  @Override
  ValidatableResponse ifValidationFails() {
    return super.ifValidationFails()
  }

  @Override
  ValidatableResponse ifValidationFails(LogDetail logDetail) {
    return super.ifValidationFails(logDetail)
  }

  @Override
  ValidatableResponse ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
    return super.ifValidationFails(logDetail, shouldPrettyPrint)
  }
// End maven workarounds
}

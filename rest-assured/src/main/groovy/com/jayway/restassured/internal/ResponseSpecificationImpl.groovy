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

package com.jayway.restassured.internal

import com.jayway.restassured.assertion.BodyMatcher
import com.jayway.restassured.assertion.BodyMatcherGroup
import com.jayway.restassured.assertion.CookieMatcher
import com.jayway.restassured.assertion.HeaderMatcher
import com.jayway.restassured.exception.AssertionFailedException
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.FilterableRequestSpecification
import com.jayway.restassured.specification.FilterableResponseSpecification
import com.jayway.restassured.specification.RequestSpecification
import com.jayway.restassured.specification.ResponseSpecification
import groovyx.net.http.ContentType
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import static com.jayway.restassured.assertion.AssertParameter.notNull
import static groovyx.net.http.ContentType.ANY
import static org.hamcrest.Matchers.equalTo

class ResponseSpecificationImpl implements FilterableResponseSpecification {

  private static final String DEFAULT_BODY_ROOT_PATH = ""
  private Matcher<Integer> expectedStatusCode;
  private Matcher<String> expectedStatusLine;
  private BodyMatcherGroup bodyMatchers = new BodyMatcherGroup()
  private HamcrestAssertionClosure assertionClosure = new HamcrestAssertionClosure();
  private List headerAssertions = []
  private List cookieAssertions = []
  private FilterableRequestSpecification requestSpecification;
  private def contentType;
  private Response restAssuredResponse;
  private String bodyRootPath;

  ResponseSpecificationImpl(String bodyRootPath, responseContentType, ResponseSpecification defaultSpec) {
    rootPath(bodyRootPath)
    this.contentType = responseContentType
    if(defaultSpec != null) {
      spec(defaultSpec)
    }
  }

  def ResponseSpecification content(Matcher matcher, Matcher...additionalMatchers) {
    notNull(matcher, "matcher")
    bodyMatchers << new BodyMatcher(key: null, matcher: matcher)
    additionalMatchers?.each { hamcrestMatcher ->
      bodyMatchers << new BodyMatcher(key: null, matcher: hamcrestMatcher)
    }
    return this
  }

  def ResponseSpecification content(String key, Matcher matcher, Object...additionalKeyMatcherPairs) {
    notNull(key, "key")
    notNull(matcher, "matcher")

    bodyMatchers << new BodyMatcher(key: bodyRootPath + key, matcher: matcher)
    if(additionalKeyMatcherPairs?.length > 0) {
      def pairs = MapCreator.createMapFromObjects(additionalKeyMatcherPairs)
      pairs.each { matchingKey, hamcrestMatcher ->
        def keyWithRoot = bodyRootPath + matchingKey
        bodyMatchers << new BodyMatcher(key: keyWithRoot, matcher: hamcrestMatcher)
      }
    }
    return this
  }

  def ResponseSpecification statusCode(Matcher<Integer> expectedStatusCode) {
    notNull(expectedStatusCode, "expectedStatusCode")
    this.expectedStatusCode = expectedStatusCode
    return this
  }

  def ResponseSpecification statusCode(int expectedStatusCode) {
    notNull(expectedStatusCode, "expectedStatusCode")
    return statusCode(equalTo(expectedStatusCode));
  }

  def ResponseSpecification statusLine(Matcher<String> expectedStatusLine) {
    notNull(expectedStatusLine, "expectedStatusLine")
    this.expectedStatusLine = expectedStatusLine
    return this
  }

  def ResponseSpecification headers(Map<String, Object> expectedHeaders){
    notNull(expectedHeaders, "expectedHeaders")

    expectedHeaders.each { headerName, matcher ->
      headerAssertions << new HeaderMatcher(headerName: headerName, matcher: matcher instanceof Matcher ? matcher : equalTo(matcher))
    }
    return this
  }

  def ResponseSpecification headers(String firstExpectedHeaderName, Object...expectedHeaders) {
    notNull firstExpectedHeaderName, "firstExpectedHeaderName"
    return headers(MapCreator.createMapFromStrings(firstExpectedHeaderName, expectedHeaders))
  }

  def ResponseSpecification header(String headerName, Matcher<String> expectedValueMatcher) {
    notNull headerName, "headerName"
    notNull expectedValueMatcher, "expectedValueMatcher"

    headerAssertions << new HeaderMatcher(headerName: headerName, matcher: expectedValueMatcher)
    this;
  }
  def ResponseSpecification header(String headerName, String expectedValue) {
    return header(headerName, equalTo(expectedValue))
  }

  def ResponseSpecification cookies(Map<String, Object> expectedCookies) {
    notNull expectedCookies, "expectedCookies"

    expectedCookies.each { cookieName, matcher ->
      cookieAssertions << new CookieMatcher(cookieName: cookieName, matcher: matcher instanceof Matcher ? matcher : equalTo(matcher))
    }
    return this
  }

  def ResponseSpecification cookies(String firstExpectedCookieName, Object... expectedCookieNameValuePairs) {
    notNull firstExpectedCookieName, "firstExpectedCookieName"
    notNull expectedCookieNameValuePairs, "expectedCookieNameValuePairs"

    return cookies(MapCreator.createMapFromStrings(firstExpectedCookieName, expectedCookieNameValuePairs))
  }

  def ResponseSpecification cookie(String cookieName, Matcher<String> expectedValueMatcher) {
    notNull cookieName,"cookieName"
    notNull expectedValueMatcher,"expectedValueMatcher"
    cookieAssertions << new CookieMatcher(cookieName: cookieName, matcher: expectedValueMatcher)
    this;
  }

  def ResponseSpecification cookie(String cookieName) {
    notNull cookieName,"cookieName"
    return cookie(cookieName, Matchers.<String> anything())
  }

  def ResponseSpecification cookie(String cookieName, String expectedValue) {
    return cookie(cookieName, equalTo(expectedValue))
  }

  def ResponseSpecification spec(ResponseSpecification responseSpecificationToMerge) {
    SpecificationMerger.merge(this, responseSpecificationToMerge);
    return this
  }

  def ResponseSpecification specification(ResponseSpecification responseSpecificationToMerge) {
    return spec(responseSpecificationToMerge)
  }

  def ResponseSpecification statusLine(String expectedStatusLine) {
    return statusLine(equalTo(expectedStatusLine))
  }

  def ResponseSpecification body(Matcher matcher, Matcher...additionalMatchers) {
    return content(matcher, additionalMatchers);
  }

  public ResponseSpecification body(String key, Matcher matcher, Object...additionalKeyMatcherPairs) {
    return content(key, matcher, additionalKeyMatcherPairs);
  }

  def ResponseSpecification when() {
    return this;
  }

  def ResponseSpecification response() {
    return this;
  }

  def RequestSpecification given() {
    return requestSpecification;
  }

  def ResponseSpecification that() {
    return this;
  }

  def RequestSpecification request() {
    return requestSpecification;
  }

  Response get(String path) {
    requestSpecification.get(path);
  }

  Response post(String path) {
    requestSpecification.post(path);
  }

  Response put(String path) {
    requestSpecification.put(path);
  }

  Response delete(String path) {
    requestSpecification.delete(path);
  }

  def Response head(String path) {
    requestSpecification.head(path);
  }

  def ResponseSpecification and() {
    return this;
  }

  def RequestSpecification with() {
    return given();
  }

  def ResponseSpecification then() {
    return this;
  }

  def ResponseSpecification expect() {
    return this;
  }

  def ResponseSpecification rootPath(String rootPath) {
    notNull rootPath, "Root path"
    this.bodyRootPath = rootPath.equals(DEFAULT_BODY_ROOT_PATH) ? rootPath : rootPath.endsWith(".") ? rootPath : rootPath + "."
    return this
  }

  def ResponseSpecification root(String rootPath) {
    return this.rootPath(rootPath);
  }

  def boolean hasAssertionsDefined() {
    return bodyMatchers.containsMatchers() || !headerAssertions.isEmpty() ||
            !cookieAssertions.isEmpty() || expectedStatusCode != null || expectedStatusLine != null
  }

  ResponseSpecification contentType(ContentType contentType) {
    notNull contentType, "contentType"
    this.contentType = contentType
    return this
  }

  ResponseSpecification contentType(String contentType) {
    notNull contentType, "contentType"
    this.contentType = contentType
    return this
  }

  class HamcrestAssertionClosure {
    def call(response, content) {
      return getClosure().call(response, content)
    }

    def call(response) {
      return getClosure().call(response, null)
    }

    def getResponseContentType() {
      return contentType ?: ANY;
    }

    private boolean requiresTextParsing() {
      return bodyMatchers.requiresTextParsing()
    }

    def getClosure() {
      return { response, content ->
        if(hasAssertionsDefined()) {
          validateHeadersAndCookies(response)
          def parsedContent = bodyMatchers.isFulfilled(response, content)
          restAssuredResponse.parseResponse( response, parsedContent )
        } else {
          restAssuredResponse.parseResponse( response, content )
        }
      }
    }

    private def validateHeadersAndCookies(response) {
      headerAssertions.each { matcher ->
        matcher.containsHeader(response.headers)
      }

      cookieAssertions.each { matcher ->
        matcher.containsCookie(response.headers.'Set-Cookie')
      }

      if (expectedStatusCode != null) {
        def actualStatusCode = response.statusLine.statusCode
        if (!expectedStatusCode.matches(actualStatusCode)) {
          throw new AssertionFailedException(String.format("Expected status code %s doesn't match actual status code <%s>.", expectedStatusCode.toString(), actualStatusCode));
        }
      }

      if (expectedStatusLine != null) {
        def actualStatusLine = response.statusLine.toString()
        if (!expectedStatusLine.matches(actualStatusLine)) {
          throw new AssertionFailedException(String.format("Expected status line %s doesn't match actual status line \"%s\".", expectedStatusLine.toString(), actualStatusLine));
        }
      }
    }
  }




  def void setRequestSpec(RequestSpecification requestSpecification) {
    this.requestSpecification = requestSpecification
  }
}
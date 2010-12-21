package com.jayway.restassured.internal

import com.jayway.restassured.assertion.BodyMatcher
import com.jayway.restassured.assertion.BodyMatcherGroup
import com.jayway.restassured.assertion.HeaderMatcher
import com.jayway.restassured.exception.AssertionFailedException
import com.jayway.restassured.specification.RequestSpecification
import com.jayway.restassured.specification.ResponseSpecification
import groovyx.net.http.ContentType
import org.hamcrest.Matcher
import static groovyx.net.http.ContentType.ANY
import static groovyx.net.http.ContentType.TEXT
import static org.hamcrest.Matchers.equalTo

class ResponseSpecificationImpl implements ResponseSpecification {

  private Matcher<Integer> expectedStatusCode;
  private Matcher<String> expectedStatusLine;
  private BodyMatcherGroup bodyMatchers = new BodyMatcherGroup()
  private HamcrestAssertionClosure assertionClosure = new HamcrestAssertionClosure();
  private List headerAssertions = []
  private RequestSpecification requestSpecification;
  private ContentType contentType;

  def ResponseSpecification content(Matcher<?> matcher, Matcher<?>...additionalMatchers) {
    assertNotNull(matcher)
    bodyMatchers << new BodyMatcher(key: null, matcher: matcher)
    additionalMatchers?.each { hamcrestMatcher ->
      bodyMatchers << new BodyMatcher(key: null, matcher: hamcrestMatcher)
    }
    return this
  }

  def ResponseSpecification content(String key, Matcher<?> matcher, Object...additionalKeyMatcherPairs) {
    assertNotNull(key, matcher)
    bodyMatchers << new BodyMatcher(key: key, matcher: matcher)
    if(additionalKeyMatcherPairs?.length > 0) {
      def pairs = MapCreator.createMapFromObjects(additionalKeyMatcherPairs)
      pairs.each { matchingKey, hamcrestMatcher ->
        bodyMatchers << new BodyMatcher(key: matchingKey, matcher: hamcrestMatcher)
      }
    }
    return this
  }

  def ResponseSpecification statusCode(Matcher<Integer> expectedStatusCode) {
    this.expectedStatusCode = expectedStatusCode
    return this
  }

  def ResponseSpecification statusCode(int expectedStatusCode) {
    return statusCode(equalTo(expectedStatusCode));
  }

  def ResponseSpecification statusLine(Matcher<String> expectedStatusLine) {
    this.expectedStatusLine = expectedStatusLine
    return this
  }

  def ResponseSpecification headers(Map<String, Object> expectedHeaders){
    expectedHeaders.each { headerName, matcher ->
      headerAssertions << new HeaderMatcher(headerName: headerName, matcher: matcher instanceof Matcher ? matcher : equalTo(matcher))
    }
    return this
  }

  /**
   * @param expectedHeaders
   * @return
   */
  def ResponseSpecification headers(String firstExpectedHeaderName, Object...expectedHeaders) {
    return headers(MapCreator.createMapFromStrings(firstExpectedHeaderName, expectedHeaders))
  }

  def ResponseSpecification header(String headerName, Matcher<String> expectedValueMatcher) {
    assertNotNull(headerName, expectedValueMatcher)
    headerAssertions << new HeaderMatcher(headerName: headerName, matcher: expectedValueMatcher)
    this;
  }
  def ResponseSpecification header(String headerName, String expectedValue) {
    return header(headerName, equalTo(expectedValue))
  }

  def ResponseSpecification statusLine(String expectedStatusLine) {
    return statusLine(equalTo(expectedStatusLine))
  }

  def ResponseSpecification body(Matcher<?> matcher, Matcher<?>...additionalMatchers) {
    return content(matcher, additionalMatchers);
  }

  def ResponseSpecification body(String key, Matcher<?> matcher, Object...additionalKeyMatcherPairs) {
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

  void get(String path) {
    requestSpecification.get(path);
  }

  void post(String path) {
    requestSpecification.post(path);
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

  ResponseSpecification contentType(ContentType contentType) {
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

    ContentType getResponseContentType() {
      return contentType ?: bodyMatchers.requiresContentTypeText() ?  TEXT : ANY;
    }


    private boolean requiresContentTypeText() {
      return bodyMatchers.requiresContentTypeText()
    }

    def getClosure() {
      return { response, content ->
        headerAssertions.each { matcher ->
          matcher.containsHeader(response.headers)
        }
        if(expectedStatusCode != null) {
          def actualStatusCode = response.statusLine.statusCode
          if(!expectedStatusCode.matches(actualStatusCode)) {
            throw new AssertionFailedException(String.format("Expected status code %s doesn't match actual status code <%s>.", expectedStatusCode.toString(), actualStatusCode));
          }
        }

        if(expectedStatusLine != null) {
          def actualStatusLine = response.statusLine.toString()
          if(!expectedStatusLine.matches(actualStatusLine)) {
            throw new AssertionFailedException(String.format("Expected status line %s doesn't match actual status line \"%s\".", expectedStatusLine.toString(), actualStatusLine));
          }
        }

        bodyMatchers.isFulfilled(response, content)
      }
    }
  }


  private def assertNotNull(Object ... objects) {
    objects.each {
      if(it == null) {
        throw new IllegalArgumentException("Argument cannot be null")
      }
    }
  }
}



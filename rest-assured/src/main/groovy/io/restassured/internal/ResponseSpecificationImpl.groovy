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

import io.restassured.assertion.*
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.LogDetail
import io.restassured.http.ContentType
import io.restassured.internal.MapCreator.CollisionStrategy
import io.restassured.internal.log.LogRepository
import io.restassured.internal.util.MatcherErrorMessageBuilder
import io.restassured.matcher.DetailedCookieMatcher
import io.restassured.parsing.Parser
import io.restassured.response.Response
import io.restassured.specification.*
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.hamcrest.Matcher
import org.hamcrest.Matchers

import java.util.concurrent.TimeUnit
import java.util.function.Function

import static io.restassured.http.ContentType.ANY
import static io.restassured.internal.common.assertion.AssertParameter.notNull
import static org.apache.commons.lang3.StringUtils.substringAfter
import static org.hamcrest.Matchers.equalTo

class ResponseSpecificationImpl implements FilterableResponseSpecification {

  private static final String EMPTY = ""
  private static final String DOT = "."
  private Matcher<Integer> expectedStatusCode;
  private Matcher<String> expectedStatusLine;
  private BodyMatcherGroup bodyMatchers = new BodyMatcherGroup()
  private HamcrestAssertionClosure assertionClosure = new HamcrestAssertionClosure();
  private def headerAssertions = []
  private def cookieAssertions = []
  private RequestSpecification requestSpecification;
  private def contentType;
  private Response restAssuredResponse;
  private String bodyRootPath;
  ResponseParserRegistrar rpr;
  RestAssuredConfig config
  private Response response
  private Tuple2<Matcher<Long>, TimeUnit> expectedResponseTime;
  private LogDetail responseLogDetail
  private boolean forceDisableEagerAssert = false

  private contentParser
  LogRepository logRepository

  ResponseSpecificationImpl(String bodyRootPath, ResponseSpecification defaultSpec, ResponseParserRegistrar rpr,
                            RestAssuredConfig config, LogRepository logRepository) {
    this(bodyRootPath, defaultSpec, rpr, config, null, logRepository)
  }

  ResponseSpecificationImpl(String bodyRootPath, ResponseSpecification defaultSpec, ResponseParserRegistrar rpr,
                            RestAssuredConfig config, Response response, LogRepository logRepository) {
    Validate.notNull(config, "RestAssuredConfig cannot be null")
    this.config = config
    this.response = response;
    rootPath(bodyRootPath)
    this.rpr = rpr
    if (defaultSpec != null) {
      spec(defaultSpec)
    }
    this.logRepository = logRepository
  }

  ResponseSpecification body(List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
    throwIllegalStateExceptionIfRootPathIsNotDefined("specify arguments")
    body("", arguments, matcher, additionalKeyMatcherPairs)
  }

  Response validate(Response response) {
    assertionClosure.validate(response)
    response
  }

  ResponseSpecification body(Matcher matcher, Matcher... additionalMatchers) {
    notNull(matcher, "matcher")
    validateResponseIfRequired {
      bodyMatchers << new BodyMatcher(key: null, matcher: matcher, rpr: rpr)
      additionalMatchers?.each { hamcrestMatcher ->
        bodyMatchers << new BodyMatcher(key: null, matcher: hamcrestMatcher, rpr: rpr)
      }
    }
    return this
  }

  ResponseSpecification body(String key, Matcher matcher, Object... additionalKeyMatcherPairs) {
    body(key, Collections.emptyList(), matcher, additionalKeyMatcherPairs)
  }

  ResponseSpecification time(Matcher<Long> matcher) {
    time(matcher, TimeUnit.MILLISECONDS)
  }

  ResponseSpecification time(Matcher<Long> matcher, TimeUnit timeUnit) {
    notNull(matcher, Matcher.class)
    notNull(timeUnit, TimeUnit.class)
    validateResponseIfRequired {
      expectedResponseTime = new Tuple2<>(matcher, timeUnit)
    }
    this
  }

  ResponseSpecification statusCode(Matcher<? super Integer> expectedStatusCode) {
    notNull(expectedStatusCode, "expectedStatusCode")
    validateResponseIfRequired {
      this.expectedStatusCode = expectedStatusCode
    }
    return this
  }

  ResponseSpecification statusCode(int expectedStatusCode) {
    notNull(expectedStatusCode, "expectedStatusCode")
    return statusCode(equalTo(expectedStatusCode));
  }

  ResponseSpecification statusLine(Matcher<? super String> expectedStatusLine) {
    notNull(expectedStatusLine, "expectedStatusLine")
    validateResponseIfRequired {
      this.expectedStatusLine = expectedStatusLine
    }
    return this
  }

  ResponseSpecification headers(Map expectedHeaders) {
    notNull(expectedHeaders, "expectedHeaders")
    validateResponseIfRequired {
      expectedHeaders.each { headerName, matcher ->
        if (matcher instanceof List) {
          matcher.each {
            headerAssertions << new HeaderMatcher(headerName: headerName, matcher: it instanceof Matcher ? it : equalTo(it))
          }
        } else {
          headerAssertions << new HeaderMatcher(headerName: headerName, matcher: matcher instanceof Matcher ? matcher : equalTo(matcher))
        }
      }
    }
    return this
  }

  ResponseSpecification headers(String firstExpectedHeaderName, Object firstExpectedHeaderValue, Object... expectedHeaders) {
    notNull firstExpectedHeaderName, "firstExpectedHeaderName"
    notNull firstExpectedHeaderValue, "firstExpectedHeaderValue"
    return headers(MapCreator.createMapFromParams(CollisionStrategy.MERGE, firstExpectedHeaderName, firstExpectedHeaderValue, expectedHeaders))
  }

  @Override
  ResponseSpecification header(String headerName, Function mappingFunction, Matcher expectedValueMatcher) {
    notNull headerName, "Header name"
    notNull mappingFunction, "Mapping function"
    notNull expectedValueMatcher, "Hamcrest matcher"
    validateResponseIfRequired {
      headerAssertions << new HeaderMatcher(headerName: headerName, mappingFunction: mappingFunction, matcher: expectedValueMatcher)
    }
    this
  }

  ResponseSpecification header(String headerName, Matcher expectedValueMatcher) {
    notNull headerName, "headerName"
    notNull expectedValueMatcher, "expectedValueMatcher"

    validateResponseIfRequired {
      headerAssertions << new HeaderMatcher(headerName: headerName, matcher: expectedValueMatcher)
    }
    this;
  }

  ResponseSpecification header(String headerName, String expectedValue) {
    return header(headerName, equalTo(expectedValue))
  }

  ResponseSpecification cookies(Map expectedCookies) {
    notNull expectedCookies, "expectedCookies"

    validateResponseIfRequired {
      expectedCookies.each { cookieName, matcher ->
        if (matcher instanceof List) {
          matcher.each {
            cookieAssertions << new CookieMatcher(cookieName: cookieName, matcher: it instanceof Matcher ? it : equalTo(it))
          }
        } else {
          cookieAssertions << new CookieMatcher(cookieName: cookieName, matcher: matcher instanceof Matcher ? matcher : equalTo(matcher))
        }
      }
    }
    return this
  }

  ResponseSpecification cookies(String firstExpectedCookieName, Object firstExpectedCookieValue, Object... expectedCookieNameValuePairs) {
    notNull firstExpectedCookieName, "firstExpectedCookieName"
    notNull firstExpectedCookieValue, "firstExpectedCookieValue"
    notNull expectedCookieNameValuePairs, "expectedCookieNameValuePairs"

    return cookies(MapCreator.createMapFromParams(CollisionStrategy.MERGE, firstExpectedCookieName, firstExpectedCookieValue, expectedCookieNameValuePairs))
  }

  ResponseSpecification cookie(String cookieName, Matcher expectedValueMatcher) {
    notNull cookieName, "cookieName"
    notNull expectedValueMatcher, "expectedValueMatcher"
    validateResponseIfRequired {
      cookieAssertions << new CookieMatcher(cookieName: cookieName, matcher: expectedValueMatcher)
    }
    this;
  }

  ResponseSpecification cookie(String cookieName, DetailedCookieMatcher detailedCookieMatcher) {
    notNull cookieName, "cookieName"
    notNull detailedCookieMatcher, "cookieMatcher"
    validateResponseIfRequired {
      cookieAssertions << new DetailedCookieAssertion(cookieName: cookieName, matcher: detailedCookieMatcher)
    }
    this;
  }

  ResponseSpecification cookie(String cookieName) {
    notNull cookieName, "cookieName"
    return cookie(cookieName, Matchers.<String> anything())
  }

  ResponseSpecification cookie(String cookieName, Object expectedValue) {
    return cookie(cookieName, equalTo(expectedValue))
  }

  ResponseSpecification spec(ResponseSpecification responseSpecificationToMerge) {
    SpecificationMerger.merge(this, responseSpecificationToMerge);
    return this
  }

  ResponseSpecification specification(ResponseSpecification responseSpecificationToMerge) {
    return spec(responseSpecificationToMerge)
  }

  ResponseSpecification statusLine(String expectedStatusLine) {
    return statusLine(equalTo(expectedStatusLine))
  }

  ResponseSpecification body(String key, List<Argument> arguments, Matcher matcher, Object... additionalKeyMatcherPairs) {
    notNull(key, "key")
    notNull(matcher, "matcher")

    validateResponseIfRequired {
      bodyMatchers << new BodyMatcher(key: applyArguments(mergeKeyWithRootPath(key), arguments), matcher: matcher, rpr: rpr)
      if (additionalKeyMatcherPairs?.length > 0) {
        def pairs = MapCreator.createMapFromObjects(CollisionStrategy.MERGE, additionalKeyMatcherPairs)
        pairs.each { matchingKey, matchingValue ->
          String keyWithRoot
          def hamcrestMatcher
          if (matchingKey instanceof List) {
            // If matching key is instance of list (we assume it's a list of arguments) then we should simply return the merged path,
            // otherwise merge the current path with the supplied key
            keyWithRoot = applyArguments(mergeKeyWithRootPath(""), matchingKey)
            hamcrestMatcher = matchingValue
          } else if (matchingValue instanceof MapCreator.ArgsAndValue) {
            String mergedPath = mergeKeyWithRootPath(matchingKey)
            keyWithRoot = applyArguments(mergedPath, matchingValue.args)
            hamcrestMatcher = matchingValue.value
          } else {
            keyWithRoot = mergeKeyWithRootPath(matchingKey)
            hamcrestMatcher = matchingValue
          }

          if (hamcrestMatcher instanceof List) {
            hamcrestMatcher.each { m ->
              def keyToUse
              def matcherToUse
              if (m instanceof MapCreator.ArgsAndValue) {
                keyToUse = applyArguments(keyWithRoot, m.args)
                matcherToUse = m.value
              } else {
                // Plain hamcrest matcher, what happens is that if a user has specified body("x", greaterThan(2), "x", lessThan(10)) then "x" will have a list of these hamcrest matchers
                keyToUse = keyWithRoot
                matcherToUse = m
              }
              bodyMatchers << new BodyMatcher(key: keyToUse, matcher: matcherToUse, rpr: rpr)
            }
          } else {
            bodyMatchers << new BodyMatcher(key: keyWithRoot, matcher: hamcrestMatcher, rpr: rpr)
          }
        }
      }
    }
    return this
  }

  ResponseLogSpecification log() {
    return new ResponseLogSpecificationImpl(responseSpecification: this, logRepository: logRepository)
  }

  ResponseSpecificationImpl logDetail(LogDetail logDetail) {
    this.responseLogDetail = logDetail
    this
  }

  LogDetail getLogDetail() {
    responseLogDetail
  }

  RequestSender when() {
    return requestSpecification;
  }

  ResponseSpecification response() {
    return this;
  }

  RequestSpecification given() {
    return requestSpecification;
  }

  ResponseSpecification that() {
    return this;
  }

  RequestSpecification request() {
    return requestSpecification;
  }

  ResponseSpecification parser(String contentType, Parser parser) {
    rpr.registerParser(contentType, parser)
    this
  }

  ResponseSpecification and() {
    return this;
  }

  RequestSpecification with() {
    return given();
  }

  ResponseSpecification then() {
    return this;
  }

  ResponseSpecification expect() {
    return this;
  }

  ResponseSpecification rootPath(String rootPath) {
    return this.rootPath(rootPath, [])
  }

  ResponseSpecification noRootPath() {
    return rootPath("")
  }

  ResponseSpecification appendRootPath(String pathToAppend) {
    return appendRootPath(pathToAppend, [])
  }

  ResponseSpecification appendRootPath(String pathToAppend, List<Argument> arguments) {
    notNull pathToAppend, "Path to append to root path"
    notNull arguments, "Arguments for path to append"
    def mergedPath = mergeKeyWithRootPath(pathToAppend)
    rootPath(mergedPath, arguments)
  }

  ResponseSpecification detachRootPath(String pathToDetach) {
    notNull pathToDetach, "Path to detach from root path"
    throwIllegalStateExceptionIfRootPathIsNotDefined("detach path")
    pathToDetach = StringUtils.trim(pathToDetach);
    if (!bodyRootPath.endsWith(pathToDetach)) {
      throw new IllegalStateException("Cannot detach path '$pathToDetach' since root path '$bodyRootPath' doesn't end with '$pathToDetach'.");
    }
    bodyRootPath = StringUtils.substringBeforeLast(bodyRootPath, pathToDetach);
    if (bodyRootPath.endsWith(".")) {
      bodyRootPath = bodyRootPath.substring(0, bodyRootPath.length() - 1);
    }
    this
  }

  ResponseSpecification rootPath(String rootPath, List<Argument> arguments) {
    notNull rootPath, "Root path"
    notNull arguments, "Arguments"
    this.bodyRootPath = applyArguments(rootPath, arguments)
    return this
  }

  ResponseSpecification root(String rootPath, List<Argument> arguments) {
    return this.rootPath(rootPath, arguments)
  }

  boolean hasBodyAssertionsDefined() {
    return bodyMatchers.containsMatchers()
  }

  boolean hasAssertionsDefined() {
    return hasBodyAssertionsDefined() || !headerAssertions.isEmpty() ||
            !cookieAssertions.isEmpty() || expectedStatusCode != null || expectedStatusLine != null ||
            contentType != null || expectedResponseTime != null
  }

  ResponseSpecification defaultParser(Parser parser) {
    notNull parser, "Parser"
    rpr.defaultParser = parser
    return this
  }

  ResponseSpecification contentType(ContentType contentType) {
    notNull contentType, "contentType"
    validateResponseIfRequired {
      this.contentType = contentType
    }
    return this
  }

  ResponseSpecification contentType(String contentType) {
    notNull contentType, "contentType"
    validateResponseIfRequired {
      this.contentType = contentType
    }
    return this
  }

  ResponseSpecification contentType(Matcher<? super String> contentType) {
    notNull contentType, "contentType"
    validateResponseIfRequired {
      this.contentType = contentType
    }
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

    private boolean requiresPathParsing() {
      return bodyMatchers.requiresPathParsing()
    }

    def getClosure() {
      return { response, content ->
        restAssuredResponse.parseResponse(response, content, hasBodyAssertionsDefined(), rpr)
      }
    }

    def validate(Response response) {
      if (hasAssertionsDefined()) {
        def validations = []
        try {
          validations.addAll(validateStatusCodeAndStatusLine(response))
          validations.addAll(validateHeadersAndCookies(response))
          validations.addAll(validateContentType(response))
          validations.addAll(validateResponseTime(response))
          if (hasBodyAssertionsDefined()) {
            RestAssuredConfig cfg = config ?: new RestAssuredConfig()
            if (requiresPathParsing() && (!isEagerAssert() || contentParser == null)) {
              contentParser = new ContentParser().parse(response, rpr, cfg, isEagerAssert())
            }
            validations.addAll(bodyMatchers.validate(response, contentParser, cfg))
          }
        } catch (Throwable e) {
          fireFailureListeners(response)
          throw e;
        }

        def errors = validations.findAll { !it.success }
        def numberOfErrors = errors.size()
        if (numberOfErrors > 0) {
          fireFailureListeners(response)
          def errorMessage = errors.collect { it.errorMessage }.join("\n")
          def s = numberOfErrors > 1 ? "s" : ""
          throw new AssertionError("$numberOfErrors expectation$s failed.\n$errorMessage")
        }
      }
    }

    private void fireFailureListeners(Response response) {
      config.getFailureConfig().getFailureListeners().each {
        it.onFailure(
                ResponseSpecificationImpl.this.requestSpecification,
                ResponseSpecificationImpl.this,
                response)
      }
    }

    private def validateContentType(Response response) {
      def errors = []
      if (contentType != null) {
        def actualContentType = response.getContentType()
        if (contentType instanceof Matcher) {
          if (!contentType.matches(actualContentType)) {
            errors << [success: false, errorMessage: String.format("Expected content-type %s doesn't match actual content-type \"%s\".\n", contentType, actualContentType)]
          }
        } else if (contentType instanceof String) {
          def normalizedExpectedContentType = normalizeContentType(contentType.toString())
          def normalizedActualContentType = normalizeContentType(actualContentType)
          if (!StringUtils.startsWithIgnoreCase(normalizedActualContentType, normalizedExpectedContentType)) {
            errors << [success: false, errorMessage: String.format("Expected content-type \"%s\" doesn't match actual content-type \"%s\".\n", contentType, actualContentType)];
          }
        } else {
          def name = contentType.name()
          def pattern = ~/(^[\w\d_\-]+\/[\w\d_\-]+)\s*(?:;)/
          def matcher = pattern.matcher(actualContentType ?: "");
          def contentTypeToMatch
          if (matcher.find()) {
            contentTypeToMatch = matcher.group(1);
          } else {
            contentTypeToMatch = actualContentType
          }
          if (ContentType.fromContentType(contentTypeToMatch) != contentType) {
            errors << [success: false, errorMessage: String.format("Expected content-type \"%s\" doesn't match actual content-type \"%s\".\n", name, actualContentType)];
          }
        }
      }
      errors
    }

    private String normalizeContentType(String actualContentType) {
      StringUtils.replaceEach(actualContentType, [" ", "\t"] as String[], ["", ""] as String[])
    }

    private def validateStatusCodeAndStatusLine(Response response) {
      def errors = []
      if (expectedStatusCode != null) {
        def actualStatusCode = response.getStatusCode()
        if (!expectedStatusCode.matches(actualStatusCode)) {

          def errorMessage = new MatcherErrorMessageBuilder<Integer, Matcher<Integer>>("status code")
                  .buildError(actualStatusCode, expectedStatusCode)

          errors << [success: false, errorMessage: errorMessage];
        }
      }

      if (expectedStatusLine != null) {
        def actualStatusLine = response.getStatusLine()
        if (!expectedStatusLine.matches(actualStatusLine)) {
          def errorMessage = String.format("Expected status line %s doesn't match actual status line \"%s\".\n", expectedStatusLine.toString(), actualStatusLine)
          errors << [success: false, errorMessage: errorMessage];
        }
      }
      errors
    }

    private def validateResponseTime(Response response) {
      def validations = []
      if (expectedResponseTime != null) {
        validations << new ResponseTimeMatcher(matcher: expectedResponseTime.first, timeUnit: expectedResponseTime.second).validate(response)
      }
      validations
    }

    private def validateHeadersAndCookies(Response response) {
      def validations = []
      validations.addAll(headerAssertions.collect { matcher ->
        matcher.validateHeader(response.getHeaders())
      })

      validations.addAll(cookieAssertions.collect { matcher ->
        def headerWithCookieList = response.getHeaders().getValues("Set-Cookie")
        def responseCookies = response.getDetailedCookies()
        matcher.validateCookies(headerWithCookieList, responseCookies)
      })
      validations
    }
  }

  Matcher<Integer> getStatusCode() {
    return expectedStatusCode
  }

  Matcher<String> getStatusLine() {
    return expectedStatusLine
  }

  boolean hasHeaderAssertions() {
    return !headerAssertions.isEmpty()
  }

  boolean hasCookieAssertions() {
    return !cookieAssertions.isEmpty()
  }

  String getResponseContentType() {
    return responseContentType != null ? responseContentType.toString() : ANY.toString()
  }

  String getRootPath() {
    return bodyRootPath
  }

  private String applyArguments(String path, List<Argument> arguments) {
    if (arguments?.size() > 0) {
      def numberArgsOfAfterMerge = StringUtils.countMatches(path, "%s");
      if (numberArgsOfAfterMerge > arguments.size()) {
        arguments = new ArrayList<>(arguments);
        for (int i = 0; i < (numberArgsOfAfterMerge - arguments.size()); i++) {
          arguments.add(new Argument("%s"));
        }
      }
      path = String.format(path, arguments.collect { it.getArgument() }.toArray(new Object[arguments.size()]))
    }
    return path
  }

  private String mergeKeyWithRootPath(String key) {
    if (bodyRootPath != null && bodyRootPath != EMPTY) {
      if (bodyRootPath.endsWith(DOT) && key.startsWith(DOT)) {
        return bodyRootPath + substringAfter(key, DOT);
      } else if (!bodyRootPath.endsWith(DOT) && !key.startsWith(DOT) && !key.startsWith("[")) {
        return bodyRootPath + DOT + key
      }
      return bodyRootPath + key
    }
    key
  }

  void throwIllegalStateExceptionIfRootPathIsNotDefined(String description) {
    if (rootPath == null || rootPath.isEmpty()) {
      throw new IllegalStateException("Cannot $description when root path is empty")
    }
  }

  /**
   * Forcefully disable eager assert. This is useful for certain language extensions to allow for validation of multiple expectations in one go.
   */
  def forceDisableEagerAssert() {
    forceDisableEagerAssert = true
    this
  }

  /**
   * Forcefully validate response expectations. This is useful for certain language extensions to allow for validation of multiple expectations in one go.
   */
  def forceValidateResponse() {
    // We parse the response as a string here because we need to enforce it otherwise we cannot use "extract" after validations are completed
    response.asString()

    assertionClosure.validate(response)
  }

  private isEagerAssert() {
    return !forceDisableEagerAssert && response != null
  }

  private void validateResponseIfRequired(Closure closure) {
    if (isEagerAssert()) {
      bodyMatchers.reset()
      // Reset the body matchers before each validation to avoid testing multiple matchers on each invocation
    }
    closure.call()
    if (isEagerAssert()) {
      assertionClosure.validate(response)
    }
  }
}

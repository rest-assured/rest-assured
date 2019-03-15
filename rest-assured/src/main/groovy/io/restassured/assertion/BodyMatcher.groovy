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


package io.restassured.assertion

import io.restassured.config.RestAssuredConfig
import io.restassured.internal.ResponseParserRegistrar
import io.restassured.response.Response
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.StringDescription
import org.hamcrest.xml.HasXPath
import org.w3c.dom.Element

import javax.xml.parsers.DocumentBuilderFactory

import static io.restassured.config.MatcherConfig.ErrorDescriptionType.REST_ASSURED
import static java.lang.String.format
import static org.apache.commons.lang3.StringUtils.*

class BodyMatcher {
  private static final String XPATH = "XPath"
  def key
  Matcher matcher
  ResponseParserRegistrar rpr

  def validate(Response response, contentParser, RestAssuredConfig config) {
    def success = true
    def errorMessage = ""

    contentParser = fallbackToResponseBodyIfContentParserIsNull(response, contentParser)
    if (key == null) {
      if (isXPathMatcher()) {
        def xmlConfig = config.getXmlConfig()
        boolean namespaceAware = xmlConfig.isNamespaceAware()
        Map<String, Boolean> features = xmlConfig.features()

        def factory = DocumentBuilderFactory.newInstance()
        factory.setNamespaceAware(namespaceAware)
        if (!features.isEmpty()) {
          features.each { featureName, isEnabled ->
            factory.setFeature(featureName, isEnabled)
          }
        }

        def properties = xmlConfig.properties()
        if (!properties.isEmpty()) {
          properties.each { name, value ->
            factory.setAttribute(name, value)
          }
        }

        Element node = factory.newDocumentBuilder().parse(new ByteArrayInputStream(response.asByteArray())).getDocumentElement()
        if (!matcher.matches(node)) {
          success = false
          if (config.matcherConfig.hasErrorDescriptionType(REST_ASSURED)) {
            errorMessage = format("Expected: %s\n  Actual: %s\n", trim(matcher.toString()), contentParser)
          } else {
            errorMessage = getDescription(matcher, contentParser)
          }
        }
      } else if (!matcher.matches(response.asString())) {
        success = false
        if (config.matcherConfig.hasErrorDescriptionType(REST_ASSURED)) {
          errorMessage = "Response body doesn't match expectation.\nExpected: $matcher\n  Actual: $contentParser\n"
        } else {
          errorMessage = format("Response body doesn't match expectation.\n%s", getDescription(matcher, response.asString()))
        }
      }
    } else {
      def assertion = StreamVerifier.newAssertion(response, key, rpr)
      def result = null
      if (contentParser != null) {
        if (contentParser instanceof String) {
          // This happens for example when expecting JSON/XML assertion but response content is empty
          def isEmpty = contentParser?.isEmpty()
          errorMessage = format("Cannot assert that path \"$key\" matches $matcher because the response body %s.", isEmpty ? "is empty" : "equal to \"$contentParser\"")
          success = false
        } else {
          result = assertion.getResult(contentParser, config)
        }
      }

      if (success && !matcher.matches(result)) {
        success = false
        if (config.matcherConfig.hasErrorDescriptionType(REST_ASSURED)) {
          if (result instanceof Object[]) {
            result = result.join(",")
          }
          errorMessage = format("%s %s doesn't match.\nExpected: %s\n  Actual: %s\n", assertion.description(), key, removeQuotesIfString(matcher.toString()), result)
        } else {
          errorMessage = format("%s %s doesn't match.\n%s", assertion.description(), key, getDescription(matcher, result))
        }
      }
    }
    return [success: success, errorMessage: errorMessage]
  }

  private static String getDescription(Matcher matcher, Object actual) {
    Description description = new StringDescription()
    description.appendText("\nExpected: ")
            .appendDescriptionOf(matcher)
            .appendText("\n  Actual: ")
    matcher.describeMismatch(actual, description)
    return description.toString()
  }

  private static String removeQuotesIfString(String string) {
    if (startsWith(string, "\"") && endsWith(string, "\"")) {
      def start = removeStart(string, "\"")
      string = removeEnd(start, "\"")
    }
    string
  }

  static def fallbackToResponseBodyIfContentParserIsNull(Response response, contentParser) {
    if (contentParser == null) {
      return response.asString()
    }
    return contentParser
  }

  private boolean isXPathMatcher() {
    def isNestedMatcherContainingXPathMatcher = {
      def description = new StringDescription()
      matcher.describeTo(description)
      description.toString().contains(XPATH)
    }

    matcher instanceof HasXPath || isNestedMatcherContainingXPathMatcher()
  }

  boolean requiresTextParsing() {
    key == null || isXPathMatcher()
  }

  boolean requiresPathParsing() {
    !requiresTextParsing()
  }
}

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

package com.jayway.restassured.assertion

import com.jayway.restassured.internal.ResponseParserRegistrar
import com.jayway.restassured.response.Response
import javax.xml.parsers.DocumentBuilderFactory
import org.hamcrest.Matcher
import org.hamcrest.xml.HasXPath
import org.w3c.dom.Element
import static org.apache.commons.lang3.StringUtils.*

class BodyMatcher {
  def key
  def Matcher matcher
  def ResponseParserRegistrar rpr

  def isFulfilled(Response response, content) {
    content = fallbackToResponseBodyIfContentHasAlreadyBeenRead(response, content)
    if(key == null) {
      if(isXPathMatcher()) {
        Element node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(response.asByteArray())).getDocumentElement();
        if (matcher.matches(node) == false) {
          throw new AssertionError(String.format("Body doesn't match.\nExpected:\n%s\nActual:\n%s", matcher.toString(), content))
        }
      } else if (!matcher.matches(response.asString())) {
        throw new AssertionError("Body doesn't match.\nExpected:\n$matcher\nActual:\n$content")
      }
    } else {
      def assertion = StreamVerifier.newAssertion(response, key, rpr)
      def result = null
      if(content != null) {
        result = assertion.getResult(content)
      }
      if (!matcher.matches(result)) {
        if(result instanceof Object[]) {
          result = result.join(",")
        }
        throw new AssertionError(String.format("%s %s doesn't match.\nExpected: %s\n  Actual: %s\n", assertion.description(), key, removeQuotesIfString(matcher.toString()), result))
      }
    }
  }

  private String removeQuotesIfString(String string) {
    if(startsWith(string, "\"") && endsWith(string, "\"")) {
      def start = removeStart(string, "\"")
      string = removeEnd(start, "\"")
    }
    string
  }

  def fallbackToResponseBodyIfContentHasAlreadyBeenRead(Response response, content) {
    if(content instanceof Reader || content instanceof InputStream) {
      return response.asString()
    }
    return  content
  }

  private boolean isXPathMatcher() {
    matcher instanceof HasXPath
  }

  def boolean requiresTextParsing() {
    isXPathMatcher() || key == null
  }

  def String getDescription() {
    String description = ""
    if(key) {
      description = "Body containing expression \"$key\" must match $matcher"
    } else {
      description = "Body must match $matcher"
    }
    return description
  }
}

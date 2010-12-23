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

import com.jayway.restassured.exception.AssertionFailedException
import javax.xml.parsers.DocumentBuilderFactory
import org.hamcrest.Matcher
import org.hamcrest.xml.HasXPath
import org.w3c.dom.Element
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.XML

class BodyMatcher {
  def key
  def Matcher matcher

  def isFulfilled(response, content) {
    if(key == null) {
      if(isXPathMatcher()) {
        Element node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(new String(content).getBytes())).getDocumentElement();
        if (matcher.matches(node) == false) {
          throw new AssertionFailedException(String.format("Body doesn't match.\nExpected:\n%s\nActual:\n%s", matcher.toString(), content))
        }
      } else if (!matcher.matches(content)) {
        throw new AssertionFailedException("Body doesn't match.\nExpected:\n$matcher\nActual:\n$content")
      }
    } else {
      def assertion;
      switch (response.contentType.toString().toLowerCase()) {
        case JSON.toString().toLowerCase():
          assertion = new JSONAssertion(key: key)
          break
        case XML.toString().toLowerCase():
          assertion = new XMLAssertion(key: key)
          break;
        default:
        throw new IllegalStateException("Expected response to have JSON or XML content type but got "+response.contentType+ ". Content was:\n$content\n")
        break;
      }
      def result = null
      if(content != null) {
        result = assertion.getResult(content)
      }
      if (!matcher.matches(result)) {
        if(result instanceof Object[]) {
          result = result.join(",")
        }
        throw new AssertionFailedException(String.format("%s %s doesn't match %s, was <%s>.", assertion.description(), key, matcher.toString(), result))
      }
    }
  }

  private boolean isXPathMatcher() {
    matcher instanceof HasXPath
  }

  def boolean requiresContentTypeText() {
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

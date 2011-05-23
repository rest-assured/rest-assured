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

package com.jayway.restassured.assertion

import com.jayway.restassured.internal.ResponseParserRegistrar
import com.jayway.restassured.parsing.Parser
import groovyx.net.http.ContentType
import static groovyx.net.http.ContentType.*
import com.jayway.restassured.response.Response

class StreamVerifier {

  def static newAssertion(Response response, key) {
    def contentType = response.getContentType()
    def assertion
    if(contentTypeMatch(JSON, contentType) ) {
      assertion = new JSONAssertion(key: key)
    } else if(contentTypeMatch(XML, contentType)) {
      assertion = new XMLAssertion(key: key)
    } else if(contentTypeMatch(HTML, contentType)) {
      assertion = new XMLAssertion(key: key)
    } else if(hasCustomParser(contentType)) {
      assertion = createAssertionForCustomParser(contentType, key)
    } else {
      def content = response.asString()
      throw new IllegalStateException("""Expected response to be verified as JSON, HTML or XML but content-type '$contentType' is not supported out of the box.
Try registering a custom parser using:
   RestAssured.registerParser(<parser type>, \"$contentType\");
Content was:\n$content\n""");
    }
    assertion
  }

  static def createAssertionForCustomParser(String contentType, key) {
    def parser = ResponseParserRegistrar.getParser(contentType);
    def assertion
    switch(parser) {
      case Parser.XML:
        assertion = new XMLAssertion(key: key)
        break;
      case Parser.JSON:
        assertion = new JSONAssertion(key: key)
        break;
      case Parser.HTML:
        assertion = new XMLAssertion(key: key, toUpperCase: true)
        break;
    }
    assertion
  }

  private static boolean hasCustomParser(String contentType) {
    def parser = ResponseParserRegistrar.getParser(contentType)
    return parser != null && (parser == Parser.XML || parser == Parser.JSON || parser == Parser.HTML);
  }

  private static boolean contentTypeMatch(ContentType expectedContentType, String actualContentType) {
    def types = expectedContentType.getContentTypeStrings();
    for(String type : types) {
      if(type == actualContentType) return true
    }
    return false
  }
}

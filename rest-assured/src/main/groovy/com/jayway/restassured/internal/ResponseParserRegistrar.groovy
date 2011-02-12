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

package com.jayway.restassured.internal

import groovyx.net.http.ContentType
import static groovyx.net.http.ContentType.*

/**
 * Takes care of registering additional content types to the parser registry as well as
 * preparing for forced text parsing when applicable.
 */
class ResponseParserRegistrar {
  private final additional = ['application/rss+xml' : 'application/xml', 'application/xhtml+xml' : 'text/html']

  def forceTextParsing

  def void registerParsers(http) {
    if(forceTextParsing) {
      parseResponsesWithBodyParser(http)
    }
  }

  private def void parseResponsesWithBodyParser(http) {
    def plainText = http.parser.'text/plain'
    registerContentTypeToParsedAs(http, XML, plainText)
    registerContentTypeToParsedAs(http, HTML, plainText)
    registerContentTypeToParsedAs(http, JSON, plainText)
    registerContentTypeToParsedAs(http, ANY, plainText)
    registerAllAdditionalContentTypesToBeParsedAs(http, plainText)
  }

  void registerAllAdditionalContentTypesToBeParsedAs(http, toBeParsedAsContentType) {
    additional.each { type, value ->
      http.parser.putAt(type, toBeParsedAsContentType)
    }
  }

  private static void registerContentTypeToParsedAs(http, ContentType contentType, toBeParsedAsContentType) {
    def types = contentType.getContentTypeStrings();
    for(String type : types) {
      http.parser.putAt(type, toBeParsedAsContentType)
    }
  }
}

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

import com.jayway.restassured.parsing.Parser
import groovyx.net.http.ContentType
import static com.jayway.restassured.assertion.AssertParameter.notNull
import static groovyx.net.http.ContentType.*

/**
 * Takes care of registering additional content types to the parser registry as well as
 * preparing for forced text parsing when applicable.
 */
class ResponseParserRegistrar {
  private final Map<String, String> additional = ['application/rss+xml' : 'application/xml', 'atom+xml' : 'application/xml',
          'xop+xml' : 'application/xml', 'xslt+xml' : 'application/xml', 'rdf+xml' : 'application/xml',
          'atomcat+xml' : 'application/xml', 'atomsvc+xml' : 'application/xml', 'auth-policy+xml' : 'application/xml']

  def ResponseParserRegistrar(){

  }

  def ResponseParserRegistrar(ResponseParserRegistrar rpr){
    this.additional.putAll(rpr.additional)
  }


  def Parser getParser(String contentType) {
    def parserAsString = additional.get(contentType)
    parserAsString == null ? null : Parser.fromContentType(parserAsString)
  }

  def void registerParser(String contentType, Parser parser) {
    notNull(parser, "Parser")
    notNull(contentType, "contentType")
    additional.put(contentType, parser.getContentType())
  }

  def void unregisterParser(String contentType) {
    notNull(contentType, "contentType")
    additional.remove(contentType)
  }

  def boolean hasCustomParser(String contentType) {
    def parser = getParser(contentType)
    return parser != null && (parser == Parser.XML || parser == Parser.JSON || parser == Parser.HTML);
  }

  def void registerParsers(http, forceTextParsing) {
    if(forceTextParsing) {
      parseResponsesWithBodyParser(http)
    } else {
      additional.each { type, value ->
        http.parser.putAt(type, http.parser.getAt(value))
      }
    }
  }

  private def void parseResponsesWithBodyParser(http) {
    def plainText = http.parser.'text/plain'
    registerContentTypeToBeParsedAs(http, XML, plainText)
    registerContentTypeToBeParsedAs(http, HTML, plainText)
    registerContentTypeToBeParsedAs(http, JSON, plainText)
    registerContentTypeToBeParsedAs(http, ANY, plainText)
    registerAllAdditionalContentTypesToBeParsedAs(http, plainText)
  }

  private void registerAllAdditionalContentTypesToBeParsedAs(http, toBeParsedAsContentType) {
    additional.each { type, value ->
      http.parser.putAt(type, toBeParsedAsContentType)
    }
  }

  private void registerContentTypeToBeParsedAs(http, ContentType contentType, toBeParsedAsContentType) {
    def types = contentType.getContentTypeStrings();
    for(String type : types) {
      http.parser.putAt(type, toBeParsedAsContentType)
    }
  }
}
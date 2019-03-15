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

import io.restassured.parsing.Parser

import static io.restassured.internal.common.assertion.AssertParameter.notNull
import static io.restassured.internal.http.ContentTypeExtractor.getContentTypeWithoutCharset

/**
 * Takes care of registering additional content types to the parser registry as well as
 * preparing for forced text parsing when applicable.
 */
class ResponseParserRegistrar {
    private final Map<String, String> additional = ['application/rss+xml' : 'application/xml', 'atom+xml' : 'application/xml',
            'xop+xml' : 'application/xml', 'xslt+xml' : 'application/xml', 'rdf+xml' : 'application/xml',
            'atomcat+xml' : 'application/xml', 'atomsvc+xml' : 'application/xml', 'auth-policy+xml' : 'application/xml']

    private Parser defaultParser = null

    def ResponseParserRegistrar(){

    }

    def ResponseParserRegistrar(ResponseParserRegistrar rpr){
        this.additional.putAll(rpr.additional)
        this.defaultParser = rpr.defaultParser
    }

    def Parser getParser(String contentType) {
        def contentTypeWithoutCharset = getContentTypeWithoutCharset(contentType);
        def parserAsString = additional.get(contentTypeWithoutCharset)
        def parser = parserAsString == null ? Parser.fromContentType(contentType) : Parser.fromContentType(parserAsString)
        parser == null ? defaultParser : parser
    }

    def Parser getNonDefaultParser(String contentType) {
        def contentTypeWithoutCharset = getContentTypeWithoutCharset(contentType);
        def parserAsString = additional.get(contentTypeWithoutCharset)
        def parser = parserAsString == null ? null : Parser.fromContentType(parserAsString)
        parser == null ? defaultParser : parser
    }

    def void registerParser(String contentType, Parser parser) {
        notNull(parser, "Parser")
        notNull(contentType, "contentType")
        def contentTypeWithoutCharset = getContentTypeWithoutCharset(contentType);
        additional.put(contentTypeWithoutCharset, parser.getContentType())
    }

    def void registerDefaultParser(Parser parser) {
        notNull(parser, "Parser")
        this.defaultParser = parser
    }

    def void unregisterParser(String contentType) {
        notNull(contentType, "contentType")
        additional.remove(contentType)
    }

    def boolean hasCustomParser(String contentType) {
        if(defaultParser != null) {
            return true
        }
        return hasCustomParserExcludingDefaultParser(contentType)
    }

    def boolean hasCustomParserExcludingDefaultParser(String contentType) {
        def parser = getNonDefaultParser(contentType)
        return parser != null && (parser == Parser.XML || parser == Parser.JSON || parser == Parser.HTML);
    }
}
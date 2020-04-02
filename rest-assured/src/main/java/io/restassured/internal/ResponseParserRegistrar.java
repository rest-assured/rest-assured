/*
 * Copyright 2020 the original author or authors.
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

package io.restassured.internal;

import io.restassured.parsing.Parser;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.http.ContentTypeExtractor.getContentTypeWithoutCharset;

public class ResponseParserRegistrar {
    private final Map<String, String> additional = new HashMap<String, String>() {{
        put("application/rss+xml", "application/xml");
        put("atom+xml", "application/xml");
        put("xop+xml", "application/xml");
        put("xslt+xml", "application/xml");
        put("rdf+xml", "application/xml");
        put("atomcat+xml", "application/xml");
        put("atomsvc+xml", "application/xml");
        put("auth-policy+xml", "application/xml");
    }};

    private Parser defaultParser = null;

    public ResponseParserRegistrar() {
    }

    public ResponseParserRegistrar(ResponseParserRegistrar rpr) {
        this.additional.putAll(rpr.additional);
        this.defaultParser = rpr.defaultParser;
    }

    public Parser getParser(String contentType) {
        String contentTypeWithoutCharset = getContentTypeWithoutCharset(contentType);
        String parserAsString = additional.get(contentTypeWithoutCharset);
        Parser parser = parserAsString == null ? Parser.fromContentType(contentType) : Parser.fromContentType(parserAsString);
        return parser == null ? defaultParser : parser;
    }

    public Parser getNonDefaultParser(String contentType) {
        String contentTypeWithoutCharset = getContentTypeWithoutCharset(contentType);
        String parserAsString = additional.get(contentTypeWithoutCharset);
        Parser parser = parserAsString == null ? null : Parser.fromContentType(parserAsString);
        return parser == null ? defaultParser : parser;
    }

    public void registerParser(String contentType, Parser parser) {
        notNull(parser, "Parser");
        notNull(contentType, "contentType");
        String contentTypeWithoutCharset = getContentTypeWithoutCharset(contentType);
        additional.put(contentTypeWithoutCharset, parser.getContentType());
    }

    public void registerDefaultParser(Parser parser) {
        notNull(parser, "Parser");
        this.defaultParser = parser;
    }

    public void unregisterParser(String contentType) {
        notNull(contentType, "contentType");
        additional.remove(contentType);
    }

    public boolean hasCustomParser(String contentType) {
        if (defaultParser != null) {
            return true;
        }
        return hasCustomParserExcludingDefaultParser(contentType);
    }

    public boolean hasCustomParserExcludingDefaultParser(String contentType) {
        Parser parser = getNonDefaultParser(contentType);
        return parser != null && (parser == Parser.XML || parser == Parser.JSON || parser == Parser.HTML);
    }
}
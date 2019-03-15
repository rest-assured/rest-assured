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

package io.restassured.parsing;

import io.restassured.internal.http.ContentTypeExtractor;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;

/**
 * The different parsers that are provided by REST Assured.
 */
public enum Parser {
    XML("application/xml","text/xml","application/xhtml+xml"), TEXT("text/plain", "*/*"),
    JSON("application/json","application/javascript","text/javascript", "text/json"), HTML("text/html");

    private static final String PLUS_XML = "+xml";
    private static final String PLUS_JSON = "+json";
    private static final String PLUS_HTML = "+html";

    private final String[] contentTypes;

    Parser(String... contentTypes) {
        this.contentTypes = contentTypes;
    }

    public String getContentType() {
        return contentTypes[0];
    }

    public static Parser fromContentType(String contentType) {
        if(contentType == null) {
            return null;
        }
        contentType = ContentTypeExtractor.getContentTypeWithoutCharset(contentType.toLowerCase());
        final Parser foundParser;
        if(contains(XML.contentTypes, contentType) || endsWithIgnoreCase(contentType, PLUS_XML)) {
            foundParser = XML;
        } else if(contains(JSON.contentTypes, contentType) || endsWithIgnoreCase(contentType, PLUS_JSON)) {
            foundParser = JSON;
        } else if(contains(TEXT.contentTypes, contentType)) {
            foundParser = TEXT;
        } else if(contains(HTML.contentTypes, contentType) || endsWithIgnoreCase(contentType, PLUS_HTML)) {
            foundParser = HTML;
        } else {
            foundParser = null;
        }
        return foundParser;
    }
}

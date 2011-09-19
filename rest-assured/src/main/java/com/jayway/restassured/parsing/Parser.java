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

package com.jayway.restassured.parsing;

/**
 * The different parsers that are provided by REST Assured.
 */
public enum Parser {
    XML("application/xml"), TEXT("text/plain"), JSON("application/json"), HTML("text/html");

    private final String contentType;

    Parser(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public static Parser fromContentType(String contentType) {
        final Parser foundParser;
        if(XML.getContentType().equals(contentType)) {
            foundParser = XML;
        } else if(JSON.getContentType().equals(contentType)) {
            foundParser = JSON;
        } else if(TEXT.getContentType().equals(contentType)) {
            foundParser = TEXT;
        } else if(HTML.getContentType().equals(contentType)) {
            foundParser = HTML;
        } else {
            throw new IllegalArgumentException("Cannot find a parser for content-type "+contentType);
        }
        return foundParser;
    }
}

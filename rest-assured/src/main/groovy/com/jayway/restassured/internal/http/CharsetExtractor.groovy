/*
 * Copyright 2013 the original author or authors.
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


package com.jayway.restassured.internal.http

import org.apache.commons.lang3.StringUtils

import static org.apache.commons.lang3.StringUtils.isBlank

class CharsetExtractor {

    private static final String CHARSET = "charset"

    public static String getCharsetFromContentType(String contentType) {
        def foundCharset = null
        if(isBlank(contentType)) {
            foundCharset = null;
        } else if(StringUtils.containsIgnoreCase(contentType, CHARSET)) {
            contentType.split(";").each {
                if(StringUtils.containsIgnoreCase(contentType, CHARSET)) {
                    def questionMark = it.split("=")
                    if(questionMark != null && questionMark.length == 2 && questionMark[0].trim().equalsIgnoreCase("charset")) {
                        foundCharset = questionMark[1]?.trim();
                        //remove quotations if present
                        foundCharset = StringUtils.removeStart(foundCharset, "\"")
                        foundCharset = StringUtils.removeEnd(foundCharset, "\"")
                    }
                }
            }
        }
        foundCharset;
    }
}

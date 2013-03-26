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
import com.jayway.restassured.internal.http.ContentTypeExtractor
import com.jayway.restassured.parsing.Parser
import com.jayway.restassured.internal.path.json.JSONAssertion
import com.jayway.restassured.response.Response

class StreamVerifier {

    def static newAssertion(Response response, key, ResponseParserRegistrar rpr) {
        def contentType = response.getContentType()
        def parserType = Parser.fromContentType(contentType)
        def assertion
        if(rpr.hasCustomParser(contentType)) {
            assertion = createAssertionForCustomParser(rpr, contentType, key)
        } else if(parserType == Parser.JSON) {
            assertion = new JSONAssertion(key: key)
        } else if(parserType == Parser.XML || parserType == Parser.HTML) {
            assertion = new XMLAssertion(key: key)
        } else {
            def content = response.asString()
            if(contentType.isEmpty()) {
                throw new IllegalStateException("""Expected response body to be verified as JSON, HTML or XML but no content-type was defined in the response.
Try registering a default parser using:
   RestAssured.defaultParser(<parser type>);
Content was:\n$content\n""");
            }
            def contentTypeWithCharset = ContentTypeExtractor.getContentTypeWithoutCharset(contentType)
            throw new IllegalStateException("""Expected response body to be verified as JSON, HTML or XML but content-type '$contentTypeWithCharset' is not supported out of the box.
Try registering a custom parser using:
   RestAssured.registerParser(\"$contentTypeWithCharset\", <parser type>);
Content was:\n$content\n""");
        }
        assertion
    }

    static def createAssertionForCustomParser(ResponseParserRegistrar rpr, String contentType, key) {
        def parser = rpr.getNonDefaultParser(contentType);
        def assertion = null
        switch(parser) {
            case Parser.JSON:
                assertion = new JSONAssertion(key: key)
                break;
            case Parser.XML:
            case Parser.HTML:
                assertion = new XMLAssertion(key: key)
                break;
        }
        assertion
    }
}

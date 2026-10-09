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

package io.restassured.assertion;

import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.http.ContentTypeExtractor;
import io.restassured.internal.path.json.JSONAssertion;
import io.restassured.internal.path.xml.XMLAssertion;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

import static io.restassured.internal.util.GroovyStringConversion.castToString;

public class StreamVerifier {

    public static Object newAssertion(Response response, Object key, ResponseParserRegistrar rpr) {
        String contentType = response.getContentType();
        Parser parserType = Parser.fromContentType(contentType);
        if (rpr.hasCustomParser(contentType)) {
            return createAssertionForCustomParser(rpr, contentType, key);
        } else if (parserType == Parser.JSON) {
            return jsonAssertion(key);
        } else if (parserType == Parser.XML || parserType == Parser.HTML) {
            return xmlAssertion(key);
        }
        String content = response.asString();
        if (contentType != null && contentType.isEmpty()) {
            throw new IllegalStateException("Expected response body to be verified as JSON, HTML or XML but no content-type was defined in the response.\n" +
                    "Try registering a default parser using:\n" +
                    "   RestAssured.defaultParser(<parser type>);\n" +
                    "Content was:\n" + content + "\n");
        }
        String contentTypeWithoutCharset = ContentTypeExtractor.getContentTypeWithoutCharset(contentType);
        throw new IllegalStateException("Expected response body to be verified as JSON, HTML or XML but content-type '" + contentTypeWithoutCharset + "' is not supported out of the box.\n" +
                "Try registering a custom parser using:\n" +
                "   RestAssured.registerParser(\"" + contentTypeWithoutCharset + "\", <parser type>);\n" +
                "Content was:\n" + content + "\n");
    }

    public static Object createAssertionForCustomParser(ResponseParserRegistrar rpr, String contentType, Object key) {
        Parser parser = rpr.getNonDefaultParser(contentType);
        if (parser == Parser.JSON) {
            return jsonAssertion(key);
        } else if (parser == Parser.XML || parser == Parser.HTML) {
            return xmlAssertion(key);
        }
        return null;
    }

    private static JSONAssertion jsonAssertion(Object key) {
        JSONAssertion assertion = new JSONAssertion();
        assertion.setKey(castToString(key));
        return assertion;
    }

    private static XMLAssertion xmlAssertion(Object key) {
        XMLAssertion assertion = new XMLAssertion();
        assertion.setKey(castToString(key));
        return assertion;
    }
}

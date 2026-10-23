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
package io.restassured.internal;

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.internal.path.CorePathSettings;
import io.restassured.internal.path.PathProviders;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

public class ContentParser {

    /**
     * Parse the response body into the document that the path engine evaluates the paths of body assertions on.
     *
     * @return The document of the path engine (for GPath, maps and lists for JSON and a parsed document for XML and HTML),
     * or the response body as an {@link java.io.InputStream} for content types that are not parsed.
     */
    public Object parse(Response response, ResponseParserRegistrar rpr, RestAssuredConfig config, boolean parseAsString) {
        return parse(response, rpr, config, parseAsString, null);
    }

    /**
     * @param path The first path that will be evaluated on the document, for the message when no path engine is on the
     *             classpath, or {@code null} if it isn't known
     * @see #parse(Response, ResponseParserRegistrar, RestAssuredConfig, boolean)
     */
    public Object parse(Response response, ResponseParserRegistrar rpr, RestAssuredConfig config, boolean parseAsString, String path) {
        Parser parser = rpr.getParser(response.contentType());
        if (parser == null) {
            return response.asInputStream();
        }
        DocumentType type;
        PathInput input;
        switch (parser) {
            case JSON:
                type = DocumentType.JSON;
                input = jsonInput(response, config, parseAsString);
                break;
            case XML:
            case HTML:
                type = parser == Parser.XML ? DocumentType.XML : DocumentType.HTML;
                // We force default charset to be backward compatible with "InputStream charset".
                // Only REST Assured's own responses are parsed as string (eagerly), see ResponseSpecificationImpl.
                input = parseAsString ? PathInput.ofText(((RestAssuredResponseOptionsImpl<?>) response).asString(true)) : PathInput.ofStream(response.asInputStream(), null);
                break;
            case TEXT:
            default:
                return response.asInputStream();
        }
        try {
            return PathProviders.forType(type, config, path).parse(input, type, CorePathSettings.forBodyAssertions(config));
        } catch (Exception e) {
            // Rethrow checked exceptions (such as SAXParseException) as is, without wrapping them
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    private static PathInput jsonInput(Response response, RestAssuredConfig config, boolean parseAsString) {
        if (parseAsString) {
            return PathInput.ofText(response.asString());
        }
        String charset = CharsetExtractor.getCharsetFromContentType(response.getContentType());
        if (charset == null || charset.isEmpty()) {
            charset = config.getDecoderConfig().defaultCharsetForContentType(response.getContentType());
        }
        return PathInput.ofStream(response.asInputStream(), charset);
    }
}

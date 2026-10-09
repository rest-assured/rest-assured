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

import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.XmlConfig;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.internal.path.json.ConfigurableJsonSlurper;
import io.restassured.internal.path.xml.XmlSlurperParser;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ContentParser {

    /**
     * Parse the response body into the object model that GPath expressions in body assertions are evaluated against.
     *
     * @return A JSON object model (maps and lists), a parsed XML/HTML document, or the response body as an
     * {@link java.io.InputStream} for content types that are not parsed.
     */
    public Object parse(Response response, ResponseParserRegistrar rpr, RestAssuredConfig config, boolean parseAsString) {
        Parser parser = rpr.getParser(response.contentType());
        if (parser == null) {
            return response.asInputStream();
        }
        try {
            switch (parser) {
                case JSON:
                    return parseJson(response, config, parseAsString);
                case XML:
                    return parseXml(response, config.getXmlConfig(), false, parseAsString);
                case HTML:
                    return parseXml(response, config.getXmlConfig(), true, parseAsString);
                case TEXT:
                default:
                    return response.asInputStream();
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            // Rethrow checked exceptions (such as SAXParseException) as is, without wrapping them
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    private static Object parseJson(Response response, RestAssuredConfig config, boolean parseAsString) throws IOException {
        JsonConfig jsonConfig = config.getJsonConfig();
        ConfigurableJsonSlurper slurper = new ConfigurableJsonSlurper(jsonConfig.numberReturnType(), jsonConfig.numberLengthLimit());
        if (parseAsString) {
            return slurper.parseText(response.asString());
        }
        String charset = CharsetExtractor.getCharsetFromContentType(response.getContentType());
        if (charset == null || charset.isEmpty()) {
            charset = config.getDecoderConfig().defaultCharsetForContentType(response.getContentType());
        }
        return slurper.parse(new InputStreamReader(new BufferedInputStream(response.asInputStream()), charset));
    }

    private static Object parseXml(Response response, XmlConfig xmlConfig, boolean html, boolean parseAsString)
            throws IOException, SAXException, ParserConfigurationException {
        // We force default charset to be backward compatible with "InputStream charset".
        // Only REST Assured's own responses are parsed as string (eagerly), see ResponseSpecificationImpl.
        Object document = parseAsString ? ((RestAssuredResponseOptionsImpl<?>) response).asString(true) : response.asInputStream();
        return XmlSlurperParser.parse(document, html, xmlConfig.isValidating(), xmlConfig.isNamespaceAware(), xmlConfig.isAllowDocTypeDeclaration(),
                xmlConfig.properties(), xmlConfig.features(), xmlConfig.isNamespaceAware() ? xmlConfig.declaredNamespaces() : null);
    }
}

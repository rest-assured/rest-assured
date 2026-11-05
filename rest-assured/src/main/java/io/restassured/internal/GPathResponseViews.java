/*
 * Copyright 2026 the original author or authors.
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

import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonParserType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.XmlPath.CompatibilityMode;
import io.restassured.path.xml.config.XmlPathConfig;

import static io.restassured.path.json.config.JsonPathConfig.jsonPathConfig;
import static io.restassured.path.xml.config.XmlPathConfig.xmlPathConfig;

/**
 * Creates the {@link JsonPath} and {@link XmlPath} of a response body. json-path-gpath and xml-path-gpath are optional,
 * so this is the only class whose code uses them. Call it only after {@code PathProviders.requireGPath(..)}.
 */
final class GPathResponseViews {

    private GPathResponseViews() {
    }

    /**
     * The JsonPath of a response deserializes with the object mapper type and factories of the request's
     * {@link ObjectMapperConfig}, so that its <code>getObject(..)</code> and the response's <code>as(..)</code> agree.
     * There's no Johnzon factory to pass: neither config can set one.
     */
    static JsonPath jsonPath(String body, String charset, RestAssuredConfig config) {
        ObjectMapperConfig objectMapperConfig = config.getObjectMapperConfig();
        return jsonPath(body, jsonPathConfig().charset(charset).
                defaultParserType(jsonParserType(objectMapperConfig.defaultObjectMapperType())).
                jackson2ObjectMapperFactory(objectMapperConfig.jackson2ObjectMapperFactory()).
                jackson3ObjectMapperFactory(objectMapperConfig.jackson3ObjectMapperFactory()).
                gsonObjectMapperFactory(objectMapperConfig.gsonObjectMapperFactory()).
                jsonbObjectMapperFactory(objectMapperConfig.jsonbObjectMapperFactory()).
                numberReturnType(config.getJsonConfig().numberReturnType()));
    }

    /**
     * @return The parser type of the object mapper type, or <code>null</code>, so that JsonPath picks one from the
     * classpath, for no type and for JAXB and Jakarta EE, which only map XML.
     */
    private static JsonParserType jsonParserType(ObjectMapperType objectMapperType) {
        if (objectMapperType == null) {
            return null;
        }
        return switch (objectMapperType) {
            case JACKSON_3 -> JsonParserType.JACKSON_3;
            case JACKSON_2 -> JsonParserType.JACKSON_2;
            case GSON -> JsonParserType.GSON;
            case JOHNZON -> JsonParserType.JOHNZON;
            case JSONB -> JsonParserType.JSONB;
            case JAXB, JAKARTA_EE -> null;
        };
    }

    static JsonPath jsonPath(String body, JsonPathConfig config) {
        return new JsonPath(body).using(config);
    }

    static XmlPath xmlPath(String body, String charset, RestAssuredConfig config) {
        return xmlPath(CompatibilityMode.XML, body, charset, config);
    }

    static XmlPath htmlPath(String body, String charset, RestAssuredConfig config) {
        return xmlPath(CompatibilityMode.HTML, body, charset, config);
    }

    static XmlPath xmlPath(CompatibilityMode mode, String body, String charset, RestAssuredConfig config) {
        return xmlPath(mode, body, xmlPathConfig().charset(charset).
                features(config.getXmlConfig().features()).
                properties(config.getXmlConfig().properties()).
                declareNamespaces(config.getXmlConfig().declaredNamespaces()).
                jaxbObjectMapperFactory(config.getObjectMapperConfig().jaxbObjectMapperFactory()));
    }

    static XmlPath xmlPath(String body, XmlPathConfig config) {
        return xmlPath(CompatibilityMode.XML, body, config);
    }

    static XmlPath xmlPath(CompatibilityMode mode, String body, XmlPathConfig config) {
        return new XmlPath(mode, body).using(config);
    }
}

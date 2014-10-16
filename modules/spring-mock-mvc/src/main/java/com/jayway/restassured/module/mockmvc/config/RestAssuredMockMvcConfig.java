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

package com.jayway.restassured.module.mockmvc.config;

import com.jayway.restassured.config.*;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

/**
 * Main configuration for REST Assured Mock MVC that allows you to configure advanced settings.
 * <p>
 * Usage example:
 * <pre>
 * RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(new LogConfig(captor, true));
 * </pre>
 * </p>
 */
public class RestAssuredMockMvcConfig {

    private final LogConfig logConfig;
    private final EncoderConfig encoderConfig;
    private final DecoderConfig decoderConfig;
    private final SessionConfig sessionConfig;
    private final ObjectMapperConfig objectMapperConfig;
    private final JsonConfig jsonConfig;
    private final XmlConfig xmlConfig;

    /**
     * Create a new RestAssuredConfiguration with the default configurations.
     */
    public RestAssuredMockMvcConfig() {
        this(new LogConfig(), new EncoderConfig(), new DecoderConfig(), new SessionConfig(), new ObjectMapperConfig(), new JsonConfig(), new XmlConfig());
    }

    /**
     * Create a new RestAssuredMockMvcConfig with the supplied configs.
     */
    public RestAssuredMockMvcConfig(LogConfig logConfig,
                                    EncoderConfig encoderConfig,
                                    DecoderConfig decoderConfig,
                                    SessionConfig sessionConfig,
                                    ObjectMapperConfig objectMapperConfig,
                                    JsonConfig jsonConfig,
                                    XmlConfig xmlConfig) {
        notNull(logConfig, "Log config");
        notNull(encoderConfig, "Encoder config");
        notNull(decoderConfig, "Decoder config");
        notNull(sessionConfig, "Session config");
        notNull(objectMapperConfig, "Object mapper config");
        notNull(jsonConfig, "Json config");
        notNull(xmlConfig, "Xml config");
        this.logConfig = logConfig;
        this.encoderConfig = encoderConfig;
        this.decoderConfig = decoderConfig;
        this.sessionConfig = sessionConfig;
        this.objectMapperConfig = objectMapperConfig;
        this.jsonConfig = jsonConfig;
        this.xmlConfig = xmlConfig;
    }

    /**
     * Set the Log config.
     *
     * @param logConfig The {@link com.jayway.restassured.config.LogConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredMockMvcConfig logConfig(LogConfig logConfig) {
        notNull(logConfig, "Log config");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig, objectMapperConfig, jsonConfig, xmlConfig);
    }

    /**
     * Set the session config.
     *
     * @param sessionConfig The {@link com.jayway.restassured.config.SessionConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredMockMvcConfig sessionConfig(SessionConfig sessionConfig) {
        notNull(sessionConfig, "Session config");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig, objectMapperConfig, jsonConfig, xmlConfig);
    }

    /**
     * Set the object mapper config.
     *
     * @param objectMapperConfig The {@link com.jayway.restassured.config.ObjectMapperConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredMockMvcConfig objectMapperConfig(ObjectMapperConfig objectMapperConfig) {
        notNull(objectMapperConfig, "Object mapper config");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig, objectMapperConfig, jsonConfig, xmlConfig);
    }

    /**
     * Set the Json config.
     *
     * @param jsonConfig The {@link com.jayway.restassured.config.JsonConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredMockMvcConfig jsonConfig(JsonConfig jsonConfig) {
        notNull(jsonConfig, "JsonConfig");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig);
    }

    /**
     * Set the Xml config.
     *
     * @param xmlConfig The {@link com.jayway.restassured.config.XmlConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredMockMvcConfig xmlConfig(XmlConfig xmlConfig) {
        notNull(xmlConfig, "XmlConfig");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig);
    }

    /**
     * Set the encoder config
     *
     * @param encoderConfig The {@link com.jayway.restassured.config.XmlConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredMockMvcConfig encoderConfig(EncoderConfig encoderConfig) {
        notNull(encoderConfig, "EncoderConfig");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredMockMvcConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredMockMvcConfig set() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredMockMvcConfig with() {
        return this;
    }

    /**
     * @return The LogConfig
     */
    public LogConfig getLogConfig() {
        return logConfig;
    }

    /**
     * @return The EncoderConfig
     */
    public EncoderConfig getEncoderConfig() {
        return encoderConfig;
    }

    /**
     * @return The DecoderConfig
     */
    public DecoderConfig getDecoderConfig() {
        return decoderConfig;
    }

    /**
     * @return The SessionConfig
     */
    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

    /**
     * @return The ObjectMapperConfig
     */
    public ObjectMapperConfig getObjectMapperConfig() {
        return objectMapperConfig;
    }

    /**
     * @return The JsonPath Config
     */
    public JsonConfig getJsonConfig() {
        return jsonConfig;
    }

    /**
     * @return The Xml Config
     */
    public XmlConfig getXmlConfig() {
        return xmlConfig;
    }

    /**
     * @return A static way to create a new RestAssuredMockMvcConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static RestAssuredMockMvcConfig newConfig() {
        return new RestAssuredMockMvcConfig();
    }

    /**
     * @return A static way to create a new RestAssuredMockMvcConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static RestAssuredMockMvcConfig config() {
        return new RestAssuredMockMvcConfig();
    }
}
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

package io.restassured.module.mockmvc.config;

import io.restassured.config.*;
import io.restassured.module.spring.commons.config.AsyncConfig;
import io.restassured.module.spring.commons.config.ClientConfig;
import io.restassured.module.spring.commons.config.SpecificationConfig;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Main configuration for REST Assured Mock MVC that allows you to configure advanced settings.
 * <p>
 * Usage example:
 * <pre>
 * RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(new LogConfig(captor, true));
 * </pre>
 * </p>
 */
public class RestAssuredMockMvcConfig implements SpecificationConfig {

    // When adding a config here don't forget to update isUserConfigured method
    private final LogConfig logConfig;
    private final EncoderConfig encoderConfig;
    private final DecoderConfig decoderConfig;
    private final SessionConfig sessionConfig;
    private final ObjectMapperConfig objectMapperConfig;
    private final JsonConfig jsonConfig;
    private final XmlConfig xmlConfig;
    private final HeaderConfig headerConfig;
    private final AsyncConfig asyncConfig;
    private final MockMvcConfig mockMvcConfig;
    private final MultiPartConfig multiPartConfig;
    private final MockMvcParamConfig paramConfig;
    private final MatcherConfig matcherConfig;

    /**
     * Create a new RestAssuredMockMvcConfig with the default configurations.
     */
    public RestAssuredMockMvcConfig() {
        this(new LogConfig(), new EncoderConfig(), new DecoderConfig(), new SessionConfig(), new ObjectMapperConfig(), new JsonConfig(), new XmlConfig(),
                new HeaderConfig(), new AsyncConfig(), new MultiPartConfig(), new MockMvcConfig(), new MockMvcParamConfig(), new MatcherConfig());
    }

    /**
     * Create a new RestAssuredMockMvcConfig with the supplied configs.
     */
    private RestAssuredMockMvcConfig(LogConfig logConfig,
                                     EncoderConfig encoderConfig,
                                     DecoderConfig decoderConfig,
                                     SessionConfig sessionConfig,
                                     ObjectMapperConfig objectMapperConfig,
                                     JsonConfig jsonConfig,
                                     XmlConfig xmlConfig,
                                     HeaderConfig headerConfig,
                                     AsyncConfig asyncConfig,
                                     MultiPartConfig multiPartConfig,
                                     MockMvcConfig mockMvcConfig, 
                                     MockMvcParamConfig paramConfig,
                                     MatcherConfig matcherConfig) {
        notNull(logConfig, "Log config");
        notNull(encoderConfig, "Encoder config");
        notNull(decoderConfig, "Decoder config");
        notNull(sessionConfig, "Session config");
        notNull(objectMapperConfig, "Object mapper config");
        notNull(jsonConfig, "Json config");
        notNull(xmlConfig, "Xml config");
        notNull(headerConfig, "Header config");
        notNull(multiPartConfig, "MultiPart config");
        notNull(mockMvcConfig, "MockMvc config");
        notNull(paramConfig, "Param config");
        notNull(matcherConfig, "Matcher config");
        this.logConfig = logConfig;
        this.encoderConfig = encoderConfig;
        this.decoderConfig = decoderConfig;
        this.sessionConfig = sessionConfig;
        this.objectMapperConfig = objectMapperConfig;
        this.jsonConfig = jsonConfig;
        this.xmlConfig = xmlConfig;
        this.headerConfig = headerConfig;
        this.asyncConfig = asyncConfig;
        this.multiPartConfig = multiPartConfig;
        this.mockMvcConfig = mockMvcConfig;
        this.paramConfig = paramConfig;
        this.matcherConfig = matcherConfig;
    }


    /**
     * Set the Log config.
     *
     * @param logConfig The {@link LogConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig logConfig(LogConfig logConfig) {
        notNull(logConfig, "Log config");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig, objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the session config.
     *
     * @param sessionConfig The {@link SessionConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig sessionConfig(SessionConfig sessionConfig) {
        notNull(sessionConfig, "Session config");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig, objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the object mapper config.
     *
     * @param objectMapperConfig The {@link ObjectMapperConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig objectMapperConfig(ObjectMapperConfig objectMapperConfig) {
        notNull(objectMapperConfig, "Object mapper config");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig, objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the Json config.
     *
     * @param jsonConfig The {@link JsonConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig jsonConfig(JsonConfig jsonConfig) {
        notNull(jsonConfig, "JsonConfig");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the Xml config.
     *
     * @param xmlConfig The {@link XmlConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig xmlConfig(XmlConfig xmlConfig) {
        notNull(xmlConfig, "XmlConfig");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the encoder config
     *
     * @param encoderConfig The {@link EncoderConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig encoderConfig(EncoderConfig encoderConfig) {
        notNull(encoderConfig, "EncoderConfig");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the decoder config
     *
     * @param decoderConfig The {@link DecoderConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig decoderConfig(DecoderConfig decoderConfig) {
        notNull(encoderConfig, DecoderConfig.class);
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the header config
     *
     * @param headerConfig The {@link HeaderConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig headerConfig(HeaderConfig headerConfig) {
        notNull(headerConfig, "HeaderConfig");
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the async config
     *
     * @param asyncConfig The {@link AsyncConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig asyncConfig(AsyncConfig asyncConfig) {
        notNull(asyncConfig, AsyncConfig.class);
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }
    
    /**
     * Set the MockMVC config
     *
     * @param mockMvcConfig The {@link MockMvcConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig mockMvcConfig(MockMvcConfig mockMvcConfig) {
        notNull(mockMvcConfig, MockMvcConfig.class);
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the multi-part config
     *
     * @param multiPartConfig The {@link MultiPartConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig multiPartConfig(MultiPartConfig multiPartConfig) {
        notNull(multiPartConfig, MultiPartConfig.class);
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    public ClientConfig getClientConfig() {
        return getMockMvcConfig();
    }

    public SpecificationConfig clientConfig(ClientConfig clientConfig) {
        if (!(clientConfig instanceof MockMvcConfig)) {
            throw new IllegalArgumentException("Wrong ClientConfig type supplied");
        }
        return mockMvcConfig((MockMvcConfig) clientConfig);
    }

    /**
     * Set the parameter config
     *
     * @param paramConfig The {@link MockMvcParamConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig paramConfig(MockMvcParamConfig paramConfig) {
        notNull(paramConfig, MultiPartConfig.class);
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Set the matcher config
     *
     * @param matcherConfig The {@link MockMvcParamConfig} to set
     * @return An updated RestAssuredMockMvcConfig
     */
    public RestAssuredMockMvcConfig matcherConfig(MatcherConfig matcherConfig) {
        notNull(matcherConfig, MatcherConfig.class);
        return new RestAssuredMockMvcConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig, mockMvcConfig, paramConfig, matcherConfig);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredMockMvcConfig instance.
     */
    public RestAssuredMockMvcConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredMockMvcConfig instance.
     */
    public RestAssuredMockMvcConfig set() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredMockMvcConfig instance.
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
     * @return The Header Config
     */
    public HeaderConfig getHeaderConfig() {
        return headerConfig;
    }

    /**
     * @return The MockMvcAsync Config
     */
    public AsyncConfig getAsyncConfig() {
        return asyncConfig;
    }

    /**
     * @return The MultiPart Config
     */
    public MultiPartConfig getMultiPartConfig() {
        return multiPartConfig;
    }

    /**
     * @return The Param Config
     */
    public ParamConfig getParamConfig() {
        return paramConfig;
    }

    public SpecificationConfig paramConfig(ParamConfig paramConfig) {
        if (!(paramConfig instanceof MockMvcParamConfig)) {
            throw new IllegalArgumentException("Wrong ClientConfig type supplied");
        }
        return paramConfig((MockMvcParamConfig) paramConfig);
    }

    public MockMvcParamConfig getMockMvcParamConfig() {
        return paramConfig;
    }

    /**
     * @return The MockMvc Config
     */
    public MockMvcConfig getMockMvcConfig() {
        return mockMvcConfig;
    }

    /**
     * @return The Matcher Config
     */
    public MatcherConfig getMatcherConfig() {
        return matcherConfig;
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

    public boolean isUserConfigured() {
        // When adding a config here don't forget to update merging in MockMvcRequestSpecificationImpl#mergeConfig and potentially also ConfigConverter#convertToRestAssuredConfig
        return decoderConfig.isUserConfigured() || encoderConfig.isUserConfigured() || logConfig.isUserConfigured() || sessionConfig.isUserConfigured()
                || objectMapperConfig.isUserConfigured() || xmlConfig.isUserConfigured() || jsonConfig.isUserConfigured() || headerConfig.isUserConfigured()
                || asyncConfig.isUserConfigured() || multiPartConfig.isUserConfigured() || mockMvcConfig.isUserConfigured() || paramConfig.isUserConfigured()
                || matcherConfig.isUserConfigured();
    }

}
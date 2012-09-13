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

package com.jayway.restassured.config;

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * Main configuration for REST Assured that allows you to configure advanced settings such as redirections and HTTP Client parameters.
 * <p>
 * Usage example:
 * <pre>
 *  RestAssured.config = config().redirect(redirectConfig().followRedirects(false));
 * </pre>
 * </p>
 */
public class RestAssuredConfig {

    private final RedirectConfig redirectConfig;
    private final HttpClientConfig httpClientConfig;
    private final LogConfig logConfig;
    private final EncoderConfig encoderConfig;
    private final DecoderConfig decoderConfig;
    private final SessionConfig sessionConfig;
    private final ObjectMapperConfig objectMapperConfig;
    private final ConnectionConfig connectionConfig;

    /**
     * Create a new RestAssuredConfiguration with the default configurations.
     */
    public RestAssuredConfig() {
        this(new RedirectConfig(), new HttpClientConfig(), new LogConfig(), new EncoderConfig(), new DecoderConfig(), 
                new SessionConfig(), new ObjectMapperConfig(), new ConnectionConfig());
    }

    /**
     * Create a new RestAssuredConfiguration with the supplied {@link RedirectConfig}, {@link HttpClientConfig}, {@link LogConfig}, 
     * {@link EncoderConfig}, {@link DecoderConfig}, {@link SessionConfig}, {@link ObjectMapperConfig} and {@link ConnectionConfig}.
     */
    public RestAssuredConfig(RedirectConfig redirectConfig,
                             HttpClientConfig httpClientConfig,
                             LogConfig logConfig,
                             EncoderConfig encoderConfig,
                             DecoderConfig decoderConfig,
                             SessionConfig sessionConfig,
                             ObjectMapperConfig objectMapperConfig,
                             ConnectionConfig connectionConfig) {
        notNull(redirectConfig, "Redirect Config");
        notNull(httpClientConfig, "HTTP Client Config");
        notNull(logConfig, "Log config");
        notNull(encoderConfig, "Encoder config");
        notNull(decoderConfig, "Decoder config");
        notNull(sessionConfig, "Session config");
        notNull(objectMapperConfig, "Object mapper config");
        notNull(connectionConfig, "Connection config");
        this.httpClientConfig = httpClientConfig;
        this.redirectConfig = redirectConfig;
        this.logConfig = logConfig;
        this.encoderConfig = encoderConfig;
        this.decoderConfig = decoderConfig;
        this.sessionConfig = sessionConfig;
        this.objectMapperConfig = objectMapperConfig;
        this.connectionConfig = connectionConfig;
    }

    /**
     * Set the redirect config.
     *
     * @param redirectConfig The {@link RedirectConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig redirect(RedirectConfig redirectConfig) {
        notNull(redirectConfig, "Redirect config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Set the HTTP Client config.
     *
     * @param httpClientConfig The {@link HttpClientConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig httpClient(HttpClientConfig httpClientConfig) {
        notNull(httpClientConfig, "HTTP Client Config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Set the Log config.
     *
     * @param logConfig The {@link LogConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig logConfig(LogConfig logConfig) {
        notNull(logConfig, "Log config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Set the Encoder config.
     *
     * @param encoderConfig The {@link EncoderConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig encoderConfig(EncoderConfig encoderConfig) {
        notNull(encoderConfig, "Encoder config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Set the Decoder config.
     *
     * @param decoderConfig The {@link DecoderConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig decoderConfig(DecoderConfig decoderConfig) {
        notNull(decoderConfig, "Decoder config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Set the session config.
     *
     * @param sessionConfig The {@link com.jayway.restassured.config.SessionConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig sessionConfig(SessionConfig sessionConfig) {
        notNull(sessionConfig, "Session config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Set the object mapper config.
     *
     * @param objectMapperConfig The {@link com.jayway.restassured.config.ObjectMapperConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig objectMapperConfig(ObjectMapperConfig objectMapperConfig) {
        notNull(objectMapperConfig, "Object mapper config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Set the connection config.
     *
     * @param connectionConfig The {@link com.jayway.restassured.config.ObjectMapperConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig connectionConfig(ConnectionConfig connectionConfig) {
        notNull(connectionConfig, "Connection config");
        return new RestAssuredConfig(redirectConfig, httpClientConfig, logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, connectionConfig);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredConfig set() {
        return this;
    }

    /**
     * @return The RedirectConfig
     */
    public RedirectConfig getRedirectConfig() {
        return redirectConfig;
    }

    /**
     * @return The LogConfig
     */
    public LogConfig getLogConfig() {
        return logConfig;
    }

    /**
     * @return The HttpClientConfig
     */
    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
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
     * @return The ConnectionConfig
     */
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    /**
     * @return A static way to create a new RestAssuredConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static RestAssuredConfig newConfig() {
        return new RestAssuredConfig();
    }

    /**
     * @return A static way to create a new RestAssuredConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static RestAssuredConfig config() {
        return new RestAssuredConfig();
    }
}
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

package com.jayway.restassured.config;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

/**
 * Main configuration for REST Assured that allows you to configure advanced settings such as redirections and HTTP Client parameters.
 * <p>
 * Usage example:
 * <pre>
 *  RestAssured.config = config().redirect(redirectConfig().followRedirects(false));
 * </pre>
 * </p>
 */
public class RestAssuredConfig implements Config {

    final Map<Class<? extends Config>, Config> configs = new HashMap<Class<? extends Config>, Config>();

    /**
     * Create a new RestAssuredConfiguration with the default configurations.
     */
    public RestAssuredConfig() {
        this(new RedirectConfig(), new HttpClientConfig(), new LogConfig(), new EncoderConfig(), new DecoderConfig(),
                new SessionConfig(), new ObjectMapperConfig(), new ConnectionConfig(), new JsonConfig(), new XmlConfig(), new SSLConfig(),
                new MatcherConfig(), new HeaderConfig());
    }

    /**
     * Create a new RestAssuredConfiguration with the supplied {@link RedirectConfig}, {@link HttpClientConfig}, {@link LogConfig},
     * {@link EncoderConfig}, {@link DecoderConfig}, {@link SessionConfig}, {@link ObjectMapperConfig}, {@link ConnectionConfig},
     * {@link com.jayway.restassured.config.JsonConfig}, {@link com.jayway.restassured.config.XmlConfig}, {@link com.jayway.restassured.config.SSLConfig},
     * {@link com.jayway.restassured.config.MatcherConfig}.
     */
    public RestAssuredConfig(RedirectConfig redirectConfig,
                             HttpClientConfig httpClientConfig,
                             LogConfig logConfig,
                             EncoderConfig encoderConfig,
                             DecoderConfig decoderConfig,
                             SessionConfig sessionConfig,
                             ObjectMapperConfig objectMapperConfig,
                             ConnectionConfig connectionConfig,
                             JsonConfig jsonConfig,
                             XmlConfig xmlConfig,
                             SSLConfig sslConfig,
                             MatcherConfig matcherConfig,
                             HeaderConfig headerConfig) {
        notNull(redirectConfig, "Redirect Config");
        notNull(httpClientConfig, "HTTP Client Config");
        notNull(logConfig, "Log config");
        notNull(encoderConfig, "Encoder config");
        notNull(decoderConfig, "Decoder config");
        notNull(sessionConfig, "Session config");
        notNull(objectMapperConfig, "Object mapper config");
        notNull(connectionConfig, "Connection config");
        notNull(jsonConfig, "Json config");
        notNull(xmlConfig, "Xml config");
        notNull(sslConfig, "SSL config");
        notNull(matcherConfig, "Matcher config");
        notNull(headerConfig, "Header config");
        configs.put(HttpClientConfig.class, httpClientConfig);
        configs.put(RedirectConfig.class, redirectConfig);
        configs.put(LogConfig.class, logConfig);
        configs.put(EncoderConfig.class, encoderConfig);
        configs.put(DecoderConfig.class, decoderConfig);
        configs.put(SessionConfig.class, sessionConfig);
        configs.put(ObjectMapperConfig.class, objectMapperConfig);
        configs.put(ConnectionConfig.class, connectionConfig);
        configs.put(JsonConfig.class, jsonConfig);
        configs.put(XmlConfig.class, xmlConfig);
        configs.put(SSLConfig.class, sslConfig);
        configs.put(MatcherConfig.class, matcherConfig);
        configs.put(HeaderConfig.class, headerConfig);
    }

    /**
     * Set the redirect config.
     *
     * @param redirectConfig The {@link RedirectConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig redirect(RedirectConfig redirectConfig) {
        notNull(redirectConfig, "Redirect config");
        return new RestAssuredConfig(redirectConfig, conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the HTTP Client config.
     *
     * @param httpClientConfig The {@link HttpClientConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig httpClient(HttpClientConfig httpClientConfig) {
        notNull(httpClientConfig, "HTTP Client Config");
        return new RestAssuredConfig(conf(RedirectConfig.class), httpClientConfig, conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the Log config.
     *
     * @param logConfig The {@link LogConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig logConfig(LogConfig logConfig) {
        notNull(logConfig, "Log config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), logConfig, conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the Encoder config.
     *
     * @param encoderConfig The {@link EncoderConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig encoderConfig(EncoderConfig encoderConfig) {
        notNull(encoderConfig, "Encoder config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), encoderConfig,
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the Decoder config.
     *
     * @param decoderConfig The {@link DecoderConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig decoderConfig(DecoderConfig decoderConfig) {
        notNull(decoderConfig, "Decoder config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                decoderConfig, conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the session config.
     *
     * @param sessionConfig The {@link com.jayway.restassured.config.SessionConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig sessionConfig(SessionConfig sessionConfig) {
        notNull(sessionConfig, "Session config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), sessionConfig, conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the object mapper config.
     *
     * @param objectMapperConfig The {@link com.jayway.restassured.config.ObjectMapperConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig objectMapperConfig(ObjectMapperConfig objectMapperConfig) {
        notNull(objectMapperConfig, "Object mapper config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), objectMapperConfig, conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the connection config.
     *
     * @param connectionConfig The {@link com.jayway.restassured.config.ConnectionConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig connectionConfig(ConnectionConfig connectionConfig) {
        notNull(connectionConfig, "Connection config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), connectionConfig,
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the Json config.
     *
     * @param jsonConfig The {@link JsonConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig jsonConfig(JsonConfig jsonConfig) {
        notNull(jsonConfig, "JsonConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                jsonConfig, conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class));
    }

    /**
     * Set the Xml config.
     *
     * @param xmlConfig The {@link XmlConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig xmlConfig(XmlConfig xmlConfig) {
        notNull(xmlConfig, "XmlConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), xmlConfig, conf(SSLConfig.class), conf(MatcherConfig.class), conf(HeaderConfig.class));
    }

    /**
     * Set the SSL config.
     *
     * @param sslConfig The {@link com.jayway.restassured.config.SSLConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig sslConfig(SSLConfig sslConfig) {
        notNull(sslConfig, "SSLConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), sslConfig, conf(MatcherConfig.class), conf(HeaderConfig.class));
    }

    /**
     * Set the Matcher config.
     *
     * @param matcherConfig The {@link com.jayway.restassured.config.MatcherConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig matcherConfig(MatcherConfig matcherConfig) {
        notNull(matcherConfig, "MatcherConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), matcherConfig, conf(HeaderConfig.class));
    }

    /**
     * Set the Header config.
     *
     * @param headerConfig The {@link com.jayway.restassured.config.HeaderConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig headerConfig(HeaderConfig headerConfig) {
        notNull(headerConfig, "HeaderConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class), headerConfig);
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
     * Syntactic sugar.
     *
     * @return The same RestAssuredConfiguration instance.
     */
    public RestAssuredConfig with() {
        return this;
    }

    /**
     * @return The RedirectConfig
     */
    public RedirectConfig getRedirectConfig() {
        return conf(RedirectConfig.class);
    }

    /**
     * @return The LogConfig
     */
    public LogConfig getLogConfig() {
        return conf(LogConfig.class);
    }

    /**
     * @return The HttpClientConfig
     */
    public HttpClientConfig getHttpClientConfig() {
        return conf(HttpClientConfig.class);
    }

    /**
     * @return The EncoderConfig
     */
    public EncoderConfig getEncoderConfig() {
        return conf(EncoderConfig.class);
    }

    /**
     * @return The DecoderConfig
     */
    public DecoderConfig getDecoderConfig() {
        return conf(DecoderConfig.class);
    }

    /**
     * @return The SessionConfig
     */
    public SessionConfig getSessionConfig() {
        return conf(SessionConfig.class);
    }

    /**
     * @return The ObjectMapperConfig
     */
    public ObjectMapperConfig getObjectMapperConfig() {
        return conf(ObjectMapperConfig.class);
    }

    /**
     * @return The ConnectionConfig
     */
    public ConnectionConfig getConnectionConfig() {
        return conf(ConnectionConfig.class);
    }

    /**
     * @return The JsonPath Config
     */
    public JsonConfig getJsonConfig() {
        return conf(JsonConfig.class);
    }

    /**
     * @return The Xml Config
     */
    public XmlConfig getXmlConfig() {
        return conf(XmlConfig.class);
    }

    /**
     * @return The SSL Config
     */
    public SSLConfig getSSLConfig() {
        return conf(SSLConfig.class);
    }

    /**
     * @return The matcher config
     */
    public MatcherConfig getMatcherConfig() {
        return conf(MatcherConfig.class);
    }

    /**
     * @return The header config
     */
    public HeaderConfig getHeaderConfig() {
        return conf(HeaderConfig.class);
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

    /**
     * @return <code>true</code> if this instance is carrying any config that is user configured.
     */
    public boolean isUserConfigured() {
        for (Config cfg : configs.values()) {
            if (cfg.isUserConfigured()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T extends Config> T conf(Class<T> type) {
        return (T) configs.get(type);
    }
}
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

package io.restassured.config;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Main configuration for REST Assured that allows you to configure advanced settings such as redirections and HTTP Client parameters.
 * <p>
 * Usage example:
 * <pre>
 *  RestAssured.config = RestAssured.config().redirect(redirectConfig().followRedirects(false));
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
                new MatcherConfig(), new HeaderConfig(), new MultiPartConfig(), new ParamConfig(), new OAuthConfig(), new FailureConfig());
    }

    /**
     * Create a new RestAssuredConfiguration with the supplied {@link RedirectConfig}, {@link HttpClientConfig}, {@link LogConfig},
     * {@link EncoderConfig}, {@link DecoderConfig}, {@link SessionConfig}, {@link ObjectMapperConfig}, {@link ConnectionConfig},
     * {@link JsonConfig}, {@link XmlConfig}, {@link SSLConfig},
     * {@link MatcherConfig}, {@link HeaderConfig}, {@link MultiPartConfig}
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
                             HeaderConfig headerConfig,
                             MultiPartConfig multiPartConfig,
                             ParamConfig paramConfig,
                             OAuthConfig oAuthConfig,
                             FailureConfig failureConfig) {
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
        notNull(multiPartConfig, "Multipart config");
        notNull(paramConfig, "Param config");
        notNull(oAuthConfig, "OAuth config");
        notNull(failureConfig, "Failre config");
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
        configs.put(MultiPartConfig.class, multiPartConfig);
        configs.put(ParamConfig.class, paramConfig);
        configs.put(OAuthConfig.class, oAuthConfig);
        configs.put(FailureConfig.class, failureConfig);
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
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
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
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
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
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
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
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
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
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the session config.
     *
     * @param sessionConfig The {@link SessionConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig sessionConfig(SessionConfig sessionConfig) {
        notNull(sessionConfig, "Session config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), sessionConfig, conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the object mapper config.
     *
     * @param objectMapperConfig The {@link ObjectMapperConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig objectMapperConfig(ObjectMapperConfig objectMapperConfig) {
        notNull(objectMapperConfig, "Object mapper config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), objectMapperConfig, conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the connection config.
     *
     * @param connectionConfig The {@link ConnectionConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig connectionConfig(ConnectionConfig connectionConfig) {
        notNull(connectionConfig, "Connection config");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), connectionConfig,
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class),
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
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
                conf(HeaderConfig.class), conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class),conf(FailureConfig.class));
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
                conf(JsonConfig.class), xmlConfig, conf(SSLConfig.class), conf(MatcherConfig.class), conf(HeaderConfig.class),
                conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the SSL config.
     *
     * @param sslConfig The {@link SSLConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig sslConfig(SSLConfig sslConfig) {
        notNull(sslConfig, "SSLConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), sslConfig, conf(MatcherConfig.class), conf(HeaderConfig.class),
                conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the Matcher config.
     *
     * @param matcherConfig The {@link MatcherConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig matcherConfig(MatcherConfig matcherConfig) {
        notNull(matcherConfig, "MatcherConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), matcherConfig, conf(HeaderConfig.class),
                conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the Header config.
     *
     * @param headerConfig The {@link HeaderConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig headerConfig(HeaderConfig headerConfig) {
        notNull(headerConfig, "HeaderConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class), headerConfig,
                conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the MultiPart config.
     *
     * @param multiPartConfig The {@link MultiPartConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig multiPartConfig(MultiPartConfig multiPartConfig) {
        notNull(multiPartConfig, "MultiPartConfig");
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class), conf(HeaderConfig.class),
                multiPartConfig, conf(ParamConfig.class), conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the parameter config.
     *
     * @param paramConfig The {@link ParamConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig paramConfig(ParamConfig paramConfig) {
        notNull(paramConfig, ParamConfig.class);
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class), conf(HeaderConfig.class),
                conf(MultiPartConfig.class), paramConfig, conf(OAuthConfig.class), conf(FailureConfig.class));
    }

    /**
     * Set the oauth config.
     *
     * @param oauthConfig The {@link OAuthConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig oauthConfig(OAuthConfig oauthConfig) {
        notNull(oauthConfig, OAuthConfig.class);
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class), conf(HeaderConfig.class),
                conf(MultiPartConfig.class), conf(ParamConfig.class), oauthConfig, conf(FailureConfig.class));
    }

    /**
     * Set the failure config.
     *
     * @param failureConfig The {@link FailureConfig} to set
     * @return An updated RestAssuredConfiguration
     */
    public RestAssuredConfig failureConfig(FailureConfig failureConfig) {
        notNull(failureConfig, FailureConfig.class);
        return new RestAssuredConfig(conf(RedirectConfig.class), conf(HttpClientConfig.class), conf(LogConfig.class), conf(EncoderConfig.class),
                conf(DecoderConfig.class), conf(SessionConfig.class), conf(ObjectMapperConfig.class), conf(ConnectionConfig.class),
                conf(JsonConfig.class), conf(XmlConfig.class), conf(SSLConfig.class), conf(MatcherConfig.class), conf(HeaderConfig.class),
                conf(MultiPartConfig.class), conf(ParamConfig.class), conf(OAuthConfig.class), failureConfig);
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
     * @return The MultiPart Config
     */
    public MultiPartConfig getMultiPartConfig() {
        return conf(MultiPartConfig.class);
    }

    /**
     * @return The Param Config
     */
    public ParamConfig getParamConfig() {
        return conf(ParamConfig.class);
    }

    /**
     * @return The Param Config
     */
    public OAuthConfig getOAuthConfig() {
        return conf(OAuthConfig.class);
    }

    /**
     * @return The Failure Config
     */
    public FailureConfig getFailureConfig() {
        return conf(FailureConfig.class);
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
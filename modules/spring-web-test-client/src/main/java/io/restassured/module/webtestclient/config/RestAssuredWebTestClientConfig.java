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

package io.restassured.module.webtestclient.config;

import io.restassured.config.*;
import io.restassured.module.spring.commons.config.AsyncConfig;
import io.restassured.module.spring.commons.config.ClientConfig;
import io.restassured.module.spring.commons.config.SpecificationConfig;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Main configuration for REST Assured WebTestClient that allows you to configure advanced settings.
 * <p>
 * Usage example:
 * <pre>
 * RestAssuredWebTestClient.config = new RestAssuredWebTestClientConfig().logConfig(new LogConfig(captor, true));
 * </pre>
 * </p>
 */
public class RestAssuredWebTestClientConfig implements SpecificationConfig {

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
	private final WebTestClientConfig webTestClientConfig;
	private final MultiPartConfig multiPartConfig;
	private final WebTestClientParamConfig paramConfig;
	private final MatcherConfig matcherConfig;


	/**
	 * Create a new RestAssuredWebTestClientConfig with the default configurations.
	 */
	public RestAssuredWebTestClientConfig() {
		this(new LogConfig(), new EncoderConfig(), new DecoderConfig(), new SessionConfig(), new ObjectMapperConfig(), new JsonConfig(), new XmlConfig(),
				new HeaderConfig(), new AsyncConfig(), new MultiPartConfig(), new WebTestClientConfig(), new WebTestClientParamConfig(), new MatcherConfig());
	}

	/**
	 * Create a new RestAssuredWebTestClientConfig with the supplied configs.
	 */
	private RestAssuredWebTestClientConfig(LogConfig logConfig,
	                                       EncoderConfig encoderConfig,
	                                       DecoderConfig decoderConfig,
	                                       SessionConfig sessionConfig,
	                                       ObjectMapperConfig objectMapperConfig,
	                                       JsonConfig jsonConfig,
	                                       XmlConfig xmlConfig,
	                                       HeaderConfig headerConfig,
	                                       AsyncConfig asyncConfig,
	                                       MultiPartConfig multiPartConfig,
	                                       WebTestClientConfig webTestClientConfig,
	                                       WebTestClientParamConfig paramConfig,
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
		notNull(webTestClientConfig, "webTestClient config");
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
		this.webTestClientConfig = webTestClientConfig;
		this.paramConfig = paramConfig;
		this.matcherConfig = matcherConfig;
	}

	/**
	 * @return A static way to create a new RestAssuredWebTestClientConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
	 */
	public static RestAssuredWebTestClientConfig newConfig() {
		return new RestAssuredWebTestClientConfig();
	}

	/**
	 * @return A static way to create a new RestAssuredWebTestClientConfiguration instance without calling "new" explicitly. Mainly for syntactic sugar.
	 */
	public static RestAssuredWebTestClientConfig config() {
		return new RestAssuredWebTestClientConfig();
	}

	/**
	 * Syntactic sugar.
	 *
	 * @return The same RestAssuredWebTestClientConfig instance.
	 */
	public RestAssuredWebTestClientConfig and() {
		return this;
	}

	/**
	 * Syntactic sugar.
	 *
	 * @return The same RestAssuredWebTestClientConfig instance.
	 */
	public RestAssuredWebTestClientConfig set() {
		return this;
	}

	/**
	 * Syntactic sugar.
	 *
	 * @return The same RestAssuredWebTestClientConfig instance.
	 */
	public RestAssuredWebTestClientConfig with() {
		return this;
	}

	public boolean isUserConfigured() {
		// When adding a config here don't forget to update merging in WebTestClientRequestSpecificationImpl#mergeConfig and potentially also ConfigConverter#convertToRestAssuredConfig
        return decoderConfig.isUserConfigured() || encoderConfig.isUserConfigured() || logConfig.isUserConfigured()
                || sessionConfig.isUserConfigured() || objectMapperConfig.isUserConfigured()
                || xmlConfig.isUserConfigured() || jsonConfig.isUserConfigured() || headerConfig.isUserConfigured()
                || asyncConfig.isUserConfigured() || multiPartConfig.isUserConfigured()
                || webTestClientConfig.isUserConfigured() || paramConfig.isUserConfigured()
				|| matcherConfig.isUserConfigured();
	}

	/**
	 * @return The DecoderConfig
	 */
	public DecoderConfig getDecoderConfig() {
		return decoderConfig;
	}

	/**
	 * Set the decoder config
	 *
	 * @param decoderConfig The {@link DecoderConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig decoderConfig(DecoderConfig decoderConfig) {
		notNull(encoderConfig, DecoderConfig.class);
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The EncoderConfig
	 */
	public EncoderConfig getEncoderConfig() {
		return encoderConfig;
	}

	/**
	 * Set the encoder config
	 *
	 * @param encoderConfig The {@link EncoderConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig encoderConfig(EncoderConfig encoderConfig) {
		notNull(encoderConfig, "EncoderConfig");
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The Header Config
	 */
	public HeaderConfig getHeaderConfig() {
		return headerConfig;
	}

	/**
	 * Set the header config
	 *
	 * @param headerConfig The {@link HeaderConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig headerConfig(HeaderConfig headerConfig) {
		notNull(headerConfig, "HeaderConfig");
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The JsonPath Config
	 */
	public JsonConfig getJsonConfig() {
		return jsonConfig;
	}

	/**
	 * Set the Json config.
	 *
	 * @param jsonConfig The {@link JsonConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig jsonConfig(JsonConfig jsonConfig) {
		notNull(jsonConfig, "JsonConfig");
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The LogConfig
	 */
	public LogConfig getLogConfig() {
		return logConfig;
	}

	/**
	 * Set the Log config.
	 *
	 * @param logConfig The {@link LogConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig logConfig(LogConfig logConfig) {
		notNull(logConfig, "Log config");
        return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The ObjectMapperConfig
	 */
	public ObjectMapperConfig getObjectMapperConfig() {
		return objectMapperConfig;
	}

	/**
	 * Set the object mapper config.
	 *
	 * @param objectMapperConfig The {@link ObjectMapperConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig objectMapperConfig(ObjectMapperConfig objectMapperConfig) {
		notNull(objectMapperConfig, "Object mapper config");
        return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The SessionConfig
	 */
	public SessionConfig getSessionConfig() {
		return sessionConfig;
	}

	/**
	 * Set the session config.
	 *
	 * @param sessionConfig The {@link SessionConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig sessionConfig(SessionConfig sessionConfig) {
		notNull(sessionConfig, "Session config");
        return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The Xml Config
	 */
	public XmlConfig getXmlConfig() {
		return xmlConfig;
	}

	/**
	 * Set the Xml config.
	 *
	 * @param xmlConfig The {@link XmlConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig xmlConfig(XmlConfig xmlConfig) {
		notNull(xmlConfig, "XmlConfig");
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The WebTestClientAsync Config
	 */
	public AsyncConfig getAsyncConfig() {
		return asyncConfig;
	}

	/**
	 * Set the async config
	 *
	 * @param asyncConfig The {@link AsyncConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig asyncConfig(AsyncConfig asyncConfig) {
		notNull(asyncConfig, AsyncConfig.class);
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The MultiPart Config
	 */
	public MultiPartConfig getMultiPartConfig() {
		return multiPartConfig;
	}

	/**
	 * Set the multi-part config
	 *
	 * @param multiPartConfig The {@link MultiPartConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig multiPartConfig(MultiPartConfig multiPartConfig) {
		notNull(multiPartConfig, MultiPartConfig.class);
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
                objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
                webTestClientConfig, paramConfig, matcherConfig);
	}

	@Override
	public ClientConfig getClientConfig() {
		return getWebTestClientConfig();
	}

	@Override
	public RestAssuredWebTestClientConfig clientConfig(ClientConfig clientConfig) {
		if (!(clientConfig instanceof WebTestClientConfig)) {
			throw new IllegalArgumentException("Wrong ClientConfig type supplied");
		}
		return webTestClientConfig((WebTestClientConfig) clientConfig);
	}

	/**
	 * Set the webTestClient config
	 *
	 * @param webTestClientConfig The {@link WebTestClientConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig webTestClientConfig(WebTestClientConfig webTestClientConfig) {
		notNull(webTestClientConfig, WebTestClientConfig.class);
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
				objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
				webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
	 * @return The Param Config
	 */
	public ParamConfig getParamConfig() {
		return paramConfig;
	}

	public WebTestClientParamConfig getWebTestClientParamConfig() {
		return paramConfig;
	}

	@Override
	public SpecificationConfig paramConfig(ParamConfig paramConfig) {
		if (!(paramConfig instanceof WebTestClientParamConfig)) {
			throw new IllegalArgumentException("Wrong ClientConfig type supplied");
		}
		return paramConfig((WebTestClientParamConfig) paramConfig);
	}

	/**
	 * Set the parameter config
	 *
	 * @param paramConfig The {@link WebTestClientParamConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig paramConfig(WebTestClientParamConfig paramConfig) {
		notNull(paramConfig, MultiPartConfig.class);
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
				objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
				webTestClientConfig, paramConfig, matcherConfig);
	}

	/**
     * @return The WebTestClient Config
	 */
	public WebTestClientConfig getWebTestClientConfig() {
		return webTestClientConfig;
	}

	/**
	 * @return The MatcherConfig
	 */
	public MatcherConfig getMatcherConfig() {
		return matcherConfig;
	}

	/**
	 * Set the matcher config.
	 *
	 * @param matcherConfig The {@link MatcherConfig} to set
	 * @return An updated RestAssuredWebTestClientConfig
	 */
	public RestAssuredWebTestClientConfig matcherConfig(MatcherConfig matcherConfig) {
		notNull(matcherConfig, "Matcher config");
		return new RestAssuredWebTestClientConfig(logConfig, encoderConfig, decoderConfig, sessionConfig,
				objectMapperConfig, jsonConfig, xmlConfig, headerConfig, asyncConfig, multiPartConfig,
				webTestClientConfig, paramConfig, matcherConfig);
	}
}

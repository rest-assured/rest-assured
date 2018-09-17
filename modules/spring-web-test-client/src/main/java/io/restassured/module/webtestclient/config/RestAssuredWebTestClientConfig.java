/*
 * Copyright 2018 the original author or authors.
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

import io.restassured.config.EncoderConfig;
import io.restassured.config.HeaderConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.SerializationConfig;

/**
 * @author Olga Maciaszek-Sharma
 * Allows you to specify what the request will look like.
 */
public class RestAssuredWebTestClientConfig implements SerializationConfig {

	private final LogConfig logConfig;
	private final WebTestClientConfig webTestClientConfig;
	private final HeaderConfig headerConfig;

	public RestAssuredWebTestClientConfig() {
		this(new LogConfig(), new WebTestClientConfig(), new HeaderConfig());
	}

	public RestAssuredWebTestClientConfig(LogConfig logConfig,
	                                      WebTestClientConfig webTestClientConfig, HeaderConfig headerConfig) {
		this.logConfig = logConfig;
		this.webTestClientConfig = webTestClientConfig;
		this.headerConfig = headerConfig;
	}


	@Override
	public ObjectMapperConfig getObjectMapperConfig() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public EncoderConfig getEncoderConfig() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public boolean isUserConfigured() {
		return logConfig.isUserConfigured() || webTestClientConfig.isUserConfigured();
	}

	public LogConfig getLogConfig() {
		return logConfig;
	}

	public WebTestClientConfig getWebTestClientConfig() {
		return webTestClientConfig;
	}

	public HeaderConfig getHeaderConfig() {
		return headerConfig;
	}
}

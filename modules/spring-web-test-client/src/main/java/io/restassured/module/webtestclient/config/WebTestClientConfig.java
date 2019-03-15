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

import io.restassured.config.Config;
import io.restassured.module.spring.commons.config.ClientConfig;

public class WebTestClientConfig implements ClientConfig, Config {

	private final boolean userConfigured;

	public WebTestClientConfig() {
		this(true);
	}

	public WebTestClientConfig(boolean userConfigured) {
		this.userConfigured = userConfigured;
	}

	@Override
	public boolean isUserConfigured() {
		return userConfigured;
	}

	/**
	 * Just syntactic sugar.
	 *
	 * @return A new instance of {@link WebTestClientConfig}.
	 */
	public static WebTestClientConfig webTestClientConfig() {
		return new WebTestClientConfig();
	}

	/**
	 * Just syntactic sugar to make the DSL more English-like.
	 */
	public WebTestClientConfig with() {
		return this;
	}
}

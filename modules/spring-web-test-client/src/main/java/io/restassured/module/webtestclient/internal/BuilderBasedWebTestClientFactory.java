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

package io.restassured.module.webtestclient.internal;

import io.restassured.module.webtestclient.config.WebTestClientConfig;

import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Olga Maciaszek-Sharma
 */
public class BuilderBasedWebTestClientFactory implements WebTestClientFactory {

	private final WebTestClient.Builder builder;

	public BuilderBasedWebTestClientFactory(WebTestClient.Builder builder) {
		this.builder = builder;
	}

	@Override
	public synchronized WebTestClient build(WebTestClientConfig webTestClientConfig) {
		return builder.build();
	}

	@Override
	public boolean isAssigned() {
		return builder != null;
	}
}

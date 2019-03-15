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
package io.restassured.module.webtestclient.internal;

import io.restassured.config.LogConfig;
import io.restassured.internal.RestAssuredResponseOptionsImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import org.springframework.test.web.reactive.server.WebTestClient;

public class WebTestClientRestAssuredResponseImpl extends RestAssuredResponseOptionsImpl<WebTestClientResponse> implements WebTestClientResponse {

	private final WebTestClient.ResponseSpec responseSpec;
	private final LogRepository logRepository;

	public WebTestClientRestAssuredResponseImpl(WebTestClient.ResponseSpec responseSpec,
												LogRepository logRepository) {
		this.responseSpec = responseSpec;
		this.logRepository = logRepository;
	}

	@Override
	public ValidatableWebTestClientResponse then() {
		ValidatableWebTestClientResponse response = new ValidatableWebTestClientResponseImpl(
				responseSpec, getRpr(), getConfig(), this, this, logRepository);
		LogConfig logConfig = getConfig().getLogConfig();
		if (logConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
			response.log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled());
		}
		return response;
	}
}

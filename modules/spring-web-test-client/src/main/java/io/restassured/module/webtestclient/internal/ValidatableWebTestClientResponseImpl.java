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

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ValidatableResponseOptionsImpl;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.response.ExtractableResponse;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static io.restassured.module.webtestclient.internal.ResponseConverter.toStandardResponse;

public class ValidatableWebTestClientResponseImpl extends ValidatableResponseOptionsImpl<ValidatableWebTestClientResponse, WebTestClientResponse>
        implements ValidatableWebTestClientResponse {

	private final WebTestClientResponse response;

	public ValidatableWebTestClientResponseImpl(WebTestClient.ResponseSpec responseSpec,
                                                ResponseParserRegistrar responseParserRegistrar,
                                                RestAssuredConfig config,
                                                WebTestClientResponse response,
                                                ExtractableResponse<WebTestClientResponse> extractableResponse,
                                                LogRepository logRepository) {
        super(responseParserRegistrar, config, toStandardResponse(response), extractableResponse, logRepository);
		AssertParameter.notNull(responseSpec, WebTestClient.ResponseSpec.class);
		this.response = response;
	}

	@Override
	public ValidatableWebTestClientResponse status(HttpStatus expectedStatus) {
		statusCode(expectedStatus.value());
		return this;
	}

	@Override
	public WebTestClientResponse originalResponse() {
		return response;
	}
}

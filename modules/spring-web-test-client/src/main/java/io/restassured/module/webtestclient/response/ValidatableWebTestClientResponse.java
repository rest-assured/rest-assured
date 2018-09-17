/*
 * Copyright 2016 the original author or authors.
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

package io.restassured.module.webtestclient.response;

import io.restassured.response.ValidatableResponseOptions;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * A validatable response of a request made by REST Assured Mock Mvc.
 * <p>
 * Usage example:
 * <pre>
 * get("/lotto").then().body("lotto.lottoId", is(5));
 * </pre>
 * </p>
 */
public interface ValidatableWebTestClientResponse extends ValidatableResponseOptions<ValidatableWebTestClientResponse, WebTestClientResponse> {

	WebTestClient.ResponseSpec expect();
}

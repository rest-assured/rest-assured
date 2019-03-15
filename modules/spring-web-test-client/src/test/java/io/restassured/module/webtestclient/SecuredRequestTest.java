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

package io.restassured.module.webtestclient;

import io.restassured.module.webtestclient.setup.SecuredProcessor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

public class SecuredRequestTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void
	basic_authentication_filter_is_applied() {
		RouterFunction<ServerResponse> routerFunction = RouterFunctions
				.route(path("/securedGreeting").and(method(HttpMethod.GET)),
						new SecuredProcessor()::processSecuredRequest);
		RestAssuredWebTestClient.standaloneSetup(routerFunction, basicAuthentication("test", "pass"));
		given()
				.when()
				.get("/securedGreeting")
				.then()
				.statusCode(200)
				.body("auth", equalTo("Basic dGVzdDpwYXNz"));
	}
}

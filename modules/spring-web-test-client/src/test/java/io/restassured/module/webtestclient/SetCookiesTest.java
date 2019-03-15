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

import io.restassured.module.webtestclient.setup.CookieProcessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

public class SetCookiesTest {

	@BeforeClass
	public static void configureWebTestClientInstance() {
		RouterFunction<ServerResponse> setAttributesRoute = RouterFunctions
				.route(path("/setCookies").and(method(HttpMethod.GET)),
						new CookieProcessor()::processCookies);
		RestAssuredWebTestClient.standaloneSetup(setAttributesRoute);
	}

	@AfterClass
	public static void restRestAssured() {
		RestAssuredWebTestClient.reset();
	}

	@Test
	public void
	can_receive_cookies() {
		RestAssuredWebTestClient.given()
				.queryParam("name", "John Doe")
				.queryParam("project", "rest assured")
				.when()
				.get("/setCookies")
				.then()
				.statusCode(200)
				.cookie("name", "John Doe")
				.cookie("project", "rest assured");
	}
}

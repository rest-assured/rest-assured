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

import io.restassured.config.SessionConfig;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.setup.CookieProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

public class SetCookiesTest {

	@BeforeAll
	public static void configureWebTestClientInstance() {
		RouterFunction<ServerResponse> setAttributesRoute = RouterFunctions
				.route(path("/setCookies").and(method(HttpMethod.GET)),
						new CookieProcessor()::processCookies);
		RestAssuredWebTestClient.standaloneSetup(setAttributesRoute);
	}

	@AfterAll
	public static void restRestAssured() {
		RestAssuredWebTestClient.reset();
	}

	@Test
	public void
	can_receive_cookies() {
		RestAssuredWebTestClient.given()
				.queryParam("name", "JohnDoe")
				.queryParam("project", "rest-assured")
				.when()
				.get("/setCookies")
				.then()
				.statusCode(200)
				.cookie("name", "JohnDoe")
				.cookie("project", "rest-assured");
	}

	@Test
	public void
	session_id_is_the_value_of_the_jsessionid_cookie_by_default() {
		WebTestClientResponse response = RestAssuredWebTestClient.given()
				.queryParam("JSESSIONID", "abc")
				.queryParam("project", "rest-assured")
				.when()
				.get("/setCookies");

		assertThat(response.sessionId()).isEqualTo("abc");
		assertThat(response.getSessionId()).isEqualTo("abc");
	}

	@Test
	public void
	session_id_is_the_value_of_the_cookie_named_by_the_session_config() {
		String sessionId = RestAssuredWebTestClient.given()
				.config(RestAssuredWebTestClientConfig.config().sessionConfig(new SessionConfig("PHPSESSID", null)))
				.queryParam("JSESSIONID", "abc")
				.queryParam("PHPSESSID", "def")
				.when()
				.get("/setCookies")
				.then()
				.extract().sessionId();

		assertThat(sessionId).isEqualTo("def");
	}
}

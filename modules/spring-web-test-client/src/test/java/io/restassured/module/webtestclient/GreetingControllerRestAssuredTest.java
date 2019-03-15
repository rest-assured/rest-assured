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

import io.restassured.module.webtestclient.setup.GreetingController;
import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.reactive.server.WebTestClient.bindToController;

public class GreetingControllerRestAssuredTest {

	@Test
	public void
	uses_predefined_web_test_client_instance() {
		WebTestClient webTestClient = bindToController(new GreetingController()).build();

		RestAssuredWebTestClient.given()
				.webTestClient(webTestClient)
				.param("name", "Johan")
				.when()
				.get("/greeting")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, Johan!"));
	}

	@Test
	public void
	param_with_int() {
		WebTestClient webTestClient = bindToController(new GreetingController()).build();

		RestAssuredWebTestClient.given()
				.webTestClient(webTestClient)
				.param("name", 1)
				.when()
				.get("/greeting")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, 1!"));
	}

	@Test
	public void
	uses_predefined_standalone() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(new GreetingController())
				.param("name", "Johan")
				.when()
				.get("/greeting")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, Johan!"));
	}

	@Test
	public void
	uses_static_web_test_client() {
		RestAssuredWebTestClient.webTestClient(bindToController(new GreetingController()).build());

		try {
			RestAssuredWebTestClient.given()
					.param("name", "Johan")
					.when()
					.get("/greeting")
					.then()
					.body("id", equalTo(1))
					.body("content", equalTo("Hello, Johan!"));

			RestAssuredWebTestClient.given().
					param("name", "Erik")
					.when()
					.get("/greeting")
					.then()
					.body("id", equalTo(2))
					.body("content", equalTo("Hello, Erik!"));
		} finally {
			RestAssuredWebTestClient.reset();
		}
	}
}

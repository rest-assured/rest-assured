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

import io.restassured.http.Method;
import io.restassured.module.webtestclient.setup.GreetingController;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.equalTo;

public class WebTestClientPathParamTest {

	@Before
	public void configureMockMvcInstance() {
		RestAssuredWebTestClient.standaloneSetup(new GreetingController());
	}

	@After
	public void restRestAssured() {
		RestAssuredWebTestClient.reset();
	}

	@Test
	public void unnamed_path_param_works() {
		RestAssuredWebTestClient.given()
				.queryParam("name", "John")
			.when()
				.get("/{x}", "greeting")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void should_succeed_with_get_method_when_named_parameter() {
		RestAssuredWebTestClient.given()
				.pathParam("name", "John")
			.when()
				.get("/greeting/{name}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void should_succeed_with_get_method_when_unnamed_parameter() {
		RestAssuredWebTestClient.given()
			.when()
				.get("/greeting/{name}", "John")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void should_succeed_with_get_method_with_named_parameter_and_unnamed_parameter() {
		RestAssuredWebTestClient.given()
				.pathParam("date", "2023-08-31")
			.when()
				.get("/greeting/{name}/{date}", "John")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John! Today is 2023-08-31"));
	}

	@Test
	public void should_succeed_with_get_method_when_named_parameter_is_single_map_value() {
		final Map<String, Object> pathParams = Collections.singletonMap("name", "John");
		RestAssuredWebTestClient.given()
				.pathParams(pathParams)
			.when()
				.get("/greeting/{name}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void should_succeed_with_get_method_when_named_parameter_is_multi_map_value() {
		final Map<String, Object> pathParams = new HashMap<>();
		pathParams.put("name", "John");
		pathParams.put("date", "2023-08-30");

		RestAssuredWebTestClient.given()
				.pathParams(pathParams)
			.when()
				.get("/greeting/{name}/{date}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John! Today is 2023-08-30"));
	}

	@Test
	public void should_succeed_with_get_method_when_named_parameter_per_add_method() {
		RestAssuredWebTestClient.given()
				.pathParam("name", "John")
				.pathParam("date", "2023-08-30")
			.when()
				.get("/greeting/{name}/{date}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John! Today is 2023-08-30"));
	}

	@Test
	public void should_fail_with_get_method_when_does_not_has_params() {
		// Supplier created to fix sonar error.
		Supplier<Void> restAssuredExecutionSupplier = () -> {
			RestAssuredWebTestClient.given()
				.when()
					.get("/greeting/{name}/{date}")
				.then();

			return null;
		};

		final Exception ex = Assert.assertThrows(IllegalArgumentException.class, restAssuredExecutionSupplier::get);
		Assert.assertEquals("No values were found for the request's pathParams.", ex.getMessage());
	}

	@Test
	public void should_succeed_with_request_method_when_named_parameter() {
		RestAssuredWebTestClient.given()
				.pathParam("name", "John")
			.when()
				.request(Method.GET, "/greeting/{name}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void should_succeed_with_request_method_when_unnamed_parameter() {
		RestAssuredWebTestClient.given()
			.when()
				.request(Method.GET, "/greeting/{name}", "John")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void should_succeed_with_request_method_with_named_parameter_and_unnamed_parameter() {
		RestAssuredWebTestClient.given()
				.pathParam("date", "2023-08-31")
			.when()
				.request(Method.GET, "/greeting/{name}/{date}", "John")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John! Today is 2023-08-31"));
	}

	@Test
	public void should_succeed_with_request_method_when_named_parameter_is_single_map_value() {
		final Map<String, Object> pathParams = Collections.singletonMap("name", "John");
		RestAssuredWebTestClient.given()
				.pathParams(pathParams)
			.when()
				.request(Method.GET, "/greeting/{name}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void should_succeed_with_request_method_when_named_parameter_is_multi_map_value() {
		final Map<String, Object> pathParams = new HashMap<>();
		pathParams.put("name", "John");
		pathParams.put("date", "2023-08-30");

		RestAssuredWebTestClient.given()
				.pathParams(pathParams)
			.when()
				.request(Method.GET, "/greeting/{name}/{date}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John! Today is 2023-08-30"));
	}

	@Test
	public void should_succeed_with_request_method_when_named_parameter_as_value_pairs() {
		RestAssuredWebTestClient.given()
				.pathParams("name", "John", "date", "2023-08-30")
			.when()
				.request(Method.GET, "/greeting/{name}/{date}")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John! Today is 2023-08-30"));
	}

	@Test
	public void should_succeed_with_request_method_when_has_only_unnamed_parameter() {
		RestAssuredWebTestClient.given()
			.when()
				.request(Method.GET, "/greeting/{name}/{date}", "John", "2023-08-30")
			.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John! Today is 2023-08-30"));
	}

	@Test
	public void should_fail_with_request_method_when_does_not_has_params() {
		// Supplier created to fix sonar error.
		Supplier<Void> restAssuredExecutionSupplier = () -> {
			RestAssuredWebTestClient.given()
				.when()
					.request(Method.GET, "/greeting/{name}/{date}")
				.then();

			return null;
		};

		final Exception ex = Assert.assertThrows(IllegalArgumentException.class, restAssuredExecutionSupplier::get);
		Assert.assertEquals("No values were found for the request's pathParams.", ex.getMessage());
	}
}

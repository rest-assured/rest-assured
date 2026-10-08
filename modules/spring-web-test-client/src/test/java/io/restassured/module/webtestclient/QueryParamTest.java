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
import io.restassured.module.webtestclient.setup.QueryParamsProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public class QueryParamTest {

	@Test
	public void param_with_int() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(new GreetingController())
				.queryParam("name", "John")
				.when()
				.get("/greeting")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void query_param() {
		RouterFunction<ServerResponse> queryParamsRoute = RouterFunctions.route(path("/queryParam")
				.and(method(HttpMethod.GET)), new QueryParamsProcessor()::processQueryParams);
		RestAssuredWebTestClient.given()
				.standaloneSetup(queryParamsRoute)
				.queryParam("name", "John")
				.queryParam("message", "Good!")
				.when()
				.get("/queryParam")
				.then().log().all()
				.body("name", equalTo("Hello, John!"))
				.body("message", equalTo("Good!"))
				.body("_link", equalTo("/queryParam?name=John&message=Good%21"));
	}

	// Issue 1904
	private static final RouterFunction<ServerResponse> ECHO_QUERY_PARAMS_ROUTE = RouterFunctions.route(path("/echo"),
			request -> ok().contentType(MediaType.TEXT_PLAIN).bodyValue(request.queryParams().toString()));

	@Test
	public void query_param_values_with_reserved_and_special_characters_are_sent_as_given() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(ECHO_QUERY_PARAMS_ROUTE)
				.queryParam("plus", "a+b")
				.queryParam("amp", "a&b")
				.queryParam("eq", "a=b")
				.queryParam("braces", "{x}")
				.queryParam("space", "a b")
				.queryParam("percent", "100%")
				.queryParam("encoded", "%20")
				.queryParam("unicode", "\u00e5\u00e4\u00f6")
				.queryParam("a+b&c=d", "name")
				.when()
				.get("/echo")
				.then()
				.body(equalTo("{plus=[a+b], amp=[a&b], eq=[a=b], braces=[{x}], space=[a b], percent=[100%], encoded=[%20], unicode=[\u00e5\u00e4\u00f6], a+b&c=d=[name]}"));
	}

	@Test
	public void query_param_with_multiple_values_containing_reserved_characters_are_sent_as_given() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(ECHO_QUERY_PARAMS_ROUTE)
				.queryParam("list", "a+b", "c&d")
				.queryParam("other", Arrays.asList("e=f", "g h"))
				.when()
				.get("/echo")
				.then()
				.body(equalTo("{list=[a+b, c&d], other=[e=f, g h]}"));
	}

	@Test
	public void query_param_without_value_is_sent_without_value() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(ECHO_QUERY_PARAMS_ROUTE)
				.queryParam("flag")
				.queryParam("name", "a+b")
				.when()
				.get("/echo")
				.then()
				.body(equalTo("{flag=[null], name=[a+b]}"));
	}

	@Test
	public void params_in_get_request_with_reserved_characters_are_sent_as_given() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(ECHO_QUERY_PARAMS_ROUTE)
				.param("plus", "a+b")
				.param("amp", "a&b", "{x}")
				.when()
				.get("/echo")
				.then()
				.body(equalTo("{plus=[a+b], amp=[a&b, {x}]}"));
	}

	@Test
	public void query_params_in_path_are_combined_with_query_params_with_reserved_characters() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(ECHO_QUERY_PARAMS_ROUTE)
				.queryParam("name", "a+b")
				.when()
				.get("/echo?first=one&second=two")
				.then()
				.body(equalTo("{first=[one], second=[two], name=[a+b]}"));
	}
}

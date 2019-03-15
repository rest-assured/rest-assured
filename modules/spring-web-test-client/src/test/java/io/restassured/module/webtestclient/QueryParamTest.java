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
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

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
				.body("_link", equalTo("/queryParam?name=John&message=Good!"));
	}

}

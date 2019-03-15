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

import io.restassured.http.ContentType;
import io.restassured.module.webtestclient.setup.GreetingController;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;

public class WebTestClientNonStaticRequestAndResponseSpecTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void
	request_and_response_spec_can_be_defined_statically() {
		RestAssuredWebTestClient.given()
				.spec(new WebTestClientRequestSpecBuilder()
						.setContentType(ContentType.JSON)
						.addHeader("accept", ContentType.JSON.toString())
						.build())
				.standaloneSetup(new GreetingController())
				.formParam("name", "Johan")
				.when()
				.post("/greeting")
				.then()
				.statusCode(200)
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, Johan!"));
	}
}

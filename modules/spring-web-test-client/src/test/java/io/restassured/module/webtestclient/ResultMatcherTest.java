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
import io.restassured.module.webtestclient.setup.HeaderController;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

public class ResultMatcherTest {

	@BeforeClass
	public static void configureWebTestClientInstance() {
		RestAssuredWebTestClient.standaloneSetup(new HeaderController());
	}

	@AfterClass
	public static void restRestAssured() {
		RestAssuredWebTestClient.reset();
	}

	@Test
	public void
	unnamed_path_params_works() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(new GreetingController())
				.when()
				.get("/greeting?name={name}", "Johan")
				.then()
				.header("Content-Type", APPLICATION_JSON_UTF8_VALUE)
				.statusCode(HttpStatus.OK.value())
				.body("id", Matchers.equalTo(1))
				.body("content", Matchers.equalTo("Hello, Johan!"));
	}
}
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

import io.restassured.module.webtestclient.setup.MultiValueController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE;
import static io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig.config;
import static io.restassured.module.webtestclient.config.WebTestClientParamConfig.paramConfig;
import static org.hamcrest.Matchers.equalTo;

public class WebTestClientParamConfigTest {

	@Before
	public void configureWebTestClientInstance() {
		RestAssuredWebTestClient.standaloneSetup(new MultiValueController());
	}

	@After
	public void restRestAssured() {
		RestAssuredWebTestClient.reset();
	}

	@Test
	public void
	merges_request_params_by_default() {
		RestAssuredWebTestClient
				.given()
				.param("list", "value1")
				.param("list", "value2")
				.when()
				.get("/multiValueParam")
				.then()
				.body("list", equalTo("value1,value2"));
	}

	@Test
	public void
	merges_query_params_by_default() {
		RestAssuredWebTestClient.given()
				.queryParam("list", "value1")
				.queryParam("list", "value2")
				.when()
				.get("/multiValueParam")
				.then()
				.body("list", equalTo("value1,value2"));
	}

	@Test
	public void
	merges_form_params_by_default() {
		RestAssuredWebTestClient.given()
				.formParam("list", "value1")
				.formParam("list", "value2")
				.when()
				.post("/multiValueParam")
				.then()
				.body("list", equalTo("value1,value2"));
	}

	@Test
	public void
	replaces_request_params_when_configured_to_do_so() {
		RestAssuredWebTestClient.given()
				.config(config().paramConfig(paramConfig().requestParamsUpdateStrategy(REPLACE)))
				.param("list", "value1")
				.param("list", "value2")
				.queryParam("list2", "value3")
				.queryParam("list2", "value4")
				.formParam("list3", "value5")
				.formParam("list3", "value6")
				.when()
				.post("/threeMultiValueParam")
				.then()
				.body("list", equalTo("value2"))
				.body("list2", equalTo("value3,value4"))
				.body("list3", equalTo("value5,value6"));
	}

	@Test
	public void
	replaces_query_params_when_configured_to_do_so() {
		RestAssuredWebTestClient.given()
				.config(config().paramConfig(paramConfig().queryParamsUpdateStrategy(REPLACE)))
				.param("list", "value1")
				.param("list", "value2")
				.queryParam("list2", "value3")
				.queryParam("list2", "value4")
				.formParam("list3", "value5")
				.formParam("list3", "value6")
				.when()
				.post("/threeMultiValueParam")
				.then()
				.body("list", equalTo("value1,value2"))
				.body("list2", equalTo("value4"))
				.body("list3", equalTo("value5,value6"));
	}

	@Test
	public void
	replaces_form_params_when_configured_to_do_so() {
		RestAssuredWebTestClient.given()
				.config(config().paramConfig(paramConfig().formParamsUpdateStrategy(REPLACE)))
				.param("list", "value1")
				.param("list", "value2")
				.queryParam("list2", "value3")
				.queryParam("list2", "value4")
				.formParam("list3", "value5")
				.formParam("list3", "value6")
				.when()
				.post("/threeMultiValueParam")
				.then()
				.body("list", equalTo("value1,value2"))
				.body("list2", equalTo("value3,value4"))
				.body("list3", equalTo("value6"));
	}

	@Test
	public void
	replaces_all_parameters_when_configured_to_do_so() {
		RestAssuredWebTestClient.given()
				.config(config().paramConfig(paramConfig().replaceAllParameters()))
				.param("list", "value1")
				.param("list", "value2")
				.queryParam("list2", "value3")
				.queryParam("list2", "value4")
				.formParam("list3", "value5")
				.formParam("list3", "value6")
				.when()
				.post("/threeMultiValueParam")
				.then()
				.body("list", equalTo("value2"))
				.body("list2", equalTo("value4"))
				.body("list3", equalTo("value6"));
	}

	@Test
	public void
	merges_all_parameters_when_configured_to_do_so() {
		RestAssuredWebTestClient.config = config().paramConfig(paramConfig().replaceAllParameters());

		RestAssuredWebTestClient.given()
				.config(config().paramConfig(paramConfig().mergeAllParameters()))
				.param("list", "value1")
				.param("list", "value2")
				.queryParam("list2", "value3")
				.queryParam("list2", "value4")
				.formParam("list3", "value5")
				.formParam("list3", "value6")
				.when()
				.post("/threeMultiValueParam")
				.then()
				.body("list", equalTo("value1,value2"))
				.body("list2", equalTo("value3,value4"))
				.body("list3", equalTo("value5,value6"));
	}
}

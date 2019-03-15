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

import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.internal.WebTestClientRequestSpecificationImpl;
import io.restassured.module.webtestclient.setup.GreetingController;
import io.restassured.module.webtestclient.setup.PostController;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.path.json.config.JsonPathConfig;
import org.apache.commons.io.output.WriterOutputStream;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.PrintStream;
import java.io.StringWriter;

import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.SessionConfig.sessionConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;

public class WebTestClientSpecificationMergingTest {

	@Test
	public void
	query_params_are_merged() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given()
				.queryParam("param2", "value2").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getQueryParams())
				.containsOnly(entry("param1", "value1"), entry("param2", "value2"));
	}

	private WebTestClientRequestSpecificationImpl implementation(WebTestClientRequestSpecification spec) {
		return (WebTestClientRequestSpecificationImpl) spec;
	}

	@Test
	public void
	form_params_are_merged() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addFormParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given()
				.formParam("param2", "value2").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getFormParams())
				.containsOnly(entry("param1", "value1"), entry("param2", "value2"));
	}

	@Test
	public void
	params_are_merged() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given()
				.param("param2", "value2").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getParams())
				.containsOnly(entry("param1", "value1"), entry("param2", "value2"));
	}

	@Test
	public void
	attributes_are_merged() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addAttribute("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given()
				.attribute("param2", "value2").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getAttributes())
				.containsOnly(entry("param1", "value1"), entry("param2", "value2"));
	}

	@Test
	public void
	multi_parts_are_merged() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addMultiPart("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given()
				.multiPart("param2", "value2").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getMultiParts()).hasSize(2);
	}

	@Test
	public void
	request_body_is_overwritten_when_defined_in_specification() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder().setBody("body2").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().body("body1").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getRequestBody()).isEqualTo("body2");
	}

	@Test
	public void
	request_body_is_not_overwritten_when_not_defined_in_specification() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().body("body1").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getRequestBody()).isEqualTo("body1");
		Assertions.assertThat(implementation(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
	}

	@Test
	public void
	base_path_is_overwritten_when_defined_in_specification() {
		// Given
		RestAssuredWebTestClient.basePath = "/something";
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.setBasePath("basePath").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().body("body1").spec(specToMerge);

		// Then
		RestAssuredWebTestClient.reset();
		Assertions.assertThat(implementation(spec).getBasePath()).isEqualTo("basePath");
	}

	@Test
	public void
	base_path_is_not_overwritten_when_not_defined_in_specification() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().body("body1").spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getBasePath()).isEqualTo(RestAssuredWebTestClient.basePath);
		Assertions.assertThat(implementation(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
	}

	@Test
	public void
	web_test_client_instance_is_overwritten_when_defined_in_specification() {
		// Given
		WebTestClient otherWebTestClientInstance = WebTestClient.bindToController(new PostController()).build();
		WebTestClient thisWebTestClientInstance = WebTestClient.bindToController(new GreetingController()).build();

		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.setWebTestClient(otherWebTestClientInstance).build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given()
				.webTestClient(thisWebTestClientInstance).spec(specToMerge);

		// Then
		WebTestClient webTestClient = Whitebox.getInternalState(implementation(spec)
				.getWebTestClientFactory(), "webTestClient");
		assertThat(webTestClient).isSameAs(otherWebTestClientInstance);
	}

	@Test
	public void
	web_test_client_factory_is_not_overwritten_when_not_defined_in_specification() {
		// Given
		WebTestClient webTestClientInstance = WebTestClient.bindToController(new GreetingController()).build();
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder().addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().webTestClient(webTestClientInstance).spec(specToMerge);

		// Then
		WebTestClient webTestClient = Whitebox.getInternalState(implementation(spec).getWebTestClientFactory(),
				"webTestClient");
		assertThat(webTestClient).isSameAs(webTestClientInstance);
		Assertions.assertThat(implementation(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
	}

	@Test
	public void
	cookies_are_merged_when_defined_in_specification() {
		// Given
		Cookie otherCookie = new Cookie.Builder("cookie1", "value1").build();
		Cookie thisCookie = new Cookie.Builder("cookie2", "value2").build();

		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder().addCookie(otherCookie).build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().cookie(thisCookie).spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getCookies()).containsOnly(thisCookie, otherCookie);
	}

	@Test
	public void
	cookies_are_not_overwritten_when_not_defined_in_specification() {
		// Given
		Cookie thisCookie = new Cookie.Builder("cookie2", "value2").build();
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().cookie(thisCookie).spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getCookies()).containsOnly(thisCookie);
		Assertions.assertThat(implementation(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
	}

	@Test
	public void
	content_type_is_overwritten_when_defined_in_specification() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.setContentType(ContentType.JSON).build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().contentType(ContentType.XML)
				.spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getRequestContentType()).isEqualTo(ContentType.JSON.toString());
	}

	@Test
	public void
	content_type_is_not_overwritten_when_not_defined_in_specification() {
		// Given
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().contentType(ContentType.XML)
				.spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getRequestContentType()).isEqualTo(ContentType.XML.toString());
		Assertions.assertThat(implementation(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
	}

	@Test
	public void
	headers_are_merged_when_defined_in_specification() {
		// Given
		Header otherHeader = new Header("header1", "value1");
		Header thisHeader = new Header("header2", "value2");

		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder().addHeader(otherHeader)
				.build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().header(thisHeader).spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getRequestHeaders()).containsOnly(thisHeader, otherHeader);
	}

	@Test
	public void
	headers_are_not_overwritten_when_not_defined_in_specification() {
		// Given
		Header thisHeader = new Header("cookie2", "value2");
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().header(thisHeader).spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getRequestHeaders()).containsOnly(thisHeader);
		Assertions.assertThat(implementation(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
	}

	@Test
	public void
	configs_of_same_type_are_overwritten_when_defined_in_specification() {
		// Given
		RestAssuredWebTestClientConfig otherConfig = new RestAssuredWebTestClientConfig()
				.with().jsonConfig(jsonConfig()
						.with().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
		RestAssuredWebTestClientConfig thisConfig = new RestAssuredWebTestClientConfig()
				.with().jsonConfig(jsonConfig()
						.with().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.setConfig(otherConfig).build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().config(thisConfig).spec(specToMerge);

		// Then
		assertThat(implementation(spec).getRestAssuredWebTestClientConfig().getJsonConfig().numberReturnType())
				.isEqualTo(JsonPathConfig.NumberReturnType.BIG_DECIMAL);
	}

	@Test
	public void
	config_is_not_overwritten_when_not_defined_in_specification() {
		// Given
		RestAssuredWebTestClientConfig thisConfig = new RestAssuredWebTestClientConfig()
				.with().jsonConfig(jsonConfig()
						.with().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.addQueryParam("param1", "value1").build();

		// When
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().config(thisConfig).spec(specToMerge);

		// Then
		Assertions.assertThat(implementation(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
		assertThat(implementation(spec).getRestAssuredWebTestClientConfig()
				.getJsonConfig().numberReturnType()).isEqualTo(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE);
	}

	@Test
	public void
	logging_is_overwritten_when_defined_in_specification() {
		// Given
		StringWriter writer = new StringWriter();
		PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.setConfig(RestAssuredWebTestClientConfig.newConfig()
						.logConfig(LogConfig.logConfig().defaultStream(captor))).and().log(LogDetail.ALL).build();

		// When
		RestAssuredWebTestClient.given()
				.log().params()
				.spec(specToMerge)
				.standaloneSetup(new GreetingController())
				.when()
				.get("/greeting?name={name}", "Johan")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, Johan!"));

		// Then
		assertThat(writer.toString()).isEqualTo(String.format("Request method:\tGET%n" +
				"Request URI:\thttp://localhost:8080/greeting?name=Johan%n" +
				"Proxy:\t\t\t<none>%n" +
				"Request params:\t<none>%n" +
				"Query params:\t<none>%n" +
				"Form params:\t<none>%n" +
				"Path params:\t<none>%n" +
				"Headers:\t\t<none>%n" +
				"Cookies:\t\t<none>%n" +
				"Multiparts:\t\t<none>%n" +
				"Body:\t\t\t<none>%n"
		));
	}

	@Test
	public void
	logging_is_not_overwritten_when_not_defined_in_specification() {
		// Given
		StringWriter writer = new StringWriter();
		PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder()
				.setConfig(RestAssuredWebTestClientConfig.newConfig()
						.logConfig(LogConfig.logConfig().defaultStream(captor))).
						addQueryParam("name", "Johan").build();

		// When
		RestAssuredWebTestClient.given()
				.spec(specToMerge)
				.log().params()
				.standaloneSetup(new GreetingController())
				.when()
				.get("/greeting")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, Johan!"));

		// Then
		assertThat(writer.toString()).isEqualTo(String.format("Request params:\t<none>%n" +
				"Query params:\tname=Johan%n" +
				"Form params:\t<none>%n" +
				"Path params:\t<none>%n" +
				"Multiparts:\t\t<none>%n"
		));
	}

	@Test
	public void
	configurations_are_merged() {
		// Given
		RestAssuredWebTestClientConfig cfg1 = new RestAssuredWebTestClientConfig()
				.with().jsonConfig(jsonConfig()
						.with().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));
		WebTestClientRequestSpecification specToMerge = new WebTestClientRequestSpecBuilder().setConfig(cfg1).build();

		// When
		RestAssuredWebTestClientConfig cfg2 = new RestAssuredWebTestClientConfig()
				.sessionConfig(sessionConfig().sessionIdName("php"));
		WebTestClientRequestSpecification spec = RestAssuredWebTestClient.given().config(cfg2).spec(specToMerge);

		// Then
		RestAssuredConfig mergedConfig = implementation(spec).getRestAssuredConfig();
		assertThat(mergedConfig.getSessionConfig().sessionIdName()).isEqualTo("php");
		assertThat(mergedConfig.getJsonConfig().numberReturnType())
				.isEqualTo(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE);
	}
}

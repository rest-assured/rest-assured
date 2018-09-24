/*
 * Copyright 2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import io.restassured.module.webtestclient.http.AttributeController;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.equalTo;

public class AttributeTest {

	@BeforeClass
	public static void configureWebTestClientInstance() {
		RestAssuredWebTestClient.standaloneSetup(new AttributeController());
	}

	@AfterClass
	public static void restRestAssured() {
		RestAssuredWebTestClient.reset();
	}

	@Test
	public void shouldEnterEndpoint() {
		WebTestClient.bindToController(new AttributeController())
				.build()
				.get()
				.uri("/attribute")
				.attribute("testAttribute", "John Doe")
				.exchange()
				.returnResult(String.class)
				.getResponseBody()
				.blockFirst();

	}

	@Test
	public void
	can_send_attribute_using_attribute_name_and_value() {
		RestAssuredWebTestClient.given().
				attribute("testAttribute", "John Doe").
				when().
				get("/attribute").
				then().
				statusCode(200).
				body("testAttribute", equalTo("John Doe"));
	}

	@Test
	public void
	can_send_attributes_using_map() {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("testAttribute1", "value1");
		attributes.put("testAttribute2", "value2");
		RestAssuredWebTestClient.given().
				attributes(attributes).
				when().
				get("/attribute").
				then().
				statusCode(200).
				body("testAttribute1", equalTo("value1")).
				body("testAttribute2", equalTo("value2"));
	}
}

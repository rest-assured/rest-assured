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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.snippet.SnippetException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

public class RestDocsTest {

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void path_parameters_are_automatically_supported() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(new GreetingController(), documentationConfiguration(restDocumentation))
				.queryParam("name", "John")
				.when()
				.consumeWith(document("greeting",
						pathParameters(
								parameterWithName("path").description("The path to greeting")),
						responseFields(
								fieldWithPath("id").description("The ID of the greeting"),
								fieldWithPath("content").description("The content of the greeting"))
				))
				.get("/{path}", "greeting")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, John!"));
	}

	@Test
	public void document_generation_is_executed() {
		exception.expect(SnippetException.class);
		exception.expectMessage("Path parameters with the following names were not documented: [path]. " +
				"Path parameters with the following names were not found in the request: [xxx]");
		RestAssuredWebTestClient.given()
				.standaloneSetup(new GreetingController(), documentationConfiguration(restDocumentation))
				.queryParam("name", "John")
				.when()
				.consumeWith(document("greeting",
						pathParameters(
								parameterWithName("xxx").description("The path to greeting")),
						responseFields(
								fieldWithPath("id").description("The ID of the greeting"),
								fieldWithPath("content").description("The content of the greeting"))
				))
				.get("/{path}", "greeting");
	}
}

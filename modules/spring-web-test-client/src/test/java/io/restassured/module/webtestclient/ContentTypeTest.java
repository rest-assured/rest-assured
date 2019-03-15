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

import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.module.webtestclient.setup.ContentTypeProcessor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class ContentTypeTest {

	@BeforeClass
	public static void configureWebTestClientInstance() {
		RouterFunction<ServerResponse> setAttributesRoute = route(path("/contentType")
				.and(method(GET)), new ContentTypeProcessor()::processContentType);
		RestAssuredWebTestClient.standaloneSetup(setAttributesRoute);
	}

	@Test
	public void
	adds_default_charset_to_content_type_by_default() {
		RestAssuredWebTestClient.given()
				.contentType(ContentType.JSON)
				.when()
				.get("/contentType")
				.then()
				.body("requestContentType", equalTo("application/json;charset="
						+ RestAssuredWebTestClient.config().getEncoderConfig().defaultContentCharset()));
	}

	@Test
	public void
	adds_specific_charset_to_content_type_by_default() {
		RestAssuredWebTestClient.given()
				.config(RestAssuredWebTestClient.config().encoderConfig(EncoderConfig.encoderConfig()
						.defaultCharsetForContentType(UTF_16.toString(), ContentType.JSON)))
				.contentType(ContentType.JSON)
				.when()
				.get("/contentType")
				.then()
				.statusCode(200)
				.body("requestContentType", equalTo("application/json;charset=" + UTF_16.toString()))
				.body("requestContentType", not(contains(RestAssuredWebTestClient.config().getEncoderConfig()
						.defaultContentCharset())));
	}

	@Test
	public void
	doesnt_add_default_charset_to_content_type_if_charset_is_defined_explicitly() {
		RestAssuredWebTestClient.given()
				.contentType(ContentType.JSON.withCharset("UTF-16"))
				.when()
				.get("/contentType")
				.then()
				.statusCode(200)
				.body("requestContentType", equalTo("application/json;charset=UTF-16"));
	}

	@Test
	public void
	doesnt_add_default_charset_to_content_type_if_configured_not_to_do_so() {
		RestAssuredWebTestClient.given()
				.config(RestAssuredWebTestClient.config()
						.encoderConfig(EncoderConfig.encoderConfig()
								.appendDefaultContentCharsetToContentTypeIfUndefined(false)))
				.contentType(ContentType.JSON)
				.when()
				.get("/contentType")
				.then()
				.statusCode(200)
				.body("requestContentType", equalTo("application/json"));
	}
}

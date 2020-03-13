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

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.http.ContentType;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.setup.PostController;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder;
import org.apache.commons.io.output.WriterOutputStream;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.web.util.UriUtils;

import java.io.PrintStream;
import java.io.StringWriter;

import static io.restassured.filter.log.LogDetail.HEADERS;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig.config;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LoggingIfValidationFailsTest {

	private StringWriter writer;
	private PrintStream captor;

	@Before
	public void
	given_writer_and_captor_is_initialized() {
		writer = new StringWriter();
		captor = new PrintStream(new WriterOutputStream(writer, defaultCharset()), true);
	}

	@After
	public void
	reset_rest_assured() {
		RestAssuredWebTestClient.reset();
	}
	public static void assertJSONEqual(String s1, String s2) throws JSONException {
		JSONAssert.assertEquals(s1, s2, false);
	}
	@Test
    public void logging_of_both_request_and_response_validation_works_when_test_fails() throws JSONException {
		RestAssuredWebTestClient.config = new RestAssuredWebTestClientConfig()
				.logConfig(new LogConfig(captor, true)
						.enableLoggingOfRequestAndResponseIfValidationFails());

		try {
			given()
					.standaloneSetup(new PostController())
					.param("name", "Johan")
					.when()
					.post("/greetingPost")
					.then()
					.body("id", equalTo(1))
					.body("content", equalTo("Hello, Johan2!"));

			fail("Should throw AssertionError");
		} catch (AssertionError e) {
			String writerString1 = writer.toString();
			String headerString1 = String.format("Request method:\tPOST%n" +
							"Request URI:\thttp://localhost:8080/greetingPost%n" +
							"Proxy:\t\t\t<none>%n" +
							"Request params:\tname=Johan%n" +
							"Query params:\t<none>%n" +
							"Form params:\t<none>%n" +
							"Path params:\t<none>%n" +
							"Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%n" +
							"Cookies:\t\t<none>%n" +
							"Multiparts:\t\t<none>%n" +
							"Body:\t\t\t<none>%n" +
							"%n" +
							"200%n" +
							"Content-Type: application/json;charset=UTF-8%n" +
							"Content-Length: 34%n" +
							"%n",
					config().getEncoderConfig().defaultContentCharset());
			assertThat(writerString1, startsWith(headerString1));
			assertJSONEqual(writerString1.replace(headerString1, "").trim(),
					        "{\n" +
							"    \"id\": 1,\n" +
							"    \"content\": \"Hello, Johan!\"\n" +
							"}%n");
		}
	}

	@Test
    public void logging_of_both_request_with_uri_function_and_response_validation_works_when_test_fails() throws JSONException {
        RestAssuredWebTestClient.config = new RestAssuredWebTestClientConfig()
                .logConfig(new LogConfig(captor, true)
                        .enableLoggingOfRequestAndResponseIfValidationFails());

        try {
            given()
                    .standaloneSetup(new PostController())
                    .contentType(ContentType.URLENC)
                    .when()
                    .post(uriBuilder -> uriBuilder.path("/greetingPost").queryParam("name", "Johan").build())
                    .then()
                    .body("id", equalTo(1))
                    .body("content", equalTo("Hello, Johan2!"));

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), containsString(UriUtils.encode("Request from uri function", defaultCharset())));
            assertThat(writer.toString(), containsString(String.format("Request method:\tPOST%n")));
			String writerString2 = writer.toString();
			String headerString2= String.format("Proxy:\t\t\t<none>%n" +
					"Request params:\t<none>%n" +
					"Query params:\t<none>%n" +
					"Form params:\t<none>%n" +
					"Path params:\t<none>%n" +
					"Headers:\t\tContent-Type=application/x-www-form-urlencoded%n" +
					"Cookies:\t\t<none>%n" +
					"Multiparts:\t\t<none>%n" +
					"Body:\t\t\t<none>%n" +
					"%n" +
					"200%n" +
					"Content-Type: application/json;charset=UTF-8%n" +
					"Content-Length: 34%n" +
					"%n"
			);
			assertThat(writerString2, containsString(headerString2));
			assertJSONEqual(writerString2.replace(writerString2.substring(0, writerString2.indexOf("{")), "").trim(),
					"{\n" +
					"    \"id\": 1,\n" +
					"    \"content\": \"Hello, Johan!\"\n" +
					"}");
		}
	}

    @Test
    public void logging_of_both_request_and_response_validation_works_when_test_fails_when_configured_with_log_detail() {
		RestAssuredWebTestClient.config = new RestAssuredWebTestClientConfig()
				.logConfig(new LogConfig(captor, true)
						.enableLoggingOfRequestAndResponseIfValidationFails(HEADERS));

		try {
			given()
					.standaloneSetup(new PostController())
					.param("name", "Johan")
					.when()
					.post("/greetingPost")
					.then()
					.body("id", equalTo(1))
					.body("content", equalTo("Hello, Johan2!"));

			fail("Should throw AssertionError");
		} catch (AssertionError e) {
			assertThat(writer.toString(), equalTo(String.format("" +
							"Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%n" +
							"%n" +
							"Content-Type: application/json;charset=UTF-8%n" +
							"Content-Length: 34%n",
					config().getEncoderConfig().defaultContentCharset())));
		}
	}

	@Test
	public void
	logging_of_both_request_and_response_validation_works_when_test_fails_when_using_static_response_and_request_specs_declared_before_enable_logging() {
		final StringWriter writer = new StringWriter();
		final PrintStream captor = new PrintStream(new WriterOutputStream(writer, UTF_8), true);

		RestAssuredWebTestClient.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
		RestAssuredWebTestClient.requestSpecification = new WebTestClientRequestSpecBuilder()
				.setConfig(config().logConfig(new LogConfig(captor, true)))
				.addHeader("Api-Key", "1234").build();

		RestAssuredWebTestClient.enableLoggingOfRequestAndResponseIfValidationFails(HEADERS);

		try {
			given()
					.standaloneSetup(new PostController())
					.param("name", "Johan")
					.when()
					.post("/greetingPost")
					.then()
					.body("id", equalTo(1))
					.body("content", equalTo("Hello, Johan2!"));

			fail("Should throw AssertionError");
		} catch (AssertionError e) {
			assertThat(writer.toString(), equalTo(String.format("Headers:\t\tApi-Key=1234%n" +
							"\t\t\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%n" +
							"%n" +
							"Content-Type: application/json;charset=UTF-8%n" +
							"Content-Length: 34%n",
					RestAssuredWebTestClientConfig.config().getEncoderConfig().defaultContentCharset())
			));
		}
	}

	@Test
	public void
	doesnt_log_if_request_or_response_when_validation_succeeds_when_request_and_response_logging_if_validation_fails_is_enabled() {
		RestAssuredWebTestClient.config = new RestAssuredWebTestClientConfig()
				.logConfig(new LogConfig(captor, true)
						.enableLoggingOfRequestAndResponseIfValidationFails());

		given()
				.standaloneSetup(new PostController())
				.param("name", "Johan")
				.when()
				.post("/greetingPost")
				.then()
				.body("id", equalTo(1))
				.body("content", equalTo("Hello, Johan!"));

		assertThat(writer.toString(), emptyString());
	}

	@Test
	public void
	logging_is_applied_when_using_non_static_response_specifications() {
		RestAssuredWebTestClient.config = new RestAssuredWebTestClientConfig()
				.logConfig(new LogConfig(captor, true)
						.enableLoggingOfRequestAndResponseIfValidationFails());

		try {
			given()
					.standaloneSetup(new PostController())
					.param("name", "Johan")
					.when()
					.post("/greetingPost")
					.then()
					.spec(new ResponseSpecBuilder()
							.expectBody("id", equalTo(2))
							.expectBody("content", equalTo("Hello, Johan2!"))
							.build());
			fail("Should throw AssertionError");
		} catch (AssertionError e) {
			assertThat(writer.toString(), not(emptyOrNullString()));
		}
	}
}

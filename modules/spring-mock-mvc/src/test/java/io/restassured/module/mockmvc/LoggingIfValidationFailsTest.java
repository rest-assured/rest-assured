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

package io.restassured.module.mockmvc;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.http.PostController;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static io.restassured.filter.log.LogDetail.HEADERS;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig.config;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

// @formatter:off
public class LoggingIfValidationFailsTest {
    private StringWriter writer;
    private PrintStream captor;

    @Before public void
    given_writer_and_captor_is_initialized() {
        writer = new StringWriter();
        captor = new PrintStream(new WriterOutputStream(writer), true);
    }

    @After public void
    reset_rest_assured() throws Exception {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    logging_of_both_request_and_response_validation_works_when_test_fails() {
        RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(new LogConfig(captor, true).enableLoggingOfRequestAndResponseIfValidationFails());

        try {
            given().
                    standaloneSetup(new PostController()).
                    param("name", "Johan").
            when().
                    post("/greetingPost").
            then().
                    body("id", equalTo(1)).
                    body("content", equalTo("Hello, Johan2!"));

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo(String.format("Request method:\tPOST%n" +
                            "Request URI:\thttp://localhost:8080/greetingPost%n" +
                            "Proxy:\t\t\t<none>%n" +
                            "Request params:\tname=Johan%n" +
                            "Query params:\t<none>%n" +
                            "Form params:\t<none>%n" +
                            "Path params:\t<none>%n" +
                            "Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%n" +
                            "Cookies:\t\t<none>%n" +
                            "Multiparts:\t\t<none>" +
                            "%nBody:\t\t\t<none>%n" +
                            "%n" +
                            "200%n" +
                            "Content-Type: application/json;charset=UTF-8%n" +
                            "%n" +
                            "{\n    \"id\": 1,\n    \"content\": \"Hello, Johan!\"\n}%n",
                    RestAssuredMockMvcConfig.config().getEncoderConfig().defaultContentCharset())));
        }
    }

    @Test public void
    logging_of_both_request_and_response_validation_works_when_test_fails_when_configured_with_log_detail() {
        RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(new LogConfig(captor, true).enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.HEADERS));

        try {
            given().
                    standaloneSetup(new PostController()).
                    param("name", "Johan").
            when().
                    post("/greetingPost").
            then().
                    body("id", equalTo(1)).
                    body("content", equalTo("Hello, Johan2!"));

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo(String.format("Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%n" +
                            "%n" +
                            "Content-Type: application/json;charset=UTF-8%n",
                    RestAssuredMockMvcConfig.config().getEncoderConfig().defaultContentCharset())));
        }
    }

    @Test public void
    logging_of_both_request_and_response_validation_works_when_test_fails_when_using_static_response_and_request_specs_declared_before_enable_logging() {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        RestAssuredMockMvc.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().setConfig(config().logConfig(new LogConfig(captor, true))).
                addHeader("Api-Key", "1234").build();

        RestAssuredMockMvc.enableLoggingOfRequestAndResponseIfValidationFails(HEADERS);

        try {
            given().
                    standaloneSetup(new PostController()).
                    param("name", "Johan").
            when().
                    post("/greetingPost").
            then().
                    body("id", equalTo(1)).
                    body("content", equalTo("Hello, Johan2!"));

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo(String.format("Headers:\t\tApi-Key=1234%n" +
                            "\t\t\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%n" +
                            "%n" +
                            "Content-Type: application/json;charset=UTF-8%n",
                    RestAssuredMockMvcConfig.config().getEncoderConfig().defaultContentCharset())));
        }
    }

    @Test public void
    doesnt_log_if_request_or_response_when_validation_succeeds_when_request_and_response_logging_if_validation_fails_is_enabled() {
        RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(new LogConfig(captor, true).enableLoggingOfRequestAndResponseIfValidationFails());

        given().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), emptyString());
    }

    @Test public void
    logging_is_applied_when_using_non_static_response_specifications() {
        RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(new LogConfig(captor, true).enableLoggingOfRequestAndResponseIfValidationFails());

        try {
        given().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                spec(new ResponseSpecBuilder().
                        expectBody("id", equalTo(2)).
                        expectBody("content", equalTo("Hello, Johan2!")).
                        build());
            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), not(emptyOrNullString()));
        }
    }
}
// @formatter:on

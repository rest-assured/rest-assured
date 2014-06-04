/*
 * Copyright 2014 the original author or authors.
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

package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import com.jayway.restassured.module.mockmvc.http.PostController;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.filter.log.LogDetail.HEADERS;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.jayway.restassured.module.mockmvc.config.RestAssuredMockMvcConfig.config;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
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
        RestAssured.reset();
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
            assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\t/greetingPost\nRequest params:\tname=Johan\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nMultiparts:\t\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\t\t\t<none>\n\n"+"" +
                    "200\nContent-Type: application/json;charset=UTF-8\n\n{\n    \"id\": 1,\n    \"content\": \"Hello, Johan!\"\n}\n"));
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
            assertThat(writer.toString(), equalTo("Headers:\t\tContent-Type=*/*\n\n" +
                    "Content-Type: application/json;charset=UTF-8\n"));
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
            assertThat(writer.toString(), equalTo("Headers:\t\tContent-Type=*/*\n\t\t\t\tApi-Key=1234\n\nContent-Type: application/json;charset=UTF-8\n"));
        } finally {
            RestAssuredMockMvc.reset();
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

        assertThat(writer.toString(), isEmptyString());
    }
}

// @formatter:on
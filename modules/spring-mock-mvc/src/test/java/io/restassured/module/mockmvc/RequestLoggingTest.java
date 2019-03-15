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

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.http.BasePathController;
import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.http.PostController;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

// @formatter:off
public class RequestLoggingTest {
    private StringWriter writer;

    @Before public void
    given_config_is_stored_in_writer() {
        writer = new StringWriter();
        PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(new LogConfig(captor, true));
    }

    @After public void
    reset_rest_assured() throws Exception {
        RestAssured.reset();
    }

    @Test public void
    logging_param_works() {
        RestAssuredMockMvc.given().
                log().all().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), equalTo(String.format("Request method:\tPOST%n" +
                        "Request URI:\thttp://localhost:8080/greetingPost%n" +
                        "Proxy:\t\t\t<none>%n" +
                        "Request params:\tname=Johan%n" +
                        "Query params:\t<none>%n" +
                        "Form params:\t<none>%n" +
                        "Path params:\t<none>%n" +
                        "Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%nCookies:\t\t<none>%n" +
                        "Multiparts:\t\t<none>%n" +
                        "Body:\t\t\t<none>%n",
                RestAssuredMockMvcConfig.config().getEncoderConfig().defaultContentCharset())));
    }

    @Test public void
    logging_query_param_works() {
        RestAssuredMockMvc.given().
                log().all().
                standaloneSetup(new GreetingController()).
                queryParam("name", "Johan").
        when().
                get("/greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), equalTo(String.format("Request method:\tGET%n" +
                "Request URI:\thttp://localhost:8080/greeting?name=Johan%n" +
                "Proxy:\t\t\t<none>%n" +
                "Request params:\t<none>%n" +
                "Query params:\tname=Johan%n" +
                "Form params:\t<none>%n" +
                "Path params:\t<none>%n" +
                "Headers:\t\t<none>%n" +
                "Cookies:\t\t<none>%n" +
                "Multiparts:\t\t<none>%n" +
                "Body:\t\t\t<none>%n")));
    }

    @Test public void
    logging_form_param_works() {
        RestAssuredMockMvc.given().
                log().all().
                standaloneSetup(new PostController()).
                formParam("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), equalTo(String.format("Request method:\tPOST%n" +
                        "Request URI:\thttp://localhost:8080/greetingPost%n" +
                        "Proxy:\t\t\t<none>%nRequest params:\t<none>%n" +
                        "Query params:\t<none>%n" +
                        "Form params:\tname=Johan%n" +
                        "Path params:\t<none>%n" +
                        "Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%nCookies:\t\t<none>%n" +
                        "Multiparts:\t\t<none>%n" +
                        "Body:\t\t\t<none>%n",
                RestAssuredMockMvcConfig.config().getEncoderConfig().defaultContentCharset())));
    }

    @Test public void
    can_supply_string_as_body_for_post() {
        RestAssuredMockMvc.given().
                standaloneSetup(new PostController()).
                log().all().
                body("a string").
        when().
                post("/stringBody").
        then().
                body(equalTo("a string"));

        assertThat(writer.toString(), equalTo(String.format("Request method:\tPOST%n" +
                "Request URI:\thttp://localhost:8080/stringBody%n" +
                "Proxy:\t\t\t<none>%n" +
                "Request params:\t<none>%n" +
                "Query params:\t<none>%n" +
                "Form params:\t<none>%n" +
                "Path params:\t<none>%n" +
                "Headers:\t\t<none>%n" +
                "Cookies:\t\t<none>%n" +
                "Multiparts:\t\t<none>%n" +
                "Body:%n" +
                "a string%n")));
    }

    @Test public void
    base_path_is_prepended_to_path_when_logging() {
        RestAssuredMockMvc.basePath = "/my-path";

        try {
            RestAssuredMockMvc.given().
                    log().all().
                    standaloneSetup(new BasePathController()).
                    param("name", "Johan").
            when().
                    get("/greetingPath").
            then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
        assertThat(writer.toString(), equalTo(String.format("Request method:\tGET%n" +
                "Request URI:\thttp://localhost:8080/my-path/greetingPath?name=Johan%n" +
                "Proxy:\t\t\t<none>%n" +
                "Request params:\tname=Johan%n" +
                "Query params:\t<none>%n" +
                "Form params:\t<none>%n" +
                "Path params:\t<none>%n" +
                "Headers:\t\t<none>%n" +
                "Cookies:\t\t<none>%n" +
                "Multiparts:\t\t<none>%n" +
                "Body:\t\t\t<none>%n")));
    }

    @Test public void
    logging_if_request_validation_fails_works() {
        try {
            RestAssuredMockMvc.given().
                    log().ifValidationFails().
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
                            "Multiparts:\t\t<none>%n" +
                            "Body:\t\t\t<none>%n",
                    RestAssuredMockMvcConfig.config().getEncoderConfig().defaultContentCharset())));
        }
    }

    @Test public void
    doesnt_log_if_request_validation_succeeds_when_request_logging_if_validation_fails_is_enabled() {
        RestAssuredMockMvc.given().
                log().ifValidationFails().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), emptyString());
    }
}

// @formatter:on

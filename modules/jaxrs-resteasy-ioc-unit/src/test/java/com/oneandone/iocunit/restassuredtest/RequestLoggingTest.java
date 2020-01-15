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

package com.oneandone.iocunit.restassuredtest;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.PrintStream;
import java.io.StringWriter;

import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.BasePathResource;
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;
import com.oneandone.iocunit.restassuredtest.http.PostResource;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;

// @formatter:off
@RunWith(IocUnitRunner.class)
@SutClasses({PostResource.class, GreetingResource.class, BasePathResource.class})
public class RequestLoggingTest {
    private StringWriter writer;
    private PrintStream captor;

    @Before
    public void
    given_config_is_stored_in_writer() {
        writer = new StringWriter();
        captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.config = RestAssured.config().logConfig(new LogConfig(captor, true));
    }

    @Test
    public void
    logging_param_works() {
        RestAssured.given().
                log().all().
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
                                                            // TODO: "Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%nCookies:\t\t<none>%n" +
                                                            "Headers:\t\tAccept=*/*\n\t\t\t\tContent-Type=application/x-www-form-urlencoded; charset=%s%nCookies:\t\t<none>%n" +
                                                            "Multiparts:\t\t<none>%n" +
                                                            "Body:\t\t\t<none>%n",
                RestAssuredConfig.config().getEncoderConfig().defaultContentCharset())));
    }

    @Test
    public void
    logging_query_param_works() {
        RestAssured.given().
                log().all().
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
                                                            // TODO: "Headers:\t\t<none>%n" +
                                                            "Headers:\t\tAccept=*/*%n" +
                                                            "Cookies:\t\t<none>%n" +
                                                            "Multiparts:\t\t<none>%n" +
                                                            "Body:\t\t\t<none>%n")));
    }

    @Test
    public void
    logging_form_param_works() {
        RestAssured.given().
                log().all().
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
                                                            // TODO: "Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%nCookies:\t\t<none>%n" +
                                                            "Headers:\t\tAccept=*/*\n\t\t\t\tContent-Type=application/x-www-form-urlencoded; charset=%s%nCookies:\t\t<none>%n" +
                                                            "Multiparts:\t\t<none>%n" +
                                                            "Body:\t\t\t<none>%n",
                RestAssuredConfig.config().getEncoderConfig().defaultContentCharset())));
    }

    @Test
    public void
    can_supply_string_as_body_for_post() {
        RestAssured.given().
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
                                                            // TODO: "Headers:\t\t<none>%n" +
                                                            "Headers:\t\tAccept=*/*\n\t\t\t\tContent-Type=text/plain; charset=ISO-8859-1%n"+

                                                            "Cookies:\t\t<none>%n" +
                                                            "Multiparts:\t\t<none>%n" +
                                                            "Body:%n" +
                                                            "a string%n")));
    }

    @Test
    public void
    base_path_is_prepended_to_path_when_logging() {
        RestAssured.basePath = "/my-path";

        try {
            RestAssured.given().
                    log().all().
                    param("name", "Johan").
                    when().
                    get("/greetingPath").
                    then().
                    statusCode(200).
                    body("content", equalTo("Hello, Johan!"));
        } finally {
            RestAssured.reset();
        }
        assertThat(writer.toString(), equalTo(String.format("Request method:\tGET%n" +
                                                            "Request URI:\thttp://localhost:8080/my-path/greetingPath?name=Johan%n" +
                                                            "Proxy:\t\t\t<none>%n" +
                                                            "Request params:\tname=Johan%n" +
                                                            "Query params:\t<none>%n" +
                                                            "Form params:\t<none>%n" +
                                                            "Path params:\t<none>%n" +
                                                            "Headers:\t\tAccept=*/*%n" +
                                                            "Cookies:\t\t<none>%n" +
                                                            "Multiparts:\t\t<none>%n" +
                                                            "Body:\t\t\t<none>%n")));
    }

    @Test
    public void
    logging_if_request_validation_fails_works() {
        try {
            RestAssured.given().
                    log().ifValidationFails().
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
                                                                // TODO: "Headers:\t\tContent-Type=application/x-www-form-urlencoded;charset=%s%n" +
                                                                "Headers:\t\tAccept=*/*\n\t\t\t\tContent-Type=application/x-www-form-urlencoded; charset=%s%n" +
                                                                "Cookies:\t\t<none>%n" +
                                                                "Multiparts:\t\t<none>%n" +
                                                                "Body:\t\t\t<none>%n",
                    RestAssuredConfig.config().getEncoderConfig().defaultContentCharset())));
        }
    }

    @Test
    public void
    doesnt_log_if_request_validation_succeeds_when_request_logging_if_validation_fails_is_enabled() {
        RestAssured.given().
                log().ifValidationFails().
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

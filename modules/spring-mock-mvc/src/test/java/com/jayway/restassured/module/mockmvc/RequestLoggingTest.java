/*
 * Copyright 2013 the original author or authors.
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
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import com.jayway.restassured.module.mockmvc.http.BasePathController;
import com.jayway.restassured.module.mockmvc.http.GreetingController;
import com.jayway.restassured.module.mockmvc.http.PostController;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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
        given().
                log().all().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\t/greetingPost\nRequest params:\tname=Johan\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\t\t\t<none>\n"));
    }

    @Test public void
    logging_query_param_works() {
        given().
                log().all().
                standaloneSetup(new GreetingController()).
                queryParam("name", "Johan").
        when().
                get("/greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), equalTo("Request method:\tGET\nRequest path:\t/greeting\nRequest params:\t<none>\nQuery params:\tname=Johan\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\t\t\t<none>\n"));
    }

    @Test public void
    logging_form_param_works() {
        given().
                log().all().
                standaloneSetup(new PostController()).
                formParam("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\t/greetingPost\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\tname=Johan\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\t\t\t<none>\n"));
    }

    @Test public void
    can_supply_string_as_body_for_post() {
        given().
                standaloneSetup(new PostController()).
                log().all().
                body("a string").
        when().
                post("/stringBody").
        then().
                body(equalTo("a string"));

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\t/stringBody\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\na string\n"));
    }

    @Test public void
    base_path_is_prepended_to_path_when_logging() {
        RestAssuredMockMvc.basePath = "/my-path";

        try {
            given().
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

        assertThat(writer.toString(), equalTo("Request method:\tGET\nRequest path:\t/my-path/greetingPath\nRequest params:\tname=Johan\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\t\t\t<none>\n"));
    }
}

// @formatter:on
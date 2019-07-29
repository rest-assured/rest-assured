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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.PostResource;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;

// @formatter:off
@RunWith(IocUnitRunner.class)
@SutClasses({PostResource.class})
public class ResponseLoggingTest {
    private StringWriter writer;

    @Before
    public void
    given_config_is_stored_in_writer() {
        writer = new StringWriter();
        PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.config = RestAssured.config().logConfig(new LogConfig(captor, true));
    }

    @After
    public void
    reset_rest_assured() throws Exception {
        RestAssured.reset();
    }

    @Test
    public void
    logging_if_response_validation_fails_works() {
        try {
            RestAssured.given().
                    param("name", "Johan").
                    when().
                    post("/greetingPost").
                    then().
                    log().ifValidationFails().
                    body("id", equalTo(1)).
                    body("content", equalTo("Hello, Johan2!"));

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo(String.format("200%n" +
                                                               // TODO: "Content-Type: application/json;charset=UTF-8%n" +
                                                                "Content-Type: application/json%n" +
                                                                "%n{\n    \"id\": 1,\n    \"content\": \"Hello, Johan!\"\n}%n")));
        }
    }

    @Test
    public void
    logging_if_response_validation_fails_doesnt_log_anything_if_validation_succeeds() {
        RestAssured.given().
                param("name", "Johan").
                when().
                post("/greetingPost").
                then().
                log().ifValidationFails().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        assertThat(writer.toString(), emptyString());
    }
}

// @formatter:on

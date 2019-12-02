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

import org.json.JSONException;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.http.PostController;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.PrintStream;
import java.io.StringWriter;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

// @formatter:off
public class ResponseLoggingTest {
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

    public void
    assertJsonEqualsNonStrict(String json1, String json2) {
        try {
            JSONAssert.assertEquals(json1, json2, false);
        } catch (JSONException jse) {
            throw new IllegalArgumentException(jse.getMessage());
        }
    }

    @Test public void
    logging_if_response_validation_fails_works() {
        try {
            RestAssuredMockMvc.given().
                    standaloneSetup(new PostController()).
                    param("name", "Johan").
            when().
                    post("/greetingPost").
            then().
                    log().ifValidationFails().
                    body("id", equalTo(1)).
                    body("content", equalTo("Hello, Johan2!"));

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            String writerString = writer.toString();
            String headerString = String.format("200%n" +
                            "Content-Type: application/json;charset=UTF-8%n");
            assertThat(writerString, startsWith(headerString));
            assertJsonEqualsNonStrict(writerString.replace(headerString, "").trim(),
                    "{\n    \"id\": 1,\n    \"content\": \"Hello, Johan!\"\n}");
        }
    }

    @Test public void
    logging_if_response_validation_fails_doesnt_log_anything_if_validation_succeeds() {
        RestAssuredMockMvc.given().
                standaloneSetup(new PostController()).
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

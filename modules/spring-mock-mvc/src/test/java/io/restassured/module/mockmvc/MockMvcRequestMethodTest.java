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

import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.http.PostController;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.restassured.http.Method.GET;
import static io.restassured.http.Method.POST;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.request;
import static org.hamcrest.Matchers.equalTo;

public class MockMvcRequestMethodTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test public void
    request_method_accepts_enum_verb() {
        given().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                request(POST, "/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    request_method_accepts_enum_verb_and_unnamed_path_params() {
        given().
                standaloneSetup(new GreetingController()).
                queryParam("name", "John").
        when().
                request(GET, "/{x}", "greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }

    @Test public void
    request_method_accepts_string_verb() {
        given().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                request("post", "/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    request_method_accepts_string_verb_and_unnamed_path_params() {
        given().
                standaloneSetup(new GreetingController()).
                queryParam("name", "John").
        when().
                request("GEt", "/{x}", "greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }

    @Test public void
    static_request_method_accepts_string_verb() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());

        try {
            request("  gEt ", "/greeting").then().body("id", equalTo(1)).body("content", equalTo("Hello, World!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    static_request_method_accepts_enum_verb_and_path_params() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());

        try {
            request(GET, "/{greeting}", "greeting").then().body("id", equalTo(1)).body("content", equalTo("Hello, World!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    throws_iae_when_http_verb_is_not_supported_by_mock_mvc() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("HTTP method 'connect' is not supported by MockMvc");

        given().standaloneSetup(new GreetingController()).request("connect", "/greeting");
    }
}

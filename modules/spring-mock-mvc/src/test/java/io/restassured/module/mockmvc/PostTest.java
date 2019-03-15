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

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.http.PostController;
import io.restassured.module.mockmvc.support.Greeting;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class PostTest {

    @BeforeClass
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.mockMvc(standaloneSetup(new PostController()).build());
    }

    @AfterClass
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    automatically_adds_x_www_form_urlencoded_as_content_type_when_posting_params() {
        RestAssuredMockMvc.given().
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    can_supply_string_as_body_for_post() {
        RestAssuredMockMvc.given().
                body("a string").
        when().
                post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    can_supply_object_as_body_and_serialize_as_json() {
        Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        RestAssuredMockMvc.given().
                contentType(ContentType.JSON).
                body(greeting).
        when().
                post("/jsonReflect").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe"));
    }
}

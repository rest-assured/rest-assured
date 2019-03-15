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
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class GreetingControllerRestAssuredTest {

    @Test public void
    uses_predefined_mock_mvc_instance() throws Exception {
        MockMvc mockMvc = standaloneSetup(new GreetingController()).build();

        RestAssuredMockMvc.given().
                mockMvc(mockMvc).
                param("name", "Johan").
        when().
                get("/greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    param_with_int() throws Exception {
        MockMvc mockMvc = standaloneSetup(new GreetingController()).build();

        RestAssuredMockMvc.given().
                mockMvc(mockMvc).
                param("name", 1).
        when().
                get("/greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, 1!"));
    }

    @Test public void
    uses_predefined_standalone() throws Exception {
        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                param("name", "Johan").
        when().
                get("/greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    uses_static_mock_mvc() throws Exception {
        RestAssuredMockMvc.mockMvc(standaloneSetup(new GreetingController()).build());

        try {
            RestAssuredMockMvc.given().
                    param("name", "Johan").
            when().
                    get("/greeting").
            then().
                    body("id", equalTo(1)).
                    body("content", equalTo("Hello, Johan!"));

            RestAssuredMockMvc.given().
                    param("name", "Erik").
            when().
                    get("/greeting").
            then().
                    body("id", equalTo(2)).
                    body("content", equalTo("Hello, Erik!"));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }
}

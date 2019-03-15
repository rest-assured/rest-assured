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
import io.restassured.module.mockmvc.response.MockMvcResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ExtractTest {

    @Test public void
    can_extract_rest_assureds_mock_mvc_response() {
        MockMvcResponse response =

        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                param("name", "Johan").
        when().
                get("/greeting").
        then().
                statusCode(200).
                body("id", equalTo(1)).
        extract().
                response();

        assertThat(response.<String>path("content")).isEqualTo("Hello, Johan!");
    }

    @Test public void
    can_extract_spring_mvcs_result() {
        MvcResult mvcResult =

        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                param("name", "Johan").
        when().
                get("/greeting").
        then().
                statusCode(200).
                body("id", equalTo(1)).
        extract().
                response().mvcResult();

        assertThat(mvcResult.getResponse().getContentType()).contains("application/json");
    }

    @Test public void
    can_extract_spring_mvcs_response() {
        MockHttpServletResponse response =

        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                param("name", "Johan").
        when().
                get("/greeting").
        then().
                statusCode(200).
                body("id", equalTo(1)).
        extract().
                response().mockHttpServletResponse();

        assertThat(response.getContentType()).contains("application/json");
    }
}

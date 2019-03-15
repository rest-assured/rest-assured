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
import io.restassured.module.mockmvc.http.QueryParamController;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

public class QueryParamTest {
// @formatter:off

    @Test
    public void param_with_int() throws Exception {
        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                queryParam("name", "John").
        when().
                get("/greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }

    @Test
    public void query_param() throws Exception {
        RestAssuredMockMvc.given().
                standaloneSetup(new QueryParamController()).
                queryParam("name", "John").
                queryParam("message", "Good!").
        when().
                get("/queryParam").
        then().log().all().
                body("name", equalTo("Hello, John!")).
                body("message", equalTo("Good!")).
                body("_link", equalTo("http://localhost/queryParam?name=John&message=Good!"));
    }

// @formatter:on
}

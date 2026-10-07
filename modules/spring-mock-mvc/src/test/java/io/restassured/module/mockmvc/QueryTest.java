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
import io.restassured.http.Method;
import io.restassured.module.mockmvc.http.QueryController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class QueryTest {
    @BeforeAll
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.mockMvc(standaloneSetup(new QueryController()).build());
    }

    @AfterAll
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test
    public void
    query_sends_query_method_with_body() {
        RestAssuredMockMvc.given().
                contentType(ContentType.JSON).
                body("{\"title\":\"rest\"}").
        when().
                query("/search/books").
        then().
                statusCode(200).
                body(equalTo("QUERY books {\"title\":\"rest\"}"));
    }

    @Test
    public void
    query_supports_path_and_query_params() {
        RestAssuredMockMvc.given().
                queryParam("limit", 10).
                body("q").
        when().
                query("/search/{collection}", "movies").
        then().
                statusCode(200).
                body(equalTo("QUERY movies?limit=10 q"));
    }

    @Test
    public void
    query_supports_named_path_params_and_uri() throws Exception {
        RestAssuredMockMvc.given().body("named").when().query("/search/{collection}", Collections.singletonMap("collection", "music")).
                then().body(equalTo("QUERY music named"));

        RestAssuredMockMvc.given().body("uri").when().query(new URI("/search/uri")).
                then().body(equalTo("QUERY uri uri"));
    }

    @Test
    public void
    static_query_and_request_with_query_method_are_supported() {
        RestAssuredMockMvc.query("/search/static").then().statusCode(200).body(equalTo("QUERY static null"));

        RestAssuredMockMvc.given().body("enum").when().request(Method.QUERY, "/search/enum").
                then().body(equalTo("QUERY enum enum"));

        RestAssuredMockMvc.given().body("string").when().request("query", "/search/string").
                then().body(equalTo("QUERY string string"));
    }
}

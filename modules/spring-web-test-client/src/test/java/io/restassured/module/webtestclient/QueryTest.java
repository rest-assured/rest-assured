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

package io.restassured.module.webtestclient;

import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.module.webtestclient.setup.QueryController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;

public class QueryTest {
    @BeforeAll
    public static void configureWebTestClientInstance() {
        RestAssuredWebTestClient.webTestClient(WebTestClient.bindToController(new QueryController()).build());
    }

    @AfterAll
    public static void restRestAssured() {
        RestAssuredWebTestClient.reset();
    }

    @Test
    public void
    query_sends_query_method_with_body() {
        RestAssuredWebTestClient.given().
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
        RestAssuredWebTestClient.given().
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
    query_supports_named_path_params_and_uri_function() {
        RestAssuredWebTestClient.given().body("named").when().query("/search/{collection}", Collections.singletonMap("collection", "music")).
                then().body(equalTo("QUERY music named"));

        RestAssuredWebTestClient.given().body("fn").when().query(uriBuilder -> uriBuilder.path("/search/fn").build()).
                then().body(equalTo("QUERY fn fn"));
    }

    @Test
    public void
    static_query_and_request_with_query_method_are_supported() {
        RestAssuredWebTestClient.query("/search/static").then().statusCode(200).body(equalTo("QUERY static null"));

        RestAssuredWebTestClient.given().body("enum").when().request(Method.QUERY, "/search/enum").
                then().body(equalTo("QUERY enum enum"));

        RestAssuredWebTestClient.given().body("string").when().request("query", "/search/string").
                then().body(equalTo("QUERY string string"));
    }
}

/*
 * Copyright 2015 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class GivenWhenThenExtractITest extends WithJetty {

    @Test public void
    extract_response_as_string_works() {
        String body = get("/hello").then().assertThat().contentType(JSON).and().extract().body().asString();

        assertThat(body, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test public void
    extract_single_path_works() {
        String hello = get("/hello").then().assertThat().contentType(JSON).and().extract().path("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test public void
    extract_entire_response_works() {
        Response response = get("/hello").then().assertThat().contentType(JSON).and().extract().response();

        assertThat(response.getHeaders().hasHeaderWithName("content-type"), is(true));
    }

    @Test public void
    extract_single_path_works_after_body_validation() {
        String hello = get("/hello").then().assertThat().contentType(JSON).and().body("hello", equalTo("Hello Scalatra")).extract().path("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test public void
    extract_single_path_works_after_status_code_and_body_validation() {
        String hello = get("/hello").then().assertThat().statusCode(200).and().body("hello", equalTo("Hello Scalatra")).extract().path("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test public void
    extract_single_path_works_after_multiple_body_validations() {
        int lottoId = get("/lotto").then().assertThat().statusCode(200).and().body("lotto.lottoId", equalTo(5)).
                and().body("lotto.winning-numbers", hasItems(2, 45, 34, 23, 7, 5, 3)).extract().path("lotto.lottoId");

        assertThat(lottoId, is(5));
    }
}

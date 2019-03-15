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

package io.restassured.itest.java;

import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static io.restassured.http.Method.GET;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class RequestMethodITest extends WithJetty {

    @Test public void
    request_method_accepts_enum_verb() {
        when().
                request(GET, "/lotto").
        then().
                body("lotto.lottoId", is(5));
    }

    @Test public void
    request_method_accepts_enum_verb_and_unnamed_path_params() {
        when().
                request(GET, "/{firstName}/{lastName}", "John", "Doe").
        then().
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe"));
    }

    @Test public void
    request_method_accepts_string_verb() {
        given().
                queryParam("firstName", "John").
                queryParam("lastName", "Doe").
        when().
                request(" opTions  ", "/greetXML").
        then().
                body("greeting.firstName", is("John")).
                body("greeting.lastName", is("Doe"));
    }

    @Test public void
    request_method_accepts_string_verb_and_unnamed_path_params() {
        when().
                request("GET", "/{firstName}/{lastName}", "John", "Doe").
        then().
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe"));
    }

    @Test public void
    static_request_method_accepts_string_verb() {
        request("get", "/John/Doe").then().assertThat().body("firstName", equalTo("John")).and().body("lastName", equalTo("Doe"));
    }

    @Test public void
    static_request_method_accepts_enum_verb_and_path_params() {
        request(GET, "/{firstName}/{lastName}", "John", "Doe").then().assertThat().body("firstName", equalTo("John")).and().body("lastName", equalTo("Doe"));
    }
}

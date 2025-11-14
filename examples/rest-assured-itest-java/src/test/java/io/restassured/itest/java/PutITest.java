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
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PutITest extends WithJetty {

    @Test
    void requestSpecificationAllowsSpecifyingCookie() {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().put("/cookie");
    }

    @Test
    void bodyHamcrestMatcherWithoutKey() {
        given().params("firstName", "John", "lastName", "Doe").expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}"))
            .when().put("/greetPut");
    }

    @Test
    void putSupportsBinaryBody() {
        final byte[] body = "a body".getBytes(StandardCharsets.UTF_8);
        given().body(body).expect().body(equalTo("97, 32, 98, 111, 100, 121")).when().put("/binaryBody");
    }

    @Test
    void putSupportsStringBody() {
        given().body("a body").expect().body(equalTo("a body")).when().put("/body");
    }

    @Test
    void putWithFormParams() {
        given().formParams("firstName", "John", "lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe"))
            .when().put("/greetPut");
    }

    @Test
    void putWithFormParam() {
        given().
                formParam("firstName", "John").
                formParam("lastName", "Doe").
        expect().
                body("greeting", equalTo("Greetings John Doe")).
        when().
                put("/greetPut");
    }

    @Test
    void putSupportsMultiValueFormParameters() {
        given().
                formParam("list", "1", "2", "3").
        expect().
                body("list", equalTo("1,2,3")).
        when().
               put("/multiValueParam");
    }

}

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PutITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() throws Exception {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().put("/cookie");
    }

    @Test
    public void bodyHamcrestMatcherWithoutKey() throws Exception {
        given().params("firstName", "John", "lastName", "Doe").expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}")).when().put("/greetPut");
    }

    @Test
    public void putSupportsBinaryBody() throws Exception {
        final byte[] body = "a body".getBytes("UTF-8");
        given().body(body).expect().body(equalTo("97, 32, 98, 111, 100, 121")).when().put("/binaryBody");
    }

    @Test
    public void putSupportsStringBody() throws Exception {
        given().body("a body").expect().body(equalTo("a body")).when().put("/body");
    }

    @Test
    public void putWithFormParams() throws Exception {
        given().formParams("firstName", "John", "lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().put("/greetPut");
    }

    @Test
    public void putWithFormParam() throws Exception {
        given().
                formParam("firstName", "John").
                formParam("lastName", "Doe").
        expect().
                body("greeting", equalTo("Greetings John Doe")).
        when().
                put("/greetPut");
    }

    @Test
    public void putSupportsMultiValueFormParameters() throws Exception {
        given().
                formParam("list", "1", "2", "3").
        expect().
                body("list", equalTo("1,2,3")).
        when().
               put("/multiValueParam");
    }

}

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

package io.restassured.itest.java.presentation;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SpecificationsDemoITest extends WithJetty {

    @Test
    public void demoRequestSpecification() throws Exception {
        final RequestSpecification books = new RequestSpecBuilder().
                addQueryParam("category", "books").addCookie("user", "admin").build();

        given().
                 spec(books).
        expect().
                 body(equalTo("Catch 22")).
        when().
                 get("/demoRequestSpecification");
    }

    @Test
    public void demoResponseSpecification() throws Exception {
        final ResponseSpecification spec = new ResponseSpecBuilder().
                expectStatusCode(200).expectBody("responseType", equalTo("simple")).build();

        given().
                 param("name", "John Doe").
        expect().
                 spec(spec).
                 body("firstName", is("John")).
                 body("lastName", is("Doe")).
        when().
                 get("/demoResponseSpecification");
    }

    @Test
    public void demoResponseSpecificationUsingGivenWhenThen() throws Exception {
        final ResponseSpecification spec = new ResponseSpecBuilder().
                expectStatusCode(200).expectBody("responseType", equalTo("simple")).build();

        given().
                 param("name", "John Doe").
        when().
                 get("/demoResponseSpecification").
        then().
                 spec(spec).
                 body("firstName", is("John")).
                 body("lastName", is("Doe"));
    }
}

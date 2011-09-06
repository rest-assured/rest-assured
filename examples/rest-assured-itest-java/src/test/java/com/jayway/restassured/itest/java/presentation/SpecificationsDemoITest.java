/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.itest.java.presentation;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SpecificationsDemoITest extends WithJetty {

    @Test
    public void demoRequestSpecification() throws Exception {
        final RequestSpecification books = new RequestSpecBuilder().
                addQueryParam("category", "books").addCookie("user", "admin").build();

        given().
                 specification(books).
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
                 specification(spec).
                 body("firstName", is("John")).
                 body("lastName", is("Doe")).
        when().
                 get("/demoResponseSpecification");
    }
}

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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.form;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

public class FilterITest {  // extends WithJetty {

    @Test
    @Ignore
    public void filter() throws Exception {
        RestAssured.authentication = form("admin", "admin");
        RestAssured.port = 9090;
//        given().
//                auth().form("admin", "admin").
//        expect().
//                statusCode(200).
//                body("countries.name", hasItems("India", "Sweden", "USA")).
//        when().
//                get("/backend/admin/countries");
        expect().statusCode(200).with().param("feedurl", "http://www.google.com").
                and().queryParam("category", "games").
                and().param("providerName", "provider name").
                and().param("titlePrefix", "").
                and().param("description", "").post("/admin/providers/provider");

    }
}

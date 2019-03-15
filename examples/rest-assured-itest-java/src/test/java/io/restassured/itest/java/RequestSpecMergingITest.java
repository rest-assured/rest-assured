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

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.http.Headers;
import io.restassured.specification.RequestSpecification;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class RequestSpecMergingITest {

    protected static final RequestSpecification jsonRequest = new RequestSpecBuilder()
            .addHeader("accept", "application/json+json")
            .setContentType("application/json")
            .build();

    protected static final RequestSpecification xmlRequest = new RequestSpecBuilder()
            .addHeader("accept", "application/xml")
            .setContentType("application/xml")
            .build();

    @BeforeClass
    public static void setupRA() {
        RestAssured.baseURI = "http://example.com";
        RestAssured.port = 80;
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("authorization", "abracadabra")
                .build();
    }

    @AfterClass
    public static void teardown() throws Exception {
        RestAssured.reset();
    }

    @Test
    public void mergesHeadersCorrectlyWhenOnlyStaticMerging() {
        given().filter((requestSpec, responseSpec, ctx) -> {
            Headers headers = requestSpec.getHeaders();
            assertThat(requestSpec.getContentType(), nullValue());
            assertThat(headers.getValue("authorization"), equalTo("abracadabra"));
            assertThat(headers.getValue("accept"), equalTo("*/*"));
            assertThat(headers.size(), is(2));
            return new ResponseBuilder().setStatusCode(200).build();
        }).when().get();
    }

    @Test
    public void mergesHeadersCorrectlyWhenUsingGivenRequestSpec() {
        given(jsonRequest).filter((requestSpec, responseSpec, ctx) -> {
            Headers headers = requestSpec.getHeaders();
            assertThat(requestSpec.getContentType(), equalTo("application/json; charset=" + config().getEncoderConfig().defaultCharsetForContentType(JSON)));
            assertThat(headers.getValue("authorization"), equalTo("abracadabra"));
            assertThat(headers.getValue("accept"), equalTo("application/json+json"));
            assertThat(headers.getValue("content-type"), equalTo("application/json; charset=" + config().getEncoderConfig().defaultCharsetForContentType(JSON)));
            assertThat(headers.size(), is(3));
            return new ResponseBuilder().setStatusCode(200).build();
        }).when().get();
    }

    @Test
    public void mergesHeadersCorrectlyWhenUsingGivenSpecRequestSpec() {
        given().spec(jsonRequest).filter((requestSpec, responseSpec, ctx) -> {
            Headers headers = requestSpec.getHeaders();
            assertThat(requestSpec.getContentType(), equalTo("application/json; charset=" + config().getEncoderConfig().defaultCharsetForContentType(JSON)));
            assertThat(headers.getValue("authorization"), equalTo("abracadabra"));
            assertThat(headers.getValue("accept"), equalTo("application/json+json"));
            assertThat(headers.getValue("content-type"), equalTo("application/json; charset=" + config().getEncoderConfig().defaultCharsetForContentType(JSON)));
            assertThat(headers.size(), is(3));
            return new ResponseBuilder().setStatusCode(200).build();
        }).when().get();
    }
}

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
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.config.RedirectConfig.redirectConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionContaining.hasItems;

public class SpecificationBuilderITest extends WithJetty {

    @Test
    public void expectingSpecificationMergesTheCurrentSpecificationWithTheSuppliedOne() throws Exception {
        final ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectBody("store.book.size()", is(4)).expectStatusCode(200);
        final ResponseSpecification responseSpecification = builder.build();

        expect().
                specification(responseSpecification).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }

    @Test
    public void supportsSpecifyingDefaultResponseSpec() throws Exception {
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectBody("store.book.size()", is(4)).expectStatusCode(200).build();

        try {
            expect().
                    body("store.book[0].author", equalTo("Nigel Rees")).
            when().
                    get("/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void expectingSpecMergesTheCurrentSpecificationWithTheSuppliedOne() throws Exception {
        final ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectBody("store.book.size()", is(4)).expectStatusCode(200);
        final ResponseSpecification responseSpecification = builder.build();

        expect().
                spec(responseSpecification).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }

    @Test
    public void bodyExpectationsAreNotOverwritten() throws Exception {
        final ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectBody("store.book.size()", is(4)).expectStatusCode(200);
        final ResponseSpecification responseSpecification = builder.build();

        expect().
                body("store.book.author", hasItems("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")).
                spec(responseSpecification).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }

    @Test
    public void responseSpecificationSupportsMergingWithAnotherResponseSpecification() throws Exception {
        final ResponseSpecification specification = expect().body("store.book.size()", equalTo(4));
        final ResponseSpecification built = new ResponseSpecBuilder().expectStatusCode(200).addResponseSpecification(specification).build();

        expect().
                body("store.book.author", hasItems("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")).
                spec(built).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }

    @Test
    public void responseSpecificationCanExpectBodyWithArgs () throws Exception {
        final ResponseSpecification spec = new ResponseSpecBuilder().rootPath("store.book[%d]").expectBody("author", withArgs(0), equalTo("Nigel Rees")).build();

        expect().
                spec(spec).
                body("title", withArgs(1), equalTo("Sword of Honour")).
        when().
                get("/jsonStore");
    }

    @Test
    public void responseSpecificationCanExpectContentWithArgs () throws Exception {
        final ResponseSpecification spec = new ResponseSpecBuilder().rootPath("store.book[%d]").expectContent("author", withArgs(0), equalTo("Nigel Rees")).build();

        expect().
                spec(spec).
                content("title", withArgs(1), equalTo("Sword of Honour")).
        when().
                get("/jsonStore");
    }

    @Test
    public void supportsSpecifyingParametersInRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addParameter("firstName", "John").addParam("lastName", "Doe").build();

        given().
                spec(spec).
        expect().
                body("greeting.firstName", equalTo("John")).
                body("greeting.lastName", equalTo("Doe")).
        when().
                get("/greetXML");
    }

    @Test
    public void supportsSpecifyingDefaultRequestSpec() throws Exception {
        RestAssured.requestSpecification = new RequestSpecBuilder().addParameter("firstName", "John").addParam("lastName", "Doe").build();
        try {
            expect().
                    body("greeting.firstName", equalTo("John")).
                    body("greeting.lastName", equalTo("Doe")).
            when().
                    get("/greetXML");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void supportsSpecifyingQueryParametersInRequestSpecBuilderWhenGet() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addQueryParameter("firstName", "John").addQueryParam("lastName", "Doe").build();

        given().
                spec(spec).
        expect().
                body("greeting.firstName", equalTo("John")).
                body("greeting.lastName", equalTo("Doe")).
        when().
                get("/greetXML");
    }

    @Test
    public void supportsSpecifyingQueryParametersInRequestSpecBuilderWhenPost() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addQueryParameter("firstName", "John").addQueryParam("lastName", "Doe").build();

        given().
                spec(spec).
        expect().
                body("greeting.firstName", equalTo("John")).
                body("greeting.lastName", equalTo("Doe")).
        when().
                post("/greetXML");
    }

    @Test
    public void supportsMergesParametersWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addParameter("firstName", "John").build();

        given().
                spec(spec).
                param("lastName", "Doe").
        expect().
                body("greeting.firstName", equalTo("John")).
                body("greeting.lastName", equalTo("Doe")).
        when().
                get("/greetXML");
    }

    @Test
    public void supportsMergingCookiesWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec1 = new RequestSpecBuilder().addCookie("cookie3", "value3").build();
        final RequestSpecification spec2 = new RequestSpecBuilder().addCookie("cookie1", "value1").addRequestSpecification(spec1).build();

        given().
                spec(spec2).
                cookie("cookie2", "value2").
        expect().
                body(equalTo("cookie1, cookie3, cookie2")).
        when().
                get("/cookie");
    }

    @Test
    public void supportsMergingHeadersWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addHeader("header1", "value1").build();

        given().
                spec(spec).
                header("header2", "value2").
        expect().
                body(containsString("header1")).
                body(containsString("header2")).
        when().
                get("/header");
    }

    @Test
    public void supportsMergingRequestSpecHeadersUsingTheBuilder() throws Exception {
        final RequestSpecification spec = given().header("header2", "value2");
        final RequestSpecification spec2 = new RequestSpecBuilder().addHeader("header1", "value1").addRequestSpecification(spec).build();

        given().
                spec(spec2).
                header("header3", "value3").
        expect().
                body(containsString("header1")).
                body(containsString("header2")).
                body(containsString("header3")).
        when().
                get("/header");
    }

    @Test
    public void requestSpecBuilderSupportsSettingAuthentication() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().setAuth(basic("jetty", "jetty")).build();

        given().
                spec(spec).
        expect().
                statusCode(200).
        when().
                get("/secured/hello");
    }

    @Test
    public void supportsMergingMultiValueParametersWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addParam("list", "1", "2", "3").build();

        given().
                spec(spec).
        expect().
                body("list", equalTo("1,2,3")).
        when().
                get("/multiValueParam");
    }

    @Test
    public void supportsMergingMultiValueQueryParametersWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addQueryParam("list", "1", "2", "3").build();

        given().
                spec(spec).
        expect().
                body("list", equalTo("1,2,3")).
        when().
                get("/multiValueParam");
    }

    @Test
    public void supportsMergingMultiValueFormParametersWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addFormParam("list", "1", "2", "3").build();

        given().
                spec(spec).
        expect().
                body("list", equalTo("1,2,3")).
        when().
                put("/multiValueParam");
    }

    @Test
    public void supportsMergingMultiValueParametersUsingListWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addParam("list", asList("1", "2", "3")).build();

        given().
                spec(spec).
        expect().
                body("list", equalTo("1,2,3")).
        when().
                get("/multiValueParam");
    }

    @Test
    public void supportsMergingMultiValueQueryParametersUsingListWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addQueryParam("list", asList("1", "2", "3")).build();

        given().
                spec(spec).
        expect().
                body("list", equalTo("1,2,3")).
        when().
                get("/multiValueParam");
    }

    @Test
    public void supportsMergingMultiValueFormParametersUsingListWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addFormParam("list", asList("1", "2", "3")).build();

        given().
                spec(spec).
        expect().
                body("list", equalTo("1,2,3")).
        when().
                put("/multiValueParam");
    }

    @Test
    public void supportsMergingFormParametersWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addFormParam("lastName", "Doe").build();

        given().
                spec(spec).
                formParameter("firstName", "John").
        expect().
                body("greeting", Matchers.equalTo("Greetings John Doe")).
        when().
                put("/greetPut");
    }

    @Test
    public void supportsMergingPathParametersWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().addPathParam("lastName", "Doe").build();

        given().
                spec(spec).
                pathParameter("firstName", "John").
         expect().
                body("fullName", equalTo("John Doe")).
        when().
               get("/{firstName}/{lastName}");
    }

    @Test
    public void supportsSettingConfigWhenUsingRequestSpecBuilder() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().setConfig(newConfig().redirect(redirectConfig().followRedirects(false))).build();

        given().
                param("url", "/hello").
                spec(spec).
        expect().
                statusCode(302).
                header("Location", Matchers.is("http://localhost:8080/hello")).
        when().
                get("/redirect");
    }


    @Test
    public void supportsSpecifyingKeystore() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().setKeystore("/truststore.jks", "test1234").build();
        given().spec(spec).expect().body(containsString("The Source for Java Technology Collaboration")).get("https://dev.java.net/");
    }

    @Test
    public void supportsOverridingKeystore() throws Exception {
        final RequestSpecification spec = new RequestSpecBuilder().setKeystore("/truststore.jks", "wrong pw").build();
        given().spec(spec).keystore("/truststore.jks", "test1234").expect().body(containsString("The Source for Java Technology Collaboration")).get("https://dev.java.net/");
    }
}

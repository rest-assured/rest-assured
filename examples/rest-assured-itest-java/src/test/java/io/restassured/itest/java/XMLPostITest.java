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
import static org.hamcrest.Matchers.*;

public class XMLPostITest extends WithJetty {

    @Test
    public void xmlParameterSupport() throws Exception {
        with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().post("/greetXML");
    }

    @Test
    public void xmlParameterSupportWithAnotherAssertion() throws Exception {
        with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).when().post("/greetXML");
    }

    @Test
    public void xmlWithLists() throws Exception {
        with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.children()", hasItems("John", "Doe")).when().post("/greetXML");
    }

    @Test
    public void postWithXPath() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName[text()='John']")).with().params("firstName", "John", "lastName", "Doe").post("/anotherGreetXML");
    }

    @Test
    public void postWithXPathContainingHamcrestMatcher() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName", containsString("Jo"))).with().params("firstName", "John", "lastName", "Doe").post("/anotherGreetXML");
    }

    @Test
    public void postWithQueryParams() throws Exception {
        expect().body("greeting.lastName", equalTo("Doe")).when().post("/greetXML?firstName=John&lastName=Doe");
    }

    @Test
    public void postWithFormParamAndQueryParams() throws Exception {
        with().param("firstName", "John").and().queryParam("lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).when().post("/greetXML");
    }

    @Test
    public void postWithOnlyQueryParams() throws Exception {
        with().queryParams("firstName", "John", "lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).when().post("/greetXML");
    }

    @Test
    public void customXmlCompatibleContentTypeWithBody() throws Exception {
        byte[] bytes = "Some Text".getBytes();
        given().
                contentType("application/vnd.myitem+xml").
                body(bytes).
        expect().
                body(equalTo("Some Text")).
        when().
                put("/reflect");

    }

    @Test
    public void requestIncludesContentTypeWhenSendingBinaryDataAsXml() throws Exception {
        byte[] bytes = "<tag attr='value'>/".getBytes( "UTF-8" );
        given().
                contentType("application/xml").
                body(bytes).
        expect().
                statusCode(200).
                contentType("application/xml").
                body(is(new String(bytes, "UTF-8"))).
        when().
                post("/validateContentTypeIsDefinedAndReturnBody");
    }
}

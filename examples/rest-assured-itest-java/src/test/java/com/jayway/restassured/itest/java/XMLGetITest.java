/*
 * Copyright 2013 the original author or authors.
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
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.parsing.Parser;
import com.jayway.restassured.specification.ResponseSpecification;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.parsing.Parser.XML;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class XMLGetITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void xmlParameterSupport() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().get("/greetXML");
    }

    @Test
    public void xmlHasItems() throws Exception {
        expect().body("greeting", hasItems("John", "Doe")).when().get("/greetXML?firstName=John&lastName=Doe");
    }

    @Test
    public void xmlParameterSupportWithAnotherAssertion() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).get("/greetXML");
    }

    @Test
    public void childrenElements() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.name.children()", hasItems("John", "Doe")).get("/anotherGreetXML");
    }

    @Test
    public void childrenElementsSize() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.name.children().size()", equalTo(2)).get("/anotherGreetXML");
    }

    @Test
    public void childrenElementsIsEmpty() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.name.notDefined.children().isEmpty()", equalTo(true)).get("/anotherGreetXML");
    }

    @Test
    public void xmlNestedElements2() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.name.firstName", equalTo("John")).get("/anotherGreetXML");
    }

    @Test
    public void xmlWithContentAssertion() throws Exception {
        String expectedBody = "<greeting>\n" +
                "      <name>\n" +
                "        <firstName>John</firstName>\n" +
                "        <lastName>Doe</lastName>\n" +
                "      </name>\n" +
                "    </greeting>";
        with().parameters("firstName", "John", "lastName", "Doe").expect().body(equalTo(expectedBody)).when().get("/anotherGreetXML");
    }

    @Test
    public void newSyntaxWithXPath() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName[text()='John']")).then().with().parameters("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test
    public void newSyntaxWithXPathWithContainsMatcher() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName", containsString("Jo"))).given().parameters("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test
    public void xmlWithContentTypeTextXML() throws Exception {
        expect().body("xml", equalTo("something")).when().get("/textXML");
    }

    @Test
    public void xmlWithContentTypeHTML() throws Exception {
        expect().body("html.head.title", equalTo("my title")).when().get("/textHTML");
    }

    @Test
    public void htmlVerification() throws Exception {
        expect().body("html.body.children()", hasItems("paragraph 1", "paragraph 2")).when().get("/textHTML");
    }

    @Test
    public void htmlValueVerification() throws Exception {
        expect().body("html.body.p.list()", hasItems("paragraph 1", "paragraph 2")).when().get("/textHTML");
    }

    @Test
    public void htmlChildElementSize() throws Exception {
        expect().body("html.body.children().size()", equalTo(2)).when().get("/textHTML");
    }

    @Test
    public void htmlBodySize() throws Exception {
        expect().body("html.body.size()", equalTo(1)).when().get("/textHTML");
    }

    @Test
    public void supportsParsingHtmlWhenContentTypeEndsWithPlusHtml() throws Exception {
        expect().body("html.head.title", equalTo("my title")).when().get("/mimeTypeWithPlusHtml");
    }

    @Test
    public void canGetSpecificEntityFromListHtmlDocument() throws Exception {
        expect().body("html.body.p[0]", equalTo("paragraph 1")).when().get("/textHTML");
    }

    @Test
    public void canGetSpecificEntityFromListHtmlDocumentUsingGetAt() throws Exception {
        expect().body("html.body.p.getAt(0)", equalTo("paragraph 1")).when().get("/textHTML");
    }

    @Test
    public void rssVerification() throws Exception {
        expect().body("rss.item.title", equalTo("rss title")).when().get("/rss");
    }

    @Test
    public void nestedListsAreConvertedToJavaLists() throws Exception {
        expect().body("rss.channel.item.size()", equalTo(2)).when().get("/bigRss");
    }

    @Test
    public void supportsParsingXmlAttributes() throws Exception {
        expect().
                body("greeting.name.@firstName", equalTo("John")).
                body("greeting.name.@lastName", equalTo("Doe")).
                when().get("/greetXMLAttribute?firstName=John&lastName=Doe");
    }

    @Test
    public void throwsIAEOnIllegalXmlExpression() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("greeting.€4324'21");

        expect().body("greeting.€4324'21", equalTo("rss title")).when().get("/rss");
    }

    @Test
    public void supportsGettingAllAttributesFromAList() throws Exception {
        expect().body("shopping.category.@type", hasItems("groceries", "supplies", "present")).when().get("/shopping");
    }

    @Test
    public void whenReturningANonCollectionAndNonArrayThenSizeIsOne() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName.size()", equalTo(1)).when().get("/greetXML");
    }

    @Test
    public void supportBodyExpectationsWithMinusInRootObject() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("a-greeting.firstName", equalTo("John")).when().get("/xmlWithMinusInRoot");
    }

    @Test
    public void supportBodyExpectationsWithMinusInChildObject() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.your-firstName", equalTo("John")).when().get("/xmlWithMinusInChild");
    }

    @Test
    public void supportBodyExpectationsWithUnderscoreInChildObject() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.your_firstName", equalTo("John")).when().get("/xmlWithUnderscoreInChild");
    }

    @Test
    public void xmlChildListSize() throws Exception {
        expect().body("shopping.category.item.size()", equalTo(5)).when().get("/shopping");
    }

    @Test
    public void supportsGettingSpecificItemFromAListArrayStyle() throws Exception {
        expect().body("shopping.category[0].@type", equalTo("groceries")).when().get("/shopping");
    }

    @Test
    public void supportsGettingSpecificItemFromAListNonArrayStyle() throws Exception {
        expect().body("shopping.category.getAt(0).@type", equalTo("groceries")).when().get("/shopping");
    }

    @Test
    public void supportsFindingElements() throws Exception {
        expect().body("shopping.category.findAll { it.@type == 'groceries' }.size()", equalTo(1)).when().get("/shopping");
    }

    @Test
    public void supportsRegisteringCustomParserForAGivenMimeType() throws Exception {
        final String mimeType = "application/something-custom";
        RestAssured.registerParser(mimeType, XML);
        try {
            expect().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
        } finally {
            RestAssured.unregisterParser(mimeType);
        }
    }

    @Test
    public void supportsRegisteringCustomParserForAGivenMimeTypePerResponse() throws Exception {
        final String mimeType = "application/something-custom";
        expect().parser(mimeType, Parser.XML).and().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
    }

    @Test
    public void supportsRegisteringCustomParserForAGivenMimeTypeUsingResponseSpec() throws Exception {
        final String mimeType = "application/something-custom";
        final ResponseSpecification specification = new ResponseSpecBuilder().registerParser(mimeType, Parser.XML).build();
        expect().specification(specification).and().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
    }

    @Test
    public void supportsParsingXmlWhenContentTypeEndsWithPlusXml() throws Exception {
        expect().body("body.message", equalTo("Custom mime-type ending with +xml")).when().get("/mimeTypeWithPlusXml");
    }

    @Test
    public void throwsNiceErrorMessageWhenIllegalPath() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Path shopping.unknown.get(0) is invalid.");

        expect().body("shopping.unknown.get(0)", hasItems("none")).when().get("/shopping");
    }

    @Test
    public void supportsGettingResponseBodyAsStringWhenUsingBodyExpectationsOnRoot() throws Exception {
        final String body = expect().body("shopping", anything()).when().get("/shopping").asString();

        assertThat(body, containsString("<shopping>"));
    }

    @Test
    public void whenExpectingContentTypeXMLThenTextXmlIsAllowedAsContentType() throws Exception {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                contentType(ContentType.XML).
        when().
                get("/xmlWithContentTypeTextXml");
    }

    @Test
    public void whenExpectingContentTypeXMLThenCustomXmlIsAllowedAsContentType() throws Exception {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                contentType(ContentType.XML).
                body("greeting.firstName", equalTo("John")).
                body("greeting.lastName", equalTo("Doe")).
        when().
                get("/xmlWithCustomXmlContentType");
    }

    @Test
    public void whenExpectingContentTypeXMLThenExceptionIsThrownIfContentTypeIsJson() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage("Expected content-type \"XML\" doesn't match actual content-type \"application/json; charset=UTF-8\".");

        given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                contentType(ContentType.XML).
        when().
                get("/greetJSON");
    }
}
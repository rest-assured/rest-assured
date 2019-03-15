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
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.parsing.Parser;
import io.restassured.specification.ResponseSpecification;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class XMLGetITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void xmlParameterSupport() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().get("/greetXML");
    }

    @Test
    public void xmlHasItems() throws Exception {
        RestAssured.expect().body("greeting", hasItems("John", "Doe")).when().get("/greetXML?firstName=John&lastName=Doe");
    }

    @Test
    public void xmlParameterSupportWithAnotherAssertion() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).when().get("/greetXML");
    }

    @Test
    public void childrenElements() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.children()", hasItems("John", "Doe")).when().get("/anotherGreetXML");
    }

    @Test
    public void childrenElementsSize() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.children().size()", equalTo(2)).when().get("/anotherGreetXML");
    }

    @Test
    public void childrenElementsIsEmpty() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.notDefined.children().isEmpty()", equalTo(true)).when().get("/anotherGreetXML");
    }

    @Test
    public void xmlNestedElements2() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.firstName", equalTo("John")).when().get("/anotherGreetXML");
    }

    @Test
    public void xmlWithContentAssertion() throws Exception {
        String expectedBody = "<greeting>\n" +
                "      <name>\n" +
                "        <firstName>John</firstName>\n" +
                "        <lastName>Doe</lastName>\n" +
                "      </name>\n" +
                "    </greeting>";
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body(equalTo(expectedBody)).when().get("/anotherGreetXML");
    }

    @Test
    public void newSyntaxWithXPath() throws Exception {
        RestAssured.expect().body(hasXPath("/greeting/name/firstName[text()='John']")).then().with().params("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test
    public void newSyntaxWithXPathWithContainsMatcher() throws Exception {
        RestAssured.expect().body(hasXPath("/greeting/name/firstName", containsString("Jo"))).given().params("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test
    public void xmlWithContentTypeTextXML() throws Exception {
        RestAssured.expect().body("xml", equalTo("something")).when().get("/textXML");
    }

    @Test
    public void xmlWithContentTypeHTML() throws Exception {
        RestAssured.expect().body("html.head.title", equalTo("my title")).when().get("/textHTML");
    }

    @Test
    public void htmlVerification() throws Exception {
        RestAssured.expect().body("html.body.children()", hasItems("paragraph 1", "paragraph 2")).when().get("/textHTML");
    }

    @Test
    public void htmlValueVerification() throws Exception {
        RestAssured.expect().body("html.body.p.list()", hasItems("paragraph 1", "paragraph 2")).when().get("/textHTML");
    }

    @Test
    public void htmlChildElementSize() throws Exception {
        RestAssured.expect().body("html.body.children().size()", equalTo(2)).when().get("/textHTML");
    }

    @Test
    public void htmlBodySize() throws Exception {
        RestAssured.expect().body("html.body.size()", equalTo(1)).when().get("/textHTML");
    }

    @Test
    public void supportsParsingHtmlWhenContentTypeEndsWithPlusHtml() throws Exception {
        RestAssured.expect().body("html.head.title", equalTo("my title")).when().get("/mimeTypeWithPlusHtml");
    }

    @Test
    public void canGetSpecificEntityFromListHtmlDocument() throws Exception {
        RestAssured.expect().body("html.body.p[0]", equalTo("paragraph 1")).when().get("/textHTML");
    }

    @Test
    public void canGetSpecificEntityFromListHtmlDocumentUsingGetAt() throws Exception {
        RestAssured.expect().body("html.body.p.getAt(0)", equalTo("paragraph 1")).when().get("/textHTML");
    }

    @Test
    public void rssVerification() throws Exception {
        RestAssured.expect().body("rss.item.title", equalTo("rss title")).when().get("/rss");
    }

    @Test
    public void nestedListsAreConvertedToJavaLists() throws Exception {
        RestAssured.expect().body("rss.channel.item.size()", equalTo(2)).when().get("/bigRss");
    }

    @Test
    public void supportsParsingXmlAttributes() throws Exception {
        RestAssured.expect().
                body("greeting.name.@firstName", equalTo("John")).
                body("greeting.name.@lastName", equalTo("Doe")).
                when().get("/greetXMLAttribute?firstName=John&lastName=Doe");
    }

    @Test
    public void throwsIAEOnIllegalXmlExpression() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("greeting.€4324'21");

        RestAssured.expect().body("greeting.€4324'21", equalTo("rss title")).when().get("/rss");
    }

    @Test
    public void supportsGettingAllAttributesFromAList() throws Exception {
        RestAssured.expect().body("shopping.category.@type", hasItems("groceries", "supplies", "present")).when().get("/shopping");
    }

    @Test
    public void whenReturningANonCollectionAndNonArrayThenSizeIsOne() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName.size()", equalTo(1)).when().get("/greetXML");
    }

    @Test
    public void supportBodyExpectationsWithMinusInRootObject() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("a-greeting.firstName", equalTo("John")).when().get("/xmlWithMinusInRoot");
    }

    @Test
    public void supportBodyExpectationsWithMinusInChildObject() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.your-firstName", equalTo("John")).when().get("/xmlWithMinusInChild");
    }

    @Test
    public void supportBodyExpectationsWithUnderscoreInChildObject() throws Exception {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.your_firstName", equalTo("John")).when().get("/xmlWithUnderscoreInChild");
    }

    @Test
    public void xmlChildListSize() throws Exception {
        RestAssured.expect().body("shopping.category.item.size()", equalTo(5)).when().get("/shopping");
    }

    @Test
    public void supportsGettingSpecificItemFromAListArrayStyle() throws Exception {
        RestAssured.expect().body("shopping.category[0].@type", equalTo("groceries")).when().get("/shopping");
    }

    @Test
    public void supportsGettingSpecificItemFromAListNonArrayStyle() throws Exception {
        RestAssured.expect().body("shopping.category.getAt(0).@type", equalTo("groceries")).when().get("/shopping");
    }

    @Test
    public void supportsFindingElements() throws Exception {
        RestAssured.expect().body("shopping.category.findAll { it.@type == 'groceries' }.size()", equalTo(1)).when().get("/shopping");
    }

    @Test
    public void supportsRegisteringCustomParserForAGivenMimeType() throws Exception {
        final String mimeType = "application/something-custom";
        RestAssured.registerParser(mimeType, Parser.XML);
        try {
            RestAssured.expect().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
        } finally {
            RestAssured.unregisterParser(mimeType);
        }
    }

    @Test
    public void supportsRegisteringCustomParserForAGivenMimeTypePerResponse() throws Exception {
        final String mimeType = "application/something-custom";
        RestAssured.expect().parser(mimeType, Parser.XML).and().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
    }

    @Test
    public void supportsRegisteringCustomParserForAGivenMimeTypeUsingResponseSpec() throws Exception {
        final String mimeType = "application/something-custom";
        final ResponseSpecification specification = new ResponseSpecBuilder().registerParser(mimeType, Parser.XML).build();
        RestAssured.expect().spec(specification).and().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
    }

    @Test
    public void supportsParsingXmlWhenContentTypeEndsWithPlusXml() throws Exception {
        RestAssured.expect().body("body.message", equalTo("Custom mime-type ending with +xml")).when().get("/mimeTypeWithPlusXml");
    }

    @Test
    public void throwsNiceErrorMessageWhenIllegalPath() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Path shopping.unknown.get(0) is invalid.");

        RestAssured.expect().body("shopping.unknown.get(0)", hasItems("none")).when().get("/shopping");
    }

    @Test
    public void supportsGettingResponseBodyAsStringWhenUsingBodyExpectationsOnRoot() throws Exception {
        final String body = RestAssured.expect().body("shopping", anything()).when().get("/shopping").asString();

        assertThat(body, containsString("<shopping>"));
    }

    @Test
    public void whenExpectingContentTypeXMLThenTextXmlIsAllowedAsContentType() throws Exception {
        RestAssured.given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                contentType(ContentType.XML).
        when().
                get("/xmlWithContentTypeTextXml");
    }

    @Test
    public void whenExpectingContentTypeXMLThenCustomXmlIsAllowedAsContentType() throws Exception {
        RestAssured.given().
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
        exception.expectMessage("Expected content-type \"XML\" doesn't match actual content-type \"application/json;charset=utf-8\".");

        RestAssured.given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                contentType(ContentType.XML).
        when().
                get("/greetJSON");
    }
}
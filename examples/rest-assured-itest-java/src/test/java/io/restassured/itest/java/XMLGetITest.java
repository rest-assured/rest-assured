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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class XMLGetITest extends WithJetty {

    @Test
    void xmlParameterSupport() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().get("/greetXML");
    }

    @Test
    void xmlHasItems() {
        RestAssured.expect().body("greeting", hasItems("John", "Doe")).when().get("/greetXML?firstName=John&lastName=Doe");
    }

    @Test
    void xmlParameterSupportWithAnotherAssertion() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).when().get("/greetXML");
    }

    @Test
    void childrenElements() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.children()", hasItems("John", "Doe")).when().get("/anotherGreetXML");
    }

    @Test
    void childrenElementsSize() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.children().size()", equalTo(2)).when().get("/anotherGreetXML");
    }

    @Test
    void childrenElementsIsEmpty() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.notDefined.children().isEmpty()", equalTo(true)).when().get("/anotherGreetXML");
    }

    @Test
    void xmlNestedElements2() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.name.firstName", equalTo("John")).when().get("/anotherGreetXML");
    }

    @Test
    void xmlWithContentAssertion() {
        String expectedBody = "<greeting>\n" +
                "      <name>\n" +
                "        <firstName>John</firstName>\n" +
                "        <lastName>Doe</lastName>\n" +
                "      </name>\n" +
                "    </greeting>";
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body(equalTo(expectedBody)).when().get("/anotherGreetXML");
    }

    @Test
    void newSyntaxWithXPath() {
        RestAssured.expect().body(hasXPath("/greeting/name/firstName[text()='John']")).then().with().params("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test
    void newSyntaxWithXPathWithContainsMatcher() {
        RestAssured.expect().body(hasXPath("/greeting/name/firstName", containsString("Jo"))).given().params("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test
    void xmlWithContentTypeTextXML() {
        RestAssured.expect().body("xml", equalTo("something")).when().get("/textXML");
    }

    @Test
    void xmlWithContentTypeHTML() {
        RestAssured.expect().body("html.head.title", equalTo("my title")).when().get("/textHTML");
    }

    @Test
    void htmlVerification() {
        RestAssured.expect().body("html.body.children()", hasItems("paragraph 1", "paragraph 2")).when().get("/textHTML");
    }

    @Test
    void htmlValueVerification() {
        RestAssured.expect().body("html.body.p.list()", hasItems("paragraph 1", "paragraph 2")).when().get("/textHTML");
    }

    @Test
    void htmlChildElementSize() {
        RestAssured.expect().body("html.body.children().size()", equalTo(2)).when().get("/textHTML");
    }

    @Test
    void htmlBodySize() {
        RestAssured.expect().body("html.body.size()", equalTo(1)).when().get("/textHTML");
    }

    @Test
    void supportsParsingHtmlWhenContentTypeEndsWithPlusHtml() {
        RestAssured.expect().body("html.head.title", equalTo("my title")).when().get("/mimeTypeWithPlusHtml");
    }

    @Test
    void canGetSpecificEntityFromListHtmlDocument() {
        RestAssured.expect().body("html.body.p[0]", equalTo("paragraph 1")).when().get("/textHTML");
    }

    @Test
    void canGetSpecificEntityFromListHtmlDocumentUsingGetAt() {
        RestAssured.expect().body("html.body.p.getAt(0)", equalTo("paragraph 1")).when().get("/textHTML");
    }

    @Test
    void rssVerification() {
        RestAssured.expect().body("rss.item.title", equalTo("rss title")).when().get("/rss");
    }

    @Test
    void nestedListsAreConvertedToJavaLists() {
        RestAssured.expect().body("rss.channel.item.size()", equalTo(2)).when().get("/bigRss");
    }

    @Test
    void supportsParsingXmlAttributes() {
        RestAssured.expect().
                body("greeting.name.@firstName", equalTo("John")).
                body("greeting.name.@lastName", equalTo("Doe")).
                when().get("/greetXMLAttribute?firstName=John&lastName=Doe");
    }

    @Test
    void throwsIAEOnIllegalXmlExpression() {
        assertThatThrownBy(() ->
            RestAssured.expect().body("greeting.€4324'21", equalTo("rss title")).when().get("/rss")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("greeting.€4324'21");
    }

    @Test
    void supportsGettingAllAttributesFromAList() {
        RestAssured.expect().body("shopping.category.@type", hasItems("groceries", "supplies", "present")).when().get("/shopping");
    }

    @Test
    void whenReturningANonCollectionAndNonArrayThenSizeIsOne() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName.size()", equalTo(1)).when().get("/greetXML");
    }

    @Test
    void supportBodyExpectationsWithMinusInRootObject() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("a-greeting.firstName", equalTo("John")).when().get("/xmlWithMinusInRoot");
    }

    @Test
    void supportBodyExpectationsWithMinusInChildObject() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.your-firstName", equalTo("John")).when().get("/xmlWithMinusInChild");
    }

    @Test
    void supportBodyExpectationsWithUnderscoreInChildObject() {
        RestAssured.with().params("firstName", "John", "lastName", "Doe").expect().body("greeting.your_firstName", equalTo("John")).when().get("/xmlWithUnderscoreInChild");
    }

    @Test
    void xmlChildListSize() {
        RestAssured.expect().body("shopping.category.item.size()", equalTo(5)).when().get("/shopping");
    }

    @Test
    void supportsGettingSpecificItemFromAListArrayStyle() {
        RestAssured.expect().body("shopping.category[0].@type", equalTo("groceries")).when().get("/shopping");
    }

    @Test
    void supportsGettingSpecificItemFromAListNonArrayStyle() {
        RestAssured.expect().body("shopping.category.getAt(0).@type", equalTo("groceries")).when().get("/shopping");
    }

    @Test
    void supportsFindingElements() {
        RestAssured.expect().body("shopping.category.findAll { it.@type == 'groceries' }.size()", equalTo(1)).when().get("/shopping");
    }

    @Test
    void supportsRegisteringCustomParserForAGivenMimeType() {
        final String mimeType = "application/something-custom";
        RestAssured.registerParser(mimeType, Parser.XML);
        try {
            RestAssured.expect().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
        } finally {
            RestAssured.unregisterParser(mimeType);
        }
    }

    @Test
    void supportsRegisteringCustomParserForAGivenMimeTypePerResponse() {
        final String mimeType = "application/something-custom";
        RestAssured.expect().parser(mimeType, Parser.XML).and().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
    }

    @Test
    void supportsRegisteringCustomParserForAGivenMimeTypeUsingResponseSpec() {
        final String mimeType = "application/something-custom";
        final ResponseSpecification specification = new ResponseSpecBuilder().registerParser(mimeType, Parser.XML).build();
        RestAssured.expect().spec(specification).and().body("body.message", equalTo("Custom mime-type")).when().get("/customMimeType");
    }

    @Test
    void supportsParsingXmlWhenContentTypeEndsWithPlusXml() {
        RestAssured.expect().body("body.message", equalTo("Custom mime-type ending with +xml")).when().get("/mimeTypeWithPlusXml");
    }

    @Test
    void throwsNiceErrorMessageWhenIllegalPath() {
        assertThatThrownBy(() ->
            RestAssured.expect().body("shopping.unknown.get(0)", hasItems("none")).when().get("/shopping")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Path shopping.unknown.get(0) is invalid.");
    }

    @Test
    void supportsGettingResponseBodyAsStringWhenUsingBodyExpectationsOnRoot() {
        final String body = RestAssured.expect().body("shopping", anything()).when().get("/shopping").asString();

        assertThat(body, containsString("<shopping>"));
    }

    @Test
    void whenExpectingContentTypeXMLThenTextXmlIsAllowedAsContentType() {
        RestAssured.given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                contentType(ContentType.XML).
        when().
                get("/xmlWithContentTypeTextXml");
    }

    @Test
    void whenExpectingContentTypeXMLThenCustomXmlIsAllowedAsContentType() {
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
    void whenExpectingContentTypeXMLThenExceptionIsThrownIfContentTypeIsJson() {
        assertThatThrownBy(() ->
            RestAssured.given().
                param("firstName", "John").
                param("lastName", "Doe").
            expect().
                contentType(ContentType.XML).
            when().
                get("/greetJSON")
        )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected content-type \"XML\" doesn't match actual content-type \"application/json;charset=utf-8\".");
    }
}


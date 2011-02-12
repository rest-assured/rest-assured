/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.exception.AssertionFailedException;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import groovyx.net.http.ContentType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class JSONGetITest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void simpleJSONAndHamcrestMatcher() throws Exception {
        expect().body("hello", equalTo("Hello Scalatra")).when().get("/hello");
    }

    @Test
    public void ognlJSONAndHamcrestMatcher() throws Exception {
        expect().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void ognlAssertionWithHamcrestMatcherAndJSONReturnsArray() throws Exception {
        expect().body("lotto.winners.winnerId", hasItems(23, 54)).when().get("/lotto");
    }

    @Test
    public void parameterSupportWithStandardHashMap() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("firstName", "John");
        parameters.put("lastName", "Doe");
        given().parameters(parameters).then().expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void paramSupportWithStandardHashMap() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("firstName", "John");
        parameters.put("lastName", "Doe");
        given().params(parameters).then().expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void parameterSupportWithMapBuilder() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void multipleParametersAreConcatenated() throws Exception {
        with().parameters("firstName", "John").and().parameters("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void multipleSingleParametersAreConcatenated() throws Exception {
        with().parameter("firstName", "John").and().parameter("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void mixingSingleAndMultipleParametersConcatenatesThem() throws Exception {
        with().parameters("firstName", "John").and().parameter("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void mixingSingleAndMultipleParamsConcatenatesThem() throws Exception {
        with().params("firstName", "John").and().param("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void restAssuredSupportsSpecifyingRequestParamsInGet() throws Exception {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
    }

    @Test
    public void restAssuredSupportsSpecifyingRequestParamsInGetWhenAlsoSpecifyingBaseUri() throws Exception {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("http://localhost:8080/greet?firstName=John&lastName=Doe");
    }

    @Test(expected = IllegalArgumentException.class)
    public void iaeIsThrownWhenMalformedGetParams() throws Exception {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void iaeIsThrownWhenNoParamsSpecifiedAfterGetPath() throws Exception {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?");
    }

    @Test(expected = IllegalArgumentException.class)
    public void iaeIsThrownWhenLastParamInGetRequestIsEmpty() throws Exception {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void iaeIsThrownWhenMiddleParamInGetRequestIsEmpty() throws Exception {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=&lastName=Doe");
    }

    @Test
    public void newSyntax() throws Exception {
        expect().content("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithParameters() throws Exception {
        expect().content("greeting", equalTo("Greetings John Doe")).with().parameters("firstName", "John", "lastName", "Doe").when().get("/greet");
    }

    @Test
    public void newSyntaxWithWrongStatusCode() throws Exception {
        // Given
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("Expected status code <300> doesn't match actual status code <200>."));

        // When
        expect().response().statusCode(300).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusCodeUsingInt() throws Exception {
        expect().statusCode(200).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusCodeUsingHamcrestMatcher() throws Exception {
        expect().statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300))).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithWrongStatusLine() throws Exception {
        // Given
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("Expected status line \"300\" doesn't match actual status line \"HTTP/1.1 200 OK\"."));

        // When
        expect().statusLine(equalTo("300")).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusLineUsingHamcrestMatcher() throws Exception {
        expect().statusLine(containsString("200 OK")).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusLineUsingStringMatching() throws Exception {
        expect().statusLine("HTTP/1.1 200 OK").and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void jsonHamcrestEqualBody() throws Exception {
        final String expectedBody = "{\"lotto\":{\"lottoId\":5,\"winning-numbers\":[2,45,34,23,7,5,3],\"winners\":[{\"winnerId\":23,\"numbers\":[2,45,34,23,3,5]},{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}]}}";
        expect().body(equalTo(expectedBody)).when().get("/lotto");
    }

    @Test
    public void restAssuredSupportsFullyQualifiedURI() throws Exception {
        final String expectedBody = "{\"lotto\":{\"lottoId\":5,\"winning-numbers\":[2,45,34,23,7,5,3],\"winners\":[{\"winnerId\":23,\"numbers\":[2,45,34,23,3,5]},{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}]}}";
        expect().body(equalTo(expectedBody)).when().get("http://localhost:8080/lotto");
    }

    @Test
    public void supportsHeaderStringMatching() throws Exception {
        expect().response().header("Content-Type", "application/json; charset=UTF-8").when().get("/lotto");
    }

    @Test
    public void multipleHeaderStatementsAreConcatenated() throws Exception {
        expect().response().header("Content-Type", "application/json; charset=UTF-8").and().header("Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingPlainStrings() throws Exception {
        expect().response().headers("Content-Type", "application/json; charset=UTF-8", "Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingHamcrestMatching() throws Exception {
        expect().response().headers("Content-Type", containsString("application/json"), "Content-Length", equalTo("160")).when().get("/lotto");
    }

    @Test
    public void multipleHeadersShortVersionUsingMixOfHamcrestMatchingAndStringMatching() throws Exception {
        expect().response().headers("Content-Type", containsString("application/json"), "Content-Length", "160").when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMap() throws Exception {
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("Content-Type", "application/json; charset=UTF-8");
        expectedHeaders.put("Content-Length", "160");

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMapWithHamcrestMatcher() throws Exception {
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("Content-Type", containsString("application/json; charset=UTF-8"));
        expectedHeaders.put("Content-Length", equalTo("160"));

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void multipleHeadersUsingMapWithMixOfStringAndHamcrestMatcher() throws Exception {
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("Content-Type", containsString("application/json; charset=UTF-8"));
        expectedHeaders.put("Content-Length", "160");

        expect().response().headers(expectedHeaders).when().get("/lotto");
    }

    @Test
    public void whenExpectedHeaderDoesntMatchAnAssertionThenAssertionFailedExceptionIsThrown() throws Exception {
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("Expected header \"Content-Length\" was not \"161\", was \"160\"."));

        expect().response().header("Content-Length", "161").when().get("/lotto");
    }

    @Test
    public void whenExpectedHeaderIsNotFoundThenAnAssertionFailedExceptionIsThrown() throws Exception {
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("Header \"Not-Defined\" was not defined in the response. Headers are: \n" +
                "Content-Type: application/json; charset=UTF-8\n" +
                "Content-Length: 160\n" +
                "Server: Jetty(6.1.26)"));

        expect().response().header("Not-Defined", "160").when().get("/lotto");
    }

    @Test
    public void whenMixingBodyMatchersRequiringContentTypeTextAndContentTypeAnyThenAnIllegalStateExceptionIsThrown() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(equalTo("Currently you cannot mix body expectations that require different content types for matching.\n" +
                "For example XPath and full body matching requires TEXT content and JSON/XML matching requires JSON/XML/ANY mapping. " +
                "You need to split conflicting matchers into two tests. Your matchers are:\n" +
                "Body containing expression \"lotto\" must match \"something\" which cannot be 'TEXT'\n" +
                "Body must match \"somethingElse\" which requires 'TEXT'"));

        expect().response().body("lotto", equalTo("something")).and().body(equalTo("somethingElse")).when().get("/lotto");
    }

    @Test
    public void multipleBodyHamcrestMatchersShortVersion() throws Exception {
        expect().body(containsString("winning-numbers"), containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleBodyHamcrestMatchersLongVersion() throws Exception {
        expect().body(containsString("winning-numbers")).and().body(containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleBodyJsonStringMatchersAndHamcrestMatchersShortVersion() throws Exception {
        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void multipleBodyJsonStringMatchersAndHamcrestMatchersLongVersion() throws Exception {
        expect().that().body("lotto.lottoId", greaterThan(2)).and().that().body("lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void multipleContentHamcrestMatchersShortVersion() throws Exception {
        expect().body(containsString("winning-numbers"), containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleContentHamcrestMatchersLongVersion() throws Exception {
        expect().body(containsString("winning-numbers")).and().body(containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleContentJsonStringMatchersAndHamcrestMatchersShortVersion() throws Exception {
        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void multipleContentJsonStringMatchersAndHamcrestMatchersLongVersion() throws Exception {
        expect().that().body("lotto.lottoId", greaterThan(2)).and().that().body("lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void hasItemHamcrestMatchingThrowsGoodErrorMessagesWhenExpectedItemNotFoundInArray() throws Exception {
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("JSON element lotto.winning-numbers doesn't match a collection containing <43>, was <[2, 45, 34, 23, 7, 5, 3]>."));

        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItem(43)).when().get("/lotto");
    }

    @Test
    public void basicAuthentication() throws Exception {
        given().auth().basic("jetty", "jetty").expect().statusCode(200).when().get("/secured/hello");
    }

    @Test
    public void basicAuthenticationWithBasePath() throws Exception {
        RestAssured.basePath = "/secured";
        try {
            given().auth().basic("jetty", "jetty").expect().statusCode(200).when().get("/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void specificationSyntax() throws Exception {
        final RequestSpecification requestSpecification = with().parameters("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).get("/greet");
    }

    @Test
    public void contentTypeSpecification() throws Exception {
        final RequestSpecification requestSpecification = given().contentType(ContentType.TEXT).with().parameters("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().contentType(ContentType.JSON).and().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).get("/greet");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingHeader() throws Exception {
        given().header("MyHeader", "Something").and().expect().body(containsString("MyHeader")).when().get("/header");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingHeaders() throws Exception {
        given().headers("MyHeader", "Something").and().expect().body(containsString("MyHeader")).when().get("/header");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultipleHeaders() throws Exception {
        given().header("MyHeader", "Something").and().headers("MyHeader2", "Something else", "MyHeader3", "H").and().
                expect().body(containsString("MyHeader"), containsString("MyHeader2"), containsString("MyHeader3")).when()
                .get("/header");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() throws Exception {
        given().cookie("username", "John").then().expect().body(equalTo("username")).when().get("/cookie");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookies() throws Exception {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().get("/cookie");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookieUsingMap() throws Exception {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put("username", "John");
        cookies.put("token", "1234");
        given().cookies(cookies).then().expect().body(equalTo("username, token")).when().get("/cookie");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingMultiple() throws Exception {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put("username", "John");
        cookies.put("token", "1234");
        given().cookies(cookies).and().cookies("key1", "value1").then().expect().body(equalTo("username, token, key1")).when().get("/cookie");
    }

    @Test
    public void getDoesntSupportsStringBody() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Cannot set a request body for a GET method");

        given().body("a body").expect().body(equalTo("a body")).when().get("/body");
    }


    @Test
    public void supportsCookieStringMatching() throws Exception {
        expect().response().cookie("key1", "value1").when().get("/setCookies");
    }

    @Test
    public void multipleCookieStatementsAreConcatenated() throws Exception {
        expect().response().cookie("key1", "value1").and().cookie("key2", "value2").when().get("/setCookies");
    }

    @Test
    public void multipleCookiesShortVersionUsingPlainStrings() throws Exception {
        expect().response().cookies("key1", "value1", "key3", "value3").when().get("/setCookies");
    }

    @Test
    public void multipleCookiesShortVersionUsingHamcrestMatching() throws Exception {
        expect().response().cookies("key2", containsString("2"), "key3", equalTo("value3")).when().get("/setCookies");
    }

    @Test
    public void multipleCookiesShortVersionUsingMixOfHamcrestMatchingAndStringMatching() throws Exception {
        expect().response().cookies("key1", containsString("1"), "key2", "value2").when().get("/setCookies");
    }

    @Test
    public void multipleCookiesUsingMap() throws Exception {
        Map expectedCookies = new HashMap();
        expectedCookies.put("key1", "value1");
        expectedCookies.put("key2", "value2");

        expect().response().cookies(expectedCookies).when().get("/setCookies");
    }

    @Test
    public void multipleCookiesUsingMapWithHamcrestMatcher() throws Exception {
        Map expectedCookies = new HashMap();
        expectedCookies.put("key1", containsString("1"));
        expectedCookies.put("key3", equalTo("value3"));

        expect().response().cookies(expectedCookies).when().get("/setCookies");
    }

    @Test
    public void multipleCookiesUsingMapWithMixOfStringAndHamcrestMatcher() throws Exception {
        Map expectedCookies = new HashMap();
        expectedCookies.put("key1", containsString("1"));
        expectedCookies.put("key2", "value2");

        expect().response().cookies(expectedCookies).when().get("/setCookies");
    }

    @Test
    public void whenExpectedCookieDoesntMatchAnAssertionThenAssertionFailedExceptionIsThrown() throws Exception {
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("Expected cookie \"key1\" was not \"value2\", was \"value1\"."));

        expect().response().cookie("key1", "value2").when().get("/setCookies");
    }

    @Test
    public void whenExpectedCookieIsNotFoundThenAnAssertionFailedExceptionIsThrown() throws Exception {
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("Cookie \"Not-Defined\" was not defined in the response. Cookies are: \n" +
                "key1 = value1\n" +
                "key2 = value2\n" +
                "key3 = value3"));

        expect().response().cookie("Not-Defined", "something").when().get("/setCookies");
    }

    @Test
    public void supportsGettingListSize() throws Exception {
        expect().body("store.book.category.size()", equalTo(4)).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingListItemInArrayStyle() throws Exception {
        expect().body("store.book[0].author", equalTo("Nigel Rees")).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingListItemInNonArrayStyle() throws Exception {
        expect().body("store.book.getAt(0).author", equalTo("Nigel Rees")).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingSingleFloat() throws Exception {
        expect().body("store.book[0].price", equalTo(8.95)).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingMap() throws Exception {
        expect().body("store.book", hasItem(hasEntry("category", "reference"))).when().get("/jsonStore");
    }
}
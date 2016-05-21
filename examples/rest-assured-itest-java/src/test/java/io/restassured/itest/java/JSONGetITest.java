/*
 * Copyright 2016 the original author or authors.
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
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import groovy.json.JsonException;
import org.junit.Test;

import java.util.*;

import static io.restassured.RestAssured.*;
import static io.restassured.parsing.Parser.JSON;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class JSONGetITest extends WithJetty {

    @Test
    public void simpleJSONAndHamcrestMatcher() throws Exception {
        expect().body("hello", equalTo("Hello Scalatra")).when().get("/hello");
    }

    @Test
    public void gpathJSONAndHamcrestMatcher() throws Exception {
        expect().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void gpathAssertionWithHamcrestMatcherAndJSONReturnsArray() throws Exception {
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
    public void queryParamWithBooleanWorks() throws Exception {
        given().
                queryParam("firstName", true).
                queryParam("lastName", false).
        expect().
                 body("greeting", equalTo("Greetings true false")).
        when().
                 get("/greet");
    }

    @Test
    public void formParamAreTreatedAsQueryParamsForGetRequests() throws Exception {
        given().
                formParam("firstName", "John").
                formParam("lastName", "Doe").
        when().
                 get("/greet").
        then().
                 body("greeting", equalTo("Greetings John Doe"));
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
    public void restAssuredSupportsPrintingTheResponse() throws Exception {
        final String greeting = expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe").print();

        assertThat(greeting, equalTo("{\"greeting\":\"Greetings John Doe\"}"));
    }

    @Test
    public void restAssuredSupportsPrettyPrintingTheResponse() throws Exception {
        final String greeting = expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe").prettyPrint();

        assertThat(greeting, equalTo("{\n    \"greeting\": \"Greetings John Doe\"\n}"));
    }

    @Test
    public void restAssuredSupportsSpecifyingRequestParamsInGetWhenAlsoSpecifyingBaseUri() throws Exception {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("http://localhost:8080/greet?firstName=John&lastName=Doe");
    }

    @Test
    public void iaeIsThrownWhenNoParamsSpecifiedAfterGetPath() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Request URI cannot end with ?");

        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?");
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
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("1 expectation failed.\n" +
                "Expected status code <300> doesn't match actual status code <200>.\n"));

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
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("1 expectation failed.\n" +
                "Expected status line \"300\" doesn't match actual status line \"HTTP/1.1 200 OK\".\n"));

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
        exception.expect(AssertionError.class);
        exception.expectMessage(containsString("JSON path lotto.winning-numbers doesn't match."));
        exception.expectMessage(containsString("Expected: a collection containing <43>"));
        exception.expectMessage(containsString("  Actual: [2, 45, 34, 23, 7, 5, 3]"));

        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItem(43)).when().get("/lotto");
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
    public void contentTypeSpecificationWithHamcrestMatcher() throws Exception {
        final RequestSpecification requestSpecification = given().contentType(ContentType.TEXT).with().parameters("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().contentType(equalTo("application/json;charset=utf-8")).and().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).get("/greet");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() throws Exception {
        given().cookie("username", "John").then().expect().body(equalTo("username")).when().get("/cookie");
    }

    @Test
    public void supportsValidatingCookiesWithNoValue() throws Exception {
        expect().cookie("some_cookie").when().get("/key_only_cookie");
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
        expect().body("store.book[0].price", equalTo(8.95f)).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingMap() throws Exception {
        expect().body("store.book", hasItem(hasEntry("category", "reference"))).when().get("/jsonStore");
    }

    @Test
    public void findAllBooksWithPriceGreaterThanTen() throws Exception {
        expect().body("store.book.findAll { book -> book.price > 10 }.size()", equalTo(2)).when().get("/jsonStore");
    }

    @Test
    public void getLastElementInList() throws Exception {
        expect().body("store.book[-1].title", equalTo("The Lord of the Rings")).when().get("/jsonStore");
    }

    @Test
    public void getFirstTwoElementsInList() throws Exception {
        expect().body("store.book[0,1].title", hasItems("Sayings of the Century", "Sword of Honour")).when().get("/jsonStore");
    }

    @Test
    public void getFirstAndLastElementsInList() throws Exception {
        expect().body("store.book[0,-1].title", hasItems("Sayings of the Century", "The Lord of the Rings")).when().get("/jsonStore");
    }

    @Test
    public void getRangeInList() throws Exception {
        expect().body("store.book[0..2].size()", equalTo(3)).when().get("/jsonStore");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void supportsGettingResponseBodyWhenStatusCodeIs401() throws Exception {
        final Response response = get("/secured/hello");

        assertThat(response.getBody().asString(), allOf(containsString("401"), containsString("Unauthorized")));
    }

    @Test
    public void throwsNiceErrorMessageWhenIllegalPath() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage("1 expectation failed.\n" +
                "JSON path store.unknown.unknown.get(0) doesn't match.\n" +
                "Expected: (a collection containing \"none\")\n" +
                "  Actual: null");

        expect().body("store.unknown.unknown.get(0)", hasItems("none")).when().get("/jsonStore");
    }

    @Test
    public void supportsParsingJsonLists() throws Exception {
        expect().body("address[0]", equalTo("Spangatan")).when().get("/jsonList");
    }

    @Test
    public void supportsGettingEmptyResponseBody() throws Exception {
        final String body = get("/emptyBody").asString();

        assertThat(body, equalTo(""));
    }

    @Test
    public void parametersAndQueryParametersAreConcatenated() throws Exception {
        with().parameters("firstName", "John").and().queryParameters("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void parameterAndQueryParameterAreConcatenated() throws Exception {
        with().parameter("firstName", "John").and().queryParameter("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void queryParametersCanBeUsedWithInts() throws Exception {
        with().queryParam("firstName", 1234).and().queryParameter("lastName", 5678).expect().body("greeting", equalTo("Greetings 1234 5678")).when().get("/greet");
    }

    @Test
    public void multiValueParametersWorks() throws Exception {
        with().param("list", "1").param("list", "2").param("list", "3").expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersWorksForSets() throws Exception {
        final Set<String> paramValues = new LinkedHashSet<String>();
        paramValues.add("1");
        paramValues.add("2");
        paramValues.add("3");
        with().param("list", paramValues).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersWorksWhenPassingInMap() throws Exception {
        final HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("list", "3");
        with().param("list", "1").param("list", "2").parameters(hashMap).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueQueryParametersWorks() throws Exception {
        with().queryParam("list", "1").queryParam("list", "2").queryParam("list", "3").expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    /**
     * Asserts that <a href="https://code.google.com/p/rest-assured/issues/detail?id=169">issue 169</a> is resolved
     */
    @Test
    public void multiValueQueryParametersWorksWhenSpecifiedInTheUrl() throws Exception {
        expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam?list=1&list=2&list=3");
    }

    /**
     * Asserts that <a href="https://code.google.com/p/rest-assured/issues/detail?id=169">issue 169</a> is resolved
     */
    @Test
    public void multiValueQueryParametersWorksWhenSpecifiedInInTheFluentAPIAsPathParameters() throws Exception {
        given().
                pathParam("one", "1").
                pathParam("two", "2").
                pathParam("three", "3").
        expect().
                body("list", equalTo("1,2,3")).
        when().
                get("/multiValueParam?list={one}&list={two}&list={three}");
    }

    @Test
    public void multiValueQueryParametersWorksWhenPassingInMap() throws Exception {
        final HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("list", "3");
        with().queryParam("list", "1").queryParam("list", "2").queryParams(hashMap).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersWorksWhenPassingInList() throws Exception {
        with().param("list", asList("1", "2", "3")).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueQueryParametersWorksWhenPassingInListWithInts() throws Exception {
        with().queryParam("list", asList(1, 2, 3)).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersSupportsAppendingWhenPassingInList() throws Exception {
        with().param("list", "1").param("list", asList("2", "3")).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueQueryParametersSupportsAppendingWhenPassingInList() throws Exception {
        with().param("list", asList("1")).param("list", asList("2", "3")).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void paramSupportsMultipleValues() throws Exception {
        with().param("list", "1", "2", "3").expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void supportsAssertingThatJsonPathDoesntExist() throws Exception {
        with().params("firstName", "John", "lastName", "Doe").expect().body("something", nullValue()).when().get("/greet");
    }

    @Test
    public void supportsAssertingThatHeaderDoesntExist() throws Exception {
        with().params("firstName", "John", "lastName", "Doe").expect().header("something", nullValue(String.class)).when().get("/greet");
    }

    @Test
    public void supportsRegisteringJsonParserForAGivenMimeType() throws Exception {
        final String mimeType = "application/vnd.uoml+json";
        RestAssured.registerParser(mimeType, JSON);
        try {
            expect().body("message", equalTo("It works")).when().get("/customMimeTypeJsonCompatible");
        } finally {
            RestAssured.unregisterParser(mimeType);
        }
    }

    @Test(expected = Exception.class)
    public void registeringJsonParserForAGivenMimeTypeButResponseIsNotJson() throws Exception {
        final String mimeType = "application/something+json";
        RestAssured.registerParser(mimeType, JSON);
        try {
            expect().body("message", equalTo("It works")).when().get("/customMimeTypeNonJsonCompatible");
        } finally {
            RestAssured.unregisterParser(mimeType);
        }
    }

    @Test
    public void givenNoBodyExpectationsThenNonBodyExpectationsWorkEvenThoughContentTypeAndBodyContentDoesNotMatch() throws Exception {
        expect().statusCode(200).and().header("Content-Type", notNullValue(String.class)).when().get("/contentTypeJsonButBodyIsNotJson");
    }

    @Test(expected = JsonException.class)
    public void malformedJson() throws Exception {
        expect().body("a", is(123456)).when().get("/malformedJson");
    }

    @Test
    public void statusCodeHasPriorityOverJsonParsingWhenErrorOccurs() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage(equalTo("Expected response body to be verified as JSON, HTML or XML but content-type 'text/plain' is not supported out of the box.\n" +
                "Try registering a custom parser using:\n" +
                "   RestAssured.registerParser(\"text/plain\", <parser type>);\n" +
                "Content was:\n" +
                "An expected error occurred\n"));

        expect().
                statusCode(200).
                body("doesnt.exist", equalTo("something")).
        when().
                get("/statusCode500");
    }

    @Test
    public void canParseJsonPathWithAFragmentStartingWithAtSign() throws Exception {
        expect().
                statusCode(200).
                body("body.@id", is(10)).
        when().
                get("/jsonWithAtSign");
    }

    @Test
    public void canParseJsonPathWithAnEscapedFragmentStartingWithAtSign() throws Exception {
        expect().
                statusCode(200).
                body("body.'@id'", is(10)).
        when().
                get("/jsonWithAtSign");
    }

    @Test
    public void supportsParsingJsonWhenContentTypeEndsWithPlusJson() throws Exception {
        expect().body("message", equalTo("It works")).when().get("/mimeTypeWithPlusJson");
    }

    @Test
    public void contentTypeButNoBody() throws Exception {
        expect().contentType(ContentType.JSON).when().get("/contentTypeButNoBody");
    }

    @Test
    public void contentTypeButNoBodyWhenError() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage("Cannot assert that path \"error\" matches null because the response body is empty.");

        expect().contentType(ContentType.JSON).body("error", equalTo(null)).when().get("/statusCode409WithNoBody");
    }

    @Test
    public void uuidIsTreatedAsString() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        given().
                queryParam("firstName", uuid1).
                queryParam("lastName", uuid2).
        when().
                get("/greet").
        then().
                body("greeting", equalTo(format("Greetings %s %s", uuid1, uuid2)));
    }
}
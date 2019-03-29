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

import groovy.json.JsonException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.Test;

import java.util.*;

import static io.restassured.RestAssured.*;
import static io.restassured.parsing.Parser.JSON;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class JSONGetITest extends WithJetty {

    @Test
    public void simpleJSONAndHamcrestMatcher() {
        expect().body("hello", equalTo("Hello Scalatra")).when().get("/hello");
    }

    @Test
    public void gpathJSONAndHamcrestMatcher() {
        expect().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void gpathAssertionWithHamcrestMatcherAndJSONReturnsArray() {
        expect().body("lotto.winners.winnerId", hasItems(23, 54)).when().get("/lotto");
    }

    @Test
    public void parameterSupportWithStandardHashMap() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("firstName", "John");
        parameters.put("lastName", "Doe");
        given().params(parameters).then().expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void paramSupportWithStandardHashMap() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("firstName", "John");
        parameters.put("lastName", "Doe");
        given().params(parameters).then().expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void queryParamWithBooleanWorks() {
        given().
                queryParam("firstName", true).
                queryParam("lastName", false).
        expect().
                 body("greeting", equalTo("Greetings true false")).
        when().
                 get("/greet");
    }

    @Test
    public void formParamAreTreatedAsQueryParamsForGetRequests() {
        given().
                formParam("firstName", "John").
                formParam("lastName", "Doe").
        when().
                 get("/greet").
        then().
                 body("greeting", equalTo("Greetings John Doe"));
    }

    @Test
    public void parameterSupportWithMapBuilder() {
        with().params("firstName", "John", "lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void multipleParametersAreConcatenated() {
        with().params("firstName", "John").and().params("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void multipleSingleParametersAreConcatenated() {
        with().param("firstName", "John").and().param("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void mixingSingleAndMultipleParametersConcatenatesThem() {
        with().params("firstName", "John").and().param("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void mixingSingleAndMultipleParamsConcatenatesThem() {
        with().params("firstName", "John").and().param("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void restAssuredSupportsSpecifyingRequestParamsInGet() {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
    }

    @Test
    public void restAssuredSupportsPrintingTheResponse() {
        final String greeting = expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe").print();

        assertThat(greeting, equalTo("{\"greeting\":\"Greetings John Doe\"}"));
    }

    @Test
    public void restAssuredSupportsPrettyPrintingTheResponse() {
        final String greeting = expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe").prettyPrint();

        assertThat(greeting, equalTo("{\n    \"greeting\": \"Greetings John Doe\"\n}"));
    }

    @Test
    public void restAssuredSupportsSpecifyingRequestParamsInGetWhenAlsoSpecifyingBaseUri() {
        expect().body("greeting", equalTo("Greetings John Doe")).when().get("http://localhost:8080/greet?firstName=John&lastName=Doe");
    }

    @Test
    public void iaeIsThrownWhenNoParamsSpecifiedAfterGetPath() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Request URI cannot end with ?");

        expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?");
    }

    @Test
    public void newSyntax() {
        expect().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithParameters() {
        expect().body("greeting", equalTo("Greetings John Doe")).with().params("firstName", "John", "lastName", "Doe").when().get("/greet");
    }

    @Test
    public void newSyntaxWithWrongStatusCode() {
        // Given
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("1 expectation failed.\n" +
                "Expected status code <300> but was <200>.\n"));

        // When
        expect().response().statusCode(300).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusCodeUsingInt() {
        expect().statusCode(200).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusCodeUsingHamcrestMatcher() {
        expect().statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300))).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithWrongStatusLine() {
        // Given
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("1 expectation failed.\n" +
                "Expected status line \"300\" doesn't match actual status line \"HTTP/1.1 200 OK\".\n"));

        // When
        expect().statusLine(equalTo("300")).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusLineUsingHamcrestMatcher() {
        expect().statusLine(containsString("200 OK")).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void newSyntaxWithCorrectStatusLineUsingStringMatching() {
        expect().statusLine("HTTP/1.1 200 OK").and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }

    @Test
    public void jsonHamcrestEqualBody() {
        final String expectedBody = "{\"lotto\":{\"lottoId\":5,\"winning-numbers\":[2,45,34,23,7,5,3],\"winners\":[{\"winnerId\":23,\"numbers\":[2,45,34,23,3,5]},{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}]}}";
        expect().body(equalTo(expectedBody)).when().get("/lotto");
    }

    @Test
    public void restAssuredSupportsFullyQualifiedURI() {
        final String expectedBody = "{\"lotto\":{\"lottoId\":5,\"winning-numbers\":[2,45,34,23,7,5,3],\"winners\":[{\"winnerId\":23,\"numbers\":[2,45,34,23,3,5]},{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}]}}";
        expect().body(equalTo(expectedBody)).when().get("http://localhost:8080/lotto");
    }

    @Test
    public void multipleBodyHamcrestMatchersShortVersion() {
        expect().body(containsString("winning-numbers"), containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleBodyHamcrestMatchersLongVersion() {
        expect().body(containsString("winning-numbers")).and().body(containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleBodyJsonStringMatchersAndHamcrestMatchersShortVersion() {
        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void multipleBodyJsonStringMatchersAndHamcrestMatchersLongVersion() {
        expect().that().body("lotto.lottoId", greaterThan(2)).and().that().body("lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void multipleContentHamcrestMatchersShortVersion() {
        expect().body(containsString("winning-numbers"), containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleContentHamcrestMatchersLongVersion() {
        expect().body(containsString("winning-numbers")).and().body(containsString("winners")).when().get("/lotto");
    }

    @Test
    public void multipleContentJsonStringMatchersAndHamcrestMatchersShortVersion() {
        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void multipleContentJsonStringMatchersAndHamcrestMatchersLongVersion() {
        expect().that().body("lotto.lottoId", greaterThan(2)).and().that().body("lotto.winning-numbers", hasItem(45)).when().get("/lotto");
    }

    @Test
    public void hasItemHamcrestMatchingThrowsGoodErrorMessagesWhenExpectedItemNotFoundInArray() {
        exception.expect(AssertionError.class);
        exception.expectMessage(containsString("JSON path lotto.winning-numbers doesn't match."));
        exception.expectMessage(containsString("Expected: a collection containing <43>"));
        exception.expectMessage(containsString("  Actual: [2, 45, 34, 23, 7, 5, 3]"));

        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItem(43)).when().get("/lotto");
    }

    @Test
    public void specificationSyntax() {
        final RequestSpecification requestSpecification = with().params("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).get("/greet");
    }

    @Test
    public void contentTypeSpecification() {
        final RequestSpecification requestSpecification = given().contentType(ContentType.TEXT).with().params("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().contentType(ContentType.JSON).and().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).get("/greet");
    }

    @Test
    public void contentTypeSpecificationWithHamcrestMatcher() {
        final RequestSpecification requestSpecification = given().contentType(ContentType.TEXT).with().params("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().contentType(equalTo("application/json;charset=utf-8")).and().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).get("/greet");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() {
        given().cookie("username", "John").then().expect().body(equalTo("username")).when().get("/cookie");
    }

    @Test
    public void supportsValidatingCookiesWithNoValue() {
        expect().cookie("some_cookie").when().get("/key_only_cookie");
    }

    @Test
    public void supportsGettingListSize() {
        expect().body("store.book.category.size()", equalTo(4)).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingListItemInArrayStyle() {
        expect().body("store.book[0].author", equalTo("Nigel Rees")).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingListItemInNonArrayStyle() {
        expect().body("store.book.getAt(0).author", equalTo("Nigel Rees")).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingSingleFloat() {
        expect().body("store.book[0].price", equalTo(8.95f)).when().get("/jsonStore");
    }

    @Test
    public void supportsGettingMap() {
        expect().body("store.book", hasItem(hasEntry("category", "reference"))).when().get("/jsonStore");
    }

    @Test
    public void findAllBooksWithPriceGreaterThanTen() {
        expect().body("store.book.findAll { book -> book.price > 10 }.size()", equalTo(2)).when().get("/jsonStore");
    }

    @Test
    public void getLastElementInList() {
        expect().body("store.book[-1].title", equalTo("The Lord of the Rings")).when().get("/jsonStore");
    }

    @Test
    public void getFirstTwoElementsInList() {
        expect().body("store.book[0,1].title", hasItems("Sayings of the Century", "Sword of Honour")).when().get("/jsonStore");
    }

    @Test
    public void getFirstAndLastElementsInList() {
        expect().body("store.book[0,-1].title", hasItems("Sayings of the Century", "The Lord of the Rings")).when().get("/jsonStore");
    }

    @Test
    public void getRangeInList() {
        expect().body("store.book[0..2].size()", equalTo(3)).when().get("/jsonStore");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void supportsGettingResponseBodyWhenStatusCodeIs401() {
        final Response response = get("/secured/hello");

        assertThat(response.getBody().asString(), allOf(containsString("401"), containsString("Unauthorized")));
    }

    @Test
    public void throwsNiceErrorMessageWhenIllegalPath() {
        exception.expect(AssertionError.class);
        exception.expectMessage("1 expectation failed.\n" +
                "JSON path store.unknown.unknown.get(0) doesn't match.\n" +
                "Expected: (a collection containing \"none\")\n" +
                "  Actual: null");

        expect().body("store.unknown.unknown.get(0)", hasItems("none")).when().get("/jsonStore");
    }

    @Test
    public void supportsParsingJsonLists() {
        expect().body("address[0]", equalTo("Spangatan")).when().get("/jsonList");
    }

    @Test
    public void supportsGettingEmptyResponseBody() {
        final String body = get("/emptyBody").asString();

        assertThat(body, equalTo(""));
    }

    @Test
    public void parametersAndQueryParametersAreConcatenated() {
        with().params("firstName", "John").and().queryParams("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void parameterAndQueryParameterAreConcatenated() {
        with().param("firstName", "John").and().queryParam("lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void queryParametersCanBeUsedWithInts() {
        with().queryParam("firstName", 1234).and().queryParam("lastName", 5678).expect().body("greeting", equalTo("Greetings 1234 5678")).when().get("/greet");
    }

    @Test
    public void multiValueParametersWorks() {
        with().param("list", "1").param("list", "2").param("list", "3").expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersWorksForSets() {
        final Set<String> paramValues = new LinkedHashSet<>();
        paramValues.add("1");
        paramValues.add("2");
        paramValues.add("3");
        with().param("list", paramValues).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersWorksWhenPassingInMap() {
        final HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("list", "3");
        with().param("list", "1").param("list", "2").params(hashMap).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueQueryParametersWorks() {
        with().queryParam("list", "1").queryParam("list", "2").queryParam("list", "3").expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    /**
     * Asserts that <a href="https://code.google.com/p/rest-assured/issues/detail?id=169">issue 169</a> is resolved
     */
    @Test
    public void multiValueQueryParametersWorksWhenSpecifiedInTheUrl() {
        expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam?list=1&list=2&list=3");
    }

    /**
     * Asserts that <a href="https://code.google.com/p/rest-assured/issues/detail?id=169">issue 169</a> is resolved
     */
    @Test
    public void multiValueQueryParametersWorksWhenSpecifiedInInTheFluentAPIAsPathParameters() {
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
    public void multiValueQueryParametersWorksWhenPassingInMap() {
        final HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("list", "3");
        with().queryParam("list", "1").queryParam("list", "2").queryParams(hashMap).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersWorksWhenPassingInList() {
        with().param("list", asList("1", "2", "3")).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueQueryParametersWorksWhenPassingInListWithInts() {
        with().queryParam("list", asList(1, 2, 3)).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueParametersSupportsAppendingWhenPassingInList() {
        with().param("list", "1").param("list", asList("2", "3")).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void multiValueQueryParametersSupportsAppendingWhenPassingInList() {
        with().param("list", singletonList("1")).param("list", asList("2", "3")).expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void paramSupportsMultipleValues() {
        with().param("list", "1", "2", "3").expect().body("list", equalTo("1,2,3")).when().get("/multiValueParam");
    }

    @Test
    public void supportsAssertingThatJsonPathDoesntExist() {
        with().params("firstName", "John", "lastName", "Doe").expect().body("something", nullValue()).when().get("/greet");
    }

    @Test
    public void supportsAssertingThatHeaderDoesntExist() {
        with().params("firstName", "John", "lastName", "Doe").expect().header("something", nullValue(String.class)).when().get("/greet");
    }

    @Test
    public void supportsRegisteringJsonParserForAGivenMimeType() {
        final String mimeType = "application/vnd.uoml+json";
        RestAssured.registerParser(mimeType, JSON);
        try {
            expect().body("message", equalTo("It works")).when().get("/customMimeTypeJsonCompatible");
        } finally {
            RestAssured.unregisterParser(mimeType);
        }
    }

    @Test(expected = Exception.class)
    public void registeringJsonParserForAGivenMimeTypeButResponseIsNotJson() {
        final String mimeType = "application/something+json";
        RestAssured.registerParser(mimeType, JSON);
        try {
            expect().body("message", equalTo("It works")).when().get("/customMimeTypeNonJsonCompatible");
        } finally {
            RestAssured.unregisterParser(mimeType);
        }
    }

    @Test
    public void givenNoBodyExpectationsThenNonBodyExpectationsWorkEvenThoughContentTypeAndBodyContentDoesNotMatch() {
        expect().statusCode(200).and().header("Content-Type", notNullValue(String.class)).when().get("/contentTypeJsonButBodyIsNotJson");
    }

    @Test(expected = JsonException.class)
    public void malformedJson() {
        expect().body("a", is(123456)).when().get("/malformedJson");
    }

    @Test
    public void statusCodeHasPriorityOverJsonParsingWhenErrorOccurs() {
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
    public void canParseJsonPathWithAFragmentStartingWithAtSign() {
        expect().
                statusCode(200).
                body("body.@id", is(10)).
        when().
                get("/jsonWithAtSign");
    }

    @Test
    public void canParseJsonPathWithAnEscapedFragmentStartingWithAtSign() {
        expect().
                statusCode(200).
                body("body.'@id'", is(10)).
        when().
                get("/jsonWithAtSign");
    }

    @Test
    public void supportsParsingJsonWhenContentTypeEndsWithPlusJson() {
        expect().body("message", equalTo("It works")).when().get("/mimeTypeWithPlusJson");
    }

    @Test
    public void contentTypeButNoBody() {
        expect().contentType(ContentType.JSON).when().get("/contentTypeButNoBody");
    }

    @Test
    public void contentTypeButNoBodyWhenError() {
        exception.expect(AssertionError.class);
        exception.expectMessage("Cannot assert that path \"error\" matches null because the response body is empty.");

        expect().contentType(ContentType.JSON).body("error", equalTo(null)).when().get("/statusCode409WithNoBody");
    }

    @Test
    public void uuidIsTreatedAsString() {
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

    @Test public void
    json_parser_is_used_for_text_json_content_type() {
        when().get("/text-json").then().body("test", is(true));
    }

    @Test public void
    throws_assertion_error_when_multiple_keys_are_the_same_in_a_multi_body_expectation_and_the_non_last_fails() {
        exception.expect(AssertionError.class);
        exception.expectMessage("Expected: <7>\n" +
                "  Actual: 5");

        when().
                get("/lotto").
        then().
                rootPath("lotto").
                body("lottoId", greaterThan(1),
                     "lottoId", equalTo(7),
                     "lottoId", lessThan(9));
    }

    @Test public void
    specifying_multiple_with_args_without_additional_paths_works() {
        get("/jsonStore").then()
                .rootPath("store.book.find { it.author == '%s' }.price")
                .body(
                        withArgs("Nigel Rees"), is(8.95f),
                        withArgs("Evelyn Waugh"), is(12.99f),
                        withArgs("Herman Melville"), is(8.99f),
                        withArgs("J. R. R. Tolkien"), is(22.99f)
                );
    }
}
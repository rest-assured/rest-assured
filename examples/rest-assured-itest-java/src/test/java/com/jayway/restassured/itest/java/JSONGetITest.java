package com.jayway.restassured.itest.java;

import com.jayway.restassured.exception.AssertionFailedException;
import com.jayway.restassured.itest.support.WithJetty;
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

public class JSONGetITest extends WithJetty {

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
    public void parameterSupportWithMapBuilder() throws Exception {
      with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet");
    }

    @Test
    public void newSyntax() throws Exception {
        expect().content("lotto.lottoId", equalTo(5)).when().get("lotto");
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
                "Server: Jetty(6.1.14)"));

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
        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItemInArray(45)).when().get("/lotto");
    }

    @Test
    public void multipleBodyJsonStringMatchersAndHamcrestMatchersLongVersion() throws Exception {
        expect().that().body("lotto.lottoId", greaterThan(2)).and().that().body("lotto.winning-numbers", hasItemInArray(45)).when().get("/lotto");
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
        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItemInArray(45)).when().get("/lotto");
    }

    @Test
    public void multipleContentJsonStringMatchersAndHamcrestMatchersLongVersion() throws Exception {
        expect().that().body("lotto.lottoId", greaterThan(2)).and().that().body("lotto.winning-numbers", hasItemInArray(45)).when().get("/lotto");
    }

    @Test
    public void hasItemInArrayHamcrestMatchingThrowsGoodErrorMessagesWhenExpectedItemNotFoundInArray() throws Exception {
        exception.expect(AssertionFailedException.class);
        exception.expectMessage(equalTo("JSON element lotto.winning-numbers doesn't match an array containing <43>, was <2,45,34,23,7,5,3>."));

        expect().body("lotto.lottoId", greaterThan(2), "lotto.winning-numbers", hasItemInArray(43)).when().get("/lotto");
    }

    @Test
    public void basicAuthentication() throws Exception {
        given().auth().basic("jetty", "jetty").expect().statusCode(200).when().get("/secured/hello");
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
    public void requestSpecificationAllowsSpecifyingHeaders() throws Exception {
        given().headers("MyHeader", "Something").and().expect().body("hello", equalTo("Hello Scalatra")).when().get("/hello");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() throws Exception {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().get("/cookie");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingCookieUsingMap() throws Exception {
        Map<String, String> cookies = new HashMap<String, String>();
        cookies.put("username", "John");
        cookies.put("token", "1234");
        given().cookies(cookies).then().expect().body(equalTo("username, token")).when().get("/cookie");
    }
}

package com.jayway.restassured.itest.java;

import com.jayway.restassured.exception.AssertionFailedException;
import com.jayway.restassured.itest.support.WithJetty;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.*;

public class JSONGetITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void setupJetty() throws Exception {
        WithJetty.itestPath = "/examples/rest-assured-itest";
    }

    @Test
    public void simpleJSONAndHamcrestMatcher() throws Exception {
        get("/hello").andAssertThat("hello", equalTo("Hello Scalatra"));
    }

    @Test
    public void ognlJSONAndHamcrestMatcher() throws Exception {
        get("/lotto").andAssertThat("lotto.lottoId", equalTo(5));
    }

    @Test
    public void ognlAssertionWithHamcrestMatcherAndJSONResturnsArray() throws Exception {
        get("/lotto").andAssertThat("lotto.winners.winnerId", hasItems(23, 54));
    }

    @Test
    public void parameterSupportWithStandardHashMap() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("firstName", "John");
        parameters.put("lastName", "Doe");
        get("/greet").with().parameters(parameters).andAssertThat("greeting", equalTo("Greetings John Doe"));
    }

    @Test
    public void parameterSupportWithMapBuilder() throws Exception {
        get("/greet").with().parameters("firstName", "John", "lastName", "Doe").andAssertThat("greeting", equalTo("Greetings John Doe"));
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
}

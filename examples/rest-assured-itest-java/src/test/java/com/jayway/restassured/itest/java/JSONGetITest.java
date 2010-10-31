package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class JSONGetITest extends WithJetty {

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
}

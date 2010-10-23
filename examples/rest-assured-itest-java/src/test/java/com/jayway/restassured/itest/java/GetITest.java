package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class GetITest extends WithJetty {

    @BeforeClass
    public static void setupJetty() throws Exception {
        WithJetty.itestPath = "/examples/rest-assured-itest/java";
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
}

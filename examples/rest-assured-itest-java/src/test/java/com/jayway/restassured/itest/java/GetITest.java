package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

public class GetITest extends WithJetty {

    @BeforeClass
    public static void setupJetty() throws Exception {
        WithJetty.itestPath = "/examples/rest-assured-itest/java";
    }

    @Test
    public void simpleJSONAndHamcrestMatcher() throws Exception {
        get("/hello").thenAssertThat("hello", equalTo("Hello Scalatra"));
    }

    @Test
    public void ognlJSONAndHamcrestMatcher() throws Exception {
        get("/lotto").thenAssertThat("lotto.lotto-id", equalTo(5));
    }
}

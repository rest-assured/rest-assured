package com.jayway.restassured.itest.java;

import com.jayway.restassured.exception.AssertionFailedException;
import com.jayway.restassured.itest.support.WithJetty;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class JSONPostITest extends WithJetty {

    @Test
    public void simpleJSONAndHamcrestMatcher() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting", equalTo("Greetings John Doe")).when().post("/greet");
    }

    @Test
    public void bodyHamcrestMatcherWithoutKey() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}")).when().post("/greet");
    }

    @Test
    @Ignore("Not yet supported")
    public void bodyHamcrestMatchingWhenPost() throws Exception {
        expect().body(containsString("winning-numbers")).when().post("/lotto");
    }
}

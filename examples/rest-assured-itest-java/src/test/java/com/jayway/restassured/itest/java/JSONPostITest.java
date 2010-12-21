package com.jayway.restassured.itest.java;

import com.jayway.restassured.exception.AssertionFailedException;
import com.jayway.restassured.itest.support.WithJetty;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import static groovyx.net.http.ContentType.*;
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
    public void bodyWithSingleHamcrestMatching() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body(containsString("greeting")).when().post("/greet");
    }

    @Test
    public void bodyHamcrestMatcherWithoutKey() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}")).when().post("/greet");
    }

    @Test
    public void requestContentType() throws Exception {
        final RequestSpecification requestSpecification = given().contentType(URLENC).with().parameters("firstName", "John", "lastName", "Doe");
        final ResponseSpecification responseSpecification = expect().contentType(JSON).and().body("greeting", equalTo("Greetings John Doe"));
        given(requestSpecification, responseSpecification).post("/greet");
    }

    @Test
    public void uriNotFoundTWhenPost() throws Exception {
        expect().statusCode(404).and().body(equalTo(null)).when().post("/lotto");
    }
}

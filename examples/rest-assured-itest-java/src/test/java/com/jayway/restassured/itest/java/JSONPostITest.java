package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static groovyx.net.http.ContentType.JSON;
import static groovyx.net.http.ContentType.URLENC;
import static groovyx.net.http.ContentType.TEXT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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

    @Test
    public void requestSpecificationAllowsSpecifyingHeaders() throws Exception {
        given().headers("MyHeader", "Something").and().expect().body("hello", equalTo("Hello Scalatra")).when().post("/hello");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingStringBodyForPost() throws Exception {
        given().request().body("some body").then().expect().response().body(equalTo("some body")).when().post("/body");
    }

    @Test
    public void requestSpecificationAllowsSpecifyingJsonBodyForPost() throws Exception {
        given().body("{ \"message\" : \"hello world\"}").with().contentType(JSON).then().expect().body(equalTo("hello world")).when().post("/jsonBody");
    }
}

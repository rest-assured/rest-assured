package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.Matchers.*;

public class XMLPostITest extends WithJetty {

    @Test
    public void xmlParameterSupport() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().post("/greetXML");
    }

    @Test
    public void xmlParameterSupportWithAnotherAssertion() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).post("/greetXML");
    }

    @Test
    public void xmlWithLists() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting", hasItems("John", "Doe")).post("/greetXML");
    }

    @Test
    public void postWithXPath() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName[text()='John']")).with().parameters("firstName", "John", "lastName", "Doe").post("/anotherGreetXML");
    }

    @Test
    public void postWithXPathContainingHamcrestMatcher() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName", containsString("Jo"))).with().parameters("firstName", "John", "lastName", "Doe").post("/anotherGreetXML");
    }
}

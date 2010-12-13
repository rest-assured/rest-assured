package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.*;

public class XMLGetITest extends WithJetty {

    @BeforeClass
    public static void setupJetty() throws Exception {
        WithJetty.itestPath = "/examples/rest-assured-itest";
    }

    @Test
    public void xmlParameterSupport() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.firstName", equalTo("John")).when().get("/greetXML");
    }

    @Test
    public void xmlParameterSupportWithAnotherAssertion() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.lastName", equalTo("Doe")).get("/greetXML");
    }

    @Test
    public void xmlWithLists() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting", hasItems("John", "Doe")).get("/greetXML");
    }

    @Test
    public void xmlNestedElements() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.name", hasItems("John", "Doe")).get("/anotherGreetXML");
    }

    @Test
    public void xmlNestedElements2() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body("greeting.name.firstName", equalTo("John")).get("/anotherGreetXML");
    }

    @Test
    @Ignore("not implemented yet")
    public void xmlWithContentAssertion() throws Exception {
        with().parameters("firstName", "John", "lastName", "Doe").expect().body(equalTo("John")).when().get("/anotherGreetXML");
    }

    @Test
    public void newSyntaxWithXPath() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName[text()='John']")).with().parameters("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test
    public void newSyntaxWithXPathWithContainsMatcher() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName", containsString("Jo"))).with().parameters("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }
}

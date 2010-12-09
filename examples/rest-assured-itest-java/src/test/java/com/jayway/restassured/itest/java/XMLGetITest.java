package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.xml.HasXPath.hasXPath;

public class XMLGetITest extends WithJetty {

    @BeforeClass
    public static void setupJetty() throws Exception {
        WithJetty.itestPath = "/examples/rest-assured-itest";
    }

    @Test
    public void xmlParameterSupport() throws Exception {
        get("/greetXML").with().parameters("firstName", "John", "lastName", "Doe").andAssertThat("greeting.firstName", equalTo("John"));
    }

    @Test
    public void xmlParameterSupportWithAnotherAssertion() throws Exception {
        get("/greetXML").with().parameters("firstName", "John", "lastName", "Doe").andAssertThat("greeting.lastName", equalTo("Doe"));
    }

    @Test
    public void xmlWithLists() throws Exception {
        get("/greetXML").with().parameters("firstName", "John", "lastName", "Doe").andAssertThat("greeting", hasItems("John", "Doe"));
    }

    @Test
    public void xmlNestedElements() throws Exception {
        get("/anotherGreetXML").with().parameters("firstName", "John", "lastName", "Doe").andAssertThat("greeting.name", hasItems("John", "Doe"));
    }

    @Test
    public void xmlNestedElements2() throws Exception {
        get("/anotherGreetXML").with().parameters("firstName", "John", "lastName", "Doe").andAssertThat("greeting.name.firstName", equalTo("John"));
    }

    @Test
    @Ignore("not implemented yet")
    public void xmlWithContentAssertion() throws Exception {
        get("/anotherGreetXML").with().parameters("firstName", "John", "lastName", "Doe").andAssertThatContent(equalTo("John"));
    }

    @Ignore("not implemented yet")
    @Test
    public void newSyntaxWithXPath() throws Exception {
        expect().body(hasXPath("/greeting/name/firstName")).with().parameters("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }
}

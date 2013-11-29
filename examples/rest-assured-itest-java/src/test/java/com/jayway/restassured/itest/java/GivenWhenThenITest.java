package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;

public class GivenWhenThenITest extends WithJetty {

    @Test public void
    simple_given_when_then_works() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                statusCode(200).
                body("greeting", equalTo("Greetings John Doe"));
    }

    @Test public void
    given_when_then_works_with_assert_that_and_and() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                assertThat().
                statusCode(200).
                and().
                body("greeting", equalTo("Greetings John Doe"));
    }

    @Test public void
    given_when_then_works_with_xpath_assertions() {
        given().
                parameters("firstName", "John", "lastName", "Doe").
        when().
                get("/greetXML").
        then().
                body(hasXPath("/greeting/firstName[text()='John']"));
    }

    @Test public void
    given_when_then_works_with_multiple_body_assertions() {
        given().
                parameters("firstName", "John", "lastName", "Doe").
        when().
                get("/greetXML").
        then().
                body(containsString("greeting")).
                body(containsString("John"));
    }
}

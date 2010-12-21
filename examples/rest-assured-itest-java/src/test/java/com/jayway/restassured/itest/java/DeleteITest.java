package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class DeleteITest extends WithJetty {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() throws Exception {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().delete("/cookie");
    }

    @Test
    public void bodyHamcrestMatcherWithoutKey() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}")).when().delete("/greet");
    }

      @Test
    public void deleteDoesntSupportsStringBody() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Cannot set a request body for a DELETE method");

        given().body("a body").expect().body(equalTo("a body")).when().delete("/body");
    }
}

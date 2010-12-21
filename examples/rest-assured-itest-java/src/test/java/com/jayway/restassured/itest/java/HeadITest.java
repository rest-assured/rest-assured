package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class HeadITest extends WithJetty {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void headDoesntSupportsStringBody() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Cannot set a request body for a HEAD method");

        given().body("a body").expect().body(equalTo("a body")).when().head("/body");
    }
}

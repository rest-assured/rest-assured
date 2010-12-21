package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PutITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void requestSpecificationAllowsSpecifyingCookie() throws Exception {
        given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().put("/cookie");
    }

    @Test
    public void bodyHamcrestMatcherWithoutKey() throws Exception {
        given().parameters("firstName", "John", "lastName", "Doe").expect().body(equalTo("{\"greeting\":\"Greetings John Doe\"}")).when().put("/greet");
    }

    @Test
    public void putDoesntSupportsBinaryBody() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("PUT doesn't support binary request data.");

        final byte[] body = "a body".getBytes("UTF-8");
        given().body(body).expect().body(equalTo("34, 126, 18")).when().put("/binaryBody");
    }

    @Test
    public void putSupportsStringBody() throws Exception {
        given().body("a body").expect().body(equalTo("a body")).when().put("/body");
    }
}

package io.restassured.http;

import io.restassured.RestAssured;
import org.junit.Test;

import static io.restassured.RestAssured.given;

public class SniSSLTest {

    /**
     * https://github.com/rest-assured/rest-assured/issues/548
     */
    @Test
    public void validateSniWorks() {
        RestAssured.baseURI = "https://sni.velox.ch"; // sample server with SNI configured
        given().expect().statusCode(200).with().get("/");
    }
}

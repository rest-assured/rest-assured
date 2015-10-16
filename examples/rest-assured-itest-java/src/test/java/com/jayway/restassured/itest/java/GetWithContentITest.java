package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;

public class GetWithContentITest extends WithJetty {

    @Test public void
    get_method_with_content() {
        given().body("hullo").expect().statusCode(200).when().get("/getWithContent");
    }
}

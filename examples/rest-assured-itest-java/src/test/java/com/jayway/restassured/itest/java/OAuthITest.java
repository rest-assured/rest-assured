package com.jayway.restassured.itest.java;

import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.authentication.OAuthSignature.QUERY_STRING;
import static org.hamcrest.Matchers.equalTo;

public class OAuthITest {

    @Test public void
    oauth1_works_with_header_signing() {
        given().
                auth().oauth("key", "secret", "accesskey", "accesssecret").
        when().
                get("http://term.ie/oauth/example/echo_api.php?works=true").
        then().
                body("html.body", equalTo("works=true"));
    }

    @Ignore @Test public void
    oauth1_works_with_query_signing() {
        given().
                auth().oauth("key", "secret", "accesskey", "accesssecret", QUERY_STRING).
        when().
                get("http://term.ie/oauth/example/echo_api.php?works=true").
        then().
                body("html.body", equalTo("works=true"));
    }
}

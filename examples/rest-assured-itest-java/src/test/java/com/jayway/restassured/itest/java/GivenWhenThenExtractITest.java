package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GivenWhenThenExtractITest extends WithJetty {

    @Test public void
    extract_response_as_string_works() {
        String body = get("/hello").then().assertThat().contentType(JSON).and().extract().body().asString();

        assertThat(body, equalTo("{\"hello\":\"Hello Scalatra\"}"));
    }

    @Test public void
    extract_single_path_works() {
        String hello = get("/hello").then().assertThat().contentType(JSON).and().extract().path("hello");

        assertThat(hello, equalTo("Hello Scalatra"));
    }

    @Test public void
    extract_entire_response_works() {
        Response response = get("/hello").then().assertThat().contentType(JSON).and().extract().response();

        assertThat(response.getHeaders().hasHeaderWithName("content-type"), is(true));
    }
}

package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.http.PostController;
import com.jayway.restassured.module.mockmvc.support.Greeting;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.http.ContentType.JSON;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class PostTest {

    @BeforeClass
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.mockMvc = standaloneSetup(new PostController()).build();
    }

    @AfterClass
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    automatically_adds_x_www_form_urlencoded_as_content_type_when_posting_params() {
        given().
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }

    @Test public void
    can_supply_string_as_body_for_post() {
        given().
                body("a string").
        when().
                post("/stringBody").
        then().
                body(equalTo("a string"));
    }

    @Test public void
    can_supply_object_as_body_and_serialize_as_json() {
        Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        given().
                contentType(JSON).
                body(greeting).
        when().
                post("/jsonReflect").
        then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe"));
    }
}

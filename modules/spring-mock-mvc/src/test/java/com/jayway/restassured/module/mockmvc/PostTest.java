package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.http.GreetingController;
import com.jayway.restassured.module.mockmvc.http.PostController;
import org.junit.Test;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;

public class PostTest {

    @Test public void
    unnamed_path_params_works() {
        given().
                standaloneSetup(new PostController()).
                param("name", "Johan").
        when().
                post("/greetingPost").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }
}

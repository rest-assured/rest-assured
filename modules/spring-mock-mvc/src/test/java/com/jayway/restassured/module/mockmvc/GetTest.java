package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.http.GreetingController;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class GetTest {

    @Test public void
    unnamed_path_params_works() {
        given().
                standaloneSetup(new GreetingController()).
        when().
                get("/greeting?name={name}", "Johan").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));
    }
}

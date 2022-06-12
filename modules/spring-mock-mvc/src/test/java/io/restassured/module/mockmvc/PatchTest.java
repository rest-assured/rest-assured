package io.restassured.module.mockmvc;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.http.PatchController;
import io.restassured.module.mockmvc.support.Greeting;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public class PatchTest {
    @BeforeClass
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.mockMvc(standaloneSetup(new PatchController()).build());
    }

    @AfterClass
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test
    public void
    can_supply_string_as_body_for_patch() {
        RestAssuredMockMvc.given().
                body("a string").
                when().
                patch("/stringBody").
                then().
                body(equalTo("a string"));
    }

    @Test
    public void
    can_supply_object_as_body_and_serialize_as_json() {
        Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        RestAssuredMockMvc.given().
                contentType(ContentType.JSON).
                body(greeting).
                when().
                patch("/jsonReflect").
                then().
                statusCode(200).
                body("firstName", equalTo("John")).
                body("lastName", equalTo("Doe"));
    }
}

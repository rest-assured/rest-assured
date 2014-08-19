package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.http.AttributeController;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;

public class AttributeTest {

    @BeforeClass
    public static void configureMockMvcInstance() {
        RestAssuredMockMvc.standaloneSetup(new AttributeController());
    }

    @AfterClass
    public static void restRestAssured() {
        RestAssuredMockMvc.reset();
    }

    @Test public void
    can_send_attribute_using_attribute_name_and_value() {
        given().
                attribute("testAttribute", "John Doe").
        when().
                get("/attribute").
        then().
                statusCode(200).
                body("testAttribute", equalTo("John Doe"));
    }

    @Test public void
    can_send_attributes_using_map() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("testAttribute1", "value1");
        attributes.put("testAttribute2", "value2");
        given().
                attributes(attributes).
        when().
                get("/attribute").
        then().
                statusCode(200).
                body("testAttribute1", equalTo("value1")).
                body("testAttribute2", equalTo("value2"));
    }
}

package io.restassured.itest.java;

import com.google.gson.reflect.TypeToken;
import io.restassured.itest.java.objects.Greeting;
import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.mapper.ObjectMapperType.JAXB;

public class TypeObjectExceptionMappingITest extends WithJetty {

    @Test(expected = RuntimeException.class)
    public void shouldSeeExceptionWhenMappingTypeForJAXB() {
        final Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        given().contentType("application/xml").body(greeting, JAXB).post("/reflect").as(type, JAXB);
    }

}

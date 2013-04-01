package com.jayway.restassured.path.json;

import com.jayway.restassured.mapper.ObjectDeserializationContext;
import com.jayway.restassured.path.json.config.JsonPathConfig;
import com.jayway.restassured.path.json.mapping.JsonPathObjectDeserializer;
import com.jayway.restassured.path.json.support.Greeting;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonPathObjectDeserializationTest {

    private static final String GREETING = "{ \"greeting\" : { \n" +
            "                \"firstName\" : \"John\", \n" +
            "                \"lastName\" : \"Doe\" \n" +
            "               }\n" +
            "}";

    @Test public void
    json_path_supports_custom_deserializer() {
        // Given
        final AtomicBoolean customDeserializerUsed = new AtomicBoolean(false);

        final JsonPath jsonPath = new JsonPath(GREETING).using(new JsonPathConfig().defaultObjectDeserializer(new JsonPathObjectDeserializer() {
            public <T> T deserialize(ObjectDeserializationContext ctx) {
                customDeserializerUsed.set(true);
                final String json = ctx.getDataToDeserialize().asString();
                final Greeting greeting = new Greeting();
                greeting.setFirstName(StringUtils.substringBetween(json, "\"firstName\":\"", "\""));
                greeting.setLastName(StringUtils.substringBetween(json, "\"lastName\":\"", "\""));
                return (T) greeting;
            }
        }));

        // When
        final Greeting greeting = jsonPath.getObject("", Greeting.class);

        // Then
        assertThat(greeting.getFirstName(), equalTo("John"));
        assertThat(greeting.getLastName(), equalTo("Doe"));
        assertThat(customDeserializerUsed.get(), is(true));
    }

    @Test public void
    json_path_supports_custom_deserializer_with_static_configuration() {
        // Given
        final AtomicBoolean customDeserializerUsed = new AtomicBoolean(false);

        JsonPath.config = new JsonPathConfig().defaultObjectDeserializer(new JsonPathObjectDeserializer() {
            public <T> T deserialize(ObjectDeserializationContext ctx) {
                customDeserializerUsed.set(true);
                final String json = ctx.getDataToDeserialize().asString();
                final Greeting greeting = new Greeting();
                greeting.setFirstName(StringUtils.substringBetween(json, "\"firstName\":\"", "\""));
                greeting.setLastName(StringUtils.substringBetween(json, "\"lastName\":\"", "\""));
                return (T) greeting;
            }
        });

        final JsonPath jsonPath = new JsonPath(GREETING);

        // When
        try {
            final Greeting greeting = jsonPath.getObject("", Greeting.class);

            // Then
            assertThat(greeting.getFirstName(), equalTo("John"));
            assertThat(greeting.getLastName(), equalTo("Doe"));
            assertThat(customDeserializerUsed.get(), is(true));
        } finally {
            JsonPath.reset();
        }
    }

}

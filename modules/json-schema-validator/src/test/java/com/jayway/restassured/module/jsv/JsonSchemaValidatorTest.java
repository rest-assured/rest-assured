package com.jayway.restassured.module.jsv;


import org.junit.Test;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class JsonSchemaValidatorTest {

    @Test public void
    reset_sets_static_json_schema_validator_settings_to_null() {
        // Given
        JsonSchemaValidator.settings = new JsonSchemaValidatorSettings();

        // When
        JsonSchemaValidator.reset();

        // Then
        try {
            assertThat(JsonSchemaValidator.settings, nullValue());
        } finally {
            JsonSchemaValidator.settings = null;
        }
    }

    @Test public void
    validates_schema_in_classpath() {
        // Given
        String greetingJson = "{\n" +
                "    \"greeting\": {\n" +
                "        \"firstName\": \"John\",\n" +
                "        \"lastName\": \"Doe\"\n" +
                "    }\n" +
                "}";

        // Then
        assertThat(greetingJson, matchesJsonSchemaInClasspath("greeting-schema.json"));
    }

}

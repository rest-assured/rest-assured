package com.jayway.restassured.module.jsv;


import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

public class JsonSchemaValidatorTest {

    @Test
    public void
    reset_sets_static_json_schema_validator_settings_to_null() {
        // Given
        JsonSchemaValidator.jsonSchemaValidatorSettings = new JsonSchemaValidatorSettings();

        // When
        JsonSchemaValidator.reset();

        // Then
        try {
            assertThat(JsonSchemaValidator.jsonSchemaValidatorSettings, nullValue());
        } finally {
            JsonSchemaValidator.jsonSchemaValidatorSettings = null;
        }
    }

}

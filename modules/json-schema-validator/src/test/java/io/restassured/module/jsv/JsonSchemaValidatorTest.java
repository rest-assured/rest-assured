/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.module.jsv;


import org.hamcrest.MatcherAssert;
import org.junit.Test;

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
        MatcherAssert.assertThat(greetingJson, JsonSchemaValidator.matchesJsonSchemaInClasspath("greeting-schema.json"));
    }

}

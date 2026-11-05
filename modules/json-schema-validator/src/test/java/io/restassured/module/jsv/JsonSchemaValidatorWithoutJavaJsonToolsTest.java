/*
 * Copyright 2026 the original author or authors.
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

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static io.restassured.module.jsv.JsonSchemaValidatorSettings.settings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs in its own surefire execution, without java-json-tools (and its Jackson 2 and Guava) on the classpath.
 */
public class JsonSchemaValidatorWithoutJavaJsonToolsTest {

    // "exclusiveMaximum": true only means "less than maximum" in draft-04
    private static final String DRAFT_04_ONLY_SCHEMA = "{ \"type\": \"object\", \"properties\": { \"n\": { \"maximum\": 10, \"exclusiveMaximum\": true }, \"kind\": { \"const\": \"product\" } } }";

    private static final String CONST_SCHEMA = "{ \"properties\": { \"kind\": { \"const\": \"product\" } } }";

    @AfterEach
    void reset() {
        JsonSchemaValidator.reset();
    }

    @Test
    void java_json_tools_jackson_2_and_guava_are_not_on_the_classpath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.github.fge.jsonschema.main.JsonSchemaFactory"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("com.google.common.collect.Lists"));
    }

    @Test
    void schema_without_dollar_schema_is_validated_as_draft_04() {
        // draft-04 doesn't know "const" so it's ignored
        assertThat("{ \"n\": 9, \"kind\": \"service\" }", matchesJsonSchema(DRAFT_04_ONLY_SCHEMA));
        assertThat("{ \"n\": 10 }", not(matchesJsonSchema(DRAFT_04_ONLY_SCHEMA)));
    }

    @Test
    void configured_draft_04_is_used_for_schema_without_dollar_schema() {
        Matcher<?> matcher = matchesJsonSchema(DRAFT_04_ONLY_SCHEMA).using(JsonSchemaVersion.DRAFT_4);

        assertThat("{ \"n\": 10 }", not((Matcher<? super String>) matcher));
    }

    @Test
    void draft_04_schema_is_validated() {
        String schema = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\", \"required\": [\"name\"] }";

        assertThat("{ \"name\": \"x\" }", matchesJsonSchema(schema));
        AssertionError error = assertThrows(AssertionError.class, () -> assertThat("{ }", matchesJsonSchema(schema)));
        assertThat(error.getMessage(), containsString("The content to match the given JSON schema.\n$: required property 'name' not found\n"));
    }

    @Test
    void draft_2020_12_schema_is_validated() {
        assertThat("{ \"id\": \"o-1\", \"line\": [\"apple\", 2] }", matchesJsonSchemaInClasspath("order-schema-2020-12.json"));

        AssertionError error = assertThrows(AssertionError.class, () ->
                assertThat("{ \"id\": \"o\", \"line\": [\"apple\", 0] }", matchesJsonSchemaInClasspath("order-schema-2020-12.json")));
        assertThat(error.getMessage(), allOf(containsString("$.id: must be at least 3 characters long\n"), containsString("$.line[1]: must have a minimum value of 1\n")));
    }

    @Test
    void draft_03_schema_requires_java_json_tools() {
        JsonSchemaValidationException exception = assertThrows(JsonSchemaValidationException.class, () ->
                matchesJsonSchemaInClasspath("greeting-schema.json").matches("{ }"));

        assertThat(exception.getMessage(), allOf(startsWith("Validating a draft-03 schema requires the java-json-tools json-schema-validator."),
                containsString("<groupId>com.github.java-json-tools</groupId>"), containsString("<artifactId>json-schema-validator</artifactId>"),
                containsString("<version>2.2.14</version>"), containsString("testImplementation(\"com.github.java-json-tools:json-schema-validator:2.2.14\")")));
    }

    @Test
    void empty_content_is_an_error() {
        JsonSchemaValidationException exception = assertThrows(JsonSchemaValidationException.class, () -> matchesJsonSchema(CONST_SCHEMA).matches(""));

        assertThat(exception.getMessage(), equalTo("The content to validate is empty, so it isn't a JSON document"));
    }

    @Test
    void default_settings_work() {
        Matcher<?> matcher = matchesJsonSchema(DRAFT_04_ONLY_SCHEMA).using(settings().with().checkedValidation(false));
        assertThat("{ \"n\": 10 }", not((Matcher<? super String>) matcher));

        JsonSchemaValidator.settings = new JsonSchemaValidatorSettings().schemaVersion(JsonSchemaVersion.DRAFT_7);
        assertThat("{ \"kind\": \"service\" }", not(matchesJsonSchema(CONST_SCHEMA)));
        assertThat("{ \"kind\": \"product\" }", matchesJsonSchema(CONST_SCHEMA));
    }

    @Test
    void json_schema_factory_requires_java_json_tools() {
        JsonSchemaValidationException exception = assertThrows(JsonSchemaValidationException.class, () -> settings().jsonSchemaFactory());

        assertThat(exception.getMessage(), startsWith("A JsonSchemaFactory requires the java-json-tools json-schema-validator."));
    }
}

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

package io.restassured.itest.java;

import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.module.jsv.JsonSchemaValidator;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.github.fge.jsonschema.SchemaVersion.DRAFTV3;
import static com.github.fge.jsonschema.SchemaVersion.DRAFTV4;
import static io.restassured.RestAssured.*;
import static io.restassured.module.jsv.JsonSchemaValidatorSettings.settings;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

public class JsonSchemaValidationITest extends WithJetty {

    @Test
    void matches_string_schema_correctly() throws IOException {
        // Given
        String schema = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json"), StandardCharsets.UTF_8);

        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));
    }

    @Test
    void matches_input_stream_schema_correctly() {
        // Given
        InputStream schema = Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json");

        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));
    }

    @Test
    public void
    matches_classpath_schema_correctly() {
        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json"));
    }

    @Test
    public void
    works_with_expectation_api() {
        // When
        expect().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json")).when().get("/products");
    }

    @Test
    public void
    can_mix_json_schema_validation_and_body_validations() {
        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json")).and().body("[0].name", equalTo("An ice sculpture"));
    }

    @Test
    void throws_assertion_error_when_schema_doesnt_match_json_content() {
        assertThatThrownBy(() ->
                get("/jsonStore").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json").using(settings().parseUriAndUrlsAsJsonNode(true)))
        )
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("The content to match the given JSON schema.")
                .hasMessageContaining("instance type (object) does not match any allowed primitive type (allowed: [\"array\"])")
                .hasMessageContaining("expected: [\"array\"]")
                .hasMessageContaining("Actual: { \"store\": {");
    }

    @Test
    public void
    json_schema_validator_supports_disabling_checked_validation_statically() {
        // Given
        JsonSchemaValidator.settings = settings().with().checkedValidation(false);

        // When
        try {
            get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json"));
        } finally {
            JsonSchemaValidator.reset();
        }
    }

    @Test
    public void
    json_schema_validator_supports_specifying_json_schema_factory_instance_statically() {
        // Given
        JsonSchemaValidator.settings = settings().with().jsonSchemaFactory(
                JsonSchemaFactory.newBuilder().setValidationConfiguration(ValidationConfiguration.newBuilder().setDefaultVersion(DRAFTV3).freeze()).freeze());

        // When
        try {
            get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json"));
        } finally {
            JsonSchemaValidator.reset();
        }
    }

    @Test
    public void
    json_schema_validator_supports_specifying_json_schema_factory_instance_and_disabling_checked_validation_statically() {
        // Given
        JsonSchemaValidator.settings = settings().with().jsonSchemaFactory(
                        JsonSchemaFactory.newBuilder().setValidationConfiguration(ValidationConfiguration.newBuilder().setDefaultVersion(DRAFTV3).freeze()).freeze()).
                and().with().checkedValidation(false);

        // When
        try {
            get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json"));
        } finally {
            JsonSchemaValidator.reset();
        }
    }

    @Test
    public void
    json_schema_validator_supports_using_a_supplied_json_schema_factory_instance() {
        // Given
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.newBuilder().setValidationConfiguration(ValidationConfiguration.newBuilder().setDefaultVersion(DRAFTV4).freeze()).freeze();

        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json").using(jsonSchemaFactory));
    }

    @Test
    public void
    json_schema_validator_supports_using_the_supplied_json_schema_validator_settings() {
        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json").using(settings().with().checkedValidation(false)));
    }

    @Test
    public void
    json_schema_validator_supports_draft_03() {
        // when
        get("/jsonStore").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("store-schema.json"));
    }

    @Test
    void json_schema_validator_supports_draft_03_failures() {
        assertThatThrownBy(() ->
                get("/jsonStore").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("store-schema-isbn-required.json"))
        )
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Response body doesn't match expectation.")
                .hasMessageContaining("object has missing required properties ([\"isbn\"])\n")
                .hasMessageContaining("store-schema-isbn-required.json#\",\"pointer\":\"/properties/store/properties/book/items/1\"")
                .hasMessageContaining("required: [\"isbn\"]")
                .hasMessageContaining("missing: [\"isbn\"]")
                .hasMessageContaining("Actual: { \"store\": {");
    }

    @Test
    public void
    greet_json_resource_conforms_to_the_greeting_schema() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
                when().
                get("/greetJSON").
                then().
                body(JsonSchemaValidator.matchesJsonSchemaInClasspath("greeting-schema.json"));
    }

    @Test
    void json_schema_validator_supports_matching_uri_json_schema_as_json_node() throws Exception {
        // Given
        String schema = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json"), StandardCharsets.UTF_8);
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse.Builder().code(200).body(schema).build());
            server.start();
            get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(new URI("http://localhost:" + server.getPort())).using(settings().parseUriAndUrlsAsJsonNode(true)));
        }
    }

    @Test
    void json_schema_validator_supports_matching_uri_json_schema_as_string_to_uri() throws Exception {
        // Given
        String schema = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json"), StandardCharsets.UTF_8);
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse.Builder().code(200).body(schema).build());
            server.start();
            get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(new URI("http://localhost:" + server.getPort())).using(settings().parseUriAndUrlsAsJsonNode(false)));
        }
    }
}

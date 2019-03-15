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
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static com.github.fge.jsonschema.SchemaVersion.DRAFTV3;
import static com.github.fge.jsonschema.SchemaVersion.DRAFTV4;
import static io.restassured.RestAssured.*;
import static io.restassured.module.jsv.JsonSchemaValidatorSettings.settings;
import static org.hamcrest.Matchers.*;

public class JsonSchemaValidationITest extends WithJetty {

    @Test public void
    matches_string_schema_correctly() throws IOException {
        // Given
        String schema = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json"));

        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));
    }

    @Test public void
    matches_input_stream_schema_correctly() throws IOException {
        // Given
        InputStream schema = Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json");

        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));
    }

    @Test public void
    matches_classpath_schema_correctly() throws IOException {
        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json"));
    }

    @Test public void
    works_with_expectation_api() throws IOException {
        // When
        expect().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json")).when().get("/products");
    }

    @Test public void
    can_mix_json_schema_validation_and_body_validations() throws IOException {
        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json")).and().body("[0].name", equalTo("An ice sculpture"));
    }

    @Test public void
    throws_assertion_error_when_schema_doesnt_match_json_content() throws IOException {
        exception.expect(AssertionError.class);
        exception.expectMessage("The content to match the given JSON schema.\n" +
                "error: instance type (object) does not match any allowed primitive type (allowed: [\"array\"])\n" +
                "    level: \"error\"\n" +
                "    schema: {\"loadingURI\":\"#\",\"pointer\":\"\"}\n" +
                "    instance: {\"pointer\":\"\"}\n" +
                "    domain: \"validation\"\n" +
                "    keyword: \"type\"\n" +
                "    found: \"object\"\n" +
                "    expected: [\"array\"]\n" +
                "\n" +
                "  Actual: { \"store\": {\n" +
                "    \"book\": [ \n" +
                "      { \"category\": \"reference\",\n" +
                "        \"author\": \"Nigel Rees\",\n" +
                "        \"title\": \"Sayings of the Century\",\n" +
                "        \"price\": 8.95\n" +
                "      },\n" +
                "      { \"category\": \"fiction\",\n" +
                "        \"author\": \"Evelyn Waugh\",\n" +
                "        \"title\": \"Sword of Honour\",\n" +
                "        \"price\": 12.99\n" +
                "      },\n" +
                "      { \"category\": \"fiction\",\n" +
                "        \"author\": \"Herman Melville\",\n" +
                "        \"title\": \"Moby Dick\",\n" +
                "        \"isbn\": \"0-553-21311-3\",\n" +
                "        \"price\": 8.99\n" +
                "      },\n" +
                "      { \"category\": \"fiction\",\n" +
                "        \"author\": \"J. R. R. Tolkien\",\n" +
                "        \"title\": \"The Lord of the Rings\",\n" +
                "        \"isbn\": \"0-395-19395-8\",\n" +
                "        \"price\": 22.99\n" +
                "      }\n" +
                "    ],\n" +
                "    \"bicycle\": {\n" +
                "      \"color\": \"red\",\n" +
                "      \"price\": 19.95    }\n" +
                "  }\n" +
                "}");

        // When
        get("/jsonStore").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json").using(settings().parseUriAndUrlsAsJsonNode(true)));
    }

    @Test public void
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

    @Test public void
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

    @Test public void
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

    @Test public void
    json_schema_validator_supports_using_a_supplied_json_schema_factory_instance() {
        // Given
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.newBuilder().setValidationConfiguration(ValidationConfiguration.newBuilder().setDefaultVersion(DRAFTV4).freeze()).freeze();

        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json").using(jsonSchemaFactory));
    }

    @Test public void
    json_schema_validator_supports_using_the_supplied_json_schema_validator_settings() {
        // When
        get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("products-schema.json").using(settings().with().checkedValidation(false)));
    }

    @Test public void
    json_schema_validator_supports_draft_03() {
        // when
        get("/jsonStore").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("store-schema.json"));
    }

    @Test public void
    json_schema_validator_supports_draft_03_failures() {
        exception.expect(AssertionError.class);
        exception.expectMessage(allOf(containsString("Response body doesn't match expectation.\n" +
                        "Expected: The content to match the given JSON schema.\n" +
                        "error: object has missing required properties ([\"isbn\"])\n" +
                        "    level: \"error\"\n" +
                        "    schema: {\"loadingURI\":\"file:"),
                containsString("store-schema-isbn-required.json#\",\"pointer\":\"/properties/store/properties/book/items/1\"}\n" +
                        "    instance: {\"pointer\":\"/store/book/1\"}\n" +
                        "    domain: \"validation\"\n" +
                        "    keyword: \"properties\"\n" +
                        "    required: [\"isbn\"]\n" +
                        "    missing: [\"isbn\"]\n" +
                        "\n" +
                        "  Actual: { \"store\": {\n" +
                        "    \"book\": [ \n" +
                        "      { \"category\": \"reference\",\n" +
                        "        \"author\": \"Nigel Rees\",\n" +
                        "        \"title\": \"Sayings of the Century\",\n" +
                        "        \"price\": 8.95\n" +
                        "      },\n" +
                        "      { \"category\": \"fiction\",\n" +
                        "        \"author\": \"Evelyn Waugh\",\n" +
                        "        \"title\": \"Sword of Honour\",\n" +
                        "        \"price\": 12.99\n" +
                        "      },\n" +
                        "      { \"category\": \"fiction\",\n" +
                        "        \"author\": \"Herman Melville\",\n" +
                        "        \"title\": \"Moby Dick\",\n" +
                        "        \"isbn\": \"0-553-21311-3\",\n" +
                        "        \"price\": 8.99\n" +
                        "      },\n" +
                        "      { \"category\": \"fiction\",\n" +
                        "        \"author\": \"J. R. R. Tolkien\",\n" +
                        "        \"title\": \"The Lord of the Rings\",\n" +
                        "        \"isbn\": \"0-395-19395-8\",\n" +
                        "        \"price\": 22.99\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"bicycle\": {\n" +
                        "      \"color\": \"red\",\n" +
                        "      \"price\": 19.95    }\n" +
                        "  }\n" +
                        "}")));

        // when
        get("/jsonStore").then().assertThat().body(JsonSchemaValidator.matchesJsonSchemaInClasspath("store-schema-isbn-required.json"));
    }

    @Test public void
    greet_json_resource_conforms_to_the_greeting_schema() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greetJSON").
        then().
                body(JsonSchemaValidator.matchesJsonSchemaInClasspath("greeting-schema.json"));
    }

    @Test public void
    json_schema_validator_supports_matching_uri_json_schema_as_json_node() throws Exception {
        // Given
        String schema = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json"));
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(schema));
        server.play();
        try {
            get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(new URI("http://localhost:"+server.getPort())).using(settings().parseUriAndUrlsAsJsonNode(true)));
        } finally {
            server.shutdown();
        }
    }

    @Test public void
    json_schema_validator_supports_matching_uri_json_schema_as_string_to_uri() throws Exception {
        // Given
        String schema = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("products-schema.json"));
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(schema));
        server.play();
        try {
            get("/products").then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(new URI("http://localhost:"+server.getPort())).using(settings().parseUriAndUrlsAsJsonNode(false)));
        } finally {
            server.shutdown();
        }
    }
}

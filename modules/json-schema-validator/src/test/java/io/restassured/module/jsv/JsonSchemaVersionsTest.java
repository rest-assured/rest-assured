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
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static io.restassured.module.jsv.JsonSchemaValidatorSettings.settings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonSchemaVersionsTest {

    private static final String SCHEMA_WITH_CONST_WITHOUT_DOLLAR_SCHEMA = "{ \"type\": \"object\", \"properties\": { \"kind\": { \"const\": \"product\" } } }";

    private static final String DRAFT_07_CONST_SCHEMA = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"properties\": { \"kind\": { \"const\": \"product\" } } }";

    @AfterEach
    void reset() {
        JsonSchemaValidator.reset();
    }

    // Draft-07

    @Test
    void draft_07_schema_with_const_and_if_then_else_matches_valid_documents() {
        assertThat("{ \"kind\": \"product\", \"currency\": \"EUR\", \"country\": \"SE\" }", matchesJsonSchemaInClasspath("product-schema-draft-07.json"));
        assertThat("{ \"kind\": \"product\", \"currency\": \"USD\" }", matchesJsonSchemaInClasspath("product-schema-draft-07.json"));
    }

    @Test
    void draft_07_schema_fails_when_const_is_violated() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                assertThat("{ \"kind\": \"service\", \"currency\": \"USD\" }", matchesJsonSchemaInClasspath("product-schema-draft-07.json")));

        assertThat(error.getMessage(), allOf(containsString("The content to match the given JSON schema."), containsString("kind"), containsString("product")));
    }

    @Test
    void draft_07_schema_fails_when_then_branch_is_violated() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                assertThat("{ \"kind\": \"product\", \"currency\": \"EUR\" }", matchesJsonSchemaInClasspath("product-schema-draft-07.json")));

        assertThat(error.getMessage(), containsString("country"));
    }

    @Test
    void draft_07_schema_fails_when_else_branch_is_violated() {
        assertThat("{ \"kind\": \"product\", \"currency\": \"USD\", \"country\": \"US\" }", not(matchesJsonSchemaInClasspath("product-schema-draft-07.json")));
    }

    @Test
    void draft_07_schema_resolves_relative_refs_from_classpath() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                assertThat("{ \"kind\": \"product\", \"currency\": \"EUR\", \"country\": \"Sweden\" }", matchesJsonSchemaInClasspath("product-schema-draft-07.json")));

        assertThat(error.getMessage(), containsString("^[A-Z]{2}$"));
    }

    @Test
    void draft_07_schema_resolves_relative_refs_inside_a_jar(@TempDir Path tempDir) throws Exception {
        Path jar = tempDir.resolve("schemas.jar");
        try (OutputStream out = Files.newOutputStream(jar); JarOutputStream jarOut = new JarOutputStream(out)) {
            for (String name : new String[]{"product-schema-draft-07.json", "definitions-draft-07.json"}) {
                jarOut.putNextEntry(new JarEntry("schemas/" + name));
                try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
                    in.transferTo(jarOut);
                }
                jarOut.closeEntry();
            }
        }
        URL schemaInJar = new URL("jar:" + jar.toUri() + "!/schemas/product-schema-draft-07.json");

        assertThat("{ \"kind\": \"product\", \"currency\": \"EUR\", \"country\": \"SE\" }", matchesJsonSchema(schemaInJar));
        assertThat("{ \"kind\": \"product\", \"currency\": \"EUR\", \"country\": \"Sweden\" }", not(matchesJsonSchema(schemaInJar)));
    }

    // Draft-06

    @Test
    void draft_06_schema_given_as_string_is_validated() {
        String schema = "{ \"$schema\": \"http://json-schema.org/draft-06/schema#\", \"type\": \"object\", \"propertyNames\": { \"maxLength\": 3 } }";

        assertThat("{ \"abc\": 1 }", matchesJsonSchema(schema));
        assertThat("{ \"abcd\": 1 }", not(matchesJsonSchema(schema)));
    }

    // 2020-12

    @Test
    void draft_2020_12_schema_matches_valid_document() {
        assertThat("{ \"id\": \"o-1\", \"line\": [\"apple\", 2] }", matchesJsonSchemaInClasspath("order-schema-2020-12.json"));
    }

    @Test
    void draft_2020_12_schema_lists_all_validation_errors_in_description() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                assertThat("{ \"id\": \"o\", \"line\": [\"apple\", 0, \"extra\"], \"discount\": 10 }", matchesJsonSchemaInClasspath("order-schema-2020-12.json")));

        assertThat(error.getMessage(), allOf(containsString("/id: must be at least 3 characters long"), containsString("/line/1: must have a minimum value of 1"), containsString("coupon"), containsString("/line: index '2'")));
    }

    // Draft-04 / draft-03 behavior is unchanged

    @Test
    void draft_04_schema_is_still_validated_by_java_json_tools() {
        String schema = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\", \"required\": [\"name\"] }";

        AssertionError error = assertThrows(AssertionError.class, () -> assertThat("{ }", matchesJsonSchema(schema)));

        // Message format of the java-json-tools validator
        assertThat(error.getMessage(), containsString("object has missing required properties ([\"name\"])"));
    }

    @Test
    void draft_03_schema_is_still_validated_by_java_json_tools() {
        assertThat("{ \"greeting\": { \"firstName\": \"John\", \"lastName\": \"Doe\" } }", matchesJsonSchemaInClasspath("greeting-schema.json"));
        assertThat("{ \"greeting\": { \"firstName\": \"John\" } }", not(matchesJsonSchemaInClasspath("greeting-schema.json")));
    }

    @Test
    void schema_without_dollar_schema_is_validated_by_java_json_tools_as_draft_04_by_default() {
        // draft-04 doesn't know "const" so it's ignored, exactly as before
        assertThat("{ \"kind\": \"service\" }", matchesJsonSchema(SCHEMA_WITH_CONST_WITHOUT_DOLLAR_SCHEMA));
    }

    // Explicit override

    @Test
    void explicit_schema_version_is_used_for_schema_without_dollar_schema() {
        Matcher<?> matcher = matchesJsonSchema(SCHEMA_WITH_CONST_WITHOUT_DOLLAR_SCHEMA).using(JsonSchemaVersion.DRAFT_7);

        assertThat("{ \"kind\": \"product\" }", (Matcher<? super String>) matcher);
        assertThat("{ \"kind\": \"service\" }", not((Matcher<? super String>) matcher));
    }

    @Test
    void explicit_schema_version_can_be_configured_in_settings() {
        Matcher<?> matcher = matchesJsonSchema(SCHEMA_WITH_CONST_WITHOUT_DOLLAR_SCHEMA).using(settings().with().schemaVersion(JsonSchemaVersion.DRAFT_2020_12));

        assertThat("{ \"kind\": \"service\" }", not((Matcher<? super String>) matcher));
    }

    @Test
    void explicit_schema_version_can_be_configured_statically() {
        JsonSchemaValidator.settings = settings().with().schemaVersion(JsonSchemaVersion.DRAFT_2019_09);

        assertThat("{ \"kind\": \"service\" }", not(matchesJsonSchema(SCHEMA_WITH_CONST_WITHOUT_DOLLAR_SCHEMA)));
    }

    @Test
    void explicit_draft_04_uses_java_json_tools_even_if_schema_declares_draft_07() {
        Matcher<?> matcher = matchesJsonSchemaInClasspath("product-schema-draft-07.json").using(JsonSchemaVersion.DRAFT_4);

        // "const" is unknown to draft-04 and therefore ignored
        assertThat("{ \"kind\": \"service\", \"currency\": \"USD\" }", (Matcher<? super String>) matcher);
    }

    // Remote schemas are downloaded once

    @Test
    void remote_draft_04_schema_is_downloaded_only_once() throws Exception {
        String schema = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\", \"required\": [\"name\"] }";
        withSchemaServer(schema, (url, requests) -> {
            assertThat("{ \"name\": \"x\" }", matchesJsonSchema(url));
            assertThat(requests.get(), is(1));
        });
    }

    @Test
    void remote_draft_07_schema_is_detected_when_parsed_as_json_node_and_downloaded_only_once() throws Exception {
        withSchemaServer(DRAFT_07_CONST_SCHEMA, (url, requests) -> {
            Matcher<?> matcher = matchesJsonSchema(url).using(settings().parseUriAndUrlsAsJsonNode(true));
            assertThat("{ \"kind\": \"service\" }", not((Matcher<? super String>) matcher));
            assertThat(requests.get(), is(1));
        });
    }

    @Test
    void remote_schema_with_explicit_version_is_downloaded_only_once() throws Exception {
        withSchemaServer(DRAFT_07_CONST_SCHEMA, (url, requests) -> {
            Matcher<?> matcher = matchesJsonSchema(url).using(JsonSchemaVersion.DRAFT_7);
            assertThat("{ \"kind\": \"service\" }", not((Matcher<? super String>) matcher));
            assertThat(requests.get(), is(1));
        });
    }

    @Test
    void parses_meta_schema_uris_leniently() {
        assertThat(JsonSchemaVersion.fromMetaSchemaUri("http://json-schema.org/draft-07/schema#"), is(JsonSchemaVersion.DRAFT_7));
        assertThat(JsonSchemaVersion.fromMetaSchemaUri("https://json-schema.org/draft-07/schema"), is(JsonSchemaVersion.DRAFT_7));
        assertThat(JsonSchemaVersion.fromMetaSchemaUri("http://json-schema.org/draft/2020-12/schema"), is(JsonSchemaVersion.DRAFT_2020_12));
        assertThat(JsonSchemaVersion.fromMetaSchemaUri("http://json-schema.org/draft-03/schema#"), nullValue());
        assertThat(JsonSchemaVersion.fromMetaSchemaUri("http://example.com/my-schema"), nullValue());
    }

    private interface SchemaServerCallback {
        void run(URL url, AtomicInteger requests) throws Exception;
    }

    private static void withSchemaServer(String schema, SchemaServerCallback callback) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/schema.json", exchange -> {
            requests.incrementAndGet();
            byte[] body = schema.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            callback.run(new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/schema.json"), requests);
        } finally {
            server.stop(0);
        }
    }
}

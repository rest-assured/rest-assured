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

package io.restassured.internal.http;

import groovy.json.JsonBuilder;
import org.apache.http.HttpEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import static io.restassured.internal.http.EncoderCharacterization.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Characterizes how {@link EncoderRegistry} turns every kind of request body into an {@link HttpEntity} for every
 * kind of request content-type.
 */
class EncoderRegistryTest {

    private static final List<String> CONTENT_TYPES = Arrays.asList(
            "application/json",
            "application/json; charset=ISO-8859-1",
            "application/vnd.api+json",
            "application/xml",
            "text/xml",
            "text/html",
            "text/plain",
            "text/plain; charset=ISO-8859-1",
            "text/csv",
            "application/x-www-form-urlencoded",
            "application/x-www-form-urlencoded; charset=ISO-8859-1",
            "application/octet-stream",
            "*/*",
            "application/unknown");

    @TempDir
    Path tmpDir;

    private final EncoderRegistry registry = new EncoderRegistry();

    private static HttpEntity encode(EncoderRegistry registry, Object contentType, Object body) throws IOException {
        return registry.getAt(contentType).encode(contentType, body);
    }

    private Map<String, Supplier<Object>> bodies() throws IOException {
        Path file = tmpDir.resolve("body.txt");
        Files.write(file, TEXT.getBytes(StandardCharsets.UTF_8));

        Map<String, Supplier<Object>> bodies = new LinkedHashMap<>();
        bodies.put("String", () -> TEXT);
        bodies.put("GString", EncoderCharacterization::gString);
        bodies.put("StringBuilder", () -> new StringBuilder(TEXT));
        bodies.put("byte[]", () -> TEXT.getBytes(StandardCharsets.UTF_8));
        bodies.put("ByteArrayInputStream", () -> new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8)));
        bodies.put("BufferedInputStream", () -> new BufferedInputStream(new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8))));
        bodies.put("ByteArrayOutputStream", () -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.writeBytes(TEXT.getBytes(StandardCharsets.UTF_8));
            return out;
        });
        bodies.put("File", file::toFile);
        bodies.put("MissingFile", () -> tmpDir.resolve("missing.txt").toFile());
        bodies.put("StringReader", () -> new StringReader(TEXT));
        bodies.put("BufferedReader", () -> new BufferedReader(new StringReader(TEXT)));
        bodies.put("Map", () -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("a", 1);
            map.put("b", Arrays.asList("x", null, "å"));
            map.put("c", null);
            map.put("d", TEXT);
            map.put("e", Collections.singletonMap("nested", true));
            return map;
        });
        bodies.put("EmptyMap", LinkedHashMap::new);
        bodies.put("List", () -> Arrays.asList(1, "two", null, Collections.singletonMap("k", "v")));
        bodies.put("Set", () -> new LinkedHashSet<>(Arrays.asList("one", "two")));
        bodies.put("Closure", EncoderCharacterization::closure);
        bodies.put("JsonBuilder (Writable)", () -> new JsonBuilder(Collections.singletonMap("writable", TEXT)));
        bodies.put("Pojo", () -> new Pojo(TEXT));
        bodies.put("Integer", () -> 42);
        bodies.put("Object[]", () -> new Object[]{"a", 1});
        bodies.put("null", () -> null);
        return bodies;
    }

    @Test
    void encodes_every_body_type_for_every_content_type_like_before() throws Exception {
        Map<String, Supplier<Object>> bodies = bodies();
        StringBuilder out = new StringBuilder();
        for (String contentType : CONTENT_TYPES) {
            out.append("== ").append(contentType).append('\n');
            for (Map.Entry<String, Supplier<Object>> body : bodies.entrySet()) {
                out.append(body.getKey()).append(" -> ").append(encodeAndRender(contentType, body.getValue().get())).append('\n');
            }
        }
        assertMatchesGoldenFile("encoder-registry.txt", out.toString());
    }

    @Test
    void uses_text_encoder_for_unregistered_text_content_types_and_binary_encoder_for_other_unregistered_content_types() {
        assertThat(encodeAndRender("text/csv", "x")).isEqualTo("StringEntity | text/csv | 1 | x");
        assertThat(encodeAndRender("application/unknown", "x")).startsWith("EXCEPTION java.lang.IllegalArgumentException: Don't know how to encode x as a byte stream.");
    }

    @Test
    void url_encoded_string_body_is_sent_as_is() {
        assertThat(encodeAndRender("application/x-www-form-urlencoded", "a=1&b=%C3%A5"))
                .isEqualTo("StringEntity | application/x-www-form-urlencoded | 12 | a=1&b=%C3%A5");
    }

    @Test
    void url_encoded_gstring_body_is_coerced_to_string() {
        assertThat(encodeAndRender("application/x-www-form-urlencoded", gString()))
                .isEqualTo("StringEntity | application/x-www-form-urlencoded | 9 | hello \\xe5\\xe4\\xf6");
    }

    @Test
    void url_encoded_body_of_another_type_throws_illegal_argument_exception() {
        Throwable t = catchThrowable(() -> encode(registry, "application/x-www-form-urlencoded", new byte[]{1}));

        assertThat(t).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Don't know how to encode a request body of type byte[] as content-type application/x-www-form-urlencoded. " +
                        "A form url-encoded request body must be a String, use formParam(..) or formParams(..) to send form parameters.");
    }

    @Test
    void writable_body_is_sent_as_its_string_representation() {
        JsonBuilder writable = new JsonBuilder(Collections.singletonMap("a", Arrays.asList(1, 2)));

        assertThat(encodeAndRender("text/plain", writable)).isEqualTo("StringEntity | text/plain | 11 | {\"a\":[1,2]}");
        assertThat(encodeAndRender("text/plain", gString())).isEqualTo(encodeAndRender("text/plain", TEXT));
    }

    @Test
    void map_and_collection_json_bodies_are_rendered_like_groovy_json_builder() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", Arrays.asList("x", null, "å"));
        map.put("date", new Date(0));

        assertThat(encodeAndRender("application/json", map))
                .isEqualTo("StringEntity | application/json | 65 | " + escape(new JsonBuilder(map).toString().getBytes(StandardCharsets.UTF_8)))
                .endsWith("\"date\":\"1970-01-01T00:00:00+0000\"}");
        assertThat(encodeAndRender("application/json", Arrays.asList(1, "two", null))).isEqualTo("StringEntity | application/json | 14 | [1,\"two\",null]");
    }

    @Test
    void closure_body_throws_illegal_argument_exception_for_every_content_type() {
        for (String contentType : CONTENT_TYPES) {
            Throwable t = catchThrowable(() -> encode(registry, contentType, closure()));

            assertThat(t).as(contentType).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageMatching("A Groovy closure \\(Script\\d+\\$_run_closure\\d+\\) is not supported as request body\\. " +
                            "Serialize the body to a String, byte\\[] or InputStream instead, for example in your ObjectMapper\\.");
        }
    }

    private String encodeAndRender(String contentType, Object body) {
        try {
            HttpEntity entity = encode(registry, contentType, body);
            String content = escape(readFully(entity.getContent()));
            String sanitizedContent = sanitize(content, tmpDir);
            // The length of content that contains an identity hash code varies between runs
            String length = content.equals(sanitizedContent) ? String.valueOf(entity.getContentLength()) : "<varies>";
            return entity.getClass().getSimpleName() +
                    " | " + (entity.getContentType() == null ? null : entity.getContentType().getValue()) +
                    " | " + length +
                    " | " + sanitizedContent;
        } catch (Throwable t) {
            return render(t, tmpDir);
        }
    }

    static class Pojo {
        private final String value;

        Pojo(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Pojo[" + value + "]";
        }
    }
}

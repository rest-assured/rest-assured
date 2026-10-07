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

package io.restassured.config;

import io.restassured.builder.ResponseBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class DecoderConfigDefaultCharsetTest {

    private final DecoderConfig latin1ByDefault = new DecoderConfig("ISO-8859-1");

    @Test
    public void json_content_type_with_parameters_defaults_to_utf_8() {
        assertThat(latin1ByDefault.defaultCharsetForContentType("application/json; version=1")).isEqualTo("UTF-8");
        assertThat(latin1ByDefault.hasDefaultCharsetForContentType("application/json; version=1")).isTrue();
    }

    @Test
    public void structured_syntax_json_content_types_default_to_utf_8() {
        assertThat(latin1ByDefault.defaultCharsetForContentType("application/problem+json")).isEqualTo("UTF-8");
        assertThat(latin1ByDefault.defaultCharsetForContentType("application/hal+json")).isEqualTo("UTF-8");
        assertThat(latin1ByDefault.defaultCharsetForContentType("application/vnd.api+json; ext=bulk")).isEqualTo("UTF-8");
    }

    @Test
    public void other_content_types_still_use_the_default_content_charset() {
        assertThat(latin1ByDefault.defaultCharsetForContentType("text/plain")).isEqualTo("ISO-8859-1");
        assertThat(latin1ByDefault.defaultCharsetForContentType("application/jsonx")).isEqualTo("ISO-8859-1");
        assertThat(latin1ByDefault.hasDefaultCharsetForContentType("text/plain")).isFalse();
    }

    @Test
    public void explicitly_configured_content_type_charset_wins_over_the_json_fallback() {
        DecoderConfig config = latin1ByDefault.defaultCharsetForContentType("UTF-16", ContentType.JSON);

        assertThat(config.defaultCharsetForContentType("application/json; version=1")).isEqualTo("UTF-16");
    }

    @Test
    public void problem_json_response_without_charset_is_decoded_as_utf_8() {
        Response response = given()
                .config(RestAssuredConfig.config().decoderConfig(latin1ByDefault))
                .filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder()
                        .setStatusCode(400)
                        .setStatusLine("HTTP/1.1 400 Bad Request")
                        .setContentType("application/problem+json")
                        .setBody("{\"title\":\"België\"}".getBytes(StandardCharsets.UTF_8))
                        .build())
                .get("http://localhost:8080/problem");

        assertThat(response.asString()).isEqualTo("{\"title\":\"België\"}");
        assertThat(response.<String>path("title")).isEqualTo("België");
    }
}

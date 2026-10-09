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
import io.restassured.config.DecoderConfig.ContentDecoder;
import io.restassured.http.ContentType;
import io.restassured.internal.RestAssuredResponseImpl;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.restassured.config.DecoderConfig.decoderConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class DecoderConfigTest {

    private final DecoderConfig latin1ByDefault = decoderConfig().defaultContentCharset("ISO-8859-1");

    @Test
    public void default_charset_for_content_type_given_as_string_does_not_change_the_default_content_charset() {
        DecoderConfig config = latin1ByDefault.defaultCharsetForContentType("UTF-16", "text/plain");

        assertThat(config.defaultContentCharset()).isEqualTo("ISO-8859-1");
        assertThat(config.defaultCharsetForContentType("text/plain")).isEqualTo("UTF-16");
        assertThat(config.defaultCharsetForContentType("application/xml")).isEqualTo("ISO-8859-1");
    }

    @Test
    public void default_charset_for_content_type_given_as_content_type_does_not_change_the_default_content_charset() {
        DecoderConfig config = latin1ByDefault.defaultCharsetForContentType("UTF-16", ContentType.TEXT);

        assertThat(config.defaultContentCharset()).isEqualTo("ISO-8859-1");
        assertThat(config.defaultCharsetForContentType(ContentType.TEXT)).isEqualTo("UTF-16");
        assertThat(config.defaultCharsetForContentType("text/plain")).isEqualTo("UTF-16");
        assertThat(config.defaultCharsetForContentType("application/xml")).isEqualTo("ISO-8859-1");
    }

    @Test
    public void default_charset_for_content_type_given_as_charset_does_not_change_the_default_content_charset() {
        DecoderConfig byString = latin1ByDefault.defaultCharsetForContentType(StandardCharsets.UTF_16, "text/plain");
        DecoderConfig byContentType = latin1ByDefault.defaultCharsetForContentType(StandardCharsets.UTF_16, ContentType.TEXT);

        assertThat(byString.defaultContentCharset()).isEqualTo("ISO-8859-1");
        assertThat(byString.defaultCharsetForContentType("text/plain")).isEqualTo("UTF-16");
        assertThat(byContentType.defaultContentCharset()).isEqualTo("ISO-8859-1");
        assertThat(byContentType.defaultCharsetForContentType("text/plain")).isEqualTo("UTF-16");
    }

    @Test
    public void default_charset_for_content_type_keeps_a_configured_default_content_charset_and_the_other_settings() {
        DecoderConfig config = decoderConfig()
                .defaultContentCharset("US-ASCII")
                .useNoWrapForInflateDecoding(true)
                .contentDecoders(ContentDecoder.DEFLATE)
                .defaultCharsetForContentType("UTF-8", ContentType.XML)
                .defaultCharsetForContentType("UTF-16BE", "text/csv");

        assertThat(config.defaultContentCharset()).isEqualTo("US-ASCII");
        assertThat(config.shouldUseNoWrapForInflateDecoding()).isTrue();
        assertThat(config.contentDecoders()).containsExactly(ContentDecoder.DEFLATE);
        assertThat(config.defaultCharsetForContentType("application/xml")).isEqualTo("UTF-8");
        assertThat(config.defaultCharsetForContentType("text/csv")).isEqualTo("UTF-16BE");
        assertThat(config.defaultCharsetForContentType("application/json")).isEqualTo("UTF-8");
        assertThat(config.defaultCharsetForContentType("text/plain")).isEqualTo("US-ASCII");
        assertThat(config.isUserConfigured()).isTrue();
    }

    @Test
    public void response_of_a_content_type_without_its_own_charset_is_decoded_with_the_default_content_charset() {
        RestAssuredResponseImpl response = (RestAssuredResponseImpl) new ResponseBuilder()
                .setStatusCode(200)
                .setContentType("text/plain")
                .setBody("België".getBytes(StandardCharsets.ISO_8859_1))
                .build();
        response.setDecoderConfig(latin1ByDefault.defaultCharsetForContentType("UTF-16", ContentType.JSON));

        assertThat(response.asString()).isEqualTo("België");
    }
}

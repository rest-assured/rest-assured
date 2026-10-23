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

package io.restassured.internal.multipart;

import org.junit.jupiter.api.Test;

import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * {@link MediaType} parses and formats content-types the way HttpClient 4 did when httpmime wrote the part headers.
 */
class MediaTypeTest {

    @Test
    void parse_keeps_the_case_of_the_mime_type_and_normalizes_the_parameter_separators() {
        assertThat(MediaType.parse("Text/Plain").toString()).isEqualTo("Text/Plain");
        assertThat(MediaType.parse("text/plain;charset=utf-8;a=b").toString()).isEqualTo("text/plain; charset=utf-8; a=b");
        assertThat(MediaType.parse("  text / plain ;  a = b  c ").toString()).isEqualTo("text / plain; a=\"b c\"");
    }

    @Test
    void parse_reads_only_the_first_element() {
        assertThat(MediaType.parse("text/plain, text/html").toString()).isEqualTo("text/plain");
        assertThat(MediaType.parse(", text/plain; a=b, c").toString()).isEqualTo("text/plain; a=b");
    }

    @Test
    void parse_unquotes_values_and_format_quotes_them_again_only_when_needed() {
        assertThat(MediaType.parse("a/b; c=\"d\"").toString()).isEqualTo("a/b; c=d");
        assertThat(MediaType.parse("a/b; c=\"d e;f\"").toString()).isEqualTo("a/b; c=\"d e;f\"");
        assertThat(MediaType.parse("a/b; c=\"d\\\"e\"").toString()).isEqualTo("a/b; c=\"d\\\"e\"");
        assertThat(MediaType.parse("a/b; c=\"unterminated").toString()).isEqualTo("a/b; c=unterminated");
        assertThat(MediaType.parse("a/b; c; d=").toString()).isEqualTo("a/b; c; d=");
    }

    @Test
    void parse_reads_the_charset_of_the_first_charset_parameter_whatever_its_case() {
        assertThat(MediaType.parse("text/plain").getCharset()).isNull();
        assertThat(MediaType.parse("text/plain; CHARSET=utf-8; charset=ISO-8859-1").getCharset()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(MediaType.parse("text/plain; charset=; charset=utf-8").getCharset()).isNull();
    }

    @Test
    void parse_rejects_values_without_an_element_and_unknown_charsets() {
        assertThat(catchThrowable(() -> MediaType.parse("  "))).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid content type:   ");
        assertThat(catchThrowable(() -> MediaType.parse("text/plain; charset=nope"))).isInstanceOf(UnsupportedCharsetException.class);
        assertThat(catchThrowable(() -> MediaType.parse("text/plain; charset=\"no pe\""))).isInstanceOf(IllegalCharsetNameException.class);
    }

    @Test
    void with_parameter_replaces_a_parameter_with_exactly_the_same_name_in_place() {
        MediaType mediaType = MediaType.parse("application/xml; charset=US-ASCII; version=2").withParameter("charset", "UTF-16");

        assertThat(mediaType.toString()).isEqualTo("application/xml; charset=UTF-16; version=2");
        assertThat(mediaType.getCharset()).isEqualTo(StandardCharsets.UTF_16);
    }

    @Test
    void with_parameter_adds_a_parameter_whose_name_differs_in_case_and_the_first_charset_parameter_wins() {
        MediaType mediaType = MediaType.parse("text/plain; Charset=US-ASCII").withParameter("charset", "UTF-8");

        assertThat(mediaType.toString()).isEqualTo("text/plain; Charset=US-ASCII; charset=UTF-8");
        assertThat(mediaType.getCharset()).isEqualTo(StandardCharsets.US_ASCII);
    }

    @Test
    void create_lower_cases_the_mime_type_and_rejects_reserved_characters() {
        assertThat(MediaType.create("Multipart/Mixed").withParameter("boundary", "B").toString()).isEqualTo("multipart/mixed; boundary=B");
        assertThat(catchThrowable(() -> MediaType.create("multipart/a;b"))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIME type may not contain reserved characters");
    }
}

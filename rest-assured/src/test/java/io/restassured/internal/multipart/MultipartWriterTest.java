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

import io.restassured.config.MultipartMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The edge cases of {@link MultipartWriter}, which writes what httpmime 4.5 wrote. Bodies are compared as ISO-8859-1
 * strings, one char per byte.
 */
class MultipartWriterTest {
    private static final String CRLF = "\r\n";

    @TempDir
    Path tmpDir;

    // Content-Type

    @Test
    void content_type_has_the_lower_case_trimmed_sub_type_then_the_boundary_then_the_charset() {
        MultipartWriter writer = new MultipartWriter(" MiXeD ", " utf-16 ", MultipartMode.STRICT, "BOUNDARY");

        assertThat(writer.getContentType()).isEqualTo("multipart/mixed; boundary=BOUNDARY; charset=UTF-16");
    }

    @Test
    void content_type_quotes_the_boundary_only_when_it_contains_a_separator() {
        assertThat(writer("abc-_.'+*").getContentType()).isEqualTo("multipart/form-data; boundary=abc-_.'+*");
        assertThat(writer("a b").getContentType()).isEqualTo("multipart/form-data; boundary=\"a b\"");
        assertThat(writer("a=b").getContentType()).isEqualTo("multipart/form-data; boundary=\"a=b\"");
        assertThat(writer("a\"b").getContentType()).isEqualTo("multipart/form-data; boundary=\"a\\\"b\"");
        assertThat(writer("a\\b").getContentType()).isEqualTo("multipart/form-data; boundary=\"a\\\\b\"");
    }

    @Test
    void sub_type_is_required_and_may_not_be_blank_or_contain_reserved_characters() {
        assertThat(catchThrowable(() -> new MultipartWriter(null, null, MultipartMode.STRICT, "b")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Multipart sub type cannot be null");
        assertThat(catchThrowable(() -> new MultipartWriter("  ", null, MultipartMode.STRICT, "b")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("MIME subtype may not be blank");
        assertThat(catchThrowable(() -> new MultipartWriter("a,b", null, MultipartMode.STRICT, "b")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("MIME type may not contain reserved characters");
        assertThat(catchThrowable(() -> new MultipartWriter("a\"b", null, MultipartMode.STRICT, "b")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("MIME type may not contain reserved characters");
    }

    @Test
    void unknown_charset_is_rejected_before_the_sub_type_is_checked() {
        assertThat(catchThrowable(() -> new MultipartWriter("", "nope", MultipartMode.STRICT, "b"))).isInstanceOf(UnsupportedCharsetException.class);
    }

    // The body

    @Test
    void writes_only_the_closing_boundary_without_parts() throws IOException {
        MultipartWriter writer = writer("B");

        assertThat(written(writer)).isEqualTo("--B--" + CRLF);
        assertThat(writer.getContentLength()).isEqualTo(7);
    }

    @Test
    void boundary_in_the_body_is_encoded_in_the_charset() throws IOException {
        MultipartWriter writer = new MultipartWriter("form-data", "UTF-16", MultipartMode.STRICT, "B");

        assertThat(written(writer)).isEqualTo("--þÿ\u0000B--" + CRLF);
    }

    @Test
    void custom_headers_come_first_in_the_given_order_and_replace_generated_ones_whatever_their_case() throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-B", "2");
        headers.put("content-type", "text/csv");
        headers.put("X-A", "1");
        MultipartWriter writer = writer("B");
        writer.addPart("name", text("value"), headers);

        assertThat(written(writer)).isEqualTo(body("B", part("value", "X-B: 2", "content-type: text/csv", "X-A: 1",
                "Content-Disposition: form-data; name=\"name\"")));
    }

    @Test
    void content_transfer_encoding_headers_are_never_written() throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Transfer-Encoding", "binary");
        headers.put("content-transfer-encoding", "8bit");
        MultipartWriter writer = writer("B");
        writer.addPart("name", text("value"), headers);

        assertThat(written(writer)).isEqualTo(body("B", part("value", disposition("name"), "Content-Type: text/plain")));
    }

    @Test
    void quotes_backslashes_and_carriage_returns_in_names_are_escaped() throws IOException {
        MultipartWriter writer = writer("B");
        writer.addPart("a\"b\\c\rd\ne", PartBody.stream(stream("x"), MediaType.parse("text/plain"), "f\"\\\r.txt"), null);

        assertThat(written(writer)).isEqualTo(body("B", part("x",
                "Content-Disposition: form-data; name=\"a\\\"b\\\\c\\\rd\ne\"; filename=\"f\\\"\\\\\\\r.txt\"", "Content-Type: text/plain")));
    }

    @Test
    void null_header_name_and_blank_part_name_are_rejected() {
        Map<String, String> nullName = new LinkedHashMap<>();
        nullName.put(null, "value");

        assertThat(catchThrowable(() -> writer("B").addPart("name", text("x"), nullName)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Field name may not be null");
        assertThat(catchThrowable(() -> writer("B").addPart(" ", text("x"), null)))
                .isInstanceOf(IllegalStateException.class).hasMessage("Name is blank");
    }

    @Test
    void header_names_and_values_that_are_not_strings_are_rendered_like_groovy() throws IOException {
        Map<Object, Object> headers = new LinkedHashMap<>();
        headers.put(new StringBuilder("X-A"), Collections.singletonList(1));
        MultipartWriter writer = writer("B");
        writer.addPart("name", text("x"), uncheckedCast(headers));

        assertThat(written(writer)).contains(CRLF + "X-A: [1]" + CRLF);
    }

    // The modes

    @Test
    void strict_mode_writes_the_headers_as_us_ascii() throws IOException {
        MultipartWriter writer = new MultipartWriter("form-data", "UTF-8", MultipartMode.STRICT, "B");
        writer.addPart("näme😀", PartBody.stream(stream("x"), MediaType.parse("text/plain"), "€.txt"), Collections.singletonMap("X-Ä", "ö"));

        // A surrogate pair is one character, so it becomes one "?"
        assertThat(written(writer)).isEqualTo(body("B", part("x", "X-?: ?",
                "Content-Disposition: form-data; name=\"n?me?\"; filename=\"?.txt\"", "Content-Type: text/plain")));
    }

    @Test
    void rfc6532_mode_writes_the_headers_as_utf_8_and_the_boundary_in_the_charset() throws IOException {
        MultipartWriter writer = new MultipartWriter("form-data", "UTF-16", MultipartMode.RFC6532, "B");
        writer.addPart("näme", text("x"), Collections.singletonMap("X-Ä", "ö"));

        assertThat(written(writer)).isEqualTo("--þÿ\u0000B" + CRLF
                + utf8("X-Ä: ö") + CRLF + utf8(disposition("näme")) + CRLF + "Content-Type: text/plain" + CRLF + CRLF + "x" + CRLF
                + "--þÿ\u0000B--" + CRLF);
    }

    @Test
    void browser_compatible_mode_writes_the_disposition_and_the_content_type_of_parts_with_a_file_name_in_the_charset() throws IOException {
        MultipartWriter writer = new MultipartWriter("form-data", "ISO-8859-1", MultipartMode.BROWSER_COMPATIBLE, "B");
        writer.addPart("näme", PartBody.stream(stream("x"), MediaType.parse("text/plain"), "fïle"), Collections.singletonMap("X-A", "1"));
        writer.addPart("ö", text("y"), Collections.singletonMap("X-A", "1"));

        assertThat(written(writer)).isEqualTo(body("B",
                part("x", "Content-Disposition: form-data; name=\"näme\"; filename=\"fïle\"", "Content-Type: text/plain"),
                part("y", "Content-Disposition: form-data; name=\"ö\"")));
    }

    @Test
    void browser_compatible_mode_without_charset_writes_us_ascii() throws IOException {
        MultipartWriter writer = new MultipartWriter("form-data", null, MultipartMode.BROWSER_COMPATIBLE, "B");
        writer.addPart("näme", text("y"), null);

        assertThat(written(writer)).isEqualTo(body("B", part("y", "Content-Disposition: form-data; name=\"n?me\"")));
    }

    @Test
    void browser_compatible_mode_writes_custom_disposition_and_content_type_headers() throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-type", "text/csv");
        headers.put("content-disposition", "attachment");
        MultipartWriter writer = new MultipartWriter("form-data", null, MultipartMode.BROWSER_COMPATIBLE, "B");
        writer.addPart("name", PartBody.stream(stream("x"), MediaType.parse("text/plain"), "f"), headers);

        assertThat(written(writer)).isEqualTo(body("B", part("x", "content-disposition: attachment", "content-type: text/csv")));
    }

    // Length and repeatability

    @Test
    void length_is_known_for_text_and_file_parts() throws IOException {
        File file = Files.write(tmpDir.resolve("file.txt"), "content".getBytes(UTF_8)).toFile();
        MultipartWriter writer = writer("B");
        writer.addPart("one", text("value"), null);
        writer.addPart("two", PartBody.file(file, MediaType.parse("text/plain"), null), null);

        String written = written(writer);

        assertThat(written).contains("filename=\"file.txt\"");
        assertThat(writer.getContentLength()).isEqualTo(written.length());
        assertThat(writer.isRepeatable()).isTrue();
        assertThat(written(writer)).isEqualTo(written);
    }

    @Test
    void length_is_unknown_with_a_stream_part() {
        MultipartWriter writer = writer("B");
        writer.addPart("one", text("value"), null);
        writer.addPart("two", PartBody.stream(stream("x"), MediaType.parse("text/plain"), null), null);

        assertThat(writer.getContentLength()).isEqualTo(-1);
        assertThat(writer.isRepeatable()).isFalse();
    }

    @Test
    void stream_part_is_closed_when_written() throws IOException {
        List<String> events = new ArrayList<>();
        InputStream stream = new ByteArrayInputStream("x".getBytes(UTF_8)) {
            @Override
            public void close() {
                events.add("closed");
            }
        };
        MultipartWriter writer = writer("B");
        writer.addPart("one", PartBody.stream(stream, MediaType.parse("text/plain"), null), null);

        written(writer);

        assertThat(events).containsExactly("closed");
    }

    @Test
    void writes_each_piece_separately_and_the_content_in_4096_byte_writes_followed_by_a_flush() throws IOException {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            content.append('x');
        }
        MultipartWriter writer = writer("B");
        writer.addPart("n", text(content.toString()), null);
        List<String> writes = new ArrayList<>();
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                writes.add("1");
            }

            @Override
            public void write(byte[] b, int off, int len) {
                writes.add(String.valueOf(len));
            }

            @Override
            public void flush() {
                writes.add("flush");
            }
        };

        writer.writeTo(out);

        // "--", "B", CRLF, two headers of four writes each, CRLF, the content, CRLF, then "--", "B", "--", CRLF
        assertThat(writes).containsExactly("2", "1", "2", "19", "2", "19", "2", "12", "2", "10", "2", "2",
                "4096", "904", "flush", "2", "2", "1", "2", "2");
    }

    private static MultipartWriter writer(String boundary) {
        return new MultipartWriter("form-data", null, MultipartMode.STRICT, boundary);
    }

    private static PartBody text(String text) {
        return PartBody.text(text, MediaType.parse("text/plain"));
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(UTF_8));
    }

    private static String written(MultipartWriter writer) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.writeTo(out);
        return new String(out.toByteArray(), ISO_8859_1);
    }

    private static String body(String boundary, String... parts) {
        StringBuilder body = new StringBuilder();
        for (String part : parts) {
            body.append("--").append(boundary).append(CRLF).append(part).append(CRLF);
        }
        return body.append("--").append(boundary).append("--").append(CRLF).toString();
    }

    private static String part(String content, String... headers) {
        return String.join(CRLF, headers) + CRLF + CRLF + content;
    }

    private static String disposition(String name) {
        return "Content-Disposition: form-data; name=\"" + name + "\"";
    }

    private static String utf8(String text) {
        return new String(text.getBytes(StandardCharsets.UTF_8), ISO_8859_1);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> uncheckedCast(Map<?, ?> map) {
        return (Map<String, String>) map;
    }
}

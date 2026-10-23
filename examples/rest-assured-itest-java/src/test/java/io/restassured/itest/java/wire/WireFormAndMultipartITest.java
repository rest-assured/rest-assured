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

package io.restassured.itest.java.wire;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.config.MultiPartConfig;
import io.restassured.config.MultipartMode;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What REST Assured sends for form parameters and multipart requests, byte for byte. The multipart writer is going to
 * be ported off httpmime and must keep producing exactly these bytes.
 * <p>
 * Bodies are compared as ISO-8859-1 strings, which map each byte to one char. {@link #utf8(String)} spells out the
 * bytes of a UTF-8 encoded text in that form.
 */
public class WireFormAndMultipartITest extends WireTest {
    private static final String BOUNDARY = "wire-boundary";
    private static final String CRLF = "\r\n";
    private static final String FORM = "application/x-www-form-urlencoded";

    @TempDir
    Path tempDir;

    // Form parameters

    @Test public void
    form_param_values_and_names_are_url_encoded_with_percent_20_for_space() {
        given().formParam("a b", "c d+e&f=g/h?i#j%k~l*m").post(server.url("/form"));

        // Today's behavior: a space is sent as %20, not as "+" like browsers send it
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("a%20b=c%20d%2Be%26f%3Dg%2Fh%3Fi%23j%25k%7El*m");
    }

    @Test public void
    form_params_are_sent_as_url_encoded_iso_8859_1_with_content_length_by_default() {
        given().formParam("a", "1").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=ISO-8859-1");
        assertThat(request.header("Content-Length")).isEqualTo("3");
        assertThat(request.hasHeader("Transfer-Encoding")).isFalse();
    }

    @Test public void
    non_ascii_form_params_are_percent_encoded_in_iso_8859_1_by_default() {
        given().formParam("ä", "åäö €").post(server.url("/form"));

        // Today's behavior: a character that ISO-8859-1 can't encode is sent as "?" (%3F)
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("%E4=%E5%E4%F6%20%3F");
    }

    @Test public void
    empty_form_value_is_sent_with_equals_sign() {
        given().formParam("a", "").formParam("b", "1").post(server.url("/form"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("a=&b=1");
    }

    @Test public void
    form_param_without_value_is_sent_without_equals_sign() {
        given().formParam("a").formParam("b", "1").post(server.url("/form"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("a&b=1");
    }

    @Test public void
    form_param_with_several_values_repeats_the_name() {
        given().formParam("a", "1", "2").formParam("b", List.of("3", "4")).post(server.url("/form"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("a=1&a=2&b=3&b=4");
    }

    @Test public void
    same_form_param_added_twice_keeps_both_values() {
        given().formParam("a", "1").formParam("a", "2").post(server.url("/form"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("a=1&a=2");
    }

    @Test public void
    form_params_from_a_map_keep_the_map_order() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("z", "1");
        params.put("a", "2");
        params.put("m", "3");

        given().formParams(params).post(server.url("/form"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("z=1&a=2&m=3");
    }

    @Test public void
    params_on_post_are_sent_in_the_body_before_the_form_params() {
        given().formParam("f", "1").param("p", "2").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("/form");
        assertThat(request.bodyAsString()).isEqualTo("p=2&f=1");
    }

    @Test public void
    form_params_on_put_are_sent_in_the_body() {
        given().formParam("a", "b c").put(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("PUT /form HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=ISO-8859-1");
        assertThat(request.bodyAsString()).isEqualTo("a=b%20c");
    }

    @Test public void
    params_on_put_are_sent_in_the_query() {
        given().formParam("f", "1").param("p", "a b").put(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("/form?p=a%20b");
        assertThat(request.bodyAsString()).isEqualTo("f=1");
    }

    @Test public void
    form_params_on_patch_are_sent_in_the_body() {
        given().formParam("a", "b c").patch(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("PATCH /form HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=ISO-8859-1");
        assertThat(request.bodyAsString()).isEqualTo("a=b%20c");
    }

    @Test public void
    params_on_patch_are_sent_in_the_query() {
        given().formParam("f", "1").param("p", "a b").patch(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("/form?p=a%20b");
        assertThat(request.bodyAsString()).isEqualTo("f=1");
    }

    @Test public void
    default_content_charset_sets_both_the_encoding_and_the_content_type_charset() {
        given().config(config().encoderConfig(encoderConfig().defaultContentCharset(UTF_8))).formParam("a", "ä€").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=UTF-8");
        assertThat(request.bodyAsString()).isEqualTo("a=%C3%A4%E2%82%AC");
    }

    @Test public void
    default_charset_for_the_form_content_type_is_used_when_no_content_type_is_set() {
        given().config(config().encoderConfig(encoderConfig().defaultCharsetForContentType(UTF_8, FORM))).formParam("a", "ä").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=UTF-8");
        assertThat(request.bodyAsString()).isEqualTo("a=%C3%A4");
    }

    @Test public void
    default_charset_for_the_form_content_type_is_used_when_the_content_type_has_no_charset() {
        given().config(config().encoderConfig(encoderConfig().defaultCharsetForContentType(UTF_8, FORM))).contentType(FORM).formParam("a", "ä").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=UTF-8");
        assertThat(request.bodyAsString()).isEqualTo("a=%C3%A4");
    }

    @Test public void
    charset_in_the_content_type_is_used_to_encode_form_params() {
        given().contentType(FORM + "; charset=UTF-8").formParam("a", "ä").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=UTF-8");
        assertThat(request.bodyAsString()).isEqualTo("a=%C3%A4");
    }

    @Test public void
    content_type_enum_gets_the_default_charset_appended() {
        given().contentType(ContentType.URLENC).formParam("a", "ä").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo(FORM + "; charset=ISO-8859-1");
        assertThat(request.bodyAsString()).isEqualTo("a=%E4");
    }

    @Test public void
    no_charset_is_appended_when_appending_the_default_charset_is_disabled() {
        given().config(config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).formParam("a", "ä").post(server.url("/form"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo(FORM);
        assertThat(request.bodyAsString()).isEqualTo("a=%E4");
    }

    @Test public void
    form_params_are_sent_as_is_when_url_encoding_is_disabled() {
        given().urlEncodingEnabled(false).formParam("a b", "c d+e&f=%41ä").post(server.url("/form"));

        assertThat(server.onlyRequest().bodyAsString(ISO_8859_1)).isEqualTo("a b=c d+e&f=%41ä");
    }

    // Multipart: the part types, in the default (strict) mode

    @Test public void
    string_part() {
        givenBoundary().multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    non_ascii_string_part_without_charset_is_sent_as_question_marks() {
        givenBoundary().multiPart("text", "åäö").post(server.url("/multipart"));

        // Today's behavior: a String part without a charset is encoded as US-ASCII
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"text\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "???" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    string_part_with_mime_type() {
        givenBoundary().multiPart("name", "{\"a\":1}", "application/json").post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: application/json" + CRLF +
                CRLF +
                "{\"a\":1}" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    string_part_with_charset_in_the_mime_type() {
        givenBoundary().multiPart("text", "åäö", "text/plain; charset=UTF-8").post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"text\"" + CRLF +
                "Content-Type: text/plain; charset=UTF-8" + CRLF +
                CRLF +
                utf8("åäö") + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    string_part_with_mime_type_and_charset_from_the_spec_builder() {
        givenBoundary().multiPart(new MultiPartSpecBuilder("åäö").controlName("text").mimeType("text/plain").charset(UTF_8).build()).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"text\"" + CRLF +
                "Content-Type: text/plain; charset=UTF-8" + CRLF +
                CRLF +
                utf8("åäö") + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    string_part_with_a_file_name_from_the_spec_builder_is_sent_without_it() {
        givenBoundary().multiPart(new MultiPartSpecBuilder("content").controlName("ctrl").fileName("name.txt").build()).post(server.url("/multipart"));

        // Today's behavior: the file name of a String part is dropped
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"ctrl\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "content" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    object_part_is_serialized_with_the_mime_type() {
        givenBoundary().multiPart("json", Map.of("a", 1), "application/json").post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"json\"" + CRLF +
                "Content-Type: application/json" + CRLF +
                CRLF +
                "{\"a\":1}" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    byte_array_part_with_file_name() {
        givenBoundary().multiPart("data", "data.bin", new byte[]{0, 1, (byte) 0xFF}).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"data\"; filename=\"data.bin\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                new String(new byte[]{0, 1, (byte) 0xFF}, ISO_8859_1) + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    byte_array_part_with_file_name_and_mime_type() {
        givenBoundary().multiPart("data", "data.txt", "abc".getBytes(UTF_8), "text/plain").post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"data\"; filename=\"data.txt\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "abc" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    byte_array_part_without_file_name_is_serialized_as_a_json_string() {
        givenBoundary().multiPart("data", "abc".getBytes(UTF_8)).post(server.url("/multipart"));

        // Today's behavior: multiPart(String, Object) serializes a byte[] (with Jackson here) instead of sending its bytes
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"data\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "\"YWJj\"" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    charset_on_a_byte_array_part_is_rejected() {
        assertThatThrownBy(() -> new MultiPartSpecBuilder("abc".getBytes(UTF_8)).charset(UTF_8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot specify charset for input streams or byte arrays.");
    }

    @Test public void
    file_part() throws IOException {
        File file = writeFile("upload.txt", "file content");

        givenBoundary().multiPart("doc", file).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"doc\"; filename=\"upload.txt\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "file content" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    file_part_with_mime_type() throws IOException {
        File file = writeFile("upload.json", "{}");

        givenBoundary().multiPart("doc", file, "application/json").post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"doc\"; filename=\"upload.json\"" + CRLF +
                "Content-Type: application/json" + CRLF +
                CRLF +
                "{}" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    file_part_without_control_name_gets_the_default_control_name() throws IOException {
        File file = writeFile("upload.txt", "x");

        givenBoundary().multiPart(file).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"file\"; filename=\"upload.txt\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "x" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    file_part_from_the_spec_builder_with_mime_type_and_charset() throws IOException {
        File file = writeFile("upload.txt", "x");

        givenBoundary().multiPart(new MultiPartSpecBuilder(file).controlName("doc").mimeType("text/plain").charset(UTF_8).build()).post(server.url("/multipart"));

        // Today's behavior: the spec builder sends the default file name, not the name of the file
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"doc\"; filename=\"file\"" + CRLF +
                "Content-Type: text/plain; charset=UTF-8" + CRLF +
                CRLF +
                "x" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    input_stream_part() {
        givenBoundary().multiPart("stream", "stream.txt", new ByteArrayInputStream("streamed".getBytes(UTF_8))).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"stream\"; filename=\"stream.txt\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "streamed" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    input_stream_part_with_mime_type() {
        givenBoundary().multiPart("stream", "stream.xml", new ByteArrayInputStream("<a/>".getBytes(UTF_8)), "application/xml").post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"stream\"; filename=\"stream.xml\"" + CRLF +
                "Content-Type: application/xml" + CRLF +
                CRLF +
                "<a/>" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    spec_builder_without_control_name_and_file_name_uses_the_defaults() {
        givenBoundary().multiPart(new MultiPartSpecBuilder("abc".getBytes(UTF_8)).build()).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"file\"; filename=\"file\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "abc" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    spec_builder_with_empty_file_name_sends_no_file_name() {
        givenBoundary().multiPart(new MultiPartSpecBuilder("abc".getBytes(UTF_8)).controlName("data").emptyFileName().build()).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"data\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "abc" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    configured_default_control_name_and_file_name_are_used() {
        givenBoundary(multiPartConfig().defaultControlName("ctrl").defaultFileName("default.bin"))
                .multiPart(new MultiPartSpecBuilder("abc".getBytes(UTF_8)).build()).post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"ctrl\"; filename=\"default.bin\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "abc" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    configured_empty_default_file_name_sends_no_file_name() {
        givenBoundary(multiPartConfig().emptyDefaultFileName()).multiPart(new MultiPartSpecBuilder("abc".getBytes(UTF_8)).controlName("data").build())
                .post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"data\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "abc" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    custom_part_headers_are_written_sorted_by_name_before_the_generated_ones() {
        givenBoundary().multiPart(new MultiPartSpecBuilder("value").controlName("name").header("X-B", "2").header("X-A", "1").header("Content-ID", "<part1>").build())
                .post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-ID: <part1>" + CRLF +
                "X-A: 1" + CRLF +
                "X-B: 2" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    custom_content_type_and_disposition_part_headers_replace_the_generated_ones() {
        givenBoundary().multiPart(new MultiPartSpecBuilder("value").controlName("name")
                        .header("Content-Disposition", "attachment; name=\"other\"").header("Content-Type", "text/csv").build())
                .post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: attachment; name=\"other\"" + CRLF +
                "Content-Type: text/csv" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    several_parts_with_the_same_name_are_all_sent_in_order() {
        givenBoundary().multiPart("name", "one").multiPart("name", "two").multiPart("other", "three").post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "one" + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "two" + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"other\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "three" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    params_and_form_params_become_string_parts_after_the_multiparts() {
        givenBoundary().multiPart("file", "a.txt", "x".getBytes(UTF_8)).formParam("f", "1", "2").param("p", "3").formParam("empty").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("/multipart");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"file\"; filename=\"a.txt\"" + CRLF +
                "Content-Type: application/octet-stream" + CRLF +
                CRLF +
                "x" + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"p\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "3" + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"f\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "1" + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"f\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "2" + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"empty\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    multipart_put_sends_the_same_body_as_post() {
        givenBoundary().multiPart("name", "value").put(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("PUT /multipart HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    multipart_patch_sends_the_same_body_as_post() {
        givenBoundary().multiPart("name", "value").patch(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.requestLine()).isEqualTo("PATCH /multipart HTTP/1.1");
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    // Multipart: the boundary, the subtype and the charset of the request

    @Test public void
    random_boundary_is_30_to_40_url_safe_characters_used_in_the_header_and_the_body() {
        given().multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        String contentType = request.header("Content-Type");
        assertThat(contentType).matches("multipart/form-data; boundary=[a-zA-Z0-9_-]{30,40}");
        String boundary = contentType.substring(contentType.indexOf('=') + 1);
        assertThat(request.bodyAsString()).startsWith("--" + boundary + CRLF).endsWith(CRLF + "--" + boundary + "--" + CRLF);
    }

    @Test public void
    random_boundary_differs_between_requests() {
        given().multiPart("name", "value").post(server.url("/multipart"));
        given().multiPart("name", "value").post(server.url("/multipart"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).header("Content-Type")).isNotEqualTo(requests.get(1).header("Content-Type"));
    }

    @Test public void
    boundary_in_the_content_type_is_used() {
        given().contentType("multipart/form-data; boundary=explicit").multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=explicit");
        assertThat(request.bodyAsString()).isEqualTo(
                "--explicit" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--explicit--" + CRLF);
    }

    @Test public void
    boundary_in_the_content_type_wins_over_the_configured_default_boundary() {
        givenBoundary().contentType("multipart/form-data; boundary=explicit").multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=explicit");
        assertThat(request.bodyAsString()).startsWith("--explicit" + CRLF).endsWith(CRLF + "--explicit--" + CRLF);
    }

    @Test public void
    subtype_from_the_content_type() {
        givenBoundary().contentType("multipart/mixed").multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/mixed; boundary=wire-boundary");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    subtype_from_the_multipart_config() {
        givenBoundary(multiPartConfig().defaultSubtype("related")).multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/related; boundary=wire-boundary");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    multipart_plus_content_type_is_sent_as_given_with_a_quoted_boundary() {
        givenBoundary().contentType("application/vnd.example.multipart+mixed").multiPart("name", "value").post(server.url("/multipart"));

        // Today's behavior: unlike a multipart/* content type, this one is sent as REST Assured built it, with the boundary quoted
        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("application/vnd.example.multipart+mixed; boundary=\"wire-boundary\"");
        assertThat(request.bodyAsString()).startsWith("--wire-boundary" + CRLF).endsWith(CRLF + "--wire-boundary--" + CRLF);
    }

    @Test public void
    charset_in_the_multipart_content_type_is_sent_after_the_boundary() {
        givenBoundary().contentType("multipart/form-data; charset=UTF-8").multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary; charset=UTF-8");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    configured_default_charset_is_sent_in_the_content_type() {
        givenBoundary(multiPartConfig().defaultCharset(UTF_8)).multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary; charset=UTF-8");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"name\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    configured_default_charset_does_not_change_the_encoding_of_string_parts() {
        givenBoundary(multiPartConfig().defaultCharset(UTF_8)).multiPart("text", "åäö").post(server.url("/multipart"));

        // Today's behavior: still US-ASCII, see non_ascii_string_part_without_charset_is_sent_as_question_marks
        assertThat(server.onlyRequest().bodyAsString()).contains(CRLF + CRLF + "???" + CRLF);
    }

    // Multipart: the modes, which write the part headers differently

    @Test public void
    strict_mode_is_the_default() {
        assertThat(config().getHttpClientConfig().multipartMode()).isEqualTo(MultipartMode.STRICT);
    }

    @Test public void
    strict_mode_writes_non_ascii_names_as_question_marks() {
        givenBoundaryAndMode(MultipartMode.STRICT, multiPartConfig()).multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8), "text/plain").multiPart("ö", "value")
                .post(server.url("/multipart"));

        // Today's behavior: part headers are written as US-ASCII
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"n?me\"; filename=\"f?le.txt\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                utf8("ä") + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"?\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    strict_mode_writes_non_ascii_names_as_question_marks_also_with_a_configured_charset() {
        givenBoundaryAndMode(MultipartMode.STRICT, multiPartConfig().defaultCharset(UTF_8)).multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8), "text/plain")
                .post(server.url("/multipart"));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"n?me\"; filename=\"f?le.txt\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                utf8("ä") + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    browser_compatible_mode_writes_only_content_disposition_and_the_content_type_of_file_parts() {
        givenBoundaryAndMode(MultipartMode.BROWSER_COMPATIBLE, multiPartConfig()).multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8), "text/plain").multiPart("ö", "value")
                .post(server.url("/multipart"));

        // Today's behavior: without a charset the names are written as US-ASCII
        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"n?me\"; filename=\"f?le.txt\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                utf8("ä") + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"?\"" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    browser_compatible_mode_writes_the_names_in_the_configured_default_charset() {
        givenBoundaryAndMode(MultipartMode.BROWSER_COMPATIBLE, multiPartConfig().defaultCharset(UTF_8)).multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8), "text/plain")
                .post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary; charset=UTF-8");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"" + utf8("näme") + "\"; filename=\"" + utf8("fïle.txt") + "\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                utf8("ä") + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    browser_compatible_mode_writes_the_names_in_the_charset_of_the_content_type() {
        givenBoundaryAndMode(MultipartMode.BROWSER_COMPATIBLE, multiPartConfig()).contentType("multipart/form-data; charset=ISO-8859-1")
                .multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8), "text/plain").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary; charset=ISO-8859-1");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"näme\"; filename=\"fïle.txt\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                utf8("ä") + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @Test public void
    rfc6532_mode_writes_non_ascii_names_as_utf_8() {
        givenBoundaryAndMode(MultipartMode.RFC6532, multiPartConfig()).multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8), "text/plain").multiPart("ö", "value")
                .post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/form-data; boundary=wire-boundary");
        assertThat(request.bodyAsString()).isEqualTo(
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"" + utf8("näme") + "\"; filename=\"" + utf8("fïle.txt") + "\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                utf8("ä") + CRLF +
                "--wire-boundary" + CRLF +
                "Content-Disposition: form-data; name=\"" + utf8("ö") + "\"" + CRLF +
                "Content-Type: text/plain" + CRLF +
                CRLF +
                "value" + CRLF +
                "--wire-boundary--" + CRLF);
    }

    @ParameterizedTest
    @EnumSource(MultipartMode.class)
    @SuppressWarnings("deprecation")
    public void
    deprecated_http_multipart_mode_sends_the_same_bytes_as_the_multipart_mode(MultipartMode mode) throws IOException {
        File file = writeFile("upload.txt", "file");
        givenBoundaryAndMode(mode, multiPartConfig()).multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8)).multiPart("doc", file).multiPart("text", "value")
                .post(server.url("/multipart"));
        RecordedRequest withMultipartMode = server.onlyRequest();
        server.reset();

        given().config(config().multiPartConfig(multiPartConfig().defaultBoundary(BOUNDARY)).httpClient(httpClientConfig().httpMultipartMode(HttpMultipartMode.valueOf(mode.name()))))
                .multiPart("näme", "fïle.txt", "ä".getBytes(UTF_8)).multiPart("doc", file).multiPart("text", "value").post(server.url("/multipart"));
        RecordedRequest withHttpMultipartMode = server.onlyRequest();

        assertThat(withHttpMultipartMode.header("Content-Type")).isEqualTo(withMultipartMode.header("Content-Type"));
        assertThat(withHttpMultipartMode.bodyAsString()).isEqualTo(withMultipartMode.bodyAsString());
    }

    // Multipart: Content-Length or chunked

    @Test public void
    multipart_with_string_parts_is_sent_with_content_length() {
        givenBoundary().multiPart("name", "value").post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Length")).isEqualTo("116").isEqualTo(String.valueOf(request.body().length));
        assertThat(request.hasHeader("Transfer-Encoding")).isFalse();
    }

    @Test public void
    multipart_with_a_file_part_is_sent_with_content_length() throws IOException {
        givenBoundary().multiPart("doc", writeFile("upload.txt", "file content")).post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Content-Length")).isEqualTo("159").isEqualTo(String.valueOf(request.body().length));
        assertThat(request.hasHeader("Transfer-Encoding")).isFalse();
    }

    @Test public void
    multipart_with_an_input_stream_part_is_sent_chunked() {
        givenBoundary().multiPart("stream", "stream.txt", new ByteArrayInputStream("streamed".getBytes(UTF_8))).post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Transfer-Encoding")).isEqualTo("chunked");
        assertThat(request.hasHeader("Content-Length")).isFalse();
    }

    @Test public void
    multipart_with_a_byte_array_part_is_sent_chunked() {
        givenBoundary().multiPart("data", "data.bin", "abc".getBytes(UTF_8)).post(server.url("/multipart"));

        // Today's behavior: a byte[] part is written from a stream of unknown length, so the whole request is chunked
        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Transfer-Encoding")).isEqualTo("chunked");
        assertThat(request.hasHeader("Content-Length")).isFalse();
    }

    @Test public void
    multipart_with_string_and_input_stream_parts_is_sent_chunked() {
        givenBoundary().multiPart("name", "value").multiPart("stream", "stream.txt", new ByteArrayInputStream("streamed".getBytes(UTF_8))).post(server.url("/multipart"));

        RecordedRequest request = server.onlyRequest();
        assertThat(request.header("Transfer-Encoding")).isEqualTo("chunked");
        assertThat(request.hasHeader("Content-Length")).isFalse();
    }

    private RequestSpecification givenBoundary() {
        return givenBoundary(multiPartConfig());
    }

    private RequestSpecification givenBoundary(MultiPartConfig multiPartConfig) {
        return given().config(config().multiPartConfig(multiPartConfig.defaultBoundary(BOUNDARY)));
    }

    private RequestSpecification givenBoundaryAndMode(MultipartMode mode, MultiPartConfig multiPartConfig) {
        return given().config(config().multiPartConfig(multiPartConfig.defaultBoundary(BOUNDARY)).httpClient(httpClientConfig().multipartMode(mode)));
    }

    private File writeFile(String name, String content) throws IOException {
        return Files.writeString(tempDir.resolve(name), content, UTF_8).toFile();
    }

    /**
     * @return the bytes of the text encoded as UTF-8, one char per byte, to compare with {@link RecordedRequest#bodyAsString()}
     */
    private static String utf8(String text) {
        return new String(text.getBytes(UTF_8), ISO_8859_1);
    }
}

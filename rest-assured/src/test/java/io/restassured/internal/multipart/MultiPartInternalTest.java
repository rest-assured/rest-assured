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

import io.restassured.internal.NoParameterValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MultiPartInternalTest {

    @TempDir
    Path tmpDir;

    @Test
    void octet_stream_constant_and_defaults() {
        MultiPartInternal multiPart = new MultiPartInternal();

        assertThat(MultiPartInternal.OCTET_STREAM).isEqualTo("application/octet-stream");
        assertThat(multiPart.getContent()).isNull();
        assertThat(multiPart.getControlName()).isNull();
        assertThat(multiPart.getFileName()).isNull();
        assertThat(multiPart.getMimeType()).isNull();
        assertThat(multiPart.getCharset()).isNull();
        assertThat(multiPart.getHeaders()).isEmpty();
        assertThat(multiPart.getHeaders()).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    void setters_and_getters() {
        MultiPartInternal multiPart = multiPart("content", "text/xml", "UTF-8");
        Map<String, String> headers = Collections.singletonMap("a", "b");
        multiPart.setHeaders(headers);

        assertThat(multiPart.getContent()).isEqualTo("content");
        assertThat(multiPart.getControlName()).isEqualTo("ctrl");
        assertThat(multiPart.getFileName()).isEqualTo("file.txt");
        assertThat(multiPart.getMimeType()).isEqualTo("text/xml");
        assertThat(multiPart.getCharset()).isEqualTo("UTF-8");
        assertThat(multiPart.getHeaders()).isSameAs(headers);
    }

    @Test
    void mime_type_defaults_depend_on_content() throws IOException {
        assertThat(multiPart(file(), null, null).getMimeType()).isEqualTo("application/octet-stream");
        assertThat(multiPart(stream(), null, null).getMimeType()).isEqualTo("application/octet-stream");
        assertThat(multiPart(new byte[]{1}, null, null).getMimeType()).isEqualTo("application/octet-stream");
        assertThat(multiPart("text", null, null).getMimeType()).isEqualTo("text/plain");
        assertThat(multiPart(42, null, null).getMimeType()).isEqualTo("text/plain");
        assertThat(multiPart(new NoParameterValue(), null, null).getMimeType()).isEqualTo("text/plain");
        assertThat(multiPart(null, null, null).getMimeType()).isNull();
    }

    @Test
    void empty_mime_type_counts_as_unset_except_without_content() throws IOException {
        assertThat(multiPart(file(), "", null).getMimeType()).isEqualTo("application/octet-stream");
        assertThat(multiPart(stream(), "", null).getMimeType()).isEqualTo("application/octet-stream");
        assertThat(multiPart(new byte[]{1}, "", null).getMimeType()).isEqualTo("application/octet-stream");
        assertThat(multiPart("text", "", null).getMimeType()).isEqualTo("text/plain");
        assertThat(multiPart(42, "", null).getMimeType()).isEqualTo("text/plain");
        assertThat(multiPart(null, "", null).getMimeType()).isEmpty();
    }

    @Test
    void explicit_mime_type_is_returned_for_all_content() throws IOException {
        assertThat(multiPart(file(), "a/b", null).getMimeType()).isEqualTo("a/b");
        assertThat(multiPart("text", "a/b", null).getMimeType()).isEqualTo("a/b");
        assertThat(multiPart(null, "a/b", null).getMimeType()).isEqualTo("a/b");
    }

    @Test
    void file_content_becomes_file_body_with_charset() throws IOException {
        PartBody body = multiPart(file(), null, "utf-8").getContentBody();

        assertThat(body.getFileName()).isEqualTo("file.txt");
        assertThat(body.getContentType()).isEqualTo("application/octet-stream; charset=UTF-8");
        assertThat(body.getContentLength()).isEqualTo(4);
        assertThat(written(body)).isEqualTo("file");
    }

    @Test
    void file_content_without_file_name_uses_the_name_of_the_file() throws IOException {
        MultiPartInternal multiPart = multiPart(file(), null, null);
        multiPart.setFileName(null);

        assertThat(multiPart.getContentBody().getFileName()).isEqualTo("file.txt");
    }

    @Test
    void file_content_without_charset_and_with_mime_type() throws IOException {
        PartBody body = multiPart(file(), "application/xml", null).getContentBody();

        assertThat(body.getContentType()).isEqualTo("application/xml");
    }

    @Test
    void input_stream_content_becomes_input_stream_body_without_charset() throws IOException {
        PartBody body = multiPart(stream(), null, "UTF-8").getContentBody();

        assertThat(body.getFileName()).isEqualTo("file.txt");
        assertThat(body.getContentType()).isEqualTo("application/octet-stream");
        assertThat(body.getContentLength()).isEqualTo(-1);
        assertThat(written(body)).isEqualTo("\u0001");
    }

    @Test
    void input_stream_content_with_mime_type() {
        PartBody body = multiPart(stream(), "text/csv; charset=UTF-16", "UTF-8").getContentBody();

        assertThat(body.getContentType()).isEqualTo("text/csv; charset=UTF-16");
    }

    @Test
    void byte_array_content_becomes_input_stream_body_and_keeps_the_byte_array_content() throws IOException {
        byte[] bytes = {65, 66};
        MultiPartInternal multiPart = multiPart(bytes, "application/pdf", "UTF-8");

        PartBody body = multiPart.getContentBody();

        assertThat(body.getContentType()).isEqualTo("application/pdf");
        assertThat(body.getFileName()).isEqualTo("file.txt");
        assertThat(body.getContentLength()).isEqualTo(-1);
        assertThat(multiPart.getContent()).isSameAs(bytes);
        assertThat(written(body)).isEqualTo("AB");
    }

    @Test
    void byte_array_content_gives_a_new_body_with_all_bytes_each_time() throws IOException {
        MultiPartInternal multiPart = multiPart(new byte[]{65, 66}, null, null);

        String first = written(multiPart.getContentBody());
        String second = written(multiPart.getContentBody());

        assertThat(first).isEqualTo("AB");
        assertThat(second).isEqualTo("AB");
    }

    @Test
    void string_content_becomes_string_body() throws IOException {
        PartBody body = multiPart("hello", null, null).getContentBody();

        assertThat(body.getFileName()).isNull();
        assertThat(body.getContentType()).isEqualTo("text/plain");
        assertThat(body.getContentLength()).isEqualTo(5);
        assertThat(written(body)).isEqualTo("hello");
    }

    @Test
    void string_content_without_charset_is_encoded_in_the_default_charset_without_adding_it_to_the_content_type() throws IOException {
        PartBody body = multiPart("åäö", null, null).getContentBody("ISO-8859-1");

        assertThat(body.getContentType()).isEqualTo("text/plain");
        assertThat(body.getContentLength()).isEqualTo(3);
        assertThat(written(body)).isEqualTo("åäö");
    }

    @Test
    void string_content_without_charset_or_default_charset_is_encoded_as_utf_8() throws IOException {
        assertThat(written(multiPart("åäö", null, null).getContentBody(null))).isEqualTo(utf8("åäö"));
        assertThat(written(multiPart("åäö", null, null).getContentBody())).isEqualTo(utf8("åäö"));
    }

    @Test
    void charset_of_the_part_or_its_mime_type_wins_over_the_default_charset() throws IOException {
        assertThat(written(multiPart("åäö", null, "UTF-8").getContentBody("ISO-8859-1"))).isEqualTo(utf8("åäö"));
        assertThat(written(multiPart("åäö", "text/plain; charset=UTF-8", null).getContentBody("ISO-8859-1"))).isEqualTo(utf8("åäö"));
    }

    @Test
    void string_content_with_charset_keeps_mime_type_parameters() throws IOException {
        PartBody body = multiPart("<a/>", "application/xml; version=2", "utf-16").getContentBody();

        assertThat(body.getContentType()).isEqualTo("application/xml; version=2; charset=UTF-16");
        assertThat(written(body)).isEqualTo(new String("<a/>".getBytes(StandardCharsets.UTF_16), StandardCharsets.ISO_8859_1));
    }

    @Test
    void string_content_with_charset_replaces_charset_of_mime_type() {
        PartBody body = multiPart("x", "text/plain; charset=US-ASCII", "ISO-8859-1").getContentBody();

        assertThat(body.getContentType()).isEqualTo("text/plain; charset=ISO-8859-1");
    }

    @Test
    void empty_mime_type_and_charset_count_as_unset() throws IOException {
        assertThat(multiPart("x", "", "").getContentBody().getContentType()).isEqualTo("text/plain");
        assertThat(multiPart(file(), "", "").getContentBody().getContentType()).isEqualTo("application/octet-stream");
        assertThat(multiPart(stream(), "", "").getContentBody().getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void no_parameter_value_content_becomes_empty_string() throws IOException {
        MultiPartInternal multiPart = multiPart(new NoParameterValue(), null, null);

        PartBody body = multiPart.getContentBody();

        assertThat(multiPart.getContent()).isEqualTo("");
        assertThat(body.getContentType()).isEqualTo("text/plain");
        assertThat(written(body)).isEmpty();
    }

    @Test
    void other_content_is_sent_as_text_the_way_groovy_renders_it() throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", Arrays.asList("x", null));

        assertThat(written(multiPart(42, null, null).getContentBody())).isEqualTo("42");
        assertThat(written(multiPart(new StringBuilder("sb"), null, null).getContentBody())).isEqualTo("sb");
        assertThat(written(multiPart(map, null, null).getContentBody())).isEqualTo("[a:1, b:[x, null]]");
        assertThat(written(multiPart(Collections.emptyMap(), null, null).getContentBody())).isEqualTo("[:]");
        assertThat(written(multiPart(Arrays.asList(1, "two"), null, null).getContentBody())).isEqualTo("[1, two]");
        assertThat(written(multiPart(new int[]{1, 2}, null, null).getContentBody())).isEqualTo("[1, 2]");
        assertThat(written(multiPart(new char[]{'h', 'i'}, null, null).getContentBody())).isEqualTo("hi");
        assertThat(written(multiPart(new Object[]{"a", new int[]{1}}, null, null).getContentBody())).isEqualTo("[a, [1]]");
    }

    @Test
    void maps_and_collections_are_rendered_the_groovy_way_even_when_they_override_to_string() throws IOException {
        String actual = written(multiPart(Collections.unmodifiableMap(Collections.singletonMap("a", "1")), null, null).getContentBody())
                + " | " + written(multiPart(new java.util.concurrent.ConcurrentHashMap<>(Collections.singletonMap("a", "1")), null, null).getContentBody())
                + " | " + written(multiPart(new java.util.TreeMap<>(Collections.singletonMap("a", "1")), null, null).getContentBody())
                + " | " + written(multiPart(Collections.unmodifiableList(Arrays.asList("a", "b")), null, null).getContentBody())
                + " | " + written(multiPart(new PublicOwnToStringList(), null, null).getContentBody())
                + " | " + written(multiPart(new PrivateOwnToStringList(), null, null).getContentBody())
                + " | " + written(multiPart(new PackagePrivateOwnToStringList(), null, null).getContentBody())
                + " | " + written(multiPart(new java.util.concurrent.CopyOnWriteArrayList<>(Arrays.asList("a")), null, null).getContentBody())
                + " | " + written(multiPart(new java.util.ArrayDeque<>(Arrays.asList("a")), null, null).getContentBody())
                + " | " + written(multiPart(java.util.List.of("a"), null, null).getContentBody())
                + " | " + written(multiPart(java.util.Map.of("a", "1"), null, null).getContentBody());

        // Groovy's content.toString() uses DefaultGroovyMethods.toString(..) unless a class it can call overrides toString()
        assertThat(actual).isEqualTo("[a:1] | {a=1} | [a:1] | [a, b] | own | own | own | [a] | [a] | [a] | [a:1]");
    }

    public static class PublicOwnToStringList extends java.util.ArrayList<String> {
        PublicOwnToStringList() { add("a"); }
        @Override
        public String toString() { return "own"; }
    }

    private static class PrivateOwnToStringList extends java.util.ArrayList<String> {
        PrivateOwnToStringList() { add("a"); }
        @Override
        public String toString() { return "own"; }
    }

    static class PackagePrivateOwnToStringList extends java.util.ArrayList<String> {
        PackagePrivateOwnToStringList() { add("a"); }
        @Override
        public String toString() { return "own"; }
    }

    @Test
    void null_content_is_illegal() {
        Throwable t = catchThrowable(() -> multiPart(null, null, null).getContentBody());

        assertThat(t).isInstanceOf(IllegalArgumentException.class).hasMessage("Illegal content: null");
    }

    @Test
    void equal_when_all_properties_are_equal() {
        MultiPartInternal one = multiPart("content", "text/xml", "UTF-8");
        MultiPartInternal two = multiPart("content", "text/xml", "UTF-8");

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one.canEqual(two)).isTrue();
        assertThat(one.canEqual("other")).isFalse();
        assertThat(one).isNotEqualTo(null);
        assertThat(one).isNotEqualTo("content");
        assertThat(one).isEqualTo(one);
    }

    @Test
    void not_equal_when_a_property_differs() {
        MultiPartInternal one = multiPart("content", "text/xml", "UTF-8");

        MultiPartInternal content = multiPart("other", "text/xml", "UTF-8");
        MultiPartInternal controlName = multiPart("content", "text/xml", "UTF-8");
        controlName.setControlName("other");
        MultiPartInternal fileName = multiPart("content", "text/xml", "UTF-8");
        fileName.setFileName("other");
        MultiPartInternal mimeType = multiPart("content", "text/plain", "UTF-8");
        MultiPartInternal charset = multiPart("content", "text/xml", "UTF-16");
        MultiPartInternal headers = multiPart("content", "text/xml", "UTF-8");
        headers.setHeaders(Collections.singletonMap("a", "b"));

        assertThat(one).isNotEqualTo(content).isNotEqualTo(controlName).isNotEqualTo(fileName).isNotEqualTo(mimeType)
                .isNotEqualTo(charset).isNotEqualTo(headers);
    }

    @Test
    void equality_uses_the_defaulted_mime_type() {
        MultiPartInternal implicit = multiPart("content", null, null);
        MultiPartInternal explicit = multiPart("content", "text/plain", null);

        assertThat(implicit).isEqualTo(explicit);
        assertThat(implicit.hashCode()).isEqualTo(explicit.hashCode());
    }

    @Test
    void byte_array_contents_are_compared_by_their_elements() {
        MultiPartInternal one = multiPart(new byte[]{1, 2}, null, null);
        MultiPartInternal two = multiPart(new byte[]{1, 2}, null, null);

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(multiPart(new byte[]{1, 3}, null, null));
    }

    @Test
    void hash_code_is_calculated_like_groovy_canonical() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("a", "b");
        MultiPartInternal all = multiPart("content", "text/xml", "UTF-8");
        all.setHeaders(headers);

        String actual = new MultiPartInternal().hashCode() + " " + all.hashCode()
                + " " + multiPart(new byte[]{1, 2}, null, null).hashCode()
                + " " + multiPart(new Object[]{"a", 1}, null, null).hashCode()
                + " " + multiPart(true, null, null).hashCode()
                + " " + multiPart(false, null, null).hashCode()
                + " " + multiPart(12L, null, null).hashCode()
                + " " + multiPart(12, null, null).hashCode()
                + " " + multiPart('c', null, null).hashCode()
                + " " + multiPart(1.5d, null, null).hashCode()
                + " " + multiPart(new int[]{3}, null, null).hashCode();

        assertThat(actual).isEqualTo("1103554295 654180309 -1420542409 737141599 -1474540999 -1490805505 -2129828776 -2129828776 -60956907 962254804 713603914");
    }

    @Test
    void to_string_is_like_groovy_canonical_and_includes_the_content_body() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("a", "b");
        MultiPartInternal multiPart = multiPart("content", "text/xml", "UTF-8");
        multiPart.setHeaders(headers);

        assertThat(multiPart.toString()).matches(
                "io\\.restassured\\.internal\\.multipart\\.MultiPartInternal\\(content, ctrl, file\\.txt, text/xml, UTF-8, \\[a:b], " +
                        "io\\.restassured\\.internal\\.multipart\\.PartBody@[0-9a-f]+\\)");
    }

    @Test
    void to_string_renders_values_the_groovy_way_and_keeps_byte_array_content() throws IOException {
        byte[] bytes = {65, 66};
        MultiPartInternal multiPart = new MultiPartInternal();
        multiPart.setContent(bytes);

        String first = multiPart.toString();
        String second = multiPart.toString();

        assertThat(first).matches(
                "io\\.restassured\\.internal\\.multipart\\.MultiPartInternal\\(\\[65, 66], null, null, application/octet-stream, null, \\[:], " +
                        "io\\.restassured\\.internal\\.multipart\\.PartBody@[0-9a-f]+\\)");
        assertThat(second).startsWith("io.restassured.internal.multipart.MultiPartInternal([65, 66], ");
        assertThat(multiPart.getContent()).isSameAs(bytes);
        assertThat(written(multiPart.getContentBody())).isEqualTo("AB");
    }

    @Test
    void to_string_fails_without_content() {
        Throwable t = catchThrowable(() -> new MultiPartInternal().toString());

        assertThat(t).isInstanceOf(IllegalArgumentException.class).hasMessage("Illegal content: null");
    }

    private static MultiPartInternal multiPart(Object content, String mimeType, String charset) {
        MultiPartInternal multiPart = new MultiPartInternal();
        multiPart.setContent(content);
        multiPart.setControlName("ctrl");
        multiPart.setFileName("file.txt");
        multiPart.setMimeType(mimeType);
        multiPart.setCharset(charset);
        return multiPart;
    }

    private File file() throws IOException {
        Path file = tmpDir.resolve("file.txt");
        Files.write(file, "file".getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    private static InputStream stream() {
        return new ByteArrayInputStream(new byte[]{1});
    }

    private static String written(PartBody body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.writeTo(out);
        return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static String utf8(String text) {
        return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }
}

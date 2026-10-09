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
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
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
        File file = file();

        ContentBody body = (ContentBody) multiPart(file, null, "utf-8").getContentBody();

        assertThat(body).isInstanceOf(FileBody.class);
        assertThat(((FileBody) body).getFile()).isSameAs(file);
        assertThat(body.getFilename()).isEqualTo("file.txt");
        assertThat(((FileBody) body).getContentType().toString()).isEqualTo("application/octet-stream; charset=UTF-8");
    }

    @Test
    void file_content_without_charset_and_with_mime_type() throws IOException {
        ContentBody body = (ContentBody) multiPart(file(), "application/xml", null).getContentBody();

        assertThat(((FileBody) body).getContentType().toString()).isEqualTo("application/xml");
    }

    @Test
    void input_stream_content_becomes_input_stream_body_without_charset() {
        InputStream stream = stream();

        ContentBody body = (ContentBody) multiPart(stream, null, "UTF-8").getContentBody();

        assertThat(body).isInstanceOf(InputStreamBody.class);
        assertThat(((InputStreamBody) body).getInputStream()).isSameAs(stream);
        assertThat(body.getFilename()).isEqualTo("file.txt");
        assertThat(((InputStreamBody) body).getContentType().toString()).isEqualTo("application/octet-stream");
    }

    @Test
    void input_stream_content_with_mime_type() {
        ContentBody body = (ContentBody) multiPart(stream(), "text/csv; charset=UTF-16", "UTF-8").getContentBody();

        assertThat(((InputStreamBody) body).getContentType().toString()).isEqualTo("text/csv; charset=UTF-16");
    }

    @Test
    void byte_array_content_becomes_input_stream_body_and_keeps_the_byte_array_content() throws IOException {
        byte[] bytes = {65, 66};
        MultiPartInternal multiPart = multiPart(bytes, "application/pdf", "UTF-8");

        ContentBody body = (ContentBody) multiPart.getContentBody();

        assertThat(body).isInstanceOf(InputStreamBody.class);
        assertThat(((InputStreamBody) body).getContentType().toString()).isEqualTo("application/pdf");
        assertThat(body.getFilename()).isEqualTo("file.txt");
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
        ContentBody body = (ContentBody) multiPart("hello", null, null).getContentBody();

        assertThat(body).isInstanceOf(StringBody.class);
        assertThat(body.getFilename()).isNull();
        assertThat(((StringBody) body).getContentType().toString()).isEqualTo("text/plain");
        assertThat(written(body)).isEqualTo("hello");
    }

    @Test
    void string_content_with_charset_keeps_mime_type_parameters() throws IOException {
        ContentBody body = (ContentBody) multiPart("<a/>", "application/xml; version=2", "utf-16").getContentBody();

        assertThat(((StringBody) body).getContentType().toString()).isEqualTo("application/xml; version=2; charset=UTF-16");
        assertThat(body.getCharset()).isEqualTo("UTF-16");
    }

    @Test
    void string_content_with_charset_replaces_charset_of_mime_type() {
        ContentBody body = (ContentBody) multiPart("x", "text/plain; charset=US-ASCII", "ISO-8859-1").getContentBody();

        assertThat(((StringBody) body).getContentType().toString()).isEqualTo("text/plain; charset=ISO-8859-1");
    }

    @Test
    void empty_mime_type_and_charset_count_as_unset() throws IOException {
        assertThat(((StringBody) multiPart("x", "", "").getContentBody()).getContentType().toString()).isEqualTo("text/plain");
        assertThat(((FileBody) multiPart(file(), "", "").getContentBody()).getContentType().toString()).isEqualTo("application/octet-stream");
        assertThat(((InputStreamBody) multiPart(stream(), "", "").getContentBody()).getContentType().toString()).isEqualTo("application/octet-stream");
    }

    @Test
    void no_parameter_value_content_becomes_empty_string() throws IOException {
        MultiPartInternal multiPart = multiPart(new NoParameterValue(), null, null);

        ContentBody body = (ContentBody) multiPart.getContentBody();

        assertThat(multiPart.getContent()).isEqualTo("");
        assertThat(((StringBody) body).getContentType().toString()).isEqualTo("text/plain");
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
                        "org\\.apache\\.http\\.entity\\.mime\\.content\\.StringBody@[0-9a-f]+\\)");
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
                        "org\\.apache\\.http\\.entity\\.mime\\.content\\.InputStreamBody@[0-9a-f]+\\)");
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

    private static String written(Object body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((ContentBody) body).writeTo(out);
        return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
    }
}

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
package io.restassured.internal.support;

import io.restassured.parsing.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Characterizes how {@link Prettifier} renders content. Non-String content (for example a {@code byte[]} multipart
 * body) is turned into text the way Groovy's {@code toString()} does it, e.g. {@code [65, 66]} rather than {@code [B@1a2b}.
 */
class PrettifierBodyFormattingTest {

    private static final String NL = System.lineSeparator();

    @TempDir
    File tempDir;

    @Test
    void null_content_is_rendered_as_empty_string() {
        assertThat(prettify(null, Parser.JSON)).isEqualTo("");
        assertThat(prettify(null, null)).isEqualTo("");
    }

    @Test
    void primitive_arrays_are_rendered_like_groovy() {
        assertThat(prettify(new byte[]{65, 66}, null)).isEqualTo("[65, 66]");
        assertThat(prettify(new byte[0], null)).isEqualTo("[]");
        assertThat(prettify(new char[]{'a', 'b'}, null)).isEqualTo("ab");
        assertThat(prettify(new int[]{1, 2}, null)).isEqualTo("[1, 2]");
        assertThat(prettify(new long[]{1L, 2L}, null)).isEqualTo("[1, 2]");
        assertThat(prettify(new short[]{1, 2}, null)).isEqualTo("[1, 2]");
        assertThat(prettify(new double[]{1.5d}, null)).isEqualTo("[1.5]");
        assertThat(prettify(new float[]{1.5f}, null)).isEqualTo("[1.5]");
        assertThat(prettify(new boolean[]{true, false}, null)).isEqualTo("[true, false]");
    }

    @Test
    void object_arrays_are_rendered_like_groovy() {
        assertThat(prettify(new Object[]{"x", 1, null}, null)).isEqualTo("[x, 1, null]");
        assertThat(prettify(new String[]{"x", "y"}, null)).isEqualTo("[x, y]");
        assertThat(prettify(new Object[0], null)).isEqualTo("[]");
        assertThat(prettify(new Object[]{new int[]{1, 2}, new String[]{"a"}}, null)).isEqualTo("[[1, 2], [a]]");
        assertThat(prettify(new int[][]{{1, 2}, {3}}, null)).isEqualTo("[[1, 2], [3]]");
    }

    @Test
    void maps_are_rendered_like_groovy() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", "x");
        map.put("c", Arrays.asList(1, 2));
        map.put("d", Collections.singletonMap("k", "v"));
        map.put("e", null);
        map.put("f", new String[]{"x"});
        map.put("g", new char[]{'a', 'b'});

        assertThat(prettify(map, null)).isEqualTo("[a:1, b:x, c:[1, 2], d:[k:v], e:null, f:[x], g:ab]");
        assertThat(prettify(new LinkedHashMap<>(), null)).isEqualTo("[:]");
        assertThat(prettify(new TreeMap<>(Collections.singletonMap("z", 1)), null)).isEqualTo("[z:1]");
        assertThat(prettify(Collections.emptyMap(), null)).isEqualTo("[:]");
        assertThat(prettify(Collections.unmodifiableMap(Collections.singletonMap("a", 1)), null)).isEqualTo("[a:1]");
        assertThat(prettify(Map.of("a", Collections.singletonMap("k", 1)), null)).isEqualTo("[a:[k:1]]");
        assertThat(prettify(new EnumMap<>(Collections.singletonMap(Thread.State.NEW, Collections.singletonMap("a", 1))), null)).isEqualTo("[NEW:[a:1]]");
        Map<Object, Object> nullKey = new HashMap<>();
        nullKey.put(null, 1);
        assertThat(prettify(nullKey, null)).isEqualTo("[null:1]");
    }

    @Test
    void collections_are_rendered_like_groovy() {
        List<Object> list = new ArrayList<>(Arrays.asList("a", 1, null, Collections.singletonMap("b", 2), new byte[]{1, 2}, 'q', new char[]{'x', 'y'}));

        assertThat(prettify(list, null)).isEqualTo("[a, 1, null, [b:2], [1, 2], q, xy]");
        assertThat(prettify(new ArrayList<>(), null)).isEqualTo("[]");
        assertThat(prettify(Collections.emptyList(), null)).isEqualTo("[]");
        assertThat(prettify(new HashSet<>(Collections.singletonList("a")), null)).isEqualTo("[a]");
        assertThat(prettify(new TreeSet<>(Arrays.asList("b", "a")), null)).isEqualTo("[a, b]");
        assertThat(prettify(new ArrayDeque<>(Collections.singletonList("a")), null)).isEqualTo("[a]");
        assertThat(prettify(new LinkedList<>(Collections.singletonList(Collections.singletonMap("a", 1))), null)).isEqualTo("[[a:1]]");
        assertThat(prettify(Collections.unmodifiableList(Collections.singletonList(Collections.singletonMap("k", 1))), null)).isEqualTo("[[k:1]]");
        assertThat(prettify(List.of("a", Collections.singletonMap("k", 1)), null)).isEqualTo("[a, [k:1]]");
        assertThat(prettify(Set.of(Collections.singletonMap("a", 1)), null)).isEqualTo("[[a:1]]");
    }

    @Test
    void nested_maps_are_rendered_like_groovy_even_when_their_own_to_string_is_overridden() {
        assertThat(prettify(Collections.singletonList(new ConcurrentHashMap<>(Collections.singletonMap("a", 1))), null)).isEqualTo("[[a:1]]");
        assertThat(prettify(Collections.singletonList(new Hashtable<>(Collections.singletonMap("a", "b"))), null)).isEqualTo("[[a:b]]");
    }

    @Test
    void public_collection_classes_that_override_to_string_keep_their_own_format() {
        assertThat(prettify(new ConcurrentHashMap<>(Collections.singletonMap("a", 1)), null)).isEqualTo("{a=1}");
        assertThat(prettify(new Hashtable<>(Collections.singletonMap("a", "b")), null)).isEqualTo("{a=b}");
        assertThat(prettify(new CopyOnWriteArrayList<>(Collections.singletonList(Collections.singletonMap("a", 1))), null)).isEqualTo("[{a=1}]");
        assertThat(prettify(new Vector<>(Collections.singletonList(Collections.singletonMap("a", 1))), null)).isEqualTo("[{a=1}]");
    }

    @Test
    void self_references_are_rendered_like_groovy() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("self", map);
        List<Object> list = new ArrayList<>();
        list.add(list);

        assertThat(prettify(map, null)).isEqualTo("[self:(this Map)]");
        assertThat(prettify(list, null)).isEqualTo("[(this Collection)]");
    }

    @Test
    void other_objects_use_their_own_to_string() {
        assertThat(prettify(new StringBuilder("sb"), null)).isEqualTo("sb");
        assertThat(prettify(42, null)).isEqualTo("42");
        assertThat(prettify(Thread.State.NEW, null)).isEqualTo("NEW");
    }

    @Test
    void byte_array_rendering_is_prettified_as_json_when_parser_is_json() {
        assertThat(prettify(new byte[]{65, 66}, Parser.JSON)).isEqualTo("[" + "\n    65,\n    66\n" + "]");
    }

    @Test
    void file_content_is_read_and_prettified() throws IOException {
        File file = new File(tempDir, "body.json");
        Files.write(file.toPath(), "{\"a\":1}".getBytes(StandardCharsets.UTF_8));

        assertThat(prettify(file, Parser.JSON)).isEqualTo("{\n    \"a\": 1\n}");
        assertThat(prettify(file, null)).isEqualTo("{\"a\":1}");
        assertThat(prettify(file, Parser.TEXT)).isEqualTo("{\"a\":1}");
    }

    @Test
    void missing_file_fails_with_the_original_io_exception() {
        File file = new File(tempDir, "does-not-exist.json");

        assertThatThrownBy(() -> prettify(file, Parser.JSON))
                .isExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test
    void content_that_cannot_be_parsed_is_returned_as_is() {
        assertThat(prettify("{not json", Parser.JSON)).isEqualTo("{not json");
        assertThat(prettify("<not xml", Parser.XML)).isEqualTo("<not xml");
        assertThat(prettify("plain", Parser.TEXT)).isEqualTo("plain");
        assertThat(prettify("plain", null)).isEqualTo("plain");
    }

    @Test
    void json_unicode_escapes_are_unescaped() {
        assertThat(prettify("{\"a\":\"\\u00e5\"}", Parser.JSON)).isEqualTo("{\n    \"a\": \"\u00e5\"\n}");
    }

    @Test
    void blank_xml_and_html_are_rendered_as_empty_string() {
        assertThat(prettify("  ", Parser.XML)).isEqualTo("");
        assertThat(prettify("  ", Parser.HTML)).isEqualTo("");
    }

    @Test
    void xml_is_prettified_without_namespace_awareness() {
        String xml = "<ns:a xmlns:ns=\"urn:x\"><ns:b attr=\"1\">x</ns:b><c/></ns:a>";

        assertThat(prettify(xml, Parser.XML)).isEqualTo(lines(
                "<ns:a xmlns:ns=\"urn:x\">",
                "  <ns:b attr=\"1\">x</ns:b>",
                "  <c/>",
                "</ns:a>"));
    }

    @Test
    void xml_with_doctype_is_returned_as_is() {
        String xml = "<!DOCTYPE greeting [<!ELEMENT greeting (#PCDATA)>]><greeting>Hello</greeting>";

        assertThat(prettify(xml, Parser.XML)).isEqualTo(xml);
    }

    @Test
    void xml_is_reindented() {
        assertThat(prettify("<a>\n   <b>  x  </b>\n</a>\n", Parser.XML)).isEqualTo(lines(
                "<a>",
                "  <b>  x  </b>",
                "</a>"));
    }

    @Test
    void html_is_prettified_leniently() {
        String html = "<html><head><title>T</title></head><body><p>hello<br>world</body></html>";

        assertThat(prettify(html, Parser.HTML)).isEqualTo(lines(
                "<html>",
                "  <head>",
                "    <title>T</title>",
                "  </head>",
                "  <body>",
                "    <p>",
                "hello      <br clear=\"none\"/>",
                "world    </p>",
                "  </body>",
                "</html>"));
    }

    private static String lines(String... lines) {
        return String.join(NL, lines);
    }

    private static String prettify(Object content, Parser parser) {
        return new Prettifier().prettify(content, parser);
    }
}

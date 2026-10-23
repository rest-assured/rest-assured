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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how JSON, XML and HTML bodies are pretty printed when they are logged, case by case.
 */
class PrettifierCharacterizationTest {

    private static final String NL = System.lineSeparator();

    @Test
    void json_is_indented_by_four_with_empty_containers_on_three_lines() {
        assertThat(json("{\"a\":1,\"b\":[1,2,{\"c\":null}],\"d\":{},\"e\":[],\"f\":\"x\"}")).isEqualTo(
                "{\n    \"a\": 1,\n    \"b\": [\n        1,\n        2,\n        {\n            \"c\": null\n        }\n    ],\n" +
                        "    \"d\": {\n        \n    },\n    \"e\": [\n        \n    ],\n    \"f\": \"x\"\n}");
        assertThat(json("{}")).isEqualTo("{\n    \n}");
        assertThat(json("[]")).isEqualTo("[\n    \n]");
        assertThat(json("{\"a\":[[]],\"b\":[{}]}")).isEqualTo(
                "{\n    \"a\": [\n        [\n            \n        ]\n    ],\n    \"b\": [\n        {\n            \n        }\n    ]\n}");
    }

    @Test
    void json_whitespace_is_replaced_and_number_text_is_kept() {
        assertThat(json("  {\"a\" : 1.50e3 ,\n\t\"b\":true, \"c\":-0.0, \"d\":false}  "))
                .isEqualTo("{\n    \"a\": 1.50e3,\n    \"b\": true,\n    \"c\": -0.0,\n    \"d\": false\n}");
    }

    @Test
    void json_scalars_are_printed_as_they_are() {
        assertThat(json("\"just a string\"")).isEqualTo("\"just a string\"");
        assertThat(json("123")).isEqualTo("123");
    }

    @Test
    void json_strings_are_re_escaped_with_other_control_characters_and_unicode_escapes_printed_raw() {
        assertThat(json("{\"q\":\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0001\"}")).isEqualTo("{\n    \"q\": \"\\\"\\\\/\\b\\f\\n\\r\\t\u0001\"\n}");
        assertThat(json("{\"\u00e5\":\"\\u00e5\u00e4\u20ac\ud83d\ude00\"}")).isEqualTo("{\n    \"\u00e5\": \"\u00e5\u00e4\u20ac\ud83d\ude00\"\n}");
    }

    @Test
    void json_escaped_backslash_followed_by_u_is_unescaped_after_the_backslash() {
        assertThat(json("{\"a\":\"\\\\u00e5\"}")).isEqualTo("{\n    \"a\": \"\\\u00e5\"\n}");
    }

    @Test
    void json_is_formatted_without_being_validated() {
        assertThat(json("{\"a\":")).isEqualTo("{\n    \"a\": ");
        assertThat(json("{\"a\" 1}")).isEqualTo("{\n    \"a\"1\n}");
        assertThat(json("[1,,2]")).isEqualTo("[\n    1,\n    ,\n    2\n]");
    }

    @Test
    void json_that_cannot_be_lexed_is_returned_as_it_is() {
        assertThat(json("{\"a\":1} trailing")).isEqualTo("{\"a\":1} trailing");
        assertThat(json("not json")).isEqualTo("not json");
    }

    @Test
    void xml_drops_the_declaration_comments_and_whitespace_only_text_and_prints_cdata_as_text() {
        String xml = "<?xml version=\"1.0\"?><!-- c --><a x='1' y=\"&quot;&amp;&lt;&gt;\"><b>text &amp; more</b><c/><d>mixed <e>in</e> tail</d>" +
                "<f><![CDATA[<raw>]]></f><h>  spaced  </h>\n   <i>\n  </i></a>";

        assertThat(xml(xml)).isEqualTo(lines(
                "<a x=\"1\" y=\"&quot;&amp;&lt;&gt;\">",
                "  <b>text &amp; more</b>",
                "  <c/>",
                "  <d>",
                "mixed     <e>in</e>",
                " tail  </d>",
                "  <f>&lt;raw&gt;</f>",
                "  <h>  spaced  </h>",
                "  <i/>",
                "</a>"));
    }

    @Test
    void xml_is_indented_by_two() {
        assertThat(xml("<a><b><c><d>deep</d></c></b></a>")).isEqualTo(lines(
                "<a>",
                "  <b>",
                "    <c>",
                "      <d>deep</d>",
                "    </c>",
                "  </b>",
                "</a>"));
        assertThat(xml("<a>only text</a>")).isEqualTo("<a>only text</a>");
    }

    @Test
    void xml_keeps_namespace_prefixes_and_declarations_as_attributes() {
        assertThat(xml("<ns:a xmlns:ns=\"urn:x\" xmlns=\"urn:y\"><ns:b/><c ns:attr=\"v\"/></ns:a>")).isEqualTo(lines(
                "<ns:a xmlns:ns=\"urn:x\" xmlns=\"urn:y\">",
                "  <ns:b/>",
                "  <c ns:attr=\"v\"/>",
                "</ns:a>"));
    }

    @Test
    void xml_entities_and_character_references_are_resolved_and_escaped_again() {
        assertThat(xml("<a>&lt;&#65;&#x42;&apos;&gt;<b c='x&#10;y' d=\"it's\" e='\u00e5'/></a>")).isEqualTo(lines(
                "<a>",
                "&lt;AB'&gt;  <b c=\"x&#10;y\" d=\"it's\" e=\"\u00e5\"/>",
                "</a>"));
    }

    @Test
    void xml_drops_processing_instructions_and_comments_inside_elements() {
        assertThat(xml("<a><?pi data?><b>\u00e5\u20ac</b><!-- inner --></a>")).isEqualTo(lines(
                "<a>",
                "  <b>\u00e5\u20ac</b>",
                "</a>"));
    }

    @Test
    void xml_elements_that_are_empty_or_hold_only_whitespace_are_self_closed() {
        assertThat(xml("<a><b x=\"1\">  </b><c></c><d>\n</d></a>")).isEqualTo(lines(
                "<a>",
                "  <b x=\"1\"/>",
                "  <c/>",
                "  <d/>",
                "</a>"));
    }

    @Test
    void html_login_form_is_printed_with_the_default_attributes_of_tagsoup() {
        String html = "<html><head><title>Login</title></head><body><form action=/login method=post><input type=text name=user>" +
                "<input name=pass type=password><input type=hidden name=_csrf value=abc></form></body></html>";

        assertThat(html(html)).isEqualTo(lines(
                "<html>",
                "  <head>",
                "    <title>Login</title>",
                "  </head>",
                "  <body>",
                "    <form enctype=\"application/x-www-form-urlencoded\" method=\"post\" action=\"/login\">",
                "      <input type=\"text\" name=\"user\"/>",
                "      <input type=\"password\" name=\"pass\"/>",
                "      <input type=\"hidden\" name=\"_csrf\" value=\"abc\"/>",
                "    </form>",
                "  </body>",
                "</html>"));
    }

    @Test
    void html_unclosed_tags_are_closed() {
        assertThat(html("<p>unclosed <b>bold")).isEqualTo(lines(
                "<html>",
                "  <body>",
                "    <p>",
                "unclosed       <b>bold</b>",
                "    </p>",
                "  </body>",
                "</html>"));
    }

    @Test
    void html_entities_are_resolved_and_script_text_is_escaped() {
        assertThat(html("<html><body><div>a &amp; b&nbsp;c</div><script>if (a < b) {}</script></body></html>")).isEqualTo(lines(
                "<html>",
                "  <body>",
                "    <div>a &amp; b\u00a0c</div>",
                "    <script>if (a &lt; b) {}</script>",
                "  </body>",
                "</html>"));
    }

    private static String json(String json) {
        return new Prettifier().prettify(json, Parser.JSON);
    }

    private static String xml(String xml) {
        return new Prettifier().prettify(xml, Parser.XML);
    }

    private static String html(String html) {
        return new Prettifier().prettify(html, Parser.HTML);
    }

    private static String lines(String... lines) {
        return String.join(NL, lines);
    }
}

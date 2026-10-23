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

import io.restassured.config.EncoderConfig;
import io.restassured.internal.NoParameterValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Pins how {@link URIBuilder} formats query parameters and parses the query already in its URI.
 */
class URIBuilderTest {

    private static final String A_UML = "ä";
    private static final String EURO = "€";

    @ParameterizedTest(name = "{0} in {1} -> {2}")
    @MethodSource("encodedValues")
    void encodes_names_and_values_like_url_encoder_but_with_a_space_as_percent_20(String value, String charset, String expected) throws Exception {
        URIBuilder builder = builder("http://h/p", true, charset).setQuery(singletonMap(value, value));

        assertThat(builder.toURI().getRawQuery()).isEqualTo(expected + "=" + expected);
        assertThat(URIBuilder.encode(value, charset)).isEqualTo(expected);
    }

    static Stream<Arguments> encodedValues() {
        return Stream.of(
                arguments("azAZ09", "UTF-8", "azAZ09"),
                arguments("-_.*", "UTF-8", "-_.*"),
                arguments("~", "UTF-8", "%7E"),
                arguments("a b", "UTF-8", "a%20b"),
                arguments("a+b", "UTF-8", "a%2Bb"),
                arguments("a%20b", "UTF-8", "a%2520b"),
                arguments("&=?#/:@!$'(),;[]", "UTF-8", "%26%3D%3F%23%2F%3A%40%21%24%27%28%29%2C%3B%5B%5D"),
                arguments("\"<>\\^`{|}", "UTF-8", "%22%3C%3E%5C%5E%60%7B%7C%7D"),
                arguments("\t\n\u007f", "UTF-8", "%09%0A%7F"),
                arguments(A_UML + EURO, "UTF-8", "%C3%A4%E2%82%AC"),
                arguments("😀", "UTF-8", "%F0%9F%98%80"),
                arguments(A_UML, "ISO-8859-1", "%E4"),
                arguments(EURO, "ISO-8859-1", "%3F"),
                arguments(A_UML, "UTF-16", "%FE%FF%00%E4"));
    }

    @Test
    void encodes_with_the_default_charset_when_no_charset_is_given() {
        assertThat(URIBuilder.encode(A_UML + " ~", null)).isEqualTo(URIBuilder.encode(A_UML + " ~", Charset.defaultCharset().name()));
    }

    @Test
    void unknown_charset_is_an_illegal_argument() {
        assertThatThrownBy(() -> URIBuilder.encode("a", "no-such-charset")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formats_value_kinds() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("string", "s");
        params.put("empty", "");
        params.put("null", null);
        params.put("noValue", new NoParameterValue());
        params.put("number", 1);
        params.put("list", Arrays.asList("x", "", null, 2));

        URIBuilder builder = builder("http://h/p", true, "UTF-8").setQuery(params);

        assertThat(builder.toURI().getRawQuery()).isEqualTo("string=s&empty=&null=&noValue&number=1&list=x&list=&list=&list=2");
    }

    @Test
    void sends_names_and_values_as_is_when_url_encoding_is_disabled() throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("a%20b", "c+d");
        params.put("e", new NoParameterValue());
        params.put("f", null);

        URIBuilder builder = builder("http://h/p", false, "UTF-8").setQuery(params);

        assertThat(builder.toURI().getRawQuery()).isEqualTo("a%20b=c+d&e&f=");
    }

    @Test
    void set_query_replaces_the_query_and_keeps_path_and_fragment() throws Exception {
        URIBuilder builder = builder("http://u@h:1/a%20b/c?x=1#frag%20x", true, "UTF-8").setQuery(singletonMap("y", "2"));

        assertThat(builder.toString()).isEqualTo("http://u@h:1/a%20b/c?y=2#frag%20x");
    }

    @Test
    void empty_or_null_map_leaves_the_query_alone() throws Exception {
        assertThat(builder("http://h/p?x=1", true, "UTF-8").setQuery(new LinkedHashMap<>()).toString()).isEqualTo("http://h/p?x=1");
        assertThat(builder("http://h/p?x=1", true, "UTF-8").setQuery(null).toString()).isEqualTo("http://h/p?x=1");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("existingQueries")
    void added_params_follow_the_existing_query_which_is_parsed_without_decoding_and_encoded_again(String uri, String expectedQuery) throws Exception {
        URIBuilder builder = builder(uri, true, "UTF-8").addQueryParams(singletonMap("z", "1"));

        assertThat(builder.toURI().getRawQuery()).isEqualTo(expectedQuery);
    }

    static Stream<Arguments> existingQueries() {
        return Stream.of(
                arguments("http://h/p", "z=1"),
                arguments("http://h/p?", "z=1"),
                arguments("http://h/p?a=1&a=2", "a=1&a=2&z=1"),
                // A parameter without "=" in the URI gets one
                arguments("http://h/p?a", "a=&z=1"),
                arguments("http://h/p?a=", "a=&z=1"),
                arguments("http://h/p?a=b=c", "a=b%3Dc&z=1"),
                arguments("http://h/p?=b", "=b&z=1"),
                // + and % aren't decoded, so they are encoded again
                arguments("http://h/p?a=b+c&d=%20", "a=b%2Bc&d=%2520&z=1"),
                arguments("http://h/p?a=%C3%A4", "a=%25C3%25A4&z=1"),
                arguments("http://h/p?a=" + A_UML, "a=%C3%A4&z=1"),
                // Only & separates parameters, and a leading or trailing & is dropped, but an empty one in between is kept
                arguments("http://h/p?a=1;b=2", "a=1%3Bb%3D2&z=1"),
                arguments("http://h/p?&a=1&&b=2&", "a=1&=&b=2&z=1"));
    }

    @Test
    void added_params_keep_the_fragment() throws Exception {
        URIBuilder builder = builder("http://h/p?a=1#f", true, "UTF-8").addQueryParams(singletonMap("b", new NoParameterValue()));

        assertThat(builder.toString()).isEqualTo("http://h/p?a=1&b#f");
    }

    @Test
    void added_params_are_sent_as_is_when_url_encoding_is_disabled() throws Exception {
        URIBuilder builder = builder("http://h/p?a=b+c&d=%20", false, "UTF-8").addQueryParams(singletonMap("e", "f%20g"));

        assertThat(builder.toURI().getRawQuery()).isEqualTo("a=b+c&d=%20&e=f%20g");
    }

    @Test
    void get_query_groups_raw_values_by_name() throws Exception {
        Map<String, Object> query = builder("http://h/p?b=2&a=1&b=%20&c&b=x+y", true, "UTF-8").getQuery();

        assertThat(query).containsExactly(
                Map.entry("a", "1"),
                Map.entry("b", Arrays.asList("2", "%20", "x+y")),
                new SimpleEntry<>("c", null));
    }

    @Test
    void has_query_param_is_false_for_a_param_without_value() throws Exception {
        URIBuilder builder = builder("http://h/p?a=1&b&c=", true, "UTF-8");

        assertThat(builder.hasQueryParam("a")).isTrue();
        assertThat(builder.hasQueryParam("b")).isFalse();
        assertThat(builder.hasQueryParam("c")).isTrue();
        assertThat(builder.hasQueryParam("d")).isFalse();
    }

    @Test
    void remove_query_param_removes_the_first_param_with_the_name_and_encodes_the_rest_again() throws Exception {
        URIBuilder builder = builder("http://h/p?a=1&b=%20&a=2", true, "UTF-8").removeQueryParam("a");

        assertThat(builder.toURI().getRawQuery()).isEqualTo("b=%2520&a=2");
        assertThatThrownBy(() -> builder.removeQueryParam("x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Param 'x' not found");
    }

    private static URIBuilder builder(String uri, boolean urlEncodingEnabled, String queryCharset) {
        EncoderConfig config = encoderConfig().defaultQueryParameterCharset(queryCharset);
        return new URIBuilder(URI.create(uri), urlEncodingEnabled, config);
    }
}

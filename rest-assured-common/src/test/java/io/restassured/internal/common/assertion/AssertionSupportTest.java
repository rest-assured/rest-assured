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

package io.restassured.internal.common.assertion;

import org.junit.jupiter.api.Test;

import static io.restassured.internal.common.assertion.AssertionSupport.attributeGetter;
import static io.restassured.internal.common.assertion.AssertionSupport.classKeyword;
import static io.restassured.internal.common.assertion.AssertionSupport.colon;
import static io.restassured.internal.common.assertion.AssertionSupport.doubleStar;
import static io.restassured.internal.common.assertion.AssertionSupport.escapePath;
import static io.restassured.internal.common.assertion.AssertionSupport.generateWhitespace;
import static io.restassured.internal.common.assertion.AssertionSupport.hyphen;
import static io.restassured.internal.common.assertion.AssertionSupport.integer;
import static io.restassured.internal.common.assertion.AssertionSupport.properties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Pins the path escaping that json-path and xml-path rely on.
 */
class AssertionSupportTest {

    // Same escapers, in the same order, as JSONAssertion
    private static Object json(String key) {
        return escapePath(key, escaper(hyphen()), escaper(attributeGetter()), escaper(integer()), escaper(classKeyword()));
    }

    // Same escapers, in the same order, as XMLAssertion
    private static Object xml(String key) {
        return escapePath(key, escaper(hyphen()), escaper(attributeGetter()), escaper(doubleStar()), escaper(colon()), escaper(classKeyword()));
    }

    private static PathFragmentEscaper escaper(Object escaper) {
        return (PathFragmentEscaper) escaper;
    }

    @Test
    void escapes_hyphenated_fragments() {
        assertThat(json("some-thing"), is("'some-thing'"));
        assertThat(json("a.some-thing.b"), is("a.'some-thing'.b"));
        assertThat(json("a-b.c-d"), is("'a-b'.'c-d'"));
        assertThat(xml("some-thing"), is("'some-thing'"));
    }

    @Test
    void escapes_hyphenated_fragments_but_keeps_index_lookups_outside_the_quotes() {
        assertThat(json("some-list[0]"), is("'some-list'[0]"));
        assertThat(json("some-list[0][1]"), is("'some-list'[0][1]"));
        assertThat(json("a.some-list[0].b"), is("a.'some-list'[0].b"));
        assertThat(json("some-list[0..-1]"), is("'some-list'[0..-1]"));
        assertThat(json("some-list[0..-1][0]"), is("'some-list'[0..-1][0]"));
    }

    @Test
    void hyphen_escaper_only_splits_out_the_index_when_the_name_is_longer_than_one_character() {
        assertThat(json("a-[0]"), is("'a-'[0]"));
        assertThat(json("-[0]"), is("'-[0]'"));
        assertThat(((PathFragmentEscaper) hyphen()).escape("a[0]"), is("'a[0]'"));
        assertThat(((PathFragmentEscaper) hyphen()).escape("ab[0]"), is("'ab'[0]"));
        assertThat(((PathFragmentEscaper) hyphen()).escape("ab[0] "), is("'ab'[0] "));
    }

    @Test
    void does_not_escape_hyphens_inside_index_lookups() {
        assertThat(json("list[-1]"), is("list[-1]"));
        assertThat(json("a.list[-1].b"), is("a.list[-1].b"));
        assertThat(json("list[0..-1]"), is("list[0..-1]"));
    }

    @Test
    void does_not_escape_fragments_inside_closures_or_method_calls() {
        assertThat(json("x.find { it.a-b == 1 }"), is("x.find { it.a-b == 1 }"));
        assertThat(json("x.collect(it-1)"), is("x.collect(it-1)"));
        assertThat(json("x.findAll { it.class == 1 }"), is("x.findAll { it.class == 1 }"));
        assertThat(xml("x.find { it.ns:a == 1 }"), is("x.find { it.ns:a == 1 }"));
    }

    @Test
    void leaves_quoted_fragments_alone() {
        assertThat(json("'already-quoted'.b"), is("'already-quoted'.b"));
        assertThat(json("a.'b-c'.d-e"), is("a.'b-c'.'d-e'"));
        assertThat(json("x.find { it.name == 'some-name' }.y-z"), is("x.find { it.name == 'some-name' }.'y-z'"));
    }

    @Test
    void trims_fragments_that_are_escaped() {
        assertThat(json("a. b-c"), is("a.'b-c'"));
        assertThat(json("a. b"), is("a. b"));
    }

    @Test
    void escapes_attribute_getters() {
        assertThat(json("@attr"), is("'@attr'"));
        assertThat(json("a.@attr"), is("a.'@attr'"));
        assertThat(xml("a.@attr"), is("a.'@attr'"));
        assertThat(xml("a.@some-attr"), is("a.'@some-attr'"));
        assertThat(json("a.@at tr"), is("a.@at tr"));
        assertThat(json("a.@at{tr"), is("a.@at{tr"));
    }

    @Test
    void escapes_double_star_in_xml_paths() {
        assertThat(xml("**"), is("'**'"));
        assertThat(xml("**.find { it.@type == 'x' }"), is("'**'.find { it.@type == 'x' }"));
        assertThat(xml("a.**.b"), is("a.'**'.b"));
        assertThat(json("**"), is("**"));
        assertThat(xml("***"), is("***"));
    }

    @Test
    void escapes_colons_in_xml_paths() {
        assertThat(xml("ns:tag"), is("'ns:tag'"));
        assertThat(xml("a.ns:tag[0]"), is("a.'ns:tag'[0]"));
        assertThat(xml("a.ns:tag(0)"), is("a.ns:tag(0)"));
        assertThat(json("ns:tag"), is("ns:tag"));
    }

    @Test
    void escapes_integer_fragments_in_json_paths() {
        assertThat(json("0"), is("'0'"));
        assertThat(json("a.1.b"), is("a.'1'.b"));
        assertThat(json("1abc"), is("'1abc'"));
        assertThat(json("a[1]"), is("a[1]"));
        assertThat(json("1 2"), is("1 2"));
        assertThat(xml("1"), is("1"));
    }

    @Test
    void escapes_class_keyword_with_get_at() {
        assertThat(json("class"), is("getAt('class')"));
        assertThat(json("a.class.b"), is("a.getAt('class').b"));
        assertThat(json("classes"), is("getAt('classes')"));
        assertThat(xml("a.class"), is("a.getAt('class')"));
        assertThat(json("my-class"), is("'my-class'"));
        assertThat(json("class[0]"), is("class[0]"));
    }

    @Test
    void joins_fragments_that_start_with_an_index_lookup_without_a_dot() {
        assertThat(json("a.[0]"), is("a[0]"));
        assertThat(json("a.b.[0].c"), is("a.b[0].c"));
    }

    @Test
    void handles_edge_case_paths() {
        assertThat(json(""), is(""));
        assertThat(json("a."), is("a"));
        assertThat(json(".a"), is(".a"));
        assertThat(json("$"), is("$"));
        assertThat(json("."), is(""));
        assertThat(json(".."), is(""));
        assertThat(json("a.."), is("a"));
        assertThat(json("'"), is("'"));
        assertThat(json("''"), is("''"));
        assertThat(json("  a-b"), is("'a-b'"));
        assertThat(json("a.b- c"), is("a.'b- c'"));
        assertThat(json("1.5"), is("'1'.'5'"));
        assertThat(xml("1.5"), is("1.5"));
        assertThat(escapePath("a-b"), is("a-b"));
    }

    @Test
    void properties_escaper_quotes_properties_fragments() {
        PathFragmentEscaper properties = (PathFragmentEscaper) properties();
        assertThat(properties.shouldEscape("properties"), is(true));
        assertThat(properties.shouldEscape("my-properties"), is(true));
        assertThat(properties.shouldEscape("'properties'"), is(false));
        assertThat(properties.shouldEscape("properties[0]"), is(false));
        assertThat(properties.shouldEscape("properties x"), is(false));
        assertThat(properties.shouldEscape("other"), is(false));
        assertThat(properties.escape("properties"), is("'properties'"));
        assertThat(escapePath("a.properties.b", properties), is("a.'properties'.b"));
    }

    @Test
    void escaper_types() {
        assertThat(hyphen() instanceof HyphenQuoteFragmentEscaper, is(true));
        assertThat(colon() instanceof HyphenQuoteFragmentEscaper, is(true));
        assertThat(properties() instanceof EndToEndQuoteFragmentEscaper, is(true));
        assertThat(attributeGetter() instanceof EndToEndQuoteFragmentEscaper, is(true));
        assertThat(doubleStar() instanceof EndToEndQuoteFragmentEscaper, is(true));
        assertThat(integer() instanceof EndToEndQuoteFragmentEscaper, is(true));
        assertThat(classKeyword() instanceof GetAtPathFragmentEscaper, is(true));
    }

    @Test
    void generates_whitespace() {
        assertThat(generateWhitespace(-1), is(""));
        assertThat(generateWhitespace(0), is(""));
        assertThat(generateWhitespace(1), is(" "));
        assertThat(generateWhitespace(3), is("   "));
    }
}

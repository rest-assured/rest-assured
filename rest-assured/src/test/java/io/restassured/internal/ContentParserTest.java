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
package io.restassured.internal;

import io.restassured.builder.ResponseBuilder;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.XmlConfig;
import io.restassured.internal.assertion.BodyMatcher;
import io.restassured.parsing.Parser;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Characterizes how {@link ContentParser} turns a response body into the object that the body matchers evaluate
 * GPath expressions against, for both the eager ({@code parseAsString = true}) and streaming path.
 */
class ContentParserTest {

    private static final String NAMESPACE_XML = "<foo xmlns:ns=\"http://localhost/\"><bar>sudo </bar><ns:bar>make me a sandwich!</ns:bar></foo>";
    private static final String DTD_XML = "<?xml version=\"1.0\"?><!DOCTYPE greeting [<!ELEMENT greeting (#PCDATA)>]><greeting>Hello</greeting>";

    // JSON

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void json_numbers_are_floats_by_default(boolean parseAsString) {
        assertMatches("application/json", "{\"n\": 1.5, \"i\": 2, \"l\": 12345678901, \"b\": 123456789012345678901234567890}",
                RestAssuredConfig.config(), parseAsString, "n", equalTo(1.5f));
        assertMatches("application/json", "{\"i\": 2}", RestAssuredConfig.config(), parseAsString, "i", equalTo(2));
        assertMatches("application/json", "{\"l\": 12345678901}", RestAssuredConfig.config(), parseAsString, "l", equalTo(12345678901L));
        assertMatches("application/json", "{\"b\": 123456789012345678901234567890}", RestAssuredConfig.config(), parseAsString, "b",
                equalTo(new BigInteger("123456789012345678901234567890")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void json_number_return_type_is_taken_from_json_config(boolean parseAsString) {
        RestAssuredConfig bigDecimal = RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL));
        RestAssuredConfig doubles = RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.DOUBLE));

        assertMatches("application/json", "{\"n\": 1.5}", bigDecimal, parseAsString, "n", equalTo(new BigDecimal("1.5")));
        assertMatches("application/json", "{\"n\": 1.5}", doubles, parseAsString, "n", equalTo(1.5d));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void json_body_is_decoded_with_charset_from_content_type(boolean parseAsString) {
        byte[] body = "{\"s\": \"åäö\"}".getBytes(StandardCharsets.ISO_8859_1);

        Object parsed = parse("application/json; charset=ISO-8859-1", body, RestAssuredConfig.config(), parseAsString);

        assertThat(validate(response("application/json; charset=ISO-8859-1", body), parsed, RestAssuredConfig.config(), "s",
                equalTo("åäö")).get("success")).isEqualTo(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void json_body_without_charset_is_decoded_with_decoder_config_default(boolean parseAsString) {
        byte[] body = "{\"s\": \"åäö\"}".getBytes(StandardCharsets.UTF_8);

        assertMatches("application/json", body, RestAssuredConfig.config(), parseAsString, "s", equalTo("åäö"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void json_arrays_are_parsed_to_lists(boolean parseAsString) {
        assertMatches("application/json", "[1, 2]", RestAssuredConfig.config(), parseAsString, "", contains(1, 2));
    }

    @Test
    void empty_json_body_is_parsed_to_null_when_streaming() {
        assertThat(parse("application/json", new byte[0], RestAssuredConfig.config(), false)).isNull();
    }

    @Test
    void empty_json_body_throws_when_parsed_as_string() {
        assertThatThrownBy(() -> parse("application/json", new byte[0], RestAssuredConfig.config(), true))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The JSON input text should neither be null nor empty.");
    }

    // XML

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void xml_body_is_parsed(boolean parseAsString) {
        assertMatches("application/xml", "<a><b>x</b><b>y</b></a>", RestAssuredConfig.config(), parseAsString, "a.b", contains("x", "y"));
        assertMatches("application/xml", "<a><b id=\"1\">x</b></a>", RestAssuredConfig.config(), parseAsString, "a.b.@id", equalTo("1"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void xml_namespaces_are_resolved_when_declared(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().declareNamespace("ns", "http://localhost/"));

        assertMatches("application/xml", NAMESPACE_XML, config, parseAsString, "foo.bar.text()", equalTo("sudo make me a sandwich!"));
        assertMatches("application/xml", NAMESPACE_XML, config, parseAsString, ":foo.:bar.text()", equalTo("sudo "));
        assertMatches("application/xml", NAMESPACE_XML, config, parseAsString, "foo.ns:bar.text()", equalTo("make me a sandwich!"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void xml_namespaces_are_not_resolved_when_not_declared(boolean parseAsString) {
        assertMatches("application/xml", NAMESPACE_XML, RestAssuredConfig.config(), parseAsString, "foo.bar.text()", equalTo("sudo make me a sandwich!"));
        assertMatches("application/xml", NAMESPACE_XML, RestAssuredConfig.config(), parseAsString, "foo.ns:bar.text()", equalTo(""));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void prefixes_are_part_of_element_names_when_not_namespace_aware(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(
                XmlConfig.xmlConfig().namespaceAware(false).declareNamespace("ns", "http://localhost/"));

        // Without namespace awareness the prefix is part of the element name
        assertMatches("application/xml", NAMESPACE_XML, config, parseAsString, "foo.bar.text()", equalTo("sudo make me a sandwich!"));
        assertMatches("application/xml", NAMESPACE_XML, config, parseAsString, "foo.ns:bar.text()", equalTo("make me a sandwich!"));
        assertMatches("application/xml", NAMESPACE_XML, config, parseAsString, "foo.'ns:bar'.text()", equalTo("make me a sandwich!"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void doctype_declaration_is_rejected_by_default_with_the_original_sax_exception(boolean parseAsString) {
        assertThatThrownBy(() -> parse("application/xml", DTD_XML.getBytes(StandardCharsets.UTF_8), RestAssuredConfig.config(), parseAsString))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("DOCTYPE");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void doctype_declaration_is_accepted_when_allowed(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().allowDocTypeDeclaration(true));

        assertMatches("application/xml", DTD_XML, config, parseAsString, "greeting", equalTo("Hello"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validating_parser_accepts_document_valid_against_its_dtd(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().allowDocTypeDeclaration(true).validating(true));

        assertMatches("application/xml", DTD_XML, config, parseAsString, "greeting", equalTo("Hello"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void features_are_applied_after_the_constructor_settings(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().allowDocTypeDeclaration(true)
                .feature("http://apache.org/xml/features/disallow-doctype-decl", true));

        assertThatThrownBy(() -> parse("application/xml", DTD_XML.getBytes(StandardCharsets.UTF_8), config, parseAsString))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("DOCTYPE");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unknown_feature_fails_with_the_original_sax_exception(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().feature("urn:unknown-feature", true));

        assertThatThrownBy(() -> parse("application/xml", "<a/>".getBytes(StandardCharsets.UTF_8), config, parseAsString))
                .isExactlyInstanceOf(SAXNotRecognizedException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unknown_property_fails_with_the_original_sax_exception(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().property("urn:unknown-property", "x"));

        assertThatThrownBy(() -> parse("application/xml", "<a/>".getBytes(StandardCharsets.UTF_8), config, parseAsString))
                .isExactlyInstanceOf(SAXNotRecognizedException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void properties_are_applied(boolean parseAsString) {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig()
                .property("http://javax.xml.XMLConstants/property/accessExternalDTD", ""));

        assertMatches("application/xml", "<a><b>x</b></a>", config, parseAsString, "a.b", equalTo("x"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void malformed_xml_fails_with_the_original_sax_exception(boolean parseAsString) {
        assertThatThrownBy(() -> parse("application/xml", "<a><b></a>".getBytes(StandardCharsets.UTF_8), RestAssuredConfig.config(), parseAsString))
                .isInstanceOf(SAXParseException.class);
    }

    // HTML

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void html_body_is_parsed_leniently(boolean parseAsString) {
        String html = "<html><head><title>T</title></head><body><p>hello<br></p><p>world</p></body></html>";

        assertMatches("text/html", html, RestAssuredConfig.config(), parseAsString, "html.head.title", equalTo("T"));
        assertMatches("text/html", html, RestAssuredConfig.config(), parseAsString, "html.body.p.size()", equalTo(2));
        assertMatches("text/html", html, RestAssuredConfig.config(), parseAsString, "html.body.p[0].text()", equalTo("hello"));
        assertMatches("text/html", html, RestAssuredConfig.config(), parseAsString, "html.body.p[1]", equalTo("world"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void html_with_doctype_is_parsed_even_when_doctype_is_not_allowed(boolean parseAsString) {
        String html = "<!DOCTYPE html><html><body><p>hello</p></body></html>";

        assertMatches("text/html", html, RestAssuredConfig.config(), parseAsString, "html.body.p", equalTo("hello"));
    }

    // Other content types

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/octet-stream"})
    void non_path_content_types_are_returned_as_input_stream(String contentType) {
        Object parsed = parse(contentType, "abc".getBytes(StandardCharsets.UTF_8), RestAssuredConfig.config(), true);

        assertThat(parsed).isInstanceOf(InputStream.class);
    }

    @Test
    void unknown_content_type_with_default_parser_uses_the_default_parser() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerDefaultParser(Parser.JSON);
        Response response = response("application/unknown", "{\"a\": 1}".getBytes(StandardCharsets.UTF_8));

        Object parsed = new ContentParser().parse(response, rpr, RestAssuredConfig.config(), true);

        assertThat(parsed).isInstanceOf(Map.class);
        assertThat(validate(rpr, response, parsed, RestAssuredConfig.config(), "a", equalTo(1)).get("success")).isEqualTo(true);
    }

    @Test
    void registered_parser_for_custom_content_type_is_used() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("application/custom", Parser.XML);
        Response response = response("application/custom", "<a>b</a>".getBytes(StandardCharsets.UTF_8));

        Object parsed = new ContentParser().parse(response, rpr, RestAssuredConfig.config(), true);

        assertThat(validate(rpr, response, parsed, RestAssuredConfig.config(), "a", equalTo("b")).get("success")).isEqualTo(true);
    }

    @Test
    void xml_is_parsed_to_a_gpath_result() {
        Object parsed = parse("application/xml", "<a>b</a>".getBytes(StandardCharsets.UTF_8), RestAssuredConfig.config(), true);

        assertThat(parsed.getClass().getName()).isEqualTo("groovy.xml.slurpersupport.NodeChild");
    }

    private static void assertMatches(String contentType, String body, RestAssuredConfig config, boolean parseAsString, String path, Matcher<?> matcher) {
        assertMatches(contentType, body.getBytes(StandardCharsets.UTF_8), config, parseAsString, path, matcher);
    }

    private static void assertMatches(String contentType, byte[] body, RestAssuredConfig config, boolean parseAsString, String path, Matcher<?> matcher) {
        Response response = response(contentType, body);
        Object parsed = new ContentParser().parse(response, new ResponseParserRegistrar(), config, parseAsString);
        Map<String, Object> result = validate(response, parsed, config, path, matcher);
        assertThat(result.get("success")).as(String.valueOf(result.get("errorMessage"))).isEqualTo(true);
    }

    private static Object parse(String contentType, byte[] body, RestAssuredConfig config, boolean parseAsString) {
        return new ContentParser().parse(response(contentType, body), new ResponseParserRegistrar(), config, parseAsString);
    }

    private static Response response(String contentType, byte[] body) {
        return new ResponseBuilder().setStatusCode(200).setBody(body).setContentType(contentType).build();
    }

    private static Map<String, Object> validate(Response response, Object parsed, RestAssuredConfig config, String path, Matcher<?> matcher) {
        return validate(new ResponseParserRegistrar(), response, parsed, config, path, matcher);
    }

    private static Map<String, Object> validate(ResponseParserRegistrar rpr, Response response, Object parsed, RestAssuredConfig config, String path, Matcher<?> matcher) {
        BodyMatcher bodyMatcher = new BodyMatcher();
        bodyMatcher.setKey(path);
        bodyMatcher.setMatcher(matcher);
        bodyMatcher.setRpr(rpr);
        return bodyMatcher.validate(response, parsed, config);
    }
}

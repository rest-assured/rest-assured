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

package io.restassured.assertion;

import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.path.json.JSONAssertion;
import io.restassured.internal.path.xml.XMLAssertion;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StreamVerifierTest {

    private static Response response(String contentType) {
        Response response = mock(Response.class);
        when(response.getContentType()).thenReturn(contentType);
        when(response.asString()).thenReturn("the content");
        return response;
    }

    private static Object keyOf(Object assertion) throws Exception {
        Field field = assertion.getClass().getDeclaredField("key");
        field.setAccessible(true);
        return field.get(assertion);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json; charset=UTF-8", "application/vnd.api+json"})
    public void creates_json_assertion_for_json_content_types(String contentType) throws Exception {
        Object assertion = StreamVerifier.newAssertion(response(contentType), "a.b", new ResponseParserRegistrar());

        assertThat(assertion).isExactlyInstanceOf(JSONAssertion.class);
        assertThat(keyOf(assertion)).isEqualTo("a.b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/xml", "text/xml; charset=UTF-8", "application/atom+xml", "text/html", "application/xhtml+xml"})
    public void creates_xml_assertion_for_xml_and_html_content_types(String contentType) throws Exception {
        Object assertion = StreamVerifier.newAssertion(response(contentType), "a.b", new ResponseParserRegistrar());

        assertThat(assertion).isExactlyInstanceOf(XMLAssertion.class);
        assertThat(keyOf(assertion)).isEqualTo("a.b");
    }

    @Test
    public void uses_parser_registered_for_content_type() throws Exception {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("application/custom", Parser.JSON);
        rpr.registerParser("application/other", Parser.HTML);

        assertThat(StreamVerifier.newAssertion(response("application/custom; charset=UTF-8"), "k", rpr)).isExactlyInstanceOf(JSONAssertion.class);
        assertThat(StreamVerifier.newAssertion(response("application/other"), "k", rpr)).isExactlyInstanceOf(XMLAssertion.class);
    }

    @Test
    public void registered_parser_takes_precedence_over_content_type() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("application/json", Parser.XML);

        assertThat(StreamVerifier.newAssertion(response("application/json"), "k", rpr)).isExactlyInstanceOf(XMLAssertion.class);
    }

    @Test
    public void default_parser_takes_precedence_over_content_type() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerDefaultParser(Parser.XML);

        assertThat(StreamVerifier.newAssertion(response("application/json"), "k", rpr)).isExactlyInstanceOf(XMLAssertion.class);
        assertThat(StreamVerifier.newAssertion(response("application/unknown"), "k", rpr)).isExactlyInstanceOf(XMLAssertion.class);
        assertThat(StreamVerifier.newAssertion(response(null), "k", rpr)).isExactlyInstanceOf(XMLAssertion.class);
    }

    @Test
    public void returns_null_when_custom_parser_is_text() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerDefaultParser(Parser.TEXT);

        assertThat(StreamVerifier.newAssertion(response("application/json"), "k", rpr)).isNull();
    }

    @Test
    public void converts_non_string_key_to_string_the_groovy_way() throws Exception {
        assertThat(keyOf(StreamVerifier.newAssertion(response("application/json"), 5, new ResponseParserRegistrar()))).isEqualTo("5");
        assertThat(keyOf(StreamVerifier.newAssertion(response("application/xml"), Arrays.asList("a", 1), new ResponseParserRegistrar()))).isEqualTo("[a, 1]");
        assertThat(keyOf(StreamVerifier.newAssertion(response("application/json"), null, new ResponseParserRegistrar()))).isNull();
    }

    @Test
    public void create_assertion_for_custom_parser_uses_non_default_parser() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("application/custom", Parser.XML);

        assertThat(StreamVerifier.createAssertionForCustomParser(rpr, "application/custom", "k")).isExactlyInstanceOf(XMLAssertion.class);
        assertThat(StreamVerifier.createAssertionForCustomParser(rpr, "application/unregistered", "k")).isNull();
    }

    @Test
    public void throws_illegal_state_exception_when_content_type_is_empty() {
        assertThatThrownBy(() -> StreamVerifier.newAssertion(response(""), "k", new ResponseParserRegistrar()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Expected response body to be verified as JSON, HTML or XML but no content-type was defined in the response.\n" +
                        "Try registering a default parser using:\n" +
                        "   RestAssured.defaultParser(<parser type>);\n" +
                        "Content was:\n" +
                        "the content\n");
    }

    @Test
    public void throws_illegal_state_exception_when_content_type_is_not_supported() {
        assertThatThrownBy(() -> StreamVerifier.newAssertion(response("text/plain; charset=UTF-8"), "k", new ResponseParserRegistrar()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Expected response body to be verified as JSON, HTML or XML but content-type 'text/plain' is not supported out of the box.\n" +
                        "Try registering a custom parser using:\n" +
                        "   RestAssured.registerParser(\"text/plain\", <parser type>);\n" +
                        "Content was:\n" +
                        "the content\n");
    }

    @Test
    public void reports_null_content_type_as_not_supported() {
        assertThatThrownBy(() -> StreamVerifier.newAssertion(response(null), "k", new ResponseParserRegistrar()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Expected response body to be verified as JSON, HTML or XML but content-type 'null' is not supported out of the box.\n" +
                        "Try registering a custom parser using:\n" +
                        "   RestAssured.registerParser(\"null\", <parser type>);\n" +
                        "Content was:\n" +
                        "the content\n");
    }
}

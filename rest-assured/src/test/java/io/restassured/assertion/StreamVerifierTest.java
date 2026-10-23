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
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.path.PathAssertion;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json; charset=UTF-8", "application/vnd.api+json"})
    public void creates_json_assertion_for_json_content_types(String contentType) {
        PathAssertion assertion = StreamVerifier.newAssertion(response(contentType), "a.b", new ResponseParserRegistrar());

        assertThat(assertion.documentType()).isEqualTo(DocumentType.JSON);
        assertThat(assertion.key()).isEqualTo("a.b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/xml", "text/xml; charset=UTF-8", "application/atom+xml", "application/xhtml+xml"})
    public void creates_xml_assertion_for_xml_content_types(String contentType) {
        PathAssertion assertion = StreamVerifier.newAssertion(response(contentType), "a.b", new ResponseParserRegistrar());

        assertThat(assertion.documentType()).isEqualTo(DocumentType.XML);
        assertThat(assertion.key()).isEqualTo("a.b");
    }

    @Test
    public void creates_html_assertion_for_html_content_type() {
        PathAssertion assertion = StreamVerifier.newAssertion(response("text/html"), "a.b", new ResponseParserRegistrar());

        assertThat(assertion.documentType()).isEqualTo(DocumentType.HTML);
        assertThat(assertion.key()).isEqualTo("a.b");
    }

    @Test
    public void uses_parser_registered_for_content_type() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("application/custom", Parser.JSON);
        rpr.registerParser("application/other", Parser.HTML);

        assertThat(StreamVerifier.newAssertion(response("application/custom; charset=UTF-8"), "k", rpr)).extracting(PathAssertion::documentType).isEqualTo(DocumentType.JSON);
        assertThat(StreamVerifier.newAssertion(response("application/other"), "k", rpr)).extracting(PathAssertion::documentType).isEqualTo(DocumentType.HTML);
    }

    @Test
    public void registered_parser_takes_precedence_over_content_type() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("application/json", Parser.XML);

        assertThat(StreamVerifier.newAssertion(response("application/json"), "k", rpr)).extracting(PathAssertion::documentType).isEqualTo(DocumentType.XML);
    }

    @Test
    public void default_parser_takes_precedence_over_content_type() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerDefaultParser(Parser.XML);

        assertThat(StreamVerifier.newAssertion(response("application/json"), "k", rpr)).extracting(PathAssertion::documentType).isEqualTo(DocumentType.XML);
        assertThat(StreamVerifier.newAssertion(response("application/unknown"), "k", rpr)).extracting(PathAssertion::documentType).isEqualTo(DocumentType.XML);
        assertThat(StreamVerifier.newAssertion(response(null), "k", rpr)).extracting(PathAssertion::documentType).isEqualTo(DocumentType.XML);
    }

    @Test
    public void returns_null_when_custom_parser_is_text() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerDefaultParser(Parser.TEXT);

        assertThat(StreamVerifier.newAssertion(response("application/json"), "k", rpr)).isNull();
    }

    @Test
    public void converts_non_string_key_to_string_the_groovy_way() {
        assertThat(StreamVerifier.newAssertion(response("application/json"), 5, new ResponseParserRegistrar()).key()).isEqualTo("5");
        assertThat(StreamVerifier.newAssertion(response("application/xml"), Arrays.asList("a", 1), new ResponseParserRegistrar()).key()).isEqualTo("[a, 1]");
        assertThat(StreamVerifier.newAssertion(response("application/json"), null, new ResponseParserRegistrar()).key()).isNull();
    }

    @Test
    public void create_assertion_for_custom_parser_uses_non_default_parser() {
        ResponseParserRegistrar rpr = new ResponseParserRegistrar();
        rpr.registerParser("application/custom", Parser.XML);

        assertThat(StreamVerifier.createAssertionForCustomParser(rpr, "application/custom", "k")).extracting(PathAssertion::documentType).isEqualTo(DocumentType.XML);
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

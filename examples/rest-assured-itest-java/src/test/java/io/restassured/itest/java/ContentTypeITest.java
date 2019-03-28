/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;

public class ContentTypeITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void canValidateResponseContentType() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage("Expected content-type \"something\" doesn't match actual content-type \"application/json;charset=utf-8\".");

        expect().contentType("something").when().get("/hello");
    }

    @Test
    public void canValidateResponseContentTypeWithHamcrestMatcher() throws Exception {
        expect().contentType(is("application/json;charset=utf-8")).when().get("/hello");
    }

    @Test
    public void doesntAppendCharsetToContentTypeWhenContentTypeIsNotExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                body(new byte[]{42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/octet-stream"));
    }

    @Test
    public void doesntAppendCharsetToContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                contentType("application/zip").
                body(new byte[] {42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/zip"));
    }

    @Test
    public void appendsCharsetToContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8").appendDefaultContentCharsetToContentTypeIfUndefined(true))).
                contentType("application/zip").
                body(new byte[] {42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/zip; charset=UTF-8"));
    }

    @Test
    public void appendsJavaNioCharsetToContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset(Charset.forName("UTF-8")).appendDefaultContentCharsetToContentTypeIfUndefined(true))).
                contentType("application/zip").
                body(new byte[] {42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/zip; charset=UTF-8"));
    }

    @Test
    public void appendCharsetToContentTypeWhenContentTypeIsNotExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(true))).
                body(new byte[] {42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/octet-stream; charset=ISO-8859-1"));
    }

    @Test
    public void doesntAppendCharsetToNonStreamingContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                contentType("application/vnd.com.example-v1+json").
                body("something").
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/vnd.com.example-v1+json"));
    }

    @Test
    public void appendsCharsetToNonStreamingContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(true))).
                contentType("application/vnd.com.example-v1+xml").
                body("something").
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/vnd.com.example-v1+xml; charset=ISO-8859-1"));
    }

    @Test
    public void doesntOverrideDefinedCharsetForNonStreamingContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(true))).
                contentType("application/vnd.com.example-v1+json; charSet=UTF-16").
                body("something").
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/vnd.com.example-v1+json; charSet=UTF-16"));
    }

    @Test public void
    content_type_is_sent_to_the_server_when_using_a_get_request() {
        given().
                param("foo", "bar").
                contentType(ContentType.XML.withCharset("utf-8")).
        when().
                get("/contentTypeAsBody").
        then().
                body(equalTo(ContentType.XML.withCharset("utf-8")));
    }

    @Test public void
    no_content_type_is_sent_by_default_when_using_get_request() {
        given().
                param("foo", "bar").
        when().
                get("/contentTypeAsBody").
        then().
                body(equalTo("null"));

    }

    @Test public void
    content_type_is_sent_to_the_server_when_using_a_post_request() {
        given().
                config(config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                param("foo", "bar").
                contentType(ContentType.XML).
        when().
                post("/contentTypeAsBody").
        then().
                body(equalTo(ContentType.XML.toString()));
    }

    @Test public void
    content_type_is_application_x_www_form_urlencoded_with_default_charset_when_no_content_type_is_specified_for_post_requests() {
        given().
                param("foo", "bar").
        when().
                post("/contentTypeAsBody").
        then().
                body(equalTo(ContentType.URLENC.withCharset(config().getEncoderConfig().defaultContentCharset())));
    }

    @Test public void
    content_type_is_text_plain_with_default_charset_when_no_content_type_is_specified_for_put_requests() {
        given().
                param("foo", "bar").
        when().
                put("/reflect").
        then().
                contentType(toJetty9(ContentType.TEXT.withCharset(config().getEncoderConfig().defaultContentCharset())));
    }

    @Test public void
    content_type_validation_is_case_insensitive() {
        // Since we provide no content-type (null) Scalatra will return a default content-type which is the
        // same as specified in config().getEncoderConfig().defaultContentCharset() but with charset as lower case.
        given().
                param("foo", "bar").
        when().
                get("/reflect").
        then().
                contentType(toJetty9(ContentType.TEXT.withCharset(config().getEncoderConfig().defaultContentCharset())));
    }

    @Test public void
    header_with_content_type_enum_works() throws Exception {
        given().
                header("Content-Type", ContentType.JSON).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo(ContentType.JSON.withCharset(config().getEncoderConfig().defaultCharsetForContentType(ContentType.JSON))));
    }

    @Test public void
    encoder_config_can_specify_a_default_charset_for_a_specific_content_type_using_enum() {
        given().
                config(config().encoderConfig(EncoderConfig.encoderConfig().defaultCharsetForContentType(StandardCharsets.ISO_8859_1.toString(), ContentType.JSON))).
                contentType(ContentType.JSON).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo(ContentType.JSON.withCharset(StandardCharsets.ISO_8859_1.toString())));
    }

    @Test public void
    encoder_config_can_specify_a_default_charset_for_a_specific_content_type_using_string() {
        given().
                config(config().encoderConfig(EncoderConfig.encoderConfig().defaultCharsetForContentType(StandardCharsets.ISO_8859_1.toString(), "application/json"))).
                contentType(ContentType.JSON).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo(ContentType.JSON.withCharset(StandardCharsets.ISO_8859_1.toString())));
    }

    @Test public void
    when_form_param_are_supplied_with_a_get_request_the_content_type_is_automatically_set_to_form_encoded() {
        given().
                formParam("firstName", "John").
                formParam("lastName", "Doe").
        when().
                 get("/returnContentTypeAsBody").
        then().
                 body(equalTo(ContentType.URLENC.withCharset(config().getEncoderConfig().defaultContentCharset())));
    }

    @Test public void
    when_form_param_are_supplied_with_a_get_request_and_content_type_is_explicitly_defined_then_content_type_is_not_automatically_set_to_form_encoded() {
        given().
                formParam("firstName", "John").
                formParam("lastName", "Doe").
                contentType(ContentType.JSON).
        when().
                 get("/returnContentTypeAsBody").
        then().
                 body(equalTo(ContentType.JSON.withCharset(config().getEncoderConfig().defaultCharsetForContentType(ContentType.JSON))));
    }

    /**
     * Solves issue https://github.com/jayway/rest-assured/issues/574
     */
    @Test public void
    non_registered_content_type_starting_with_text_slash_is_encoded_as_text() {
        String uriList = "http://www.example.com/raindrops-on-roses\n" +
                "ftp://www.example.com/sleighbells\n" +
                "http://www.example.com/crisp-apple-strudel\n" +
                "http://www.example.com/doorbells\n" +
                "tag:foo@example.com,2012-07-01:bright-copper-kettles\n" +
                "urn:isbn:0-061-99881-8";

        given().
                contentType("text/uri-list").
                body(uriList).
        when().
                post("/textUriList").
        then().
                statusCode(200).
                body("uris.size()", is(6));
    }

    @Test public void
    non_registered_content_type_containing_plus_text_is_encoded_as_text() {
        String uriList = "http://www.example.com/raindrops-on-roses\n" +
                "ftp://www.example.com/sleighbells\n" +
                "http://www.example.com/crisp-apple-strudel\n" +
                "http://www.example.com/doorbells\n" +
                "tag:foo@example.com,2012-07-01:bright-copper-kettles\n" +
                "urn:isbn:0-061-99881-8";

        given().
                contentType("application/uri-list+text").
                body(uriList).
        when().
                post("/textUriList").
        then().
                statusCode(200).
                body("uris.size()", is(6));
    }

    @Test public void
    custom_registered_encoding_of_content_type_is_applied_through_encoder_config() {
        String uriList = "http://www.example.com/raindrops-on-roses\n" +
                "ftp://www.example.com/sleighbells\n" +
                "http://www.example.com/crisp-apple-strudel\n" +
                "http://www.example.com/doorbells\n" +
                "tag:foo@example.com,2012-07-01:bright-copper-kettles\n" +
                "urn:isbn:0-061-99881-8";

        given().
                config(config().encoderConfig(EncoderConfig.encoderConfig().encodeContentTypeAs("my-text", ContentType.TEXT))).
                contentType("my-text").
                body(uriList).
        when().
                post("/textUriList").
        then().
                statusCode(200).
                body("uris.size()", is(6));
    }

    @Test public void
    shows_a_nice_error_message_when_failed_to_encode_content() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Don't know how to encode encode as a byte stream.\n\n" +
                "Please use EncoderConfig (EncoderConfig#encodeContentTypeAs) to specify how to serialize data for this content-type.\n" +
                "For example: \"given().config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs(\"my-text\", ContentType.TEXT))). ..");

        given().
                contentType("my-text").
                body("encode").
        when().
                post("/textUriList");
    }

    @Test public void
    validates_content_type_even_when_it_is_a_204_response() {
        given().post("/return204WithContentType")
                .then()
                .statusCode(204)
                .contentType(ContentType.JSON);
    }

    @Test public void
    validates_content_type_json() {
        given().contentType(ContentType.JSON)
                .get("/contentTypeAsContentType")
                .then()
                .contentType(ContentType.JSON);
    }

    @Test public void
    validates_content_type_binary() {
        given().contentType(ContentType.BINARY)
                .get("/contentTypeAsContentType")
                .then()
                .contentType(ContentType.BINARY);
    }

    @Test public void
    can_assert_empty_or_null_content_type() {
        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder().setStatusCode(200).setHeader("Some", "Value").setBody("Test").build()).
        when().
                get("/something").
        then().
                contentType(emptyOrNullString());
    }

    @Test public void
    ignores_spacing_between_content_type_and_charset_when_server_returns_single_space_between_content_type_and_charset() {
        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder()
                        .setStatusCode(200)
                        .setHeader("Content-Type", "application/json; charset=UTF-8")
                        .setHeader("Some", "Value")
                        .setBody("Test").build()).
        when().
                get("/something").
        then().
                contentType(ContentType.JSON.withCharset(UTF_8));
    }

    @Test public void
    ignores_spacing_between_content_type_and_charset_when_server_returns_multiple_spaces_between_content_type_and_charset() {
        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder()
                        .setStatusCode(200)
                        .setHeader("Content-Type", "application/json;    charset=UTF-8")
                        .setHeader("Some", "Value")
                        .setBody("Test").build()).
        when().
                get("/something").
        then().
                contentType(ContentType.JSON.withCharset(UTF_8));
    }

    @Test public void
    ignores_spacing_between_content_type_and_charset_when_server_returns_no_spaces_between_content_type_and_charset() {
        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder()
                        .setStatusCode(200)
                        .setHeader("Content-Type", "application/json;charset=UTF-8")
                        .setHeader("Some", "Value")
                        .setBody("Test").build()).
        when().
                get("/something").
        then().
                contentType(ContentType.JSON.withCharset(UTF_8));
    }

    @Test public void
    ignores_tabs_between_content_type_and_charset_when_server_returns_no_spaces_between_content_type_and_charset() {
        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder()
                        .setStatusCode(200)
                        .setHeader("Content-Type", "application/json;\t\tcharset=UTF-8")
                        .setHeader("Some", "Value")
                        .setBody("Test").build()).
        when().
                get("/something").
        then().
                contentType(ContentType.JSON.withCharset(UTF_8));
    }

    @Test public void
    doesnt_ignore_tabs_between_content_type_and_charset_when_server_returns_no_spaces_between_content_type_and_charset_but_when_using_equal_to_hamcrest_matcher() {
        given().
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder()
                        .setStatusCode(200)
                        .setHeader("Content-Type", "application/json;\t\tcharset=UTF-8")
                        .setHeader("Some", "Value")
                        .setBody("Test").build()).
        when().
                get("/something").
        then().
                contentType(not(equalTo(ContentType.JSON.withCharset(UTF_8))));
    }

    @Test public void
    doesnt_send_a_content_type_header_when_there_is_no_body() {
        when().
                get("/headersWithValues").
        then().
                body("containsKey('Content-Type')", is(false));
    }

    private String toJetty9(String charset) {
        return StringUtils.lowerCase(StringUtils.remove(charset, " "));
    }
}

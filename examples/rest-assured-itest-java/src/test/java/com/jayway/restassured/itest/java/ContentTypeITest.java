/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ContentTypeITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void canValidateResponseContentType() throws Exception {
        exception.expect(AssertionError.class);
        exception.expectMessage("Expected content-type \"something\" doesn't match actual content-type \"application/json; charset=UTF-8\".");

        expect().contentType("something").when().get("/hello");
    }

    @Test
    public void canValidateResponseContentTypeWithHamcrestMatcher() throws Exception {
        expect().contentType(is("application/json; charset=UTF-8")).when().get("/hello");
    }

    @Test
    public void doesntAppendCharsetToContentTypeWhenContentTypeIsNotExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                body(new byte[]{42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/octet-stream"));
    }

    @Test
    public void doesntAppendCharsetToContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
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
                config(RestAssured.config().encoderConfig(encoderConfig().defaultContentCharset("UTF-8").appendDefaultContentCharsetToContentTypeIfUndefined(true))).
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
                config(RestAssured.config().encoderConfig(encoderConfig().defaultContentCharset(Charset.forName("UTF-8")).appendDefaultContentCharsetToContentTypeIfUndefined(true))).
                contentType("application/zip").
                body(new byte[] {42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/zip; charset=UTF-8"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void appendCharsetToContentTypeWhenContentTypeIsNotExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(encoderConfig().appendDefaultContentCharsetToStreamingContentTypeIfUndefined(true))).
                body(new byte[] {42}).
        when().
                post("/returnContentTypeAsBody").
        then().
                body(equalTo("application/octet-stream; charset=ISO-8859-1"));
    }

    @Test
    public void doesntAppendCharsetToNonStreamingContentTypeWhenContentTypeIsExplicitlyDefinedAndEncoderConfigIsConfiguredAccordingly() throws Exception {
        given().
                config(RestAssured.config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
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
                config(RestAssured.config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(true))).
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
                config(RestAssured.config().encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(true))).
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
                get("/reflect").
        then().
                contentType(ContentType.XML.withCharset("utf-8"));
    }

    @Test public void
    content_type_is_text_plain_with_default_charset_when_no_content_type_is_specified_for_get_requests() {
        given().
                param("foo", "bar").
        when().
                get("/reflect").
        then().
                contentType(ContentType.TEXT.withCharset(config().getEncoderConfig().defaultContentCharset()));
    }

    @Test public void
    content_type_is_sent_to_the_server_when_using_a_post_request() {
        given().
                param("foo", "bar").
                contentType(ContentType.XML).
        when().
                post("/reflect").
        then().
                contentType(ContentType.XML);
    }

    @Test public void
    content_type_is_application_x_www_form_urlencoded_with_default_charset_when_no_content_type_is_specified_for_post_requests() {
        given().
                param("foo", "bar").
        when().
               post("/reflect").
        then().
                contentType(ContentType.URLENC.withCharset(config().getEncoderConfig().defaultContentCharset()));
    }

    @Test public void
    content_type_is_text_plain_with_default_charset_when_no_content_type_is_specified_for_put_requests() {
        given().
                param("foo", "bar").
        when().
                put("/reflect").
        then().
                contentType(ContentType.TEXT.withCharset(config().getEncoderConfig().defaultContentCharset()));
    }
}

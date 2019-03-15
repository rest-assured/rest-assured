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

package io.restassured.module.mockmvc;

import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.http.GreetingController;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentTypeTest {

    private static final String UTF_16 = "UTF-16";

    @Test public void
    adds_default_charset_to_content_type_by_default() {
        final AtomicReference<String> contentType = new AtomicReference<>();

        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                contentType(ContentType.JSON).
                interceptor(requestBuilder -> contentType.set(extractContentType(requestBuilder))).
        when().
                get("/greeting?name={name}", "Johan").
        then().
               statusCode(200);

        assertThat(contentType.get()).isEqualTo("application/json;charset=" + RestAssuredMockMvc.config().getEncoderConfig().defaultContentCharset());
    }

    @Test public void
    adds_specific_charset_to_content_type_by_default() {
        final AtomicReference<String> contentType = new AtomicReference<>();

        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                config(RestAssuredMockMvc.config().encoderConfig(EncoderConfig.encoderConfig().defaultCharsetForContentType(UTF_16, ContentType.JSON))).
                contentType(ContentType.JSON).
                interceptor(requestBuilder -> contentType.set(extractContentType(requestBuilder))).
        when().
                get("/greeting?name={name}", "Johan").
        then().
               statusCode(200);

        assertThat(contentType.get()).isEqualTo("application/json;charset=" + UTF_16);
        assertThat(contentType.get()).doesNotContain(RestAssuredMockMvc.config().getEncoderConfig().defaultContentCharset());
    }

    @Test public void
    doesnt_add_default_charset_to_content_type_if_charset_is_defined_explicitly() {
        final AtomicReference<String> contentType = new AtomicReference<>();

        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                contentType(ContentType.JSON.withCharset(UTF_16)).
                interceptor(requestBuilder -> contentType.set(extractContentType(requestBuilder))).
        when().
                get("/greeting?name={name}", "Johan").
        then().
               statusCode(200);

        assertThat(contentType.get()).isEqualTo("application/json;charset=UTF-16");
    }

    @Test public void
    doesnt_add_default_charset_to_content_type_if_configured_not_to_do_so() {
        final AtomicReference<String> contentType = new AtomicReference<>();

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvc.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                standaloneSetup(new GreetingController()).
                contentType(ContentType.JSON).
                interceptor(requestBuilder -> contentType.set(extractContentType(requestBuilder))).
        when().
                get("/greeting?name={name}", "Johan").
        then().
                statusCode(200);

        assertThat(contentType.get()).isEqualTo("application/json");
    }

    @Test public void
    doesnt_duplication_of_content_type_with_default_charset() {
        final List<String> contentTypes = new ArrayList<>();

        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                contentType(ContentType.JSON).
                interceptor(requestBuilder -> contentTypes.add(extractContentType(requestBuilder))).
        when().
                get("/greeting?name={name}", "Johan").
        then().
                statusCode(200);

        assertThat(contentTypes.size()).isEqualTo(1);
        assertThat(contentTypes.get(0)).isEqualTo("application/json;charset=ISO-8859-1");
    }

    @Test public void
    doesnt_duplication_of_content_type() {
        final List<String> contentTypes = new ArrayList<>();

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvc.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                standaloneSetup(new GreetingController()).
                contentType(ContentType.JSON).
                interceptor(requestBuilder -> contentTypes.add(extractContentType(requestBuilder))).
        when().
                get("/greeting?name={name}", "Johan").
        then().
                statusCode(200);

        assertThat(contentTypes.size()).isEqualTo(1);
        assertThat(contentTypes.get(0)).isEqualTo("application/json");
    }

    private String extractContentType(MockHttpServletRequestBuilder requestBuilder) {
        Object contentType = Whitebox.getInternalState(requestBuilder, "contentType");
        if (contentType instanceof String) {
            return (String) contentType;
        } else if (contentType instanceof char[]) {
            return String.valueOf(contentType);
        }
        throw new IllegalArgumentException("Failed to extract content-type from " + MockHttpServletRequestBuilder.class.getName());
    }
}
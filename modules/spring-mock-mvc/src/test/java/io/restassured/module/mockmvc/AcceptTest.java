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
import io.restassured.module.mockmvc.intercept.MockHttpServletRequestBuilderInterceptor;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

public class AcceptTest {

    @Test
    public void
    adds_accept_by_content_type() {
        final List<String> accept = new ArrayList<String>();

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvc.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                standaloneSetup(new GreetingController()).
                accept(ContentType.JSON).
                interceptor(new MockHttpServletRequestBuilderInterceptor() {
                    public void intercept(MockHttpServletRequestBuilder requestBuilder) {
                        MultiValueMap<String, Object> headers = Whitebox.getInternalState(requestBuilder, "headers");
                        for (Object header : headers.get("Accept")) {
                            accept.add(String.valueOf(header));
                        }
                    }
                }).
        when().
                get("/greeting").
        then().
                statusCode(200);

        assertThat(accept.get(0)).isEqualTo("application/json, application/javascript, text/javascript, text/json");
    }

    @Test
    public void
    adds_accept_by_string_value() {
        final List<String> accept = new ArrayList<String>();

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvc.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                standaloneSetup(new GreetingController()).
                accept("application/json, application/javascript").
                interceptor(new MockHttpServletRequestBuilderInterceptor() {
                    public void intercept(MockHttpServletRequestBuilder requestBuilder) {
                        MultiValueMap<String, Object> headers = Whitebox.getInternalState(requestBuilder, "headers");
                        for (Object header : headers.get("Accept")) {
                            accept.add(String.valueOf(header));
                        }
                    }
                }).
        when().
                get("/greeting").
        then().
                statusCode(200);

        assertThat(accept.get(0)).isEqualTo("application/json, application/javascript");
    }

    @Test
    public void
    adds_accept_by_media_type() {
        final List<String> accept = new ArrayList<String>();

        RestAssuredMockMvc.given().
                config(RestAssuredMockMvc.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                standaloneSetup(new GreetingController()).
                accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED).
                interceptor(new MockHttpServletRequestBuilderInterceptor() {
                    public void intercept(MockHttpServletRequestBuilder requestBuilder) {
                        MultiValueMap<String, Object> headers = Whitebox.getInternalState(requestBuilder, "headers");
                        for (Object header : headers.get("Accept")) {
                            accept.add(String.valueOf(header));
                        }
                    }
                }).
        when().
                get("/greeting").
        then().
                statusCode(200);

        assertThat(accept.get(0)).isEqualTo("application/json, application/x-www-form-urlencoded");
    }
}

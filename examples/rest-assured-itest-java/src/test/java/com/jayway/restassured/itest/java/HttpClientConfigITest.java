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
import com.jayway.restassured.builder.ResponseBuilder;
import com.jayway.restassured.config.HttpClientConfig;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.DecoderConfig.ContentDecoder.DEFLATE;
import static com.jayway.restassured.config.DecoderConfig.decoderConfig;
import static com.jayway.restassured.config.HttpClientConfig.httpClientConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.http.client.params.ClientPNames.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class HttpClientConfigITest extends WithJetty {

    @Test
    public void doesntFollowRedirectsIfSpecifiedInTheHttpClientConfig() throws Exception {
        final List<Header> httpClientHeaders = new ArrayList<Header>();
        httpClientHeaders.add(new BasicHeader("header1", "value1"));
        httpClientHeaders.add(new BasicHeader("header2", "value2"));
        RestAssured.config = newConfig().httpClient(httpClientConfig().setParam(DEFAULT_HEADERS, httpClientHeaders));
        try {
            given().
                    param("url", "/hello").
            expect().
                    statusCode(200).
                    header("header1", "value1").
                    header("header2", "value2").
            when().
                    get("/multiHeaderReflect");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void doesntThrowClientProtocolExceptionIfMaxNumberOfRedirectAreExceededInHttpClientConfigBecauseRedirectConfigHasPrecedence() throws Exception {
        RestAssured.config = newConfig().httpClient(httpClientConfig().setParam(HANDLE_REDIRECTS, true).and().setParam(MAX_REDIRECTS, 0));
        try {
            given().
                    param("url", "/hello").
            expect().
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void httpClientIsConfigurableFromANonStaticHttpClientConfig() {
        // Given
        final MutableObject<HttpClient> client = new MutableObject<>();

        // When
        given().
                config(newConfig().httpClient(HttpClientConfig.httpClientConfig().httpClientFactory(SystemDefaultHttpClient::new))).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        client.setValue(requestSpec.getHttpClient());
                        return new ResponseBuilder().setStatusCode(200).setContentType("application/json").setBody("{ \"message\" : \"hello\"}").build();
                    }
                }).
        expect().
                body("message", equalTo("hello")).
        when().
                get("/something");

        // Then
        assertThat(client.getValue(), instanceOf(SystemDefaultHttpClient.class));
    }

    @Test
    public void httpClientIsConfigurableFromAStaticHttpClientConfigWithOtherConfigurations() {
        // Given
        final MutableObject<HttpClient> client = new MutableObject<>();
        RestAssured.config = newConfig().httpClient(httpClientConfig().setParam(HANDLE_REDIRECTS, true).and().setParam(MAX_REDIRECTS, 0).and().httpClientFactory(SystemDefaultHttpClient::new));

        // When
        try {
            given().
                    param("url", "/hello").
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            client.setValue(requestSpec.getHttpClient());
                            return ctx.next(requestSpec, responseSpec);
                        }
                    }).
            expect().
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");
        } finally {
            RestAssured.reset();
        }

        // Then
        assertThat(client.getValue(), instanceOf(SystemDefaultHttpClient.class));
    }

    @Test public void
    http_client_config_allows_specifying_that_the_http_client_instance_is_reused_in_multiple_requests() {
        final MutableObject<HttpClient> client1 = new MutableObject<HttpClient>();
        final MutableObject<HttpClient> client2 = new MutableObject<HttpClient>();

        RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());

        // When
        try {
            given().
                    param("url", "/hello").
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            client1.setValue(requestSpec.getHttpClient());
                            return ctx.next(requestSpec, responseSpec);
                        }
                    }).
            expect().
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");

            given().
                    header("name", "value").
                    filter((requestSpec, responseSpec, ctx) -> {
                        client2.setValue(requestSpec.getHttpClient());
                        return ctx.next(requestSpec, responseSpec);
                    }).
            when().
                    post("/reflect");
        } finally {
            RestAssured.reset();
        }

        assertThat(client1.getValue(), sameInstance(client2.getValue()));
    }

    @Test public void
    local_http_client_config_doesnt_reuse_static_http_client_instance_when_local_config_specifies_reuse() {
        final MutableObject<HttpClient> client1 = new MutableObject<HttpClient>();
        final MutableObject<HttpClient> client2 = new MutableObject<HttpClient>();

        RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());

        // When
        try {
            given().
                    param("url", "/hello").
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            client1.setValue(requestSpec.getHttpClient());
                            return ctx.next(requestSpec, responseSpec);
                        }
                    }).
            expect().
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");

            given().
                    config(newConfig().httpClient(httpClientConfig().reuseHttpClientInstance())).
                    header("name", "value").
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            client2.setValue(requestSpec.getHttpClient());
                            return ctx.next(requestSpec, responseSpec);
                        }
                    }).
            when().
                    post("/reflect");
        } finally {
            RestAssured.reset();
        }

        assertThat(client1.getValue(), not(sameInstance(client2.getValue())));
    }

    @Test public void
    local_http_client_config_reuse_reuse_static_http_client_instance_when_local_config_changes_other_configs_than_http_client_config() {
        final MutableObject<HttpClient> client1 = new MutableObject<HttpClient>();
        final MutableObject<HttpClient> client2 = new MutableObject<HttpClient>();

        RestAssured.config = newConfig().httpClient(httpClientConfig().reuseHttpClientInstance());

        // When
        try {
            given().
                    param("url", "/hello").
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            client1.setValue(requestSpec.getHttpClient());
                            return ctx.next(requestSpec, responseSpec);
                        }
                    }).
            expect().
                    body("hello", equalTo("Hello Scalatra")).
            when().
                    get("/redirect");

            given().
                    // Here we only change the decoder config
                    config(RestAssured.config.decoderConfig(decoderConfig().with().contentDecoders(DEFLATE))).
                    filter(new Filter() {
                        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                            client2.setValue(requestSpec.getHttpClient());
                            return ctx.next(requestSpec, responseSpec);
                        }
                    }).
            expect().
                    body("Accept-Encoding", contains("deflate")).
            when().
                    get("/headersWithValues");
        } finally {
            RestAssured.reset();
        }

        assertThat(client1.getValue(), sameInstance(client2.getValue()));
    }

}

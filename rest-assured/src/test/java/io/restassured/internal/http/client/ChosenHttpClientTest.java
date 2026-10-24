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

package io.restassured.internal.http.client;

import io.restassured.common.exception.ResponseTimeoutException;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Requests sent with a client chosen with {@link HttpClientConfig#client(Class)}, a fake that sends nothing.
 */
class ChosenHttpClientTest {
    private static final HttpClientConfig APACHE5 = httpClientConfig().client(FakeHttpClients.Apache5.class);

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
    }

    private static RequestSpecification with(HttpClientConfig httpClientConfig) {
        return with(config().httpClient(httpClientConfig));
    }

    private static RequestSpecification with(RestAssuredConfig config) {
        return given().config(config);
    }

    @Test
    void the_request_is_sent_with_the_chosen_client_which_is_closed_after_the_response() {
        with(APACHE5.readTimeout(Duration.ofSeconds(3)).connectTimeout(Duration.ofSeconds(2))).
                queryParam("q", "a b").
        when().
                get("http://localhost:1234/path").
        then().
                statusCode(200).
                body(equalTo("ok"));

        HttpRequest request = FakeHttpClients.sent.get(0);
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.uri()).isEqualTo(URI.create("http://localhost:1234/path?q=a%20b"));
        assertThat(request.options().readTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(FakeHttpClients.created).singleElement().satisfies(client -> {
            assertThat(client.settings.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(client.closed).isTrue();
        });
    }

    @Test
    void certificate_authentication_gives_the_client_its_key_store() {
        with(APACHE5).auth().certificate("client.p12", "secret").get("https://localhost/path");

        assertThat(FakeHttpClients.sent.get(0).uri()).isEqualTo(URI.create("https://localhost/path"));
        assertThat(FakeHttpClients.created.get(0).settings.tls().keyStore().path()).isEqualTo("client.p12");
    }

    @Test
    void preemptive_basic_authentication_sends_the_header() {
        with(APACHE5).auth().preemptive().basic("user", "password").get("http://localhost/path");

        assertThat(FakeHttpClients.sent.get(0).headers()).anySatisfy(header -> {
            assertThat(header.name()).isEqualTo("Authorization");
            assertThat(header.value()).isEqualTo("Basic dXNlcjpwYXNzd29yZA==");
        });
    }

    @Test
    void authentication_that_answers_a_challenge_fails() {
        assertThatThrownBy(() -> with(APACHE5).auth().basic("user", "password").get("http://localhost/path"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Apache HttpClient 5 (chosen with httpClientConfig().client(" + FakeHttpClients.Apache5.class.getName() + ".class)) can't "
                        + "authenticate with BasicAuthScheme yet. Remove httpClientConfig().client(..) to use Apache HttpClient 4, "
                        + "or use preemptive basic authentication.");
        assertThat(FakeHttpClients.sent).isEmpty();
    }

    @Test
    void an_http_client_factory_fails() {
        HttpClientConfig factory = APACHE5.httpClientFactory(DefaultHttpClient::new);

        assertThatThrownBy(() -> with(factory).get("http://localhost/path"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Apache HttpClient 5 (chosen with httpClientConfig().client(" + FakeHttpClients.Apache5.class.getName() + ".class)) can't "
                        + "use the HttpClient 4 client of httpClientConfig().httpClientFactory(..). Remove httpClientConfig().client(..) to use "
                        + "Apache HttpClient 4, or customize the client with httpClientConfig().customizeClient(..).");
        assertThat(FakeHttpClients.created).isEmpty();
    }

    @Test
    @SuppressWarnings("deprecation")
    void an_http_client_4_socket_factory_fails() {
        assertThatThrownBy(() -> with(config().httpClient(APACHE5).sslConfig(sslConfig().sslSocketFactory(SSLSocketFactory.getSocketFactory()))).get("https://localhost/"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Apache HttpClient 5 (chosen with httpClientConfig().client(" + FakeHttpClients.Apache5.class.getName() + ".class)) can't "
                        + "use the HttpClient 4 SSLSocketFactory of sslConfig().sslSocketFactory(..). Remove httpClientConfig().client(..) to use "
                        + "Apache HttpClient 4, or use sslConfig().sslContext(..).");
    }

    @Test
    void customizers_are_given_to_the_chosen_client() {
        with(APACHE5.customizeClient(FakeHttpClients.Apache5Builder.class, builder -> { })).get("http://localhost/path");

        assertThat(FakeHttpClients.created.get(0).settings.customizers()).singleElement()
                .satisfies(customizer -> assertThat(customizer.builderType()).isEqualTo(FakeHttpClients.Apache5Builder.class));
    }

    @Test
    void a_customizer_for_another_client_fails_before_the_request_is_sent() {
        assertThatThrownBy(() -> with(APACHE5.customizeClient(HttpClient.Builder.class, builder -> { })).get("http://localhost/path"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("A customizer for java.net.http.HttpClient$Builder is configured, but REST Assured uses Apache HttpClient 5.");
        assertThat(FakeHttpClients.created).isEmpty();
    }

    @Test
    void a_customizer_fails_with_http_client_4() {
        assertThatThrownBy(() -> with(httpClientConfig().customizeClient(HttpClient.Builder.class, builder -> { })).get("http://localhost/path"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("A customizer for java.net.http.HttpClient$Builder is configured, but REST Assured uses Apache HttpClient 4, "
                        + "which you customize with httpClientConfig().httpClientFactory(..).");
    }

    @Test
    void what_the_client_cant_do_fails_with_how_it_was_chosen() {
        FakeHttpClients.response = request -> {
            throw new UnsupportedFeatureException("do this", "do that");
        };

        assertThatThrownBy(() -> with(APACHE5).get("http://localhost/path"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Apache HttpClient 5 (chosen with httpClientConfig().client(" + FakeHttpClients.Apache5.class.getName() + ".class)) can't do this. "
                        + "Choose another client, or do that.");
        assertThat(FakeHttpClients.created.get(0).closed).isTrue();
    }

    @Test
    void a_timeout_of_the_client_throws_response_timeout_exception() {
        HttpTimeoutException timeout = new HttpTimeoutException("request timed out");
        FakeHttpClients.response = request -> {
            throw timeout;
        };

        assertThatThrownBy(() -> with(APACHE5).get("http://localhost/path")).isExactlyInstanceOf(ResponseTimeoutException.class).hasCause(timeout);
        assertThat(FakeHttpClients.created.get(0).closed).isTrue();
    }
}

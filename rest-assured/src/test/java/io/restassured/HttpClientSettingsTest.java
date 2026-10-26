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

package io.restassured;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.common.exception.ResponseTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * The HTTP client settings added in 6.3.0 as replacements for HttpClient 4 parameters and redirect strategies.
 */
class HttpClientSettingsTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect-302", exchange -> redirect(exchange, 302, "/echo"));
        server.createContext("/redirect-307", exchange -> redirect(exchange, 307, "/echo"));
        server.createContext("/set-cookie-and-redirect", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "session=abc; Path=/");
            redirect(exchange, 302, "/echo");
        });
        server.createContext("/echo", exchange -> {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            String expect = exchange.getRequestHeaders().getFirst("Expect");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, exchange.getRequestMethod() + " body=" + body + " cookie=" + cookie + " expect=" + expect);
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, "slow");
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        RestAssured.reset();
        server.stop(0);
    }

    @Test
    void post_redirect_is_not_followed_by_default() {
        given().
                body("data").
        when().
                post(url("/redirect-302")).
        then().
                statusCode(302);
    }

    @Test
    void post_redirect_is_followed_with_get_when_following_redirects_for_all_methods() {
        given().
                config(config().redirect(redirectConfig().followRedirectsForAllMethods(true))).
                body("data").
        when().
                post(url("/redirect-302")).
        then().
                statusCode(200).
                body(equalTo("GET body= cookie=null expect=null"));
    }

    @Test
    void put_307_redirect_is_repeated_with_the_same_method_and_body_when_following_redirects_for_all_methods() {
        given().
                config(config().redirect(redirectConfig().followRedirectsForAllMethods(true))).
                body("data").
        when().
                put(url("/redirect-307")).
        then().
                statusCode(200).
                body(equalTo("PUT body=data cookie=null expect=null"));
    }

    @Test
    void post_307_redirect_is_repeated_with_the_same_method_and_body_when_following_redirects_for_all_methods() {
        given().
                config(config().redirect(redirectConfig().followRedirectsForAllMethods(true))).
                body("data").
        when().
                post(url("/redirect-307")).
        then().
                statusCode(200).
                body(equalTo("POST body=data cookie=null expect=null"));
    }

    @Test
    void response_cookies_are_not_sent_after_a_redirect_by_default() {
        given().
        when().
                get(url("/set-cookie-and-redirect")).
        then().
                statusCode(200).
                body(equalTo("GET body= cookie=null expect=null"));
    }

    @Test
    void response_cookies_are_sent_after_a_redirect_when_keeping_response_cookies() {
        given().
                config(config().httpClient(httpClientConfig().keepResponseCookies(true))).
        when().
                get(url("/set-cookie-and-redirect")).
        then().
                statusCode(200).
                body(equalTo("GET body= cookie=session=abc expect=null"));
    }

    @Test
    void expect_continue_header_is_sent_when_configured() {
        given().
                config(config().httpClient(httpClientConfig().expectContinue(true))).
                body("data").
        when().
                post(url("/echo")).
        then().
                statusCode(200).
                // JDK differences table, Expect: 100-continue: the JDK client writes 100-Continue
                body(equalTo("POST body=data cookie=null expect=100-Continue"));
    }

    @Test
    void read_timeout_fails_a_slow_response() {
        assertThatThrownBy(() ->
                given().
                        config(config().httpClient(httpClientConfig().readTimeout(Duration.ofMillis(200)))).
                when().
                        get(url("/slow")))
                .isExactlyInstanceOf(ResponseTimeoutException.class);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static void redirect(HttpExchange exchange, int status, String location) throws IOException {
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}

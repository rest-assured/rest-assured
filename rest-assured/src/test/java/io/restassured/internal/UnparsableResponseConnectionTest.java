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

import com.sun.net.httpserver.HttpServer;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.http.ResponseParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * A response that can't be parsed leaves its body unread, which keeps the single connection of a reused http client.
 */
class UnparsableResponseConnectionTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/unparsable", exchange -> {
            // REST Assured fails to parse the cookie's Max-Age
            exchange.getResponseHeaders().add("Set-Cookie", "name=value; Max-Age=never");
            respond(exchange, "unread");
        });
        server.createContext("/ok", exchange -> respond(exchange, "ok"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void with_a_reused_http_client_instance_the_request_after_a_response_that_could_not_be_parsed_is_sent() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());

        Throwable unparsable = catchThrowable(() -> given().config(config).get(url("/unparsable")));
        String next = given().config(config).get(url("/ok")).asString();

        assertThat(unparsable).isInstanceOf(ResponseParseException.class).hasCauseInstanceOf(NumberFormatException.class);
        assertThat(next).isEqualTo("ok");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}

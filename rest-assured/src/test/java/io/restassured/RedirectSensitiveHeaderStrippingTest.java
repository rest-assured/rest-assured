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

package io.restassured;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static org.assertj.core.api.Assertions.assertThat;

class RedirectSensitiveHeaderStrippingTest {

    @AfterEach
    public void reset() {
        RestAssured.reset();
    }

    @Test
    void stripsSensitiveHeadersOnCrossHostRedirectByDefault() throws Exception {
        Headers leaked = new Headers();
        HttpServer sink = sinkServer(leaked);
        HttpServer origin = redirectingServer("http://127.0.0.1:" + sink.getAddress().getPort() + "/leak");
        try {
            given().
                    header("Authorization", "Bearer secret").
                    header("Cookie", "session=cookie").
            when().
                    get("http://127.0.0.1:" + origin.getAddress().getPort() + "/start").
            then().
                    statusCode(200);

            assertThat(leaked.authorization.get()).isNull();
            assertThat(leaked.cookie.get()).isNull();
        } finally {
            origin.stop(0);
            sink.stop(0);
        }
    }

    @Test
    void keepsSensitiveHeadersOnSameHostRedirect() throws Exception {
        Headers received = new Headers();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().set("Location", "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            received.authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            received.cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            send(exchange, "ok");
        });
        server.start();
        try {
            given().
                    header("Authorization", "Bearer secret").
                    header("Cookie", "session=cookie").
            when().
                    get("http://127.0.0.1:" + server.getAddress().getPort() + "/start").
            then().
                    statusCode(200);

            assertThat(received.authorization.get()).isEqualTo("Bearer secret");
            assertThat(received.cookie.get()).isEqualTo("session=cookie");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void forwardsSensitiveHeadersOnCrossHostRedirectWhenStrippingIsDisabled() throws Exception {
        Headers leaked = new Headers();
        HttpServer sink = sinkServer(leaked);
        HttpServer origin = redirectingServer("http://127.0.0.1:" + sink.getAddress().getPort() + "/leak");
        try {
            given().
                    config(config().redirect(redirectConfig().stripSensitiveHeadersOnCrossHostRedirect(false))).
                    header("Authorization", "Bearer secret").
                    header("Cookie", "session=cookie").
            when().
                    get("http://127.0.0.1:" + origin.getAddress().getPort() + "/start").
            then().
                    statusCode(200);

            assertThat(leaked.authorization.get()).isEqualTo("Bearer secret");
            assertThat(leaked.cookie.get()).isEqualTo("session=cookie");
        } finally {
            origin.stop(0);
            sink.stop(0);
        }
    }

    private static HttpServer sinkServer(Headers captured) throws IOException {
        HttpServer sink = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        sink.createContext("/leak", exchange -> {
            captured.authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            captured.cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            send(exchange, "ok");
        });
        sink.start();
        return sink;
    }

    private static HttpServer redirectingServer(String location) throws IOException {
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin.createContext("/start", exchange -> {
            exchange.getResponseHeaders().set("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        origin.start();
        return origin;
    }

    private static void send(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static final class Headers {
        final AtomicReference<String> authorization = new AtomicReference<>();
        final AtomicReference<String> cookie = new AtomicReference<>();
    }
}

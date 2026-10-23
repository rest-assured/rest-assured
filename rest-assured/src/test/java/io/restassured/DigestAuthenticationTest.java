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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Digest authentication only answers Digest challenges, so the password is never sent in clear text.
 */
class DigestAuthenticationTest {

    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/basic", exchange -> challengeOrAccept(exchange, "Basic realm=\"test\""));
        server.createContext("/both", exchange -> challengeOrAccept(exchange, "Basic realm=\"test\"",
                "Digest realm=\"test\", qop=\"auth\", nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\""));
        server.start();
    }

    @AfterEach
    void stopServer() {
        RestAssured.reset();
        server.stop(0);
    }

    @Test
    void digest_returns_the_401_of_a_server_that_only_asks_for_basic_authentication() {
        given().auth().digest("user", "pass").when().get(url("/basic")).then().statusCode(401);

        assertThat(authorizations).isEmpty();
    }

    @Test
    void static_digest_returns_the_401_of_a_server_that_only_asks_for_basic_authentication() {
        RestAssured.authentication = RestAssured.digest("user", "pass");

        given().when().get(url("/basic")).then().statusCode(401);

        assertThat(authorizations).isEmpty();
    }

    @Test
    void digest_answers_the_digest_challenge_of_a_server_that_asks_for_basic_or_digest_authentication() {
        given().auth().digest("user", "pass").when().get(url("/both")).then().statusCode(200);

        assertThat(authorizations).singleElement().asString().startsWith("Digest username=\"user\"");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private void challengeOrAccept(HttpExchange exchange, String... challenges) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null) {
            for (String challenge : challenges) {
                exchange.getResponseHeaders().add("WWW-Authenticate", challenge);
            }
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }
        authorizations.add(authorization);
        byte[] bytes = "authenticated".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}

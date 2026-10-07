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
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Verifies support for the HTTP QUERY method (a safe, idempotent method that carries a request body), see issue #1872.
 */
class HttpQueryMethodTest {

    private HttpServer server;
    private final AtomicReference<String> receivedMethod = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedContentType = new AtomicReference<>();
    private final AtomicReference<String> receivedUri = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedUri.set(exchange.getRequestURI().toString());
            receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            send(exchange, "{\"method\":\"" + exchange.getRequestMethod() + "\"}");
        });
        server.start();
        RestAssured.port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        RestAssured.reset();
    }

    @Test
    void query_sends_query_method_with_body() {
        given().
                contentType(ContentType.JSON).
                body("{\"title\":\"rest\"}").
        when().
                query("/search").
        then().
                statusCode(200).
                body("method", equalTo("QUERY"));

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedBody.get()).isEqualTo("{\"title\":\"rest\"}");
        assertThat(receivedContentType.get()).startsWith("application/json");
        assertThat(receivedUri.get()).isEqualTo("/search");
    }

    @Test
    void query_supports_unnamed_path_params_and_query_params() {
        given().
                queryParam("limit", 10).
                body("select *").
        when().
                query("/{collection}/search", "books").
        then().
                statusCode(200);

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedUri.get()).isEqualTo("/books/search?limit=10");
        assertThat(receivedBody.get()).isEqualTo("select *");
        assertThat(receivedContentType.get()).startsWith("text/plain");
    }

    @Test
    void query_supports_named_path_params() {
        given().
                body("q").
        when().
                query("/{collection}/search", Collections.singletonMap("collection", "movies")).
        then().
                statusCode(200);

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedUri.get()).isEqualTo("/movies/search");
        assertThat(receivedBody.get()).isEqualTo("q");
    }

    @Test
    void query_serializes_object_body() {
        given().
                contentType(ContentType.JSON).
                body(Collections.singletonMap("name", "John")).
        when().
                query("/search").
        then().
                statusCode(200);

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedBody.get()).isEqualTo("{\"name\":\"John\"}");
    }

    @Test
    void query_with_uri_and_url() throws Exception {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/search";

        given().body("uri").when().query(new URI(url)).then().statusCode(200);
        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedBody.get()).isEqualTo("uri");

        given().body("url").when().query(new URI(url).toURL()).then().statusCode(200);
        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedBody.get()).isEqualTo("url");
    }

    @Test
    void query_without_path_uses_configured_base_path() {
        RestAssured.basePath = "/base";

        given().body("body").when().query().then().statusCode(200);

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedUri.get()).isEqualTo("/base");
        assertThat(receivedBody.get()).isEqualTo("body");
    }

    @Test
    void static_query_shortcut_sends_query_method() {
        RestAssured.query("/search").then().statusCode(200).body("method", equalTo("QUERY"));

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedBody.get()).isEmpty();
    }

    @Test
    void request_with_query_method_enum_sends_body() {
        given().body("enum").when().request(Method.QUERY, "/search").then().statusCode(200);

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedBody.get()).isEqualTo("enum");
    }

    @Test
    void filters_see_query_method_and_body() {
        AtomicReference<String> filteredMethod = new AtomicReference<>();
        AtomicReference<Object> filteredBody = new AtomicReference<>();

        given().
                body("filtered").
                filter((requestSpec, responseSpec, ctx) -> {
                    filteredMethod.set(requestSpec.getMethod());
                    filteredBody.set(requestSpec.getBody());
                    return ctx.next(requestSpec, responseSpec);
                }).
        when().
                query("/search").
        then().
                statusCode(200);

        assertThat(filteredMethod.get()).isEqualTo("QUERY");
        assertThat(filteredBody.get()).isEqualTo("filtered");
        assertThat(receivedBody.get()).isEqualTo("filtered");
    }

    @Test
    void query_can_be_used_from_response_specification_when() {
        given().
                body("expect").
        expect().
                statusCode(200).
        when().
                query("/search");

        assertThat(receivedMethod.get()).isEqualTo("QUERY");
        assertThat(receivedBody.get()).isEqualTo("expect");
    }

    private static void send(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

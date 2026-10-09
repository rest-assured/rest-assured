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

package io.restassured.internal.filter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A tiny HTTP server for filter tests: routes are keyed by {@code "METHOD /path"} and every request is recorded.
 */
final class RecordingServer implements AutoCloseable {

    record Request(String method, String path, String query, Map<String, List<String>> headers, String body) {
        String header(String name) {
            return headers.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(name))
                    .map(e -> String.join(",", e.getValue()))
                    .findFirst().orElse(null);
        }

        String cookies() {
            return header("Cookie");
        }
    }

    record Reply(int status, String contentType, String body, Map<String, List<String>> headers) {
        static Reply html(String body) {
            return new Reply(200, "text/html; charset=utf-8", body, Map.of());
        }

        static Reply text(String body) {
            return new Reply(200, "text/plain; charset=utf-8", body, Map.of());
        }

        static Reply redirect(String location) {
            return new Reply(302, null, null, Map.of("Location", List.of(location)));
        }

        Reply withCookie(String cookie) {
            Map<String, List<String>> copy = new LinkedHashMap<>(headers);
            List<String> cookies = new ArrayList<>(copy.getOrDefault("Set-Cookie", List.of()));
            cookies.add(cookie);
            copy.put("Set-Cookie", cookies);
            return new Reply(status, contentType, body, copy);
        }
    }

    private final HttpServer server;
    private final Map<String, Function<Request, Reply>> routes = new ConcurrentHashMap<>();
    final List<Request> requests = new CopyOnWriteArrayList<>();

    RecordingServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    RecordingServer route(String methodAndPath, Function<Request, Reply> handler) {
        routes.put(methodAndPath, handler);
        return this;
    }

    List<String> requestLines() {
        return requests.stream().map(r -> r.method() + " " + r.path()).collect(Collectors.toList());
    }

    Request lastRequestTo(String methodAndPath) {
        Request found = null;
        for (Request request : requests) {
            if ((request.method() + " " + request.path()).equals(methodAndPath)) {
                found = request;
            }
        }
        return found;
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Request request = new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getRawQuery(), new LinkedHashMap<>(exchange.getRequestHeaders()), body);
        requests.add(request);
        Function<Request, Reply> handler = routes.get(request.method() + " " + request.path());
        Reply reply = handler == null ? new Reply(404, "text/plain", "not found", Map.of()) : handler.apply(request);
        reply.headers().forEach((name, values) -> values.forEach(v -> exchange.getResponseHeaders().add(name, v)));
        if (reply.contentType() != null) {
            exchange.getResponseHeaders().set("Content-Type", reply.contentType());
        }
        if (reply.body() == null || "HEAD".equals(request.method())) {
            exchange.sendResponseHeaders(reply.status(), -1);
        } else {
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}

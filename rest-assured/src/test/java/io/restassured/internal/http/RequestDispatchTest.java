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

package io.restassured.internal.http;

import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.internal.http.EncoderCharacterization.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.equalTo;

/**
 * Characterizes, through the public API, how a request is configured (uri path, query, headers, content-type, body)
 * and how the response handlers treat success and failure responses, for every HTTP method that REST Assured
 * dispatches through {@link HTTPBuilder}'s {@code request}, {@code post} and {@code patch} methods.
 */
class RequestDispatchTest {

    private static final List<String> METHODS = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "PURGE");

    private HttpServer server;
    private volatile String received;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            StringBuilder request = new StringBuilder();
            request.append(exchange.getRequestMethod()).append(' ').append(exchange.getRequestURI().toString());
            new TreeMap<>(exchange.getRequestHeaders()).forEach((name, values) -> {
                if (!name.equalsIgnoreCase("Host") && !name.equalsIgnoreCase("User-agent")) {
                    request.append("\n    > ").append(name).append(": ").append(values);
                }
            });
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            request.append("\n    > body: ").append(escape(requestBody));
            received = request.toString();

            String[] parts = exchange.getRequestURI().getPath().split("/");
            String kind = parts[1];
            int status = Integer.parseInt(parts[2]);
            exchange.getResponseHeaders().add("X-Reply", "r1");
            exchange.getResponseHeaders().add("X-Reply", "r2");
            byte[] responseBody;
            if (kind.equals("s")) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                responseBody = ("body " + status).getBytes(StandardCharsets.UTF_8);
            } else if (kind.equals("json")) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                responseBody = ("{\"status\":" + status + "}").getBytes(StandardCharsets.UTF_8);
            } else {
                responseBody = new byte[0];
            }
            if (responseBody.length == 0 || exchange.getRequestMethod().equals("HEAD")) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, responseBody.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            }
            exchange.close();
        });
        server.start();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        RestAssured.reset();
    }

    private static Map<String, Supplier<RequestSpecification>> variants() {
        Map<String, Supplier<RequestSpecification>> variants = new LinkedHashMap<>();
        variants.put("query, headers and cookies", () -> given().
                queryParam("q", "a b").queryParam("multi", "1", "2").
                header("X-Dup", "1").header("X-Dup", "2").header("X-Dup", "3").header("X-Single", "s").
                cookie("c", "v").cookie("d", "w"));
        variants.put("path params", () -> given().pathParam("kind", "s").basePath("/{kind}"));
        variants.put("form params", () -> given().formParam("f", "1").formParam("g", "åä").queryParam("q", "1"));
        variants.put("json body", () -> given().contentType(ContentType.JSON).body("{\"a\":1}"));
        variants.put("no content-type with form params", () -> given().noContentType().formParam("f", "1"));
        variants.put("no content-type with body", () -> given().noContentType().body("text"));
        variants.put("explicit accept header", () -> given().header("Accept", "text/plain"));
        variants.put("expected response content-type", () -> given().expect().contentType(ContentType.JSON).given());
        variants.put("multipart", () -> given().multiPart("part", "content").config(RestAssured.config().multiPartConfig(
                io.restassured.config.MultiPartConfig.multiPartConfig().defaultBoundary("BOUNDARY"))));
        return variants;
    }

    @Test
    void configures_requests_and_handles_responses_like_before() throws Exception {
        StringBuilder out = new StringBuilder();
        for (String method : METHODS) {
            out.append("== ").append(method).append(" plain\n");
            for (String path : Arrays.asList("/s/200", "/s/404", "/s/500", "/json/201", "/e/204", "/e/200", "/e/404")) {
                out.append(path).append('\n').append(send(given(), method, path)).append('\n');
            }
            for (Map.Entry<String, Supplier<RequestSpecification>> variant : variants().entrySet()) {
                out.append("== ").append(method).append(' ').append(variant.getKey()).append('\n');
                for (String path : Arrays.asList("/s/200", "/s/404")) {
                    String pathToUse = variant.getKey().equals("path params") ? path.substring(2) : path;
                    out.append(pathToUse).append('\n').append(send(variant.getValue().get(), method, pathToUse)).append('\n');
                }
            }
        }
        assertMatchesGoldenFile("request-dispatch.txt", out.toString());
    }

    @Test
    void validates_expectations_given_before_the_request_like_before() throws Exception {
        Map<String, Function<RequestSpecification, RequestSpecification>> specs = new LinkedHashMap<>();
        specs.put("plain", spec -> spec);
        specs.put("form params", spec -> spec.formParam("f", "1"));
        StringBuilder out = new StringBuilder();
        for (String method : METHODS) {
            for (Map.Entry<String, Function<RequestSpecification, RequestSpecification>> spec : specs.entrySet()) {
                out.append("== ").append(method).append(' ').append(spec.getKey()).append('\n');
                String expectedBody200 = method.equals("HEAD") ? "" : "body 200";
                String expectedBody404 = method.equals("HEAD") ? "" : "body 404";
                out.append("matching 200: ").append(sendAndRender(() -> spec.getValue().apply(given()).
                        expect().statusCode(200).body(equalTo(expectedBody200)).when().request(method, "/s/200"))).append('\n');
                out.append("matching 404: ").append(sendAndRender(() -> spec.getValue().apply(given()).
                        expect().statusCode(404).body(equalTo(expectedBody404)).when().request(method, "/s/404"))).append('\n');
                out.append("not matching 200: ").append(sendAndRender(() -> spec.getValue().apply(given()).
                        expect().statusCode(201).body(equalTo("other")).when().request(method, "/s/200"))).append('\n');
                out.append("not matching 404: ").append(sendAndRender(() -> spec.getValue().apply(given()).
                        expect().statusCode(200).when().request(method, "/s/404"))).append('\n');
            }
        }
        assertMatchesGoldenFile("request-dispatch-expectations.txt", out.toString());
    }

    @Test
    void wraps_an_exception_thrown_while_handling_the_response_in_a_response_parse_exception() throws Exception {
        for (String method : Arrays.asList("GET", "POST", "PUT")) {
            try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                Thread serverThread = truncatedResponseServer(serverSocket);

                Throwable t = catchThrowable(() -> given().port(serverSocket.getLocalPort()).
                        expect().body(equalTo("never")).when().request(method, "/"));
                serverThread.join(10_000);

                assertThat(t).as(method).isInstanceOf(ResponseParseException.class);
                assertThat(t.getCause()).as(method).isInstanceOf(org.apache.http.ConnectionClosedException.class)
                        .hasMessageContaining("Premature end of Content-Length delimited message body");
            }
        }
    }

    private static Thread truncatedResponseServer(ServerSocket serverSocket) {
        Thread thread = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                InputStream in = socket.getInputStream();
                // Read the request head
                int matched = 0;
                byte[] terminator = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
                while (matched < terminator.length) {
                    int b = in.read();
                    if (b == -1) {
                        break;
                    }
                    matched = b == terminator[matched] ? matched + 1 : (b == terminator[0] ? 1 : 0);
                }
                OutputStream out = socket.getOutputStream();
                out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 100\r\nConnection: close\r\n\r\nhello".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } catch (IOException ignored) {
                // The test fails on the client side if the server misbehaves
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private String send(RequestSpecification spec, String method, String path) {
        return sendAndRender(() -> spec.request(method, path));
    }

    private String sendAndRender(Supplier<Response> request) {
        received = "    > <nothing sent>";
        AtomicReference<String> response = new AtomicReference<>();
        try {
            Response r = request.get();
            StringBuilder sb = new StringBuilder();
            sb.append("    < ").append(r.statusLine()).append(" | status ").append(r.statusCode())
                    .append(" | content-type ").append(r.contentType())
                    .append(" | X-Reply ").append(r.headers().getValues("X-Reply"))
                    .append(" | body ").append(escape(r.asByteArray()));
            response.set(sb.toString());
        } catch (Throwable t) {
            response.set("    < " + render(t, null));
        }
        return received + "\n" + response.get();
    }
}

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
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.time.TimingFilter;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;

/**
 * Characterizes the response accessors end to end against a local HTTP server, with and without body expectations.
 */
class ResponseAccessorsTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/json", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=abc");
            exchange.getResponseHeaders().add("Set-Cookie", "other=x");
            send(exchange, "application/json; charset=UTF-8", "{\"s\":\"åäö\"}");
        });
        server.createContext("/html", exchange -> send(exchange, "text/html", "<html><body><p>x</p></body></html>"));
        server.start();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        RestAssured.reset();
    }

    @Test
    void response_without_expectations_exposes_status_headers_cookies_and_body() {
        Response response = given().get("/json");

        assertThat(((RestAssuredResponseImpl) response).getLogRepository()).isNotNull();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.statusLine()).isEqualTo("HTTP/1.1 200 OK");
        assertThat(response.contentType()).isEqualTo("application/json; charset=UTF-8");
        assertThat(response.header("Content-Type")).isEqualTo("application/json; charset=UTF-8");
        assertThat(response.cookies()).containsExactly(entry("JSESSIONID", "abc"), entry("other", "x"));
        assertThat(response.sessionId()).isEqualTo("abc");
        assertThat(((RestAssuredResponseImpl) response).isInputStream()).isTrue();
        assertThat(response.asString()).isEqualTo("{\"s\":\"åäö\"}");
        assertThat(response.asString()).isEqualTo("{\"s\":\"åäö\"}");
        assertThat(response.asByteArray()).isEqualTo("{\"s\":\"åäö\"}".getBytes(StandardCharsets.UTF_8));
        assertThat(response.<String>path("s")).isEqualTo("åäö");
        assertThat(response.jsonPath().getString("s")).isEqualTo("åäö");
        assertThat(response.time()).isGreaterThanOrEqualTo(0);
        Map<?, ?> map = response.as(Map.class);
        assertThat(map).isEqualTo(Map.of("s", "åäö"));
    }

    @Test
    void response_with_expectations_has_buffered_body() {
        Response response = given().expect().body("s", equalTo("åäö")).when().get("/json");

        assertThat(((RestAssuredResponseImpl) response).getHasExpectations()).isTrue();
        assertThat(((RestAssuredResponseImpl) response).getContent()).isEqualTo("{\"s\":\"åäö\"}".getBytes(StandardCharsets.UTF_8));
        assertThat(response.asString()).isEqualTo("{\"s\":\"åäö\"}");
        assertThat(response.asByteArray()).isEqualTo("{\"s\":\"åäö\"}".getBytes(StandardCharsets.UTF_8));
        assertThat(response.<String>path("s")).isEqualTo("åäö");
    }

    @Test
    void html_path_returns_xml_path() {
        Response response = given().get("/html");

        assertThat((Object) response.path("html.body.p")).isInstanceOf(XmlPath.class);
        assertThat(response.htmlPath().getString("html.body.p")).isEqualTo("x");
    }

    @Test
    void cloned_response_keeps_body_and_timing() {
        Response response = given().filter(new TimingFilter()).get("/json");
        response.asString();

        Response clone = new ResponseBuilder().clone(response).build();

        assertThat(clone.asString()).isEqualTo("{\"s\":\"åäö\"}");
        assertThat(clone.time()).isEqualTo(response.time());
        assertThat(clone.sessionId()).isEqualTo("abc");
    }

    private static void send(HttpExchange exchange, String contentType, String body) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }
}

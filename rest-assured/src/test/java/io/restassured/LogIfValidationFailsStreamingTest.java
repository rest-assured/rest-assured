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
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD;

/**
 * Logging if validation fails doesn't read the body of a server-sent events response, which may never end, while
 * logging that is asked for explicitly does.
 */
@Timeout(value = 10, threadMode = SEPARATE_THREAD)
class LogIfValidationFailsStreamingTest {

    private static final String EVENT = "data: tick\n\n";

    private HttpServer server;
    private ExecutorService executor;
    private volatile boolean stopped;
    private final ByteArrayOutputStream log = new ByteArrayOutputStream();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/events", this::sendEventsUntilStopped);
        server.createContext("/one-event", exchange -> send(exchange, "text/event-stream", EVENT));
        server.createContext("/text", exchange -> send(exchange, "text/plain", "hello"));
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        stopped = true;
        server.stop(0);
        executor.shutdownNow();
        RestAssured.reset();
    }

    @Test
    void status_code_validation_of_an_endless_event_stream_returns() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        given().get("/events").then().statusCode(200);
    }

    @Test
    void status_code_expectation_of_an_endless_event_stream_returns() {
        RestAssured.config = logToCaptor(true);

        given().expect().statusCode(200).when().get("/events");
    }

    @Test
    void log_if_validation_fails_after_the_request_returns_for_an_endless_event_stream() {
        RestAssured.config = logToCaptor(false);

        given().get("/events").then().log().ifValidationFails().statusCode(200);
    }

    @Test
    void failed_validation_of_an_endless_event_stream_logs_the_response_without_the_body() {
        RestAssured.config = logToCaptor(true);

        assertThatThrownBy(() -> given().get("/events").then().statusCode(201)).isInstanceOf(AssertionError.class);

        assertThat(log()).contains("HTTP/1.1 200 OK", "text/event-stream", "<streaming body not logged>").doesNotContain("tick");
    }

    @Test
    void failed_validation_of_other_responses_still_logs_the_body() {
        RestAssured.config = logToCaptor(true);

        assertThatThrownBy(() -> given().get("/text").then().statusCode(201)).isInstanceOf(AssertionError.class);

        assertThat(log()).contains("HTTP/1.1 200 OK", "hello").doesNotContain("<streaming body not logged>");
    }

    @Test
    void explicit_logging_still_logs_the_body_of_an_event_stream() {
        RestAssured.config = logToCaptor(false);

        given().get("/one-event").then().log().body().statusCode(200);
        given().log().all().expect().statusCode(200).when().get("/one-event");

        assertThat(log()).contains("data: tick").doesNotContain("<streaming body not logged>");
    }

    private RestAssuredConfig logToCaptor(boolean ifValidationFails) {
        PrintStream captor = new PrintStream(log, true, StandardCharsets.UTF_8);
        RestAssuredConfig config = RestAssuredConfig.config().logConfig(logConfig().defaultStream(captor).enablePrettyPrinting(false));
        return ifValidationFails ? config.logConfig(config.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails()) : config;
    }

    private String log() {
        return log.toString(StandardCharsets.UTF_8);
    }

    private void sendEventsUntilStopped(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream os = exchange.getResponseBody()) {
            while (!stopped) {
                os.write(EVENT.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void send(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

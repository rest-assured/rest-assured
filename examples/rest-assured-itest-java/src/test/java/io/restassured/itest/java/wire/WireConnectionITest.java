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

package io.restassured.itest.java.wire;

import io.restassured.common.exception.ConnectionTimeoutException;
import io.restassured.common.exception.ResponseTimeoutException;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.http.ResponseParseException;
import io.restassured.itest.java.support.TestClient;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ConnectionConfig.connectionConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.itest.java.wire.Responder.REMOTE_PORT_HEADER;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.Matchers.equalTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The exceptions for connections that fail or time out, {@link ConnectionConfig}, and what happens to the connection
 * of a response that is streamed or never read. Which connection a request arrives on is told by the client port that
 * the server answers in a header.
 */
@Timeout(10)
public class WireConnectionITest extends WireTest {
    private static final int LARGE_BODY_SIZE = 1024 * 1024;
    // Larger than the 8 KiB the client buffers along with the headers, small enough for the socket buffers to take
    private static final int UNREAD_BODY_SIZE = 200 * 1024;
    // How long the server holds a response back at most, when the test doesn't release it
    private static final int SERVER_HOLD_SECONDS = 5;

    private CountDownLatch releaseServer;
    private ExecutorService executor;

    @BeforeEach
    void answerWithTheRemotePort() throws Exception {
        // JDK differences table, connection reuse: the shared JDK client keeps the connections of earlier tests in its
        // pool, so each test gets a server of its own, on a new port
        server.close();
        server = startServer();
        releaseServer = new CountDownLatch(1);
        server.respond("/port", Responder.ok().withRemotePort());
        server.respond("/large", bodyOfSize(LARGE_BODY_SIZE).withRemotePort());
        server.respond("/unread", bodyOfSize(UNREAD_BODY_SIZE).withRemotePort());
        server.respond("/slow", slowBody(releaseServer).withRemotePort());
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void releaseWaitingServerThreads() {
        releaseServer.countDown();
        executor.shutdownNow();
    }

    // Exceptions

    @Test public void
    a_refused_connection_throws_connect_exception() throws IOException {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }

        assertThatThrownBy(() -> given().get("http://127.0.0.1:" + port + "/"))
                .isExactlyInstanceOf(ConnectException.class);
    }

    @Test public void
    a_read_timeout_throws_response_timeout_exception() {
        CountDownLatch release = releaseServer;
        server.respond("/wait", (request, response) -> {
            await(release);
            Responder.ok().respond(request, response);
        });

        assertThatThrownBy(() -> given().config(config().httpClient(httpClientConfig().readTimeout(Duration.ofMillis(300)))).get(server.url("/wait")))
                .isExactlyInstanceOf(ResponseTimeoutException.class)
                .satisfies(WireConnectionITest::isCausedByTheHttpClient);
        assertThat(server.onlyRequest().target()).isEqualTo("/wait");
    }

    @Test public void
    a_read_timeout_while_the_body_is_read_for_the_assertions_throws_response_timeout_exception() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().readTimeout(Duration.ofMillis(300)));
        if (TestClient.isJdk()) {
            // JDK differences table, read timeout: it covers only the wait for the headers
            releaseServerAfter(Duration.ofMillis(600));
            given().config(config).expect().body(equalTo("first part, rest")).when().get(server.url("/slow"));
            return;
        }

        assertThatThrownBy(() -> given().config(config).expect().body(equalTo("first part, rest")).when().get(server.url("/slow")))
                .isExactlyInstanceOf(ResponseParseException.class)
                .cause()
                .isExactlyInstanceOf(ResponseTimeoutException.class)
                .satisfies(WireConnectionITest::isCausedByTheHttpClient);
    }

    @Test public void
    a_read_timeout_while_the_body_is_read_after_the_request_throws_response_timeout_exception() {
        Response response = given().config(config().httpClient(httpClientConfig().readTimeout(Duration.ofMillis(300)))).get(server.url("/slow"));
        if (TestClient.isJdk()) {
            // JDK differences table, read timeout: it covers only the wait for the headers
            releaseServerAfter(Duration.ofMillis(600));
            assertThat(response.asString()).isEqualTo("first part, rest");
            return;
        }

        assertThatThrownBy(response::asString)
                .isExactlyInstanceOf(ResponseTimeoutException.class)
                .satisfies(WireConnectionITest::isCausedByTheHttpClient);
    }

    @Test public void
    a_connect_timeout_throws_connection_timeout_exception() throws IOException {
        try (UnresponsiveServer unresponsive = UnresponsiveServer.start()) {
            assertThatThrownBy(() -> given().config(config().httpClient(httpClientConfig().connectTimeout(Duration.ofMillis(300)))).get("http://127.0.0.1:" + unresponsive.port() + "/"))
                    .isExactlyInstanceOf(ConnectionTimeoutException.class)
                    .satisfies(WireConnectionITest::isCausedByTheHttpClient);
        }
    }

    @Test public void
    on_the_jdk_client_a_read_timeout_also_covers_connecting() throws IOException {
        // JDK differences table, read timeout: it covers connecting plus waiting for the headers, and is reported as a
        // connect timeout when it fires before the connection is up
        Assumptions.assumeTrue(TestClient.isJdk(), "HttpClient 4's read timeout doesn't cover connecting");
        try (UnresponsiveServer unresponsive = UnresponsiveServer.start()) {
            assertThatThrownBy(() -> given().config(config().httpClient(httpClientConfig().readTimeout(Duration.ofMillis(300)))).get("http://127.0.0.1:" + unresponsive.port() + "/"))
                    .isExactlyInstanceOf(ConnectionTimeoutException.class)
                    .satisfies(WireConnectionITest::isCausedByTheHttpClient);
        }
    }

    // ConnectionConfig

    @Test public void
    by_default_each_request_is_sent_on_a_new_connection() {
        String first = remotePortOfReadResponse(config(), server.url("/port"));
        String second = remotePortOfReadResponse(config(), server.url("/port"));

        if (TestClient.isJdk()) {
            // JDK differences table, connection reuse: the JDK client is shared, and so are its connections
            assertThat(second).isEqualTo(first);
        } else {
            assertThat(second).isNotEqualTo(first);
        }
    }

    @Test public void
    a_reused_http_client_instance_sends_the_next_request_on_the_same_connection() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());

        String first = remotePortOfReadResponse(config, server.url("/port"));
        String second = remotePortOfReadResponse(config, server.url("/port"));

        assertThat(second).isEqualTo(first);
    }

    @Test public void
    close_idle_connections_after_each_response_sends_the_next_request_on_a_new_connection() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse());

        String first = remotePortOfReadResponse(config, server.url("/port"));
        String second = remotePortOfReadResponse(config, server.url("/port"));

        assertThat(second).isNotEqualTo(first);
    }

    @Test public void
    close_idle_connections_after_an_idle_time_keeps_connections_that_were_idle_for_less() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponseAfter(1, TimeUnit.HOURS));

        String first = remotePortOfReadResponse(config, server.url("/port"));
        String second = remotePortOfReadResponse(config, server.url("/port"));

        assertThat(second).isEqualTo(first);
    }

    @Test public void
    close_idle_connections_after_each_response_closes_the_connection_after_a_large_body_is_read() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse());
        Response large = given().config(config).get(server.url("/unread"));

        assertThat(large.asByteArray()).hasSize(UNREAD_BODY_SIZE);
        String next = remotePortOfReadResponse(config, server.url("/port"));
        assertThat(next).isNotEqualTo(large.header(REMOTE_PORT_HEADER));
    }

    @Test public void
    dont_close_idle_connections_after_each_response_sends_the_next_request_on_the_same_connection() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse().dontCloseIdleConnectionsAfterEachResponse());

        String first = remotePortOfReadResponse(config, server.url("/port"));
        String second = remotePortOfReadResponse(config, server.url("/port"));

        assertThat(second).isEqualTo(first);
    }

    // Streaming responses

    @Test public void
    closing_a_partly_read_stream_waits_for_the_rest_of_the_body() throws Exception {
        InputStream body = given().get(server.url("/slow")).asInputStream();
        assertThat(body.readNBytes(5)).isEqualTo("first".getBytes(ISO_8859_1));

        Future<?> closing = closeInBackground(body);

        if (TestClient.isJdk()) {
            // JDK differences table, not in the table (W5): closing a partly read stream closes the connection
            // instead of reading the rest
            closing.get(SERVER_HOLD_SECONDS / 2, TimeUnit.SECONDS);
            return;
        }
        assertThatThrownBy(() -> closing.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        releaseServer.countDown();
        closing.get(SERVER_HOLD_SECONDS, TimeUnit.SECONDS);
    }

    @Test public void
    without_a_reused_http_client_instance_the_request_after_a_closed_stream_is_sent_on_a_new_connection() throws IOException {
        Response streamed = given().get(server.url("/large"));
        try (InputStream body = streamed.asInputStream()) {
            assertThat(body.readNBytes(100)).hasSize(100);
        }

        String next = given().get(server.url("/port")).header(REMOTE_PORT_HEADER);

        assertThat(next).isNotEqualTo(streamed.header(REMOTE_PORT_HEADER));
    }

    @Test public void
    with_a_reused_http_client_instance_the_connection_of_a_closed_stream_is_reused() throws IOException {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());
        Response streamed = given().config(config).get(server.url("/large"));
        try (InputStream body = streamed.asInputStream()) {
            assertThat(body.readNBytes(100)).hasSize(100);
        }

        String next = remotePortOfReadResponse(config, server.url("/port"));

        if (TestClient.isJdk()) {
            // JDK differences table, not in the table (W5): closing a partly read stream closes the connection
            assertThat(next).isNotEqualTo(streamed.header(REMOTE_PORT_HEADER));
        } else {
            assertThat(next).isEqualTo(streamed.header(REMOTE_PORT_HEADER));
        }
    }

    @Test public void
    with_close_idle_connections_closing_a_partly_read_stream_closes_the_connection_without_reading_the_rest() throws Exception {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse());
        Response streamed = given().config(config).get(server.url("/slow"));
        InputStream body = streamed.asInputStream();
        assertThat(body.readNBytes(5)).isEqualTo("first".getBytes(ISO_8859_1));

        Future<?> closing = closeInBackground(body);

        // Well before the server sends the rest, which it holds back for up to SERVER_HOLD_SECONDS
        closing.get(SERVER_HOLD_SECONDS / 2, TimeUnit.SECONDS);
        String next = remotePortOfReadResponse(config, server.url("/port"));
        assertThat(next).isNotEqualTo(streamed.header(REMOTE_PORT_HEADER));
    }

    @Test public void
    a_response_that_is_never_read_doesnt_block_the_next_request() {
        Response unread = given().get(server.url("/unread"));

        String next = given().get(server.url("/port")).then().statusCode(200).extract().header(REMOTE_PORT_HEADER);

        assertThat(next).isNotEqualTo(unread.header(REMOTE_PORT_HEADER));
        assertThat(server.requests()).hasSize(2);
    }

    @Test @Timeout(60) public void
    responses_that_are_never_read_dont_keep_their_connections_open_once_they_are_unreachable() throws InterruptedException {
        for (int i = 1; i <= 500; i++) {
            given().get(server.url("/unread"));
            if (i % 50 == 0) {
                System.gc();
            }
        }

        // The connections of the unread bodies are closed once the garbage collector has found the responses
        while (server.openConnections() > 20) {
            System.gc();
            Thread.sleep(100);
        }
        assertThat(server.requests()).hasSize(500);
    }

    @Test public void
    with_a_reused_http_client_instance_the_request_after_a_response_that_is_never_read_is_sent_on_the_same_connection() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());
        Response unread = given().config(config).get(server.url("/unread"));

        String next = remotePortOfReadResponse(config, server.url("/port"));

        if (TestClient.isJdk()) {
            // JDK differences table, connection reuse: the unread body keeps its connection until the response is
            // unreachable
            assertThat(next).isNotEqualTo(unread.header(REMOTE_PORT_HEADER));
        } else {
            assertThat(next).isEqualTo(unread.header(REMOTE_PORT_HEADER));
        }
        assertThat(server.requests()).hasSize(2);
    }

    @Test public void
    with_a_reused_http_client_instance_the_request_after_a_response_whose_status_is_checked_but_body_never_read_is_sent_on_the_same_connection() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());
        Response checked = given().config(config).get(server.url("/port"));
        String first = checked.then().statusCode(200).extract().header(REMOTE_PORT_HEADER);
        awaitJdkConnectionReturned(checked);

        String next = remotePortOfReadResponse(config, server.url("/port"));

        assertThat(next).isEqualTo(first);
        assertThat(server.requests()).hasSize(2);
    }

    @Test public void
    with_a_reused_http_client_instance_the_request_after_an_empty_response_is_sent_on_the_same_connection() {
        server.respond("/empty", Responder.text(200, "").withRemotePort());
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());
        Response empty = given().config(config).get(server.url("/empty"));
        String first = empty.header(REMOTE_PORT_HEADER);
        awaitJdkConnectionReturned(empty);

        String next = remotePortOfReadResponse(config, server.url("/port"));

        assertThat(next).isEqualTo(first);
        assertThat(server.requests()).hasSize(2);
    }

    // The JDK client puts a connection back in its pool on its own thread once it has the whole body, whether it's read
    // or not, so a request sent right after may get a new connection. The end of the body comes after that, so reading
    // the body waits for it.
    private static void awaitJdkConnectionReturned(Response response) {
        if (TestClient.isJdk()) {
            response.asString();
        }
    }

    // By the exception of the HTTP client, which REST Assured wraps once
    private static void isCausedByTheHttpClient(Throwable timeout) {
        assertThat(timeout.getCause()).isInstanceOf(IOException.class).isNotInstanceOfAny(ConnectionTimeoutException.class, ResponseTimeoutException.class);
    }

    private void releaseServerAfter(Duration delay) {
        executor.submit(() -> {
            Thread.sleep(delay.toMillis());
            releaseServer.countDown();
            return null;
        });
    }

    private Future<?> closeInBackground(InputStream body) {
        return executor.submit(() -> {
            body.close();
            return null;
        });
    }

    // A server thread waits for the latch of the test that registered its responder, not for that of a later test
    private static void await(CountDownLatch release) {
        try {
            release.await(SERVER_HOLD_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Responder bodyOfSize(int size) {
        return (request, response) -> {
            response.setStatus(200);
            response.setContentType("application/octet-stream");
            byte[] chunk = new byte[8192];
            for (int written = 0; written < size; written += chunk.length) {
                response.getOutputStream().write(chunk, 0, Math.min(chunk.length, size - written));
            }
        };
    }

    // Sends the start of the body, then the rest once the test releases the server
    private static Responder slowBody(CountDownLatch release) {
        return (request, response) -> {
            response.setStatus(200);
            response.setContentType("text/plain;charset=ISO-8859-1");
            response.getOutputStream().write("first part".getBytes(ISO_8859_1));
            response.flushBuffer();
            await(release);
            response.getOutputStream().write(", rest".getBytes(ISO_8859_1));
        };
    }

    /**
     * A server socket that never accepts and whose accept queue is full, so that the kernel drops further connection
     * attempts (SYNs) and a connect times out. A non-routable address isn't used because without a network route the
     * connect fails at once instead of timing out.
     */
    private static final class UnresponsiveServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final List<Socket> queued = new ArrayList<>();

        private UnresponsiveServer(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        static UnresponsiveServer start() throws IOException {
            UnresponsiveServer server = new UnresponsiveServer(new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")));
            try {
                // Linux queues backlog + 1 connections, other systems may queue a few more
                while (server.queued.size() < 64) {
                    Socket socket = new Socket();
                    server.queued.add(socket);
                    try {
                        socket.connect(server.serverSocket.getLocalSocketAddress(), 200);
                    } catch (SocketTimeoutException e) {
                        return server;
                    } catch (ConnectException e) {
                        Assumptions.abort("This system refuses connections when the accept queue is full instead of dropping them");
                    }
                }
                throw new IllegalStateException("Couldn't fill the accept queue of the server socket");
            } catch (IOException | RuntimeException e) {
                server.close();
                throw e;
            }
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        @Override
        public void close() throws IOException {
            for (Socket socket : queued) {
                socket.close();
            }
            serverSocket.close();
        }
    }
}

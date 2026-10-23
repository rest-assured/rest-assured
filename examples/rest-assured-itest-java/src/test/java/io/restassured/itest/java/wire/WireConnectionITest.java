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

import io.restassured.config.ConnectionConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.apache.http.conn.ConnectTimeoutException;
import org.eclipse.jetty.server.HttpConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The exceptions for connections that fail or time out, {@link ConnectionConfig}, and what happens to the connection
 * of a response that is streamed or never read. Which connection a request arrives on is told by the client port that
 * the server answers in a header.
 */
@Timeout(10)
public class WireConnectionITest extends WireTest {
    private static final String REMOTE_PORT_HEADER = "X-Remote-Port";
    private static final int LARGE_BODY_SIZE = 1024 * 1024;
    // Larger than the 8 KiB the client buffers along with the headers, small enough for the socket buffers to take
    private static final int UNREAD_BODY_SIZE = 200 * 1024;

    private CountDownLatch releaseServer;
    private ExecutorService executor;

    @BeforeEach
    void answerWithTheRemotePort() {
        releaseServer = new CountDownLatch(1);
        server.respond("/port", withRemotePort(Responder.ok()));
        server.respond("/large", withRemotePort(bodyOfSize(LARGE_BODY_SIZE)));
        server.respond("/unread", withRemotePort(bodyOfSize(UNREAD_BODY_SIZE)));
        server.respond("/slow", withRemotePort(slowBody()));
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
    a_read_timeout_throws_socket_timeout_exception() {
        server.respond("/wait", (request, response) -> {
            awaitRelease();
            Responder.ok().respond(request, response);
        });

        assertThatThrownBy(() -> given().config(config().httpClient(httpClientConfig().readTimeout(Duration.ofMillis(300)))).get(server.url("/wait")))
                .isExactlyInstanceOf(SocketTimeoutException.class);
        assertThat(server.onlyRequest().target()).isEqualTo("/wait");
    }

    @Test public void
    a_connect_timeout_throws_connect_timeout_exception() throws IOException {
        try (UnresponsiveServer unresponsive = UnresponsiveServer.start()) {
            assertThatThrownBy(() -> given().config(config().httpClient(httpClientConfig().connectTimeout(Duration.ofMillis(300)))).get("http://127.0.0.1:" + unresponsive.port() + "/"))
                    // Today's behavior: HttpClient 4's ConnectTimeoutException, an InterruptedIOException but not a SocketTimeoutException
                    .isExactlyInstanceOf(ConnectTimeoutException.class)
                    .isInstanceOf(InterruptedIOException.class)
                    .isNotInstanceOf(SocketTimeoutException.class);
        }
    }

    // ConnectionConfig

    @Test public void
    by_default_each_request_is_sent_on_a_new_connection() {
        String first = given().get(server.url("/port")).header(REMOTE_PORT_HEADER);
        String second = given().get(server.url("/port")).header(REMOTE_PORT_HEADER);

        assertThat(second).isNotEqualTo(first);
    }

    @Test public void
    a_reused_http_client_instance_sends_the_next_request_on_the_same_connection() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());

        String first = remotePortOfReadResponse(config);
        String second = remotePortOfReadResponse(config);

        assertThat(second).isEqualTo(first);
    }

    @Test public void
    close_idle_connections_after_each_response_sends_the_next_request_on_a_new_connection() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse());

        String first = remotePortOfReadResponse(config);
        String second = remotePortOfReadResponse(config);

        assertThat(second).isNotEqualTo(first);
    }

    @Test public void
    close_idle_connections_after_an_idle_time_keeps_connections_that_were_idle_for_less() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponseAfter(1, TimeUnit.HOURS));

        String first = remotePortOfReadResponse(config);
        String second = remotePortOfReadResponse(config);

        assertThat(second).isEqualTo(first);
    }

    @Test public void
    close_idle_connections_after_each_response_closes_the_connection_before_a_large_body_is_read() {
        RestAssuredConfig config = config().connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse());

        // Today's behavior: the connection is closed as soon as the response arrives, so only what the client already
        // buffered can be read, and a larger body fails
        assertThatThrownBy(() -> given().config(config).get(server.url("/unread")).asByteArray())
                .isExactlyInstanceOf(SocketException.class)
                .hasMessage("Socket closed");
    }

    @Test public void
    dont_close_idle_connections_after_each_response_throws_illegal_argument_exception() {
        // Today's behavior: it passes null to the constructor that rejects null
        assertThatThrownBy(() -> connectionConfig().dontCloseIdleConnectionsAfterEachResponse())
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    // Streaming responses

    @Test public void
    closing_a_partly_read_stream_waits_for_the_rest_of_the_body() throws Exception {
        InputStream body = given().get(server.url("/slow")).asInputStream();
        assertThat(body.readNBytes(5)).isEqualTo("first".getBytes(ISO_8859_1));

        Future<?> closing = executor.submit(() -> {
            body.close();
            return null;
        });

        assertThatThrownBy(() -> closing.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        releaseServer.countDown();
        closing.get(5, TimeUnit.SECONDS);
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

        String next = remotePortOfReadResponse(config);

        assertThat(next).isEqualTo(streamed.header(REMOTE_PORT_HEADER));
    }

    @Test public void
    with_close_idle_connections_closing_a_partly_read_stream_closes_the_connection_without_reading_the_rest() throws Exception {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance()).connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse());
        Response streamed = given().config(config).get(server.url("/slow"));
        InputStream body = streamed.asInputStream();
        assertThat(body.readNBytes(5)).isEqualTo("first".getBytes(ISO_8859_1));

        Future<?> closing = executor.submit(() -> {
            body.close();
            return null;
        });

        closing.get(5, TimeUnit.SECONDS);
        String next = remotePortOfReadResponse(config);
        assertThat(next).isNotEqualTo(streamed.header(REMOTE_PORT_HEADER));
    }

    @Test public void
    a_response_that_is_never_read_doesnt_block_the_next_request() {
        Response unread = given().get(server.url("/unread"));

        String next = given().get(server.url("/port")).then().statusCode(200).extract().header(REMOTE_PORT_HEADER);

        assertThat(next).isNotEqualTo(unread.header(REMOTE_PORT_HEADER));
        assertThat(server.requests()).hasSize(2);
    }

    @Test public void
    with_a_reused_http_client_instance_a_response_that_is_never_read_makes_the_next_request_fail() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());
        given().config(config).get(server.url("/unread"));

        // Today's behavior: the unread response keeps the only connection of HttpClient 4's BasicClientConnectionManager
        assertThatThrownBy(() -> given().config(config).get(server.url("/port")))
                .isExactlyInstanceOf(IllegalStateException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test public void
    with_a_reused_http_client_instance_a_response_whose_status_is_checked_but_body_never_read_makes_the_next_request_fail() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());
        given().config(config).get(server.url("/port")).then().statusCode(200);

        // Today's behavior: only body expectations read the body, so the connection stays allocated
        assertThatThrownBy(() -> given().config(config).get(server.url("/port")))
                .isExactlyInstanceOf(IllegalStateException.class);
    }

    private String remotePortOfReadResponse(RestAssuredConfig config) {
        Response response = given().config(config).get(server.url("/port"));
        response.asString();
        return response.header(REMOTE_PORT_HEADER);
    }

    private void awaitRelease() {
        try {
            releaseServer.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Responder withRemotePort(Responder responder) {
        return (request, response) -> {
            InetSocketAddress remote = (InetSocketAddress) HttpConnection.getCurrentConnection().getEndPoint().getRemoteAddress();
            response.setHeader(REMOTE_PORT_HEADER, String.valueOf(remote.getPort()));
            responder.respond(request, response);
        };
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
    private Responder slowBody() {
        return (request, response) -> {
            response.setStatus(200);
            response.setContentType("text/plain;charset=ISO-8859-1");
            response.getOutputStream().write("first part".getBytes(ISO_8859_1));
            response.flushBuffer();
            awaitRelease();
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
            // Linux queues backlog + 1 connections, other systems may queue a few more
            while (server.queued.size() < 64) {
                Socket socket = new Socket();
                try {
                    socket.connect(server.serverSocket.getLocalSocketAddress(), 200);
                } catch (SocketTimeoutException e) {
                    socket.close();
                    return server;
                }
                server.queued.add(socket);
            }
            server.close();
            throw new IllegalStateException("Couldn't fill the accept queue of the server socket");
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

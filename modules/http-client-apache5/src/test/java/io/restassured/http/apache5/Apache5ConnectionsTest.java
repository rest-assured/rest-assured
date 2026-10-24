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

package io.restassured.http.apache5;

import io.restassured.common.exception.ConnectionTimeoutException;
import io.restassured.common.exception.ResponseTimeoutException;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.RequestOptions;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.customizer;
import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.request;
import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.send;
import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.settings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * The connections of the adapter: when one is used again or dropped, the timeouts, and the exceptions of a connection
 * that fails.
 */
class Apache5ConnectionsTest {
    // A body that's sent slowly, so that it's never read to its end in a test
    private static final RawServer.Handler ENDLESS_BODY = (request, out) -> {
        out.write("HTTP/1.1 200 OK\r\nContent-Length: 100000000\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
        byte[] chunk = new byte[1024];
        while (true) {
            out.write(chunk);
            out.flush();
            Thread.sleep(5);
        }
    };

    private RawServer server;
    private HttpClientInstance client;

    @BeforeEach
    void startServer() throws IOException {
        server = new RawServer();
        client = new Apache5HttpClientProvider().newClient(settings());
    }

    @AfterEach
    void stopServer() throws IOException {
        client.close();
        server.close();
    }

    @Test
    void a_body_read_to_its_end_gives_the_connection_back_for_the_next_request() throws IOException {
        send(client, request("GET", server.uri("/1"), null));
        send(client, request("GET", server.uri("/2"), null));

        assertThat(server.connectionCount()).isEqualTo(1);
    }

    @Test
    void a_body_that_has_arrived_but_wasnt_read_gives_the_connection_back_too() throws Exception {
        HttpResponse response = client.send(request("GET", server.uri("/1"), null));
        // Until the body has arrived
        Thread.sleep(100);
        response.close();
        send(client, request("GET", server.uri("/2"), null));

        assertThat(server.connectionCount()).isEqualTo(1);
    }

    @Test
    void closing_a_partly_read_body_drops_the_connection_without_reading_the_rest() throws Exception {
        server.handle(ENDLESS_BODY);

        HttpResponse response = client.send(request("GET", server.uri("/"), null));
        response.body().readNBytes(10);
        response.close();

        assertThat(server.awaitConnectionsClosed(5_000)).isTrue();
    }

    @Test
    void with_a_pool_of_one_connection_the_next_request_gets_a_new_one_after_a_partly_read_body_is_closed() throws Exception {
        client.close();
        client = new Apache5HttpClientProvider().newClient(settings(customizer(builder -> builder.setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create().setMaxConnTotal(1).setMaxConnPerRoute(1).build()))));
        server.handle((request, out) -> request.requestLine().startsWith("GET /endless") ? ENDLESS_BODY.handle(request, out)
                : write(out, RawServer.response(200, "ok")));

        HttpResponse endless = client.send(request("GET", server.uri("/endless"), null));
        endless.body().readNBytes(10);
        endless.close();
        HttpResponse next = send(client, request("GET", server.uri("/next"), null));

        assertThat(next.statusCode()).isEqualTo(200);
        assertThat(server.connectionCount()).isEqualTo(2);
    }

    @Test
    void the_connection_request_timeout_fires_when_a_customizer_limits_the_pool() throws IOException {
        client.close();
        client = new Apache5HttpClientProvider().newClient(new ClientSettings(null, null, HttpVersion.HTTP_1_1, null, Duration.ofMillis(200),
                List.of(customizer(builder -> builder.setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(1).setMaxConnPerRoute(1).build())))));

        try (HttpResponse unread = client.send(request("GET", server.uri("/1"), null))) {
            assertThatThrownBy(() -> client.send(request("GET", server.uri("/2"), null)))
                    .isInstanceOf(ConnectionTimeoutException.class)
                    .hasCauseInstanceOf(ConnectionRequestTimeoutException.class);
        }
    }

    @Test
    void no_limit_on_the_connections_by_default() throws IOException {
        List<HttpResponse> unread = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            unread.add(client.send(request("GET", server.uri("/" + i), null)));
        }
        for (HttpResponse response : unread) {
            response.close();
        }

        assertThat(server.requests()).hasSize(8);
    }

    @Test
    void close_idle_connections_closes_the_connections_idle_for_longer_than_the_idle_time() throws Exception {
        send(client, request("GET", server.uri("/1"), null));

        assertThat(client.closeIdleConnections(Duration.ofHours(1))).isTrue();
        send(client, request("GET", server.uri("/2"), null));
        assertThat(server.connectionCount()).isEqualTo(1);

        Thread.sleep(50);
        assertThat(client.closeIdleConnections(Duration.ofMillis(10))).isTrue();
        assertThat(server.awaitConnectionsClosed(5_000)).isTrue();
        send(client, request("GET", server.uri("/3"), null));
        assertThat(server.connectionCount()).isEqualTo(2);
    }

    @Test
    void closing_the_client_closes_its_connections() throws Exception {
        send(client, request("GET", server.uri("/"), null));

        client.close();

        assertThat(server.awaitConnectionsClosed(5_000)).isTrue();
    }

    @Test
    void the_read_timeout_covers_waiting_for_the_response_head() {
        server.respond(request -> null);

        assertThatThrownBy(() -> client.send(withReadTimeout(server.uri("/"), Duration.ofMillis(200))))
                .isInstanceOf(ResponseTimeoutException.class)
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void the_read_timeout_covers_each_read_of_the_body() throws IOException {
        server.handle((request, out) -> {
            out.write("HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\nhalf".getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            return true;
        });

        try (HttpResponse response = client.send(withReadTimeout(server.uri("/"), Duration.ofMillis(200)))) {
            InputStream body = response.body();
            assertThat(body.readNBytes(4)).isEqualTo("half".getBytes());
            assertThatThrownBy(body::read).isInstanceOf(ResponseTimeoutException.class);
        }
    }

    @Test
    void the_connect_timeout_fires_when_the_server_doesnt_accept_the_connection() throws IOException {
        // A full backlog leaves the next connection waiting
        try (ServerSocket full = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            List<Socket> backlog = new ArrayList<>();
            try {
                for (int i = 0; i < 3; i++) {
                    Socket socket = new Socket();
                    try {
                        socket.connect(full.getLocalSocketAddress(), 200);
                    } catch (IOException e) {
                        socket.close();
                        break;
                    }
                    backlog.add(socket);
                }
                HttpClientInstance timingOut = new Apache5HttpClientProvider().newClient(new ClientSettings(null, null, HttpVersion.HTTP_1_1,
                        Duration.ofMillis(300), null, List.of()));
                try {
                    assertThatThrownBy(() -> timingOut.send(request("GET", URI.create("http://" + RawServer.LOOPBACK + ":" + full.getLocalPort() + "/"), null)))
                            .isInstanceOf(ConnectionTimeoutException.class)
                            .hasCauseInstanceOf(ConnectTimeoutException.class);
                } finally {
                    timingOut.close();
                }
            } finally {
                for (Socket socket : backlog) {
                    socket.close();
                }
            }
        }
    }

    @Test
    void the_connect_timeout_covers_the_tls_handshake() throws IOException {
        // A server that accepts the connection but never answers the ClientHello. HttpClient 5.2.1 to 5.4.4 time the
        // handshake with the SocketConfig, whose default is 3 minutes, and 5.6.5 with the connect timeout.
        try (ServerSocket silent = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            List<Socket> accepted = new CopyOnWriteArrayList<>();
            RawServer.daemon(() -> {
                try {
                    while (true) {
                        accepted.add(silent.accept());
                    }
                } catch (IOException e) {
                    // Closed
                }
            });
            HttpClientInstance timingOut = new Apache5HttpClientProvider().newClient(new ClientSettings(null, null, HttpVersion.HTTP_1_1,
                    Duration.ofMillis(300), null, List.of()));
            try {
                assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                        assertThatThrownBy(() -> timingOut.send(request("GET", URI.create("https://" + RawServer.LOOPBACK + ":" + silent.getLocalPort() + "/"), null)))
                                .isInstanceOf(ConnectionTimeoutException.class)
                                .hasCauseInstanceOf(ConnectTimeoutException.class));
            } finally {
                timingOut.close();
                for (Socket socket : accepted) {
                    socket.close();
                }
            }
        }
    }

    @Test
    void a_connection_manager_that_a_customizer_keeps_for_later_clients_fails_with_a_message_that_says_what_to_do() throws IOException {
        // Each request's client is closed after its response, which shuts down the connection manager that it owns
        PoolingHttpClientConnectionManager kept = PoolingHttpClientConnectionManagerBuilder.create().build();
        ClientSettings settings = settings(customizer(builder -> builder.setConnectionManager(kept)));
        HttpClientInstance first = new Apache5HttpClientProvider().newClient(settings);
        send(first, request("GET", server.uri("/"), null));
        first.close();

        HttpClientInstance second = new Apache5HttpClientProvider().newClient(settings);
        try {
            assertThatThrownBy(() -> second.send(request("GET", server.uri("/"), null)))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessageStartingWith("The connection manager that a customizer of HttpClientBuilder set has been shut down")
                    .hasMessageContaining("setConnectionManagerShared(true)")
                    .hasMessageContaining("reuseHttpClientInstance()")
                    .cause().hasMessage("Connection pool shut down");
        } finally {
            second.close();
        }
    }

    @Test
    void a_connection_manager_that_a_customizer_marks_as_shared_outlives_the_clients_and_keeps_its_connections() throws IOException {
        try (PoolingHttpClientConnectionManager shared = PoolingHttpClientConnectionManagerBuilder.create().build()) {
            ClientSettings settings = settings(customizer(builder -> builder.setConnectionManager(shared).setConnectionManagerShared(true)));
            for (int i = 0; i < 2; i++) {
                HttpClientInstance each = new Apache5HttpClientProvider().newClient(settings);
                try {
                    send(each, request("GET", server.uri("/"), null));
                } finally {
                    each.close();
                }
            }

            assertThat(server.requests()).hasSize(2);
            assertThat(server.connectionCount()).isEqualTo(1);
        }
    }

    @Test
    void a_refused_connection_and_an_unknown_host_fail_with_the_exceptions_of_http_client() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }

        assertThatThrownBy(() -> client.send(request("GET", URI.create("http://localhost:" + closedPort + "/"), null)))
                .isInstanceOf(HttpHostConnectException.class);
        assertThatThrownBy(() -> client.send(request("GET", URI.create("http://unknown-host.invalid/"), null)))
                .isInstanceOf(UnknownHostException.class)
                .hasMessageStartingWith("unknown-host.invalid");
    }

    private static HttpRequest withReadTimeout(URI uri, Duration readTimeout) {
        return new HttpRequest("GET", uri, List.of(), null, new RequestOptions(readTimeout, false, null));
    }

    private static boolean write(OutputStream out, String response) throws IOException {
        out.write(response.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
        return true;
    }
}

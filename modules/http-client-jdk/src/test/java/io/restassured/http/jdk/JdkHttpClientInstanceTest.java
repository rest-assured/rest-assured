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

package io.restassured.http.jdk;

import com.sun.net.httpserver.HttpServer;
import io.restassured.common.exception.ResponseTimeoutException;
import io.restassured.common.spi.http.ClientCustomizer;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.ClientSharing;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpClientProvider;
import io.restassured.common.spi.http.HttpClientSpi;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.KeyMaterial;
import io.restassured.common.spi.http.NtlmCredentials;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieManager;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class JdkHttpClientInstanceTest {
    private RawServer server;
    private HttpClientInstance client;

    @BeforeEach
    void startServer() throws IOException {
        server = new RawServer();
        client = new JdkHttpClientProvider().newClient(settings());
    }

    @AfterEach
    void stopServer() throws IOException {
        client.close();
        server.close();
    }

    static ClientSettings settings(ClientCustomizer... customizers) {
        return new ClientSettings(TlsSettings.builder().build(), null, HttpVersion.HTTP_1_1, null, null, List.of(customizers));
    }

    static HttpRequest request(String method, URI uri, RequestBody body, HttpHeader... headers) {
        return new HttpRequest(method, uri, List.of(headers), body, RequestOptions.DEFAULT);
    }

    private HttpResponse send(HttpRequest request) throws IOException {
        HttpResponse response = client.send(request);
        if (response.body() != null) {
            response.body().readAllBytes();
        }
        response.close();
        return response;
    }

    @Test
    void the_provider_is_found_with_service_loader_and_describes_the_jdk_client() {
        List<HttpClientProvider> providers = ServiceLoader.load(HttpClientProvider.class).stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());

        assertThat(providers).hasSize(1);
        HttpClientProvider provider = providers.get(0);
        assertThat(provider).isInstanceOf(JdkHttpClientProvider.class);
        assertThat(provider.name()).isEqualTo("jdk");
        assertThat(provider.displayName()).isEqualTo("the JDK HTTP client");
        assertThat(provider.artifact()).isEqualTo("io.rest-assured:http-client-jdk");
        assertThat(provider.libraryDescription()).isEqualTo("Java " + Runtime.version());
        assertThat(provider.sharing()).isEqualTo(ClientSharing.SHARED);
        assertThat(provider.builderType()).isEqualTo(HttpClient.Builder.class);
    }

    @Test
    void the_version_file_names_the_http_spi_revision_the_module_was_built_for() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/META-INF/rest-assured/http-client-jdk.properties")) {
            properties.load(in);
        }

        assertThat(properties.getProperty("http-spi-revision")).isEqualTo(String.valueOf(HttpClientSpi.REVISION));
        assertThat(properties.getProperty("version")).doesNotContain("${");
    }

    @Test
    void sends_http_1_1_with_the_method_in_upper_case() throws IOException {
        send(request("get", server.uri("/x?a=b"), null));

        assertThat(server.onlyRequest().requestLine()).isEqualTo("GET /x?a=b HTTP/1.1");
        assertThat(server.onlyRequest().header("Content-Length")).isNull();
    }

    @Test
    void sends_the_headers_in_their_order_and_case() throws IOException {
        send(request("GET", server.uri("/"), null, new HttpHeader("X-First", "1"), new HttpHeader("x-second", "2"), new HttpHeader("X-First", "3")));

        assertThat(server.onlyRequest().headerLines()).containsSubsequence("X-First: 1", "X-First: 3");
        assertThat(server.onlyRequest().header("x-second")).isEqualTo("2");
    }

    @Test
    void head_sends_no_content_length_from_java_18_and_a_zero_one_on_java_17() throws IOException {
        server.respond(request -> "HTTP/1.1 200 OK\r\nContent-Length: 5\r\nConnection: close\r\n\r\n");

        HttpResponse response = send(request("HEAD", server.uri("/"), null));

        assertThat(response.body()).isNull();
        assertThat(response.contentLength()).isEqualTo(5);
        // JDK differences table: Content-Length: 0 on a body-less HEAD on Java 17
        assertThat(server.onlyRequest().header("Content-Length")).isEqualTo(Runtime.version().feature() >= 18 ? null : "0");
    }

    @Test
    void a_custom_method_without_body_sends_a_zero_content_length() throws IOException {
        send(request("purge", server.uri("/"), null));

        assertThat(server.onlyRequest().requestLine()).isEqualTo("PURGE / HTTP/1.1");
        // JDK differences table: Content-Length: 0 on OPTIONS, TRACE and custom methods
        assertThat(server.onlyRequest().header("Content-Length")).isEqualTo("0");
    }

    @Test
    void sends_bytes_with_their_length_and_the_content_type_of_the_body() throws IOException {
        send(request("POST", server.uri("/"), RequestBody.bytes("hello".getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8")));

        RawServer.Request received = server.onlyRequest();
        assertThat(received.bodyAsString()).isEqualTo("hello");
        assertThat(received.header("Content-Length")).isEqualTo("5");
        assertThat(received.header("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
    }

    @Test
    void a_content_type_header_wins_over_the_content_type_of_the_body() throws IOException {
        send(request("POST", server.uri("/"), RequestBody.bytes(new byte[]{1}, "application/octet-stream"), new HttpHeader("Content-Type", "application/x-custom")));

        assertThat(server.onlyRequest().headerLines()).filteredOn(line -> line.toLowerCase().startsWith("content-type")).containsExactly("Content-Type: application/x-custom");
    }

    @Test
    void sends_a_stream_of_unknown_length_chunked_and_one_of_known_length_with_its_length() throws IOException {
        send(request("PUT", server.uri("/chunked"), RequestBody.stream(new ByteArrayInputStream("chunked".getBytes()), RequestBody.UNKNOWN_LENGTH, null)));
        send(request("PUT", server.uri("/length"), RequestBody.stream(new ByteArrayInputStream("length".getBytes()), 6, null)));

        RawServer.Request chunked = server.requests().get(0);
        // JDK differences table: the JDK client spells the header Transfer-encoding
        assertThat(chunked.headerLines()).contains("Transfer-encoding: chunked");
        assertThat(chunked.bodyAsString()).isEqualTo("chunked");
        RawServer.Request withLength = server.requests().get(1);
        assertThat(withLength.header("Content-Length")).isEqualTo("6");
        assertThat(withLength.bodyAsString()).isEqualTo("length");
    }

    @Test
    void sends_a_file(@TempDir Path directory) throws IOException {
        Path file = Files.writeString(directory.resolve("body.txt"), "from a file");

        send(request("POST", server.uri("/"), RequestBody.file(file.toFile(), "text/plain")));

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("from a file");
        assertThat(server.onlyRequest().header("Content-Length")).isEqualTo("11");
    }

    @Test
    void sends_a_small_repeatable_writer_with_its_length_and_another_writer_through_a_pipe() throws IOException {
        byte[] large = new byte[100_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) ('a' + i % 26);
        }
        send(request("POST", server.uri("/small"), RequestBody.writer(out -> out.write("small".getBytes()), 5, true, "text/plain")));
        send(request("POST", server.uri("/piped"), RequestBody.writer(out -> {
            for (int i = 0; i < large.length; i += 1000) {
                out.write(large, i, 1000);
            }
        }, RequestBody.UNKNOWN_LENGTH, false, "text/plain")));

        assertThat(server.requests().get(0).header("Content-Length")).isEqualTo("5");
        assertThat(server.requests().get(0).bodyAsString()).isEqualTo("small");
        assertThat(server.requests().get(1).header("Transfer-encoding")).isEqualTo("chunked");
        assertThat(server.requests().get(1).body).isEqualTo(large);
    }

    @Test
    void a_writer_that_fails_fails_the_request() {
        RequestBody failing = RequestBody.writer(out -> {
            out.write("partly".getBytes());
            throw new IOException("the writer failed");
        }, RequestBody.UNKNOWN_LENGTH, false, null);

        assertThatThrownBy(() -> send(request("POST", server.uri("/"), failing)))
                .isInstanceOf(IOException.class)
                .satisfies(e -> assertThat(rootCause(e)).hasMessage("the writer failed"));
    }

    @Test
    void expect_100_continue_from_the_header_or_the_options_is_sent_as_the_jdk_spells_it() throws IOException {
        send(request("POST", server.uri("/header"), RequestBody.bytes("a".getBytes(), null), new HttpHeader("Expect", "100-continue")));
        send(new HttpRequest("POST", server.uri("/options"), List.of(), RequestBody.bytes("a".getBytes(), null), new RequestOptions(null, true, null)));

        // JDK differences table: Expect: 100-Continue with a capital C
        assertThat(server.requests()).allSatisfy(request -> assertThat(request.headerLines()).contains("Expect: 100-Continue"));
    }

    @Test
    void another_expect_header_is_unsupported() {
        assertThatThrownBy(() -> send(request("GET", server.uri("/"), null, new HttpHeader("Expect", "something"))))
                .isInstanceOf(UnsupportedFeatureException.class)
                .hasMessage("The HTTP client can't send the header Expect: something, only Expect: 100-continue.");
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void a_restricted_header_is_unsupported_with_the_system_property_that_allows_it() {
        for (String name : List.of("Host", "Connection", "Content-Length", "Upgrade")) {
            assertThatThrownBy(() -> send(request("GET", server.uri("/"), null, new HttpHeader(name, "1"))))
                    .isInstanceOf(UnsupportedFeatureException.class)
                    .hasMessage("The HTTP client can't send a " + name + " header. To do it anyway, start the JVM with -Djdk.httpclient.allowRestrictedHeaders="
                            + name.toLowerCase() + ".");
        }
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void ntlm_is_unsupported() {
        RequestOptions ntlm = new RequestOptions(null, false, new NtlmCredentials("localhost", -1, "user", "password", "workstation", "domain"));

        assertThatThrownBy(() -> client.send(new HttpRequest("GET", server.uri("/"), List.of(), null, ntlm)))
                .isInstanceOf(UnsupportedFeatureException.class)
                .hasMessage("The HTTP client can't authenticate with NTLM.");
    }

    @Test
    void a_proxy_with_the_https_scheme_is_unsupported() {
        ClientSettings settings = new ClientSettings(TlsSettings.builder().build(), new ProxySettings("https", "localhost", 8888, null, null),
                HttpVersion.HTTP_1_1, null, null, List.of());

        assertThatThrownBy(() -> new JdkHttpClientProvider().newClient(settings))
                .isInstanceOf(UnsupportedFeatureException.class)
                .hasMessage("The HTTP client can't connect to a proxy over TLS (a proxy with the scheme https).");
    }

    @Test
    void tls_settings_that_cant_be_loaded_fail_only_https_requests() throws IOException {
        TlsSettings missing = TlsSettings.builder().trustStore(new KeyMaterial("does-not-exist.p12", "secret", "PKCS12", null)).build();
        client.close();
        client = new JdkHttpClientProvider().newClient(new ClientSettings(missing, null, HttpVersion.HTTP_1_1, null, null, List.of()));

        send(request("GET", server.uri("/"), null));

        assertThatThrownBy(() -> client.send(request("GET", URI.create("https://localhost:" + server.port() + "/"), null)))
                .isInstanceOf(SSLException.class)
                .hasMessageStartingWith("Failed to create the SSLContext from the configured SSL settings: ")
                .hasCauseInstanceOf(FileNotFoundException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void returns_the_response_without_reason_phrase_and_with_the_headers_the_jdk_gives() throws IOException {
        server.respond(request -> RawServer.response(201, "created", "X-Custom: a", "X-Custom: b"));

        HttpResponse response = client.send(request("POST", server.uri("/"), null));

        assertThat(response.protocolVersion()).isEqualTo("HTTP/1.1");
        assertThat(response.statusCode()).isEqualTo(201);
        // JDK differences table: no reason phrase, which core fills in
        assertThat(response.reasonPhrase()).isNull();
        assertThat(response.contentLength()).isEqualTo(7);
        assertThat(new String(response.body().readAllBytes())).isEqualTo("created");
        // JDK differences table: header names in lower case and sorted
        assertThat(response.headers()).containsExactly(new HttpHeader("connection", "close"), new HttpHeader("content-length", "7"),
                new HttpHeader("x-custom", "a"), new HttpHeader("x-custom", "b"));
        response.close();
    }

    @Test
    void a_204_and_a_304_have_no_body_and_a_chunked_body_has_no_length() throws IOException {
        server.respond(request -> request.requestLine().contains("/chunked")
                ? "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n2\r\nok\r\n0\r\n\r\n"
                : "HTTP/1.1 " + request.requestLine().substring(5, 8) + " X\r\nConnection: close\r\n\r\n");

        assertThat(send(request("GET", server.uri("/204"), null)).body()).isNull();
        assertThat(send(request("GET", server.uri("/304"), null)).body()).isNull();
        HttpResponse chunked = client.send(request("GET", server.uri("/chunked"), null));
        assertThat(chunked.contentLength()).isEqualTo(-1);
        assertThat(new String(chunked.body().readAllBytes())).isEqualTo("ok");
        chunked.close();
    }

    @Test
    void doesnt_follow_redirects_or_keep_cookies_even_when_a_customizer_asks_for_them() throws IOException {
        client.close();
        client = new JdkHttpClientProvider().newClient(settings(new ClientCustomizer(HttpClient.Builder.class,
                builder -> builder.followRedirects(HttpClient.Redirect.ALWAYS).cookieHandler(new CookieManager()))));
        server.respond(request -> RawServer.response(302, "", "Location: /elsewhere", "Set-Cookie: a=b"));

        HttpResponse first = send(request("GET", server.uri("/"), null));
        send(request("GET", server.uri("/"), null));

        assertThat(first.statusCode()).isEqualTo(302);
        assertThat(server.requests()).hasSize(2);
        assertThat(server.requests().get(1).header("Cookie")).isNull();
    }

    @Test
    void an_authenticator_from_a_customizer_is_unsupported() {
        ClientSettings settings = settings(new ClientCustomizer(HttpClient.Builder.class, builder -> builder.authenticator(new Authenticator() {
        })));

        HttpClientInstance withAuthenticator = new JdkHttpClientProvider().newClient(settings);

        assertThatThrownBy(() -> withAuthenticator.send(request("GET", server.uri("/"), null)))
                .isInstanceOf(UnsupportedFeatureException.class)
                .hasMessageStartingWith("The HTTP client can't use an Authenticator set with customizeClient(..)");
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void the_customizers_run_once_for_each_client_that_is_used() throws IOException {
        AtomicInteger customized = new AtomicInteger();
        client.close();
        client = new JdkHttpClientProvider().newClient(settings(new ClientCustomizer(HttpClient.Builder.class, builder -> customized.incrementAndGet())));
        assertThat(customized).hasValue(0);

        send(request("GET", server.uri("/"), null));
        send(request("GET", server.uri("/"), null));

        assertThat(customized).hasValue(1);
    }

    @Test
    void the_writer_thread_ends_when_the_server_answers_before_reading_the_body() throws Exception {
        server.answerBeforeTheBody().respond(request -> RawServer.response(413, "too large"));
        long before = writerThreads();

        Throwable failure = null;
        try {
            client.send(request("POST", server.uri("/"), endlessWriter())).close();
        } catch (IOException e) {
            // The JDK client may also fail while it's still sending the body that the server doesn't read
            failure = e;
        }

        awaitWriterThreads(before, failure);
    }

    @Test
    void the_writer_thread_ends_when_the_request_times_out() throws Exception {
        server.answerBeforeTheBody().respond(request -> {
            sleep(Duration.ofSeconds(2));
            return null;
        });
        long before = writerThreads();
        HttpRequest request = new HttpRequest("POST", server.uri("/"), List.of(), endlessWriter(), new RequestOptions(Duration.ofMillis(300), false, null));

        Throwable failure = catchThrowable(() -> client.send(request));

        assertThat(failure).isInstanceOf(ResponseTimeoutException.class);
        awaitWriterThreads(before, failure);
    }

    @Test
    void a_stream_body_is_closed_with_the_response_and_when_the_request_fails() throws IOException {
        AtomicInteger closed = new AtomicInteger();

        // Never read, since a body of length 0 isn't sent
        send(request("POST", server.uri("/"), RequestBody.stream(closeCounting("", closed), 0, null)));
        assertThat(closed).hasValue(1);

        assertThatThrownBy(() -> client.send(request("POST", server.uri("/"), RequestBody.stream(closeCounting("body", closed), 4, null),
                new HttpHeader("Host", "other")))).isInstanceOf(UnsupportedFeatureException.class);
        assertThat(closed).hasValue(2);
    }

    private static InputStream closeCounting(String content, AtomicInteger closed) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() {
                closed.incrementAndGet();
            }
        };
    }

    // A body of unknown length, written by a writer that never stops, so that only a closed pipe ends its thread
    private static RequestBody endlessWriter() {
        byte[] chunk = new byte[8192];
        return RequestBody.writer(out -> {
            while (true) {
                out.write(chunk);
            }
        }, RequestBody.UNKNOWN_LENGTH, false, "application/octet-stream");
    }

    private static long writerThreads() {
        return Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().equals("rest-assured-request-body")).count();
    }

    private static void awaitWriterThreads(long expected, Throwable failure) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (writerThreads() > expected && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(writerThreads()).as("writer threads, after %s", String.valueOf(failure)).isEqualTo(expected);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void a_customizer_gets_the_builder() throws IOException {
        client.close();
        client = new JdkHttpClientProvider().newClient(settings(new ClientCustomizer(HttpClient.Builder.class,
                builder -> builder.version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)))));

        send(request("GET", server.uri("/"), null));

        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void the_read_timeout_is_the_time_to_wait_for_the_response_head() throws IOException {
        HttpServer slow = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        slow.createContext("/", exchange -> {
            try {
                Thread.sleep(2000);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        slow.start();
        try {
            URI uri = URI.create("http://localhost:" + slow.getAddress().getPort() + "/");
            HttpRequest request = new HttpRequest("GET", uri, List.of(), null, new RequestOptions(Duration.ofMillis(200), false, null));

            assertThatThrownBy(() -> client.send(request)).isInstanceOf(ResponseTimeoutException.class);
        } finally {
            slow.stop(0);
        }
    }

    @Test
    void reuses_connections_to_a_server_that_keeps_them_open() throws IOException {
        HttpServer keepAlive = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        List<Object> connections = new java.util.concurrent.CopyOnWriteArrayList<>();
        keepAlive.createContext("/", exchange -> {
            connections.add(exchange.getRemoteAddress());
            exchange.sendResponseHeaders(200, 2);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write("ok".getBytes());
            }
        });
        keepAlive.start();
        try {
            URI uri = URI.create("http://localhost:" + keepAlive.getAddress().getPort() + "/");
            for (int i = 0; i < 3; i++) {
                send(request("GET", uri, null));
            }

            assertThat(connections).hasSize(3);
            assertThat(connections.stream().distinct()).hasSize(1);
        } finally {
            keepAlive.stop(0);
        }
    }

    @Test
    void without_proxy_settings_the_default_proxy_selector_is_asked_at_each_request() throws IOException {
        ProxySelector original = ProxySelector.getDefault();
        try {
            send(request("GET", URI.create("http://localhost:" + server.port() + "/direct"), null));
            // Set after the client was created, as a system property may be
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", server.port())));
                }

                @Override
                public void connectFailed(URI uri, SocketAddress address, IOException e) {
                }
            });

            send(request("GET", URI.create("http://example.invalid/proxied"), null));
        } finally {
            ProxySelector.setDefault(original);
        }

        assertThat(server.requests().get(0).requestLine()).isEqualTo("GET /direct HTTP/1.1");
        assertThat(server.requests().get(1).requestLine()).isEqualTo("GET http://example.invalid/proxied HTTP/1.1");
    }

    @Test
    void close_can_be_called_on_every_java_version() {
        client.close();
        client.close();
    }

    private static Throwable rootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}

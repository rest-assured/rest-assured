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
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.entity.InputStreamFactory;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * How the adapter turns a request of REST Assured into one of HttpClient 5, and its response back, on the wire.
 */
class Apache5HttpClientInstanceTest {
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

    static ClientSettings settings(ClientCustomizer... customizers) {
        return new ClientSettings(null, null, HttpVersion.HTTP_1_1, null, null, List.of(customizers));
    }

    static ClientCustomizer customizer(Consumer<HttpClientBuilder> customizer) {
        return new ClientCustomizer(HttpClientBuilder.class, customizer);
    }

    static HttpRequest request(String method, URI uri, RequestBody body, HttpHeader... headers) {
        return new HttpRequest(method, uri, List.of(headers), body, RequestOptions.DEFAULT);
    }

    // Sends the request and reads the whole response
    static HttpResponse send(HttpClientInstance client, HttpRequest request) throws IOException {
        try (HttpResponse response = client.send(request)) {
            if (response.body() != null) {
                response.body().readAllBytes();
            }
            return response;
        }
    }

    private HttpResponse send(HttpRequest request) throws IOException {
        return send(client, request);
    }

    @Test
    void the_provider_is_found_with_service_loader_and_describes_the_client() {
        List<HttpClientProvider> providers = ServiceLoader.load(HttpClientProvider.class).stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());

        assertThat(providers).hasSize(1);
        HttpClientProvider provider = providers.get(0);
        assertThat(provider).isInstanceOf(Apache5HttpClientProvider.class);
        assertThat(provider.name()).isEqualTo("apache5");
        assertThat(provider.displayName()).isEqualTo("Apache HttpClient 5");
        assertThat(provider.artifact()).isEqualTo("io.rest-assured:http-client-apache5");
        assertThat(provider.libraryDescription()).matches("Apache HttpClient 5\\.\\d+.*");
        assertThat(provider.sharing()).isEqualTo(ClientSharing.PER_REQUEST);
        assertThat(provider.builderType()).isEqualTo(HttpClientBuilder.class);
    }

    @Test
    void the_version_file_names_the_http_spi_revision_the_module_was_built_for() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/META-INF/rest-assured/http-client-apache5.properties")) {
            properties.load(in);
        }

        assertThat(properties.getProperty("http-spi-revision")).isEqualTo(String.valueOf(HttpClientSpi.REVISION));
        assertThat(properties.getProperty("version")).doesNotContain("${");
    }

    @Test
    void sends_http_1_1_with_the_method_in_upper_case_and_the_uri_as_is() throws IOException {
        send(request("patch", server.uri("/x/y?q=a%20b&r=%2F"), null));

        assertThat(server.onlyRequest().requestLine()).isEqualTo("PATCH /x/y?q=a%20b&r=%2F HTTP/1.1");
    }

    @Test
    void sends_the_characters_of_the_uri_that_arent_ascii_percent_encoded_as_utf_8() throws IOException {
        // As the JDK client and HttpClient 4 do, for a path param when URL encoding is disabled
        send(request("GET", server.uri("/John%20å/Doe?q=å"), null));

        assertThat(server.onlyRequest().requestLine()).isEqualTo("GET /John%20%C3%A5/Doe?q=%C3%A5 HTTP/1.1");
    }

    @Test
    void sends_a_uri_with_user_info_without_it() throws IOException {
        // As REST Assured 6.x and the JDK client do. HttpClient 5 refuses a request URI with user info.
        send(request("GET", URI.create("http://user:secret@localhost:" + server.port() + "/x?q=1"), null));

        assertThat(server.onlyRequest().requestLine()).isEqualTo("GET /x?q=1 HTTP/1.1");
        assertThat(server.onlyRequest().header("Host")).isEqualTo("localhost:" + server.port());
    }

    @Test
    void sends_the_headers_in_their_order_and_case_also_repeated_and_empty_ones() throws IOException {
        send(request("GET", server.uri("/"), null, new HttpHeader("X-B", "2"), new HttpHeader("x-a", "1"), new HttpHeader("X-B", "3"),
                new HttpHeader("X-Empty", "")));

        assertThat(server.onlyRequest().headerLines()).filteredOn(line -> line.toLowerCase().startsWith("x-"))
                .containsExactly("X-B: 2", "x-a: 1", "X-B: 3", "X-Empty: ");
    }

    @Test
    void sends_the_headers_that_the_jdk_client_restricts() throws IOException {
        send(request("GET", server.uri("/"), null, new HttpHeader("Host", "example.test"), new HttpHeader("Upgrade", "example/1"),
                new HttpHeader("Expect", "something")));

        RawServer.Request request = server.onlyRequest();
        assertThat(request.headers("Host")).containsExactly("example.test");
        assertThat(request.header("Upgrade")).isEqualTo("example/1");
        assertThat(request.header("Expect")).isEqualTo("something");
    }

    @Test
    void sends_a_connection_header_as_given() throws IOException {
        server.respond(request -> RawServer.response(200, "ok", "Connection: close"));

        send(request("GET", server.uri("/"), null, new HttpHeader("Connection", "close")));

        assertThat(server.onlyRequest().header("Connection")).isEqualTo("close");
    }

    @Test
    void asks_for_no_encoding_and_doesnt_decode_the_body() throws IOException {
        byte[] gzipped = gzip("hello");
        server.handle((request, out) -> {
            out.write(("HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Length: " + gzipped.length + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(gzipped);
            return true;
        });

        try (HttpResponse response = client.send(request("GET", server.uri("/"), null))) {
            assertThat(response.body().readAllBytes()).isEqualTo(gzipped);
            assertThat(response.firstHeader("Content-Encoding")).isEqualTo("gzip");
        }
        assertThat(server.onlyRequest().header("Accept-Encoding")).isNull();
    }

    @Test
    void sends_bytes_with_their_length_and_the_content_type_of_the_body() throws IOException {
        send(request("POST", server.uri("/"), RequestBody.bytes("hello".getBytes(StandardCharsets.UTF_8), "text/plain; charset=UTF-8")));

        RawServer.Request request = server.onlyRequest();
        assertThat(request.header("Content-Length")).isEqualTo("5");
        assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=UTF-8");
        assertThat(request.header("Transfer-Encoding")).isNull();
        assertThat(request.bodyAsString()).isEqualTo("hello");
    }

    @Test
    void a_content_type_header_wins_over_the_content_type_of_the_body() throws IOException {
        send(request("POST", server.uri("/"), RequestBody.bytes("hello".getBytes(), "text/plain"), new HttpHeader("content-type", "application/json")));

        assertThat(server.onlyRequest().headers("Content-Type")).containsExactly("application/json");
    }

    @Test
    void sends_a_stream_of_unknown_length_chunked_and_one_of_known_length_with_its_length() throws IOException {
        send(request("POST", server.uri("/"), RequestBody.stream(new ByteArrayInputStream("chunked".getBytes()), RequestBody.UNKNOWN_LENGTH, null)));
        send(request("POST", server.uri("/"), RequestBody.stream(new ByteArrayInputStream("known".getBytes()), 5, null)));

        List<RawServer.Request> requests = server.requests();
        assertThat(requests.get(0).header("Transfer-Encoding")).isEqualTo("chunked");
        assertThat(requests.get(0).header("Content-Length")).isNull();
        assertThat(requests.get(0).bodyAsString()).isEqualTo("chunked");
        assertThat(requests.get(1).header("Content-Length")).isEqualTo("5");
        assertThat(requests.get(1).bodyAsString()).isEqualTo("known");
    }

    @Test
    void sends_a_file(@TempDir Path directory) throws IOException {
        Path file = Files.writeString(directory.resolve("body.txt"), "from a file");

        send(request("PUT", server.uri("/"), RequestBody.file(file.toFile(), "text/plain")));

        assertThat(server.onlyRequest().header("Content-Length")).isEqualTo("11");
        assertThat(server.onlyRequest().header("Content-Type")).isEqualTo("text/plain");
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("from a file");
    }

    @Test
    void sends_a_writer_with_its_length_or_chunked_when_the_length_is_unknown() throws IOException {
        RequestBody.ContentWriter writer = out -> out.write("written".getBytes());

        send(request("POST", server.uri("/"), RequestBody.writer(writer, 7, true, "text/plain")));
        send(request("POST", server.uri("/"), RequestBody.writer(writer, RequestBody.UNKNOWN_LENGTH, false, null)));

        List<RawServer.Request> requests = server.requests();
        assertThat(requests.get(0).header("Content-Length")).isEqualTo("7");
        assertThat(requests.get(0).header("Content-Type")).isEqualTo("text/plain");
        assertThat(requests.get(0).bodyAsString()).isEqualTo("written");
        assertThat(requests.get(1).header("Transfer-Encoding")).isEqualTo("chunked");
        assertThat(requests.get(1).bodyAsString()).isEqualTo("written");
    }

    @Test
    void a_writer_that_fails_fails_the_request() {
        RequestBody failing = RequestBody.writer(out -> {
            throw new IOException("can't write");
        }, RequestBody.UNKNOWN_LENGTH, false, null);

        assertThatThrownBy(() -> client.send(request("POST", server.uri("/"), failing))).isInstanceOf(IOException.class).hasMessage("can't write");
    }

    @Test
    void a_trace_request_with_a_body_is_unsupported_and_its_stream_is_closed() {
        AtomicInteger closed = new AtomicInteger();
        RequestBody body = RequestBody.stream(closeCounting("body", closed), 4, null);

        assertThatThrownBy(() -> client.send(request("trace", server.uri("/"), body)))
                .isInstanceOf(UnsupportedFeatureException.class)
                .hasMessage("The HTTP client can't send a body with a TRACE request.");
        assertThat(closed).hasValue(1);
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void expect_continue_from_the_options_waits_for_the_100_and_sends_the_body() throws IOException {
        HttpRequest request = new HttpRequest("POST", server.uri("/"), List.of(), RequestBody.bytes("body".getBytes(), null),
                new RequestOptions(null, true, null));

        send(request);

        assertThat(server.onlyRequest().header("Expect")).isEqualTo("100-continue");
        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("body");
    }

    @Test
    void returns_the_status_line_and_the_headers_as_the_server_sent_them() throws IOException {
        server.respond(request -> "HTTP/1.1 201 Made It\r\nX-Z: 1\r\nx-a: 2\r\nX-Z: 3\r\nContent-Length: 2\r\n\r\nok");

        HttpResponse response = client.send(request("GET", server.uri("/"), null));

        assertThat(response.protocolVersion()).isEqualTo("HTTP/1.1");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.reasonPhrase()).isEqualTo("Made It");
        assertThat(response.headers()).containsExactly(new HttpHeader("X-Z", "1"), new HttpHeader("x-a", "2"), new HttpHeader("X-Z", "3"),
                new HttpHeader("Content-Length", "2"));
        assertThat(response.contentLength()).isEqualTo(2);
        assertThat(new String(response.body().readAllBytes())).isEqualTo("ok");
        response.close();
    }

    @Test
    void skips_an_interim_response() throws IOException {
        server.respond(request -> "HTTP/1.1 102 Processing\r\n\r\n" + RawServer.response(200, "done"));

        HttpResponse response = send(request("GET", server.uri("/"), null));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void a_head_204_and_304_have_no_body_and_a_chunked_body_has_no_length() throws IOException {
        server.respond(request -> {
            switch (request.requestLine().split(" ")[1]) {
                case "/head":
                    return "HTTP/1.1 200 OK\r\nContent-Length: 10\r\n\r\n";
                case "/204":
                    return "HTTP/1.1 204 No Content\r\n\r\n";
                case "/304":
                    return "HTTP/1.1 304 Not Modified\r\n\r\n";
                default:
                    return "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n2\r\nok\r\n0\r\n\r\n";
            }
        });

        for (HttpResponse response : List.of(send(request("HEAD", server.uri("/head"), null)), send(request("GET", server.uri("/204"), null)),
                send(request("GET", server.uri("/304"), null)))) {
            assertThat(response.body()).isNull();
            assertThat(response.contentLength()).isZero();
        }
        try (HttpResponse chunked = client.send(request("GET", server.uri("/chunked"), null))) {
            assertThat(chunked.contentLength()).isEqualTo(-1);
            assertThat(new String(chunked.body().readAllBytes())).isEqualTo("ok");
        }
    }

    @Test
    void the_customizers_get_the_builder_but_dont_turn_on_what_rest_assured_does_itself() throws IOException {
        server.respond(request -> request.requestLine().startsWith("GET /redirect")
                ? RawServer.response(302, "", "Location: /target", "Set-Cookie: a=b")
                : RawServer.response(503, "", "Retry-After: 0", "Connection: close"));
        client.close();
        client = new Apache5HttpClientProvider().newClient(settings(customizer(builder -> builder
                .addRequestInterceptorFirst((request, entity, context) -> request.addHeader("X-Intercepted", "yes"))
                .setUserAgent("custom-agent")
                .setRedirectStrategy(DefaultRedirectStrategy.INSTANCE)
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, TimeValue.ofMilliseconds(1)))
                .setDefaultCookieStore(new BasicCookieStore())
                .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofMilliseconds(1)).setRedirectsEnabled(true).build())
                .setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost("unknown-proxy.invalid", 1)))
                .setProxy(new HttpHost("unknown-proxy.invalid", 1)))));

        assertThat(send(request("GET", server.uri("/redirect"), null)).statusCode()).isEqualTo(302);
        assertThat(send(request("GET", server.uri("/unavailable"), null)).statusCode()).isEqualTo(503);

        List<RawServer.Request> requests = server.requests();
        assertThat(requests).extracting(RawServer.Request::requestLine).containsExactly("GET /redirect HTTP/1.1", "GET /unavailable HTTP/1.1");
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.header("X-Intercepted")).isEqualTo("yes");
            assertThat(request.header("User-Agent")).isEqualTo("custom-agent");
            assertThat(request.header("Cookie")).isNull();
        });
    }

    @Test
    void a_customizer_cant_turn_on_decompression() throws IOException {
        byte[] gzipped = gzip("hello");
        server.handle((request, out) -> {
            out.write(("HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Length: " + gzipped.length + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(gzipped);
            return true;
        });
        LinkedHashMap<String, InputStreamFactory> decoders = new LinkedHashMap<>();
        decoders.put("gzip", GZIPInputStream::new);
        client.close();
        client = new Apache5HttpClientProvider().newClient(settings(customizer(builder -> builder.setContentDecoderRegistry(decoders))));

        try (HttpResponse response = client.send(request("GET", server.uri("/"), null))) {
            assertThat(response.body().readAllBytes()).isEqualTo(gzipped);
        }
        assertThat(server.onlyRequest().header("Accept-Encoding")).isNull();
    }

    @Test
    void a_customizer_cant_make_the_client_answer_the_challenges_of_the_server() throws IOException {
        server.respond(request -> RawServer.response(401, "", "WWW-Authenticate: Basic realm=\"x\""));
        AtomicBoolean asked = new AtomicBoolean();
        client.close();
        client = new Apache5HttpClientProvider().newClient(settings(customizer(builder -> builder.setTargetAuthenticationStrategy((type, challenges, context) -> {
            asked.set(true);
            return List.of(new BasicScheme());
        }))));

        assertThat(send(request("GET", server.uri("/"), null)).statusCode()).isEqualTo(401);
        assertThat(asked).isFalse();
        assertThat(server.onlyRequest().header("Authorization")).isNull();
    }

    @Test
    void doesnt_ask_to_upgrade_a_plain_connection_to_tls() throws IOException {
        // HttpClient 5.4 and later do so for a GET, HEAD or OPTIONS through a proxy (RFC 2817), which HttpClient 4 and
        // the JDK client don't
        client.close();
        client = new Apache5HttpClientProvider().newClient(new ClientSettings(null, new ProxySettings("http", "localhost", server.port(), null, null),
                HttpVersion.HTTP_1_1, null, null, List.of()));

        send(request("GET", URI.create("http://example.invalid/"), null));

        assertThat(server.onlyRequest().header("Upgrade")).isNull();
        assertThat(server.onlyRequest().header("Connection")).isNotEqualToIgnoringCase("Upgrade");
    }

    @Test
    void the_user_agent_is_the_one_of_http_client() throws IOException {
        send(request("GET", server.uri("/"), null));

        assertThat(server.onlyRequest().header("User-Agent")).startsWith("Apache-HttpClient/5.");
    }

    @Test
    void a_server_that_closes_the_connection_without_answering_isnt_asked_again() {
        server.handle((request, out) -> false);

        assertThatThrownBy(() -> client.send(request("GET", server.uri("/"), null))).isInstanceOf(NoHttpResponseException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void a_401_is_returned_whatever_the_server_offers() throws IOException {
        server.respond(request -> RawServer.response(401, "", "WWW-Authenticate: Basic realm=\"x\"", "WWW-Authenticate: NTLM",
                "WWW-Authenticate: Digest realm=\"x\", nonce=\"abc\""));

        assertThat(send(request("GET", server.uri("/"), null)).statusCode()).isEqualTo(401);
        assertThat(server.onlyRequest().header("Authorization")).isNull();
    }

    @Test
    void http_2_is_unsupported() {
        ClientSettings http2 = new ClientSettings(null, null, HttpVersion.HTTP_2, null, null, List.of());

        assertThatThrownBy(() -> new Apache5HttpClientProvider().newClient(http2))
                .isInstanceOf(UnsupportedFeatureException.class)
                .hasMessage("The HTTP client can't send HTTP/2 requests.");
    }

    @Test
    void tls_settings_that_cant_be_loaded_fail_only_https_requests() throws IOException {
        TlsSettings missing = TlsSettings.builder().trustStore(new KeyMaterial("does-not-exist.p12", "secret", "PKCS12", null)).build();
        client.close();
        client = new Apache5HttpClientProvider().newClient(new ClientSettings(missing, null, HttpVersion.HTTP_1_1, null, null, List.of()));

        send(request("GET", server.uri("/"), null));

        assertThatThrownBy(() -> client.send(request("GET", URI.create("https://localhost:" + server.port() + "/"), null)))
                .isInstanceOf(SSLException.class)
                .hasMessageStartingWith("Failed to create the SSLContext from the configured SSL settings: ")
                .hasCauseInstanceOf(FileNotFoundException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void tls_settings_that_cant_be_loaded_fail_a_request_through_a_proxy_over_tls() throws IOException {
        TlsSettings missing = TlsSettings.builder().trustStore(new KeyMaterial("does-not-exist.p12", "secret", "PKCS12", null)).build();
        client.close();
        client = new Apache5HttpClientProvider().newClient(new ClientSettings(missing, new ProxySettings("https", "localhost", server.port(), null, null),
                HttpVersion.HTTP_1_1, null, null, List.of()));

        // Without the check, the handshake would wait for the server, which doesn't speak TLS
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertThatThrownBy(() -> client.send(request("GET", URI.create("http://example.invalid/"), null)))
                .isInstanceOf(SSLException.class)
                .hasMessageStartingWith("Failed to create the SSLContext from the configured SSL settings: ")
                .hasCauseInstanceOf(FileNotFoundException.class));
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void a_stream_whose_close_fails_doesnt_hide_why_the_request_failed() throws IOException {
        InputStream failingClose = new ByteArrayInputStream("body".getBytes()) {
            @Override
            public void close() {
                throw new IllegalStateException("close failed");
            }
        };
        TlsSettings missing = TlsSettings.builder().trustStore(new KeyMaterial("does-not-exist.p12", "secret", "PKCS12", null)).build();
        client.close();
        client = new Apache5HttpClientProvider().newClient(new ClientSettings(missing, null, HttpVersion.HTTP_1_1, null, null, List.of()));

        assertThatThrownBy(() -> client.send(request("POST", URI.create("https://localhost:" + server.port() + "/"), RequestBody.stream(failingClose, 4, null))))
                .isInstanceOf(SSLException.class)
                .hasMessageStartingWith("Failed to create the SSLContext");
    }

    @Test
    void a_stream_body_is_closed_with_the_response_and_when_the_request_fails() throws IOException {
        // HttpClient closes a stream that it has sent too
        AtomicInteger sent = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        HttpResponse response = client.send(request("POST", server.uri("/"), RequestBody.stream(closeCounting("body", sent), 4, null)));
        response.close();
        assertThat(sent.get()).isPositive();

        server.close();
        assertThatThrownBy(() -> client.send(request("POST", server.uri("/"), RequestBody.stream(closeCounting("body", failed), 4, null))))
                .isInstanceOf(IOException.class);
        assertThat(failed.get()).isPositive();
    }

    @Test
    void without_proxy_settings_the_proxy_system_properties_of_the_moment_are_used() throws IOException {
        try (RawServer proxy = new RawServer().respond(request -> RawServer.response(200, "from the proxy"))) {
            ProxySelector original = ProxySelector.getDefault();
            send(request("GET", server.uri("/direct"), null));
            ProxySelector.setDefault(ProxySelector.of(new InetSocketAddress("localhost", proxy.port())));
            try {
                send(request("GET", URI.create("http://example.invalid/proxied"), null));
            } finally {
                ProxySelector.setDefault(original);
            }

            assertThat(server.onlyRequest().requestLine()).isEqualTo("GET /direct HTTP/1.1");
            assertThat(proxy.onlyRequest().requestLine()).isEqualTo("GET http://example.invalid/proxied HTTP/1.1");
        }
    }

    static InputStream closeCounting(String content, AtomicInteger closed) {
        return new ByteArrayInputStream(content.getBytes()) {
            @Override
            public void close() {
                closed.incrementAndGet();
            }
        };
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }
}

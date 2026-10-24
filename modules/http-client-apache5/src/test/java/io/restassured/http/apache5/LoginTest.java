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

import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.NtlmCredentials;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.common.spi.http.TlsSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.request;
import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.send;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The logins that HttpClient answers itself: a proxy's, and NTLM.
 */
class LoginTest {
    // user:secret
    private static final String CREDENTIALS = "Basic dXNlcjpzZWNyZXQ=";

    private final List<AutoCloseable> closeables = new CopyOnWriteArrayList<>();

    @AfterEach
    void close() throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
    }

    private HttpClientInstance client(TlsSettings tls, ProxySettings proxy) {
        HttpClientInstance client = new Apache5HttpClientProvider().newClient(new ClientSettings(tls, proxy, HttpVersion.HTTP_1_1, null, null, List.of()));
        closeables.add(client);
        return client;
    }

    private RawServer loggingInProxy() throws IOException {
        RawServer proxy = new RawServer().respond(request -> CREDENTIALS.equals(request.header("Proxy-Authorization"))
                ? RawServer.response(200, "through the proxy")
                : RawServer.response(407, "", "Proxy-Authenticate: Basic realm=\"proxy\""));
        closeables.add(proxy);
        return proxy;
    }

    @Test
    void answers_the_proxy_login_once_and_then_sends_it_without_being_asked() throws IOException {
        RawServer proxy = loggingInProxy();
        HttpClientInstance client = client(null, new ProxySettings("http", "localhost", proxy.port(), "user", "secret"));

        HttpResponse first = send(client, request("POST", URI.create("http://example.invalid/x"), RequestBody.bytes("body".getBytes(), "text/plain")));
        HttpResponse second = send(client, request("GET", URI.create("http://example.invalid/y"), null));

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(proxy.requests()).extracting(RawServer.Request::requestLine)
                .containsExactly("POST http://example.invalid/x HTTP/1.1", "POST http://example.invalid/x HTTP/1.1", "GET http://example.invalid/y HTTP/1.1");
        assertThat(proxy.requests()).extracting(request -> request.header("Proxy-Authorization")).containsExactly(null, CREDENTIALS, CREDENTIALS);
        assertThat(proxy.requests().get(1).bodyAsString()).isEqualTo("body");
    }

    @Test
    void returns_the_407_when_the_body_cant_be_sent_again() throws IOException {
        RawServer proxy = loggingInProxy();
        HttpClientInstance client = client(null, new ProxySettings("http", "localhost", proxy.port(), "user", "secret"));

        HttpResponse response = send(client, request("POST", URI.create("http://example.invalid/x"),
                RequestBody.stream(new ByteArrayInputStream("body".getBytes()), 4, null)));

        assertThat(response.statusCode()).isEqualTo(407);
        assertThat(proxy.requests()).hasSize(1);
    }

    @Test
    void returns_the_407_without_a_user_name_for_the_proxy() throws IOException {
        RawServer proxy = loggingInProxy();
        HttpClientInstance client = client(null, new ProxySettings("http", "localhost", proxy.port(), null, null));

        assertThat(send(client, request("GET", URI.create("http://example.invalid/x"), null)).statusCode()).isEqualTo(407);
        assertThat(proxy.requests()).hasSize(1);
    }

    @Test
    void logs_in_to_the_proxy_of_an_https_tunnel_and_the_target_never_sees_the_login() throws Exception {
        try (RawServer target = RawServer.tls("wire_mismatched_name_server.p12"); TunnelingProxy tunnel = TunnelingProxy.plain(target.port(), CREDENTIALS)) {
            HttpClientInstance client = client(TlsSettings.builder().relaxedProtocol("TLS").build(),
                    new ProxySettings("http", "localhost", tunnel.port(), "user", "secret"));

            HttpResponse response = send(client, request("GET", URI.create("https://localhost:" + target.port() + "/"), null));

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(tunnel.connects()).extracting(RawServer.Request::requestLine).containsOnly("CONNECT localhost:" + target.port() + " HTTP/1.1");
            assertThat(tunnel.connects()).extracting(request -> request.header("Proxy-Authorization")).containsExactly(null, CREDENTIALS);
            assertThat(target.onlyRequest().header("Proxy-Authorization")).isNull();
        }
    }

    @Test
    void answers_an_ntlm_challenge_with_the_credentials_of_the_request() throws IOException {
        RawServer server = ntlmServer();
        HttpClientInstance client = client(null, null);

        HttpResponse response = send(client, new HttpRequest("GET", server.uri("/"), List.of(), null,
                new RequestOptions(null, false, new NtlmCredentials("localhost", server.port(), "user", "secret", "WORKSTATION", "DOMAIN"))));

        assertThat(response.statusCode()).isEqualTo(200);
        List<RawServer.Request> requests = server.requests();
        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).header("Authorization")).isNull();
        assertThat(ntlmMessageType(requests.get(1))).isEqualTo(1);
        assertThat(ntlmMessageType(requests.get(2))).isEqualTo(3);
        String type3 = new String(Base64.getDecoder().decode(requests.get(2).header("Authorization").substring("NTLM ".length())), StandardCharsets.UTF_16LE);
        assertThat(type3).contains("user", "DOMAIN");
        // The handshake is on one connection, as NTLM needs
        assertThat(requests).extracting(request -> request.clientPort).containsOnly(requests.get(0).clientPort);
    }

    @Test
    void doesnt_answer_an_ntlm_challenge_without_ntlm_credentials() throws IOException {
        RawServer server = ntlmServer();
        HttpClientInstance client = client(null, null);

        assertThat(send(client, request("GET", server.uri("/"), null)).statusCode()).isEqualTo(401);
        assertThat(server.onlyRequest().header("Authorization")).isNull();
    }

    @Test
    void doesnt_answer_another_challenge_with_ntlm_credentials() throws IOException {
        RawServer server = new RawServer().respond(request -> RawServer.response(401, "", "WWW-Authenticate: Basic realm=\"x\""));
        closeables.add(server);
        HttpClientInstance client = client(null, null);

        HttpResponse response = send(client, new HttpRequest("GET", server.uri("/"), List.of(), null,
                new RequestOptions(null, false, new NtlmCredentials("localhost", server.port(), "user", "secret", null, null))));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(server.onlyRequest().header("Authorization")).isNull();
    }

    // Asks for NTLM, besides Basic, and answers a negotiate message with a challenge and an authenticate message with 200
    private RawServer ntlmServer() throws IOException {
        RawServer server = new RawServer().respond(request -> {
            int type = ntlmMessageType(request);
            if (type == 1) {
                return RawServer.response(401, "", "WWW-Authenticate: NTLM " + Base64.getEncoder().encodeToString(challengeMessage()));
            }
            return type == 3 ? RawServer.response(200, "logged in")
                    : RawServer.response(401, "", "WWW-Authenticate: NTLM", "WWW-Authenticate: Basic realm=\"x\"");
        });
        closeables.add(server);
        return server;
    }

    private static int ntlmMessageType(RawServer.Request request) {
        String authorization = request.header("Authorization");
        if (authorization == null || !authorization.startsWith("NTLM ")) {
            return 0;
        }
        byte[] message = Base64.getDecoder().decode(authorization.substring("NTLM ".length()));
        return ByteBuffer.wrap(message, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    // A challenge message with no target name or information, for NTLM with Unicode strings
    private static byte[] challengeMessage() {
        ByteBuffer message = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);
        message.put("NTLMSSP\0".getBytes(StandardCharsets.US_ASCII));
        message.putInt(2);
        // Target name: length, maximum length, offset
        message.putShort((short) 0).putShort((short) 0).putInt(48);
        // Negotiate Unicode and NTLM
        message.putInt(0x00000201);
        message.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        message.put(new byte[8]);
        // Target information: length, maximum length, offset
        message.putShort((short) 0).putShort((short) 0).putInt(48);
        return message.array();
    }
}

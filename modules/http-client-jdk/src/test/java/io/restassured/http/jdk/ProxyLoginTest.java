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

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.http.jdk.JdkHttpClientInstanceTest.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The JDK client answers a proxy's 407 only with an {@link java.net.Authenticator}, which would also answer (and fail)
 * every 401, so the adapter answers a Basic challenge of the proxy itself.
 */
class ProxyLoginTest {
    // user:secret
    private static final String CREDENTIALS = "Basic dXNlcjpzZWNyZXQ=";

    private RawServer proxy;
    private HttpClientInstance client;

    @BeforeEach
    void startProxy() throws IOException {
        proxy = new RawServer().respond(request -> CREDENTIALS.equals(request.header("Proxy-Authorization"))
                ? RawServer.response(200, "through the proxy")
                : RawServer.response(407, "", "Proxy-Authenticate: Basic realm=\"proxy\""));
        client = new JdkHttpClientProvider().newClient(new ClientSettings(TlsSettings.builder().build(),
                new ProxySettings("http", "localhost", proxy.port(), "user", "secret"), HttpVersion.HTTP_1_1, null, null, List.of()));
    }

    @AfterEach
    void stopProxy() throws IOException {
        client.close();
        proxy.close();
    }

    @Test
    void answers_a_basic_challenge_of_the_proxy_by_sending_the_request_again_with_the_credentials() throws IOException {
        HttpResponse response = client.send(request("POST", URI.create("http://example.invalid/x"), RequestBody.bytes("body".getBytes(), "text/plain")));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body().readAllBytes())).isEqualTo("through the proxy");
        response.close();
        assertThat(proxy.requests()).hasSize(2);
        assertThat(proxy.requests().get(0).header("Proxy-Authorization")).isNull();
        assertThat(proxy.requests().get(1).requestLine()).isEqualTo("POST http://example.invalid/x HTTP/1.1");
        assertThat(proxy.requests().get(1).bodyAsString()).isEqualTo("body");
    }

    @Test
    void returns_the_407_when_the_body_cant_be_sent_again() throws IOException {
        RequestBody stream = RequestBody.stream(new ByteArrayInputStream("body".getBytes()), 4, null);

        HttpResponse response = client.send(request("POST", URI.create("http://example.invalid/x"), stream));

        assertThat(response.statusCode()).isEqualTo(407);
        response.close();
        assertThat(proxy.requests()).hasSize(1);
    }

    @Test
    void returns_the_407_when_the_proxy_doesnt_offer_basic() throws IOException {
        proxy.respond(request -> RawServer.response(407, "", "Proxy-Authenticate: Digest realm=\"proxy\", nonce=\"abc\""));

        HttpResponse response = client.send(request("GET", URI.create("http://example.invalid/x"), null));

        assertThat(response.statusCode()).isEqualTo(407);
        response.close();
        assertThat(proxy.requests()).hasSize(1);
    }

    @Test
    void a_proxy_authorization_header_of_the_request_is_sent_as_is() throws IOException {
        proxy.respond(request -> RawServer.response(407, "", "Proxy-Authenticate: Basic realm=\"proxy\""));

        HttpResponse response = client.send(request("GET", URI.create("http://example.invalid/x"), null, new HttpHeader("Proxy-Authorization", "Basic other")));

        assertThat(response.statusCode()).isEqualTo(407);
        response.close();
        assertThat(proxy.onlyRequest().header("Proxy-Authorization")).isEqualTo("Basic other");
    }

    @Test
    void answers_the_407_to_the_connect_of_an_https_tunnel_whatever_the_body_and_the_target_never_sees_the_proxy_login() throws Exception {
        List<String> proxyAuthorizationsAtTheTarget = new CopyOnWriteArrayList<>();
        HttpsServer target = echoingHttpsServer(proxyAuthorizationsAtTheTarget);
        // The JDK client sends the Proxy-Authorization header in the CONNECT, since the build's JVM allows Basic for tunnels
        try (TunnelingProxy tunnel = new TunnelingProxy(target.getAddress().getPort())) {
            HttpClientInstance tunneling = new JdkHttpClientInstance(new ClientSettings(TlsSettings.builder().relaxedProtocol("TLS").build(),
                    new ProxySettings("http", "localhost", tunnel.port(), "user", "secret"), HttpVersion.HTTP_1_1, null, null, List.of()), false);
            // A stream can't be sent twice, but the 407 comes before it's sent
            RequestBody stream = RequestBody.stream(new ByteArrayInputStream("body".getBytes()), 4, null);
            try (HttpResponse response = tunneling.send(request("POST", URI.create("https://localhost:" + target.getAddress().getPort() + "/"), stream))) {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(new String(response.body().readAllBytes())).isEqualTo("body");
            } finally {
                tunneling.close();
            }

            assertThat(tunnel.connects()).extracting(RawServer.Request::requestLine)
                    .containsExactly("CONNECT localhost:" + target.getAddress().getPort() + " HTTP/1.1", "CONNECT localhost:" + target.getAddress().getPort() + " HTTP/1.1");
            assertThat(tunnel.connects()).extracting(request -> request.header("Proxy-Authorization")).containsExactly(null, CREDENTIALS);
            assertThat(proxyAuthorizationsAtTheTarget).containsExactly("none");
        } finally {
            target.stop(0);
        }
    }

    @Test
    void an_https_request_through_a_proxy_with_login_is_unsupported_while_basic_is_disabled_for_tunnels() {
        HttpClientInstance basicDisabledForTunnels = new JdkHttpClientInstance(new ClientSettings(TlsSettings.builder().build(),
                new ProxySettings("http", "localhost", proxy.port(), "user", "secret"), HttpVersion.HTTP_1_1, null, null, List.of()), true);

        assertThatThrownBy(() -> basicDisabledForTunnels.send(request("GET", URI.create("https://example.invalid/x"), null)))
                .isInstanceOf(UnsupportedFeatureException.class)
                .hasMessage("The HTTP client can't log in to a proxy with Basic authentication for an HTTPS request, since "
                        + "jdk.http.auth.tunneling.disabledSchemes disables Basic for HTTPS tunnels. To do it anyway, start the JVM with "
                        + "-Djdk.http.auth.tunneling.disabledSchemes= (an empty value).");
        assertThat(proxy.requests()).isEmpty();
    }

    @Test
    void basic_is_disabled_for_tunnels_when_the_system_property_names_it() {
        assertThat(JdkHttpClientInstance.disablesBasic("Basic")).isTrue();
        assertThat(JdkHttpClientInstance.disablesBasic("Digest, basic")).isTrue();
        assertThat(JdkHttpClientInstance.disablesBasic("")).isFalse();
        assertThat(JdkHttpClientInstance.disablesBasic("Digest")).isFalse();
    }

    // Answers with the request body, and records the Proxy-Authorization header of each request, or "none"
    private static HttpsServer echoingHttpsServer(List<String> proxyAuthorizations) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = ProxyLoginTest.class.getResourceAsStream("/wire_mismatched_name_server.p12")) {
            keyStore.load(in, "test1234".toCharArray());
        }
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, "test1234".toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, null);
        HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context));
        server.createContext("/", exchange -> {
            String proxyAuthorization = exchange.getRequestHeaders().getFirst("Proxy-Authorization");
            proxyAuthorizations.add(proxyAuthorization == null ? "none" : proxyAuthorization);
            byte[] body = exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return server;
    }

    /**
     * A proxy that asks for a Basic login with a 407 and closes the connection, and tunnels a CONNECT with the login
     * to the target port.
     */
    private static final class TunnelingProxy implements AutoCloseable {
        private final ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        private final List<RawServer.Request> connects = new CopyOnWriteArrayList<>();
        private final int targetPort;

        TunnelingProxy(int targetPort) throws IOException {
            this.targetPort = targetPort;
            daemon(() -> {
                while (!socket.isClosed()) {
                    try {
                        Socket connection = socket.accept();
                        daemon(() -> tunnel(connection));
                    } catch (IOException e) {
                        return;
                    }
                }
            });
        }

        int port() {
            return socket.getLocalPort();
        }

        List<RawServer.Request> connects() {
            return connects;
        }

        private void tunnel(Socket connection) {
            try (connection) {
                String head = RawServer.readHead(connection.getInputStream());
                if (head == null) {
                    return;
                }
                RawServer.Request connect = new RawServer.Request(head, new byte[0]);
                connects.add(connect);
                OutputStream out = connection.getOutputStream();
                if (!CREDENTIALS.equals(connect.header("Proxy-Authorization"))) {
                    out.write(RawServer.response(407, "", "Proxy-Authenticate: Basic realm=\"proxy\"").getBytes(StandardCharsets.ISO_8859_1));
                    return;
                }
                out.write("HTTP/1.1 200 Connection established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                out.flush();
                try (Socket target = new Socket(InetAddress.getLoopbackAddress(), targetPort)) {
                    daemon(() -> {
                        try {
                            connection.getInputStream().transferTo(target.getOutputStream());
                        } catch (IOException ignored) {
                            // One side closed the tunnel
                        }
                    });
                    target.getInputStream().transferTo(out);
                }
            } catch (IOException ignored) {
                // One side closed the tunnel
            }
        }

        private static void daemon(Runnable runnable) {
            Thread thread = new Thread(runnable, "tunneling-proxy");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}

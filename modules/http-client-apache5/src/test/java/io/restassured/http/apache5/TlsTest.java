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
import io.restassured.common.spi.http.HostnameVerification;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.KeyMaterial;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.TlsSettings;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.request;
import static io.restassured.http.apache5.Apache5HttpClientInstanceTest.send;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The server's certificate is for {@code not-this-host.example} and is trusted by the trust store, so only the host
 * name check can reject it.
 */
class TlsTest {
    private static final KeyMaterial TRUST_STORE = new KeyMaterial("wire_mismatched_name_truststore.p12", RawServer.PASSWORD, "PKCS12", null);

    private RawServer server;
    private URI uri;

    @BeforeEach
    void startServer() throws Exception {
        server = RawServer.tls("wire_mismatched_name_server.p12");
        uri = URI.create("https://" + RawServer.LOOPBACK + ":" + server.port() + "/");
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
    }

    private int status(TlsSettings tls) throws IOException {
        HttpClientInstance client = new Apache5HttpClientProvider().newClient(new ClientSettings(tls, null, HttpVersion.HTTP_1_1, null, null, List.of()));
        try {
            return send(client, request("GET", uri, null)).statusCode();
        } finally {
            client.close();
        }
    }

    @Test
    void strict_host_name_checking_rejects_the_certificate() {
        assertThatThrownBy(() -> status(TlsSettings.builder().trustStore(TRUST_STORE).build()))
                .isInstanceOf(SSLPeerUnverifiedException.class)
                .hasMessageStartingWith("Certificate for <" + InetAddress.getLoopbackAddress().getHostAddress() + "> doesn't match");
    }

    @Test
    void allow_all_host_names_accepts_the_certificate_from_a_trust_store_and_from_a_context() throws Exception {
        assertThat(status(TlsSettings.builder().trustStore(TRUST_STORE).hostnames(HostnameVerification.ALLOW_ALL).build())).isEqualTo(200);
        TlsSettings fromContext = TlsSettings.builder().sslContext(TlsSettings.builder().trustStore(TRUST_STORE).build().createSslContext())
                .hostnames(HostnameVerification.ALLOW_ALL).build();
        assertThat(status(fromContext)).isEqualTo(200);
    }

    @Test
    void a_certificate_that_isnt_trusted_is_rejected_also_without_tls_settings() {
        assertThatThrownBy(() -> status(TlsSettings.builder().hostnames(HostnameVerification.ALLOW_ALL).build())).isInstanceOf(SSLHandshakeException.class);
        assertThatThrownBy(() -> status(null)).isInstanceOf(SSLHandshakeException.class);
    }

    @Test
    void a_custom_verifier_decides_and_a_host_name_it_rejects_fails_with_ssl_peer_unverified() throws IOException {
        List<String> asked = new CopyOnWriteArrayList<>();

        assertThat(status(TlsSettings.builder().trustStore(TRUST_STORE).hostnameVerifier((host, session) -> asked.add(host)).build())).isEqualTo(200);
        assertThat(asked).containsExactly(InetAddress.getLoopbackAddress().getHostAddress());
        assertThatThrownBy(() -> status(TlsSettings.builder().trustStore(TRUST_STORE).hostnameVerifier((host, session) -> false).build()))
                .isInstanceOf(SSLPeerUnverifiedException.class);
    }

    @Test
    void relaxed_validation_accepts_the_certificate_without_a_trust_store() throws IOException {
        assertThat(status(TlsSettings.builder().relaxedProtocol("TLS").build())).isEqualTo(200);
    }

    @Test
    void strict_host_name_checking_matches_a_wildcard_against_one_label_only() throws Exception {
        try (RawServer wildcard = RawServer.tls("wildcard_server.p12")) {
            TlsSettings strict = TlsSettings.builder().trustStore(new KeyMaterial(null, null, null, trusting("wildcard_server.p12"))).build();
            LayeredConnectionSocketFactory factory = Tls.socketFactory(strict);

            connect(factory, "a.example.com", wildcard.port());
            assertThatThrownBy(() -> connect(factory, "a.b.example.com", wildcard.port())).isInstanceOf(SSLPeerUnverifiedException.class);
        }
    }

    @Test
    void connects_to_a_proxy_over_tls() throws Exception {
        try (RawServer proxy = RawServer.tls("wire_mismatched_name_server.p12")) {
            HttpClientInstance client = new Apache5HttpClientProvider().newClient(new ClientSettings(TlsSettings.builder().relaxedProtocol("TLS").build(),
                    new ProxySettings("https", "localhost", proxy.port(), null, null), HttpVersion.HTTP_1_1, null, null, List.of()));
            try {
                HttpResponse response = send(client, request("GET", URI.create("http://example.invalid/x"), null));

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(proxy.onlyRequest().requestLine()).isEqualTo("GET http://example.invalid/x HTTP/1.1");
            } finally {
                client.close();
            }
        }
    }

    @Test
    void tunnels_an_https_request_through_a_proxy_over_tls() throws Exception {
        ServerSocket proxySocket = RawServer.sslContext("wire_mismatched_name_server.p12").getServerSocketFactory()
                .createServerSocket(0, 50, InetAddress.getLoopbackAddress());
        try (TunnelingProxy proxy = new TunnelingProxy(proxySocket, server.port(), null)) {
            HttpClientInstance client = new Apache5HttpClientProvider().newClient(new ClientSettings(TlsSettings.builder().relaxedProtocol("TLS").build(),
                    new ProxySettings("https", "localhost", proxy.port(), null, null), HttpVersion.HTTP_1_1, null, null, List.of()));
            try {
                HttpResponse response = send(client, request("GET", uri, null));

                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(proxy.connects()).extracting(RawServer.Request::requestLine).containsExactly("CONNECT " + RawServer.LOOPBACK + ":" + server.port() + " HTTP/1.1");
                assertThat(server.onlyRequest().requestLine()).isEqualTo("GET / HTTP/1.1");
            } finally {
                client.close();
            }
        }
    }

    // Connects to the port on the loopback address, and checks the certificate against the host name
    @SuppressWarnings("deprecation")
    private static void connect(LayeredConnectionSocketFactory factory, String hostName, int port) throws IOException {
        HttpHost host = new HttpHost("https", hostName, port);
        try (Socket socket = factory.connectSocket(TimeValue.ofSeconds(5), null, host, new InetSocketAddress("localhost", port), null,
                new BasicHttpContext())) {
            assertThat(socket.isConnected()).isTrue();
        }
    }

    private static KeyStore trusting(String keyStore) throws Exception {
        KeyStore keys = KeyStore.getInstance("PKCS12");
        try (InputStream in = TlsTest.class.getResourceAsStream("/" + keyStore)) {
            keys.load(in, RawServer.PASSWORD.toCharArray());
        }
        Certificate certificate = keys.getCertificate(keys.aliases().nextElement());
        KeyStore trusted = KeyStore.getInstance(KeyStore.getDefaultType());
        trusted.load(null, null);
        trusted.setCertificateEntry("server", certificate);
        return trusted;
    }
}

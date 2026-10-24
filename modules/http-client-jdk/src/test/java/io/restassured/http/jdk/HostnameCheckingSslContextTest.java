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
import io.restassured.common.spi.http.HostnameVerification;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.KeyMaterial;
import io.restassured.common.spi.http.TlsSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.http.jdk.JdkHttpClientInstanceTest.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The server's certificate is for {@code not-this-host.example} and is trusted by the trust store, so only the host
 * name check can reject it.
 */
class HostnameCheckingSslContextTest {
    private static final String PASSWORD = "test1234";
    private static final KeyMaterial TRUST_STORE = new KeyMaterial("wire_mismatched_name_truststore.p12", PASSWORD, "PKCS12", null);
    private static HttpsServer server;
    private static URI uri;

    @BeforeAll
    static void startServer() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = HostnameCheckingSslContextTest.class.getResourceAsStream("/wire_mismatched_name_server.p12")) {
            keyStore.load(in, PASSWORD.toCharArray());
        }
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, PASSWORD.toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, null);
        server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context));
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, 2);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write("ok".getBytes());
            }
        });
        server.start();
        uri = URI.create("https://localhost:" + server.getAddress().getPort() + "/");
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static int status(TlsSettings tls) throws IOException {
        HttpClientInstance client = new JdkHttpClientProvider().newClient(new ClientSettings(tls, null, HttpVersion.HTTP_1_1, null, null, List.of()));
        try (HttpResponse response = client.send(request("GET", uri, null))) {
            response.body().readAllBytes();
            return response.statusCode();
        } finally {
            client.close();
        }
    }

    @Test
    void strict_host_name_checking_rejects_the_certificate() {
        assertThatThrownBy(() -> status(TlsSettings.builder().trustStore(TRUST_STORE).build())).isInstanceOf(SSLException.class);
    }

    @Test
    void allow_all_host_names_accepts_the_certificate_from_a_trust_store_and_from_a_context() throws Exception {
        SSLContext context = TlsSettings.builder().trustStore(TRUST_STORE).build().createSslContext();

        assertThat(status(TlsSettings.builder().trustStore(TRUST_STORE).hostnames(HostnameVerification.ALLOW_ALL).build())).isEqualTo(200);
        assertThat(status(TlsSettings.builder().sslContext(context).hostnames(HostnameVerification.ALLOW_ALL).build())).isEqualTo(200);
    }

    @Test
    void allow_all_host_names_still_rejects_a_certificate_that_isnt_trusted() {
        assertThatThrownBy(() -> status(TlsSettings.builder().hostnames(HostnameVerification.ALLOW_ALL).build())).isInstanceOf(SSLException.class);
    }

    @Test
    void a_custom_verifier_decides_once_the_handshake_has_finished() throws IOException {
        List<String> asked = new CopyOnWriteArrayList<>();

        int status = status(TlsSettings.builder().trustStore(TRUST_STORE).hostnameVerifier((host, session) -> {
            asked.add(host + " " + session.getPeerHost());
            return true;
        }).build());

        assertThat(status).isEqualTo(200);
        assertThat(asked).containsExactly("localhost localhost");
    }

    @Test
    void a_host_name_that_the_custom_verifier_rejects_fails_with_ssl_peer_unverified_as_the_cause() {
        TlsSettings rejecting = TlsSettings.builder().trustStore(TRUST_STORE).hostnameVerifier((host, session) -> false).build();

        assertThatThrownBy(() -> status(rejecting))
                .isInstanceOf(SSLException.class)
                .satisfies(e -> assertThat(causes(e)).anySatisfy(cause -> assertThat(cause).isInstanceOf(SSLPeerUnverifiedException.class)
                        .hasMessageStartingWith("Host name 'localhost' was rejected by the configured HostnameVerifier ")));
    }

    @Test
    void relaxed_validation_accepts_the_certificate_without_a_trust_store() throws IOException {
        assertThat(status(TlsSettings.builder().relaxedProtocol("TLS").build())).isEqualTo(200);
    }

    private static List<Throwable> causes(Throwable e) {
        List<Throwable> causes = new java.util.ArrayList<>();
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            causes.add(cause);
        }
        return causes;
    }
}

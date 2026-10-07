/*
 * Copyright 2019 the original author or authors.
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
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Verifies that user configured SSL settings (such as relaxed HTTPS validation) are applied when a request that starts
 * as plain http is redirected to https (issue #790).
 */
class SSLConfigOnHttpToHttpsRedirectTest {

    // Self-signed certificate for localhost/127.0.0.1, not trusted by the JVM default trust store.
    private static final String SELF_SIGNED_KEYSTORE = "self_signed_localhost.p12";
    private static final String KEYSTORE_PASSWORD = "changeit";

    private HttpsServer httpsServer;
    private HttpServer httpServer;

    @BeforeEach
    void startServers() throws Exception {
        httpsServer = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(serverSslContext()));
        httpsServer.createContext("/target", exchange -> send(exchange, "secure"));
        httpsServer.start();

        String location = "https://localhost:" + httpsServer.getAddress().getPort() + "/target";
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/start", exchange -> {
            exchange.getResponseHeaders().set("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        httpServer.createContext("/plain", exchange -> send(exchange, "plain"));
        httpServer.start();
    }

    @AfterEach
    void stopServers() {
        RestAssured.reset();
        httpServer.stop(0);
        httpsServer.stop(0);
    }

    @Test
    void redirect_from_http_to_untrusted_https_fails_without_user_configured_ssl() {
        assertThatThrownBy(() -> given().get(httpUrl("/start")))
                .hasStackTraceContaining("PKIX");
    }

    @Test
    void relaxed_https_validation_is_applied_when_http_request_is_redirected_to_https() {
        given().
                relaxedHTTPSValidation().
        when().
                get(httpUrl("/start")).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void statically_configured_relaxed_https_validation_is_applied_when_http_request_is_redirected_to_https() {
        RestAssured.useRelaxedHTTPSValidation();

        given().
        when().
                get(httpUrl("/start")).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void user_configured_trust_store_is_applied_when_http_request_is_redirected_to_https() {
        given().
                config(config().sslConfig(sslConfig().trustStore(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD).trustStoreType("PKCS12"))).
        when().
                get(httpUrl("/start")).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void certificate_auth_is_applied_when_http_request_is_redirected_to_https() {
        given().
                auth().certificate(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD, certAuthSettings().trustStoreType("PKCS12").allowAllHostnames()).
        when().
                get(httpUrl("/start")).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void https_scheme_registered_for_http_request_uses_default_https_port_rather_than_the_http_port() {
        AtomicReference<DefaultHttpClient> client = new AtomicReference<>();

        given().
                config(config().httpClient(httpClientConfig().httpClientFactory(() -> capture(client))).sslConfig(sslConfig().relaxedHTTPSValidation())).
        when().
                get(httpUrl("/start")).
        then().
                statusCode(200);

        assertThat(client.get().getConnectionManager().getSchemeRegistry().getScheme("https").getDefaultPort()).isEqualTo(443);
    }

    @Test
    void https_scheme_registered_by_certificate_auth_for_http_request_uses_default_https_port_rather_than_the_http_port() {
        AtomicReference<DefaultHttpClient> client = new AtomicReference<>();

        given().
                config(config().httpClient(httpClientConfig().httpClientFactory(() -> capture(client)))).
                auth().certificate(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD, certAuthSettings().trustStoreType("PKCS12").allowAllHostnames()).
        when().
                get(httpUrl("/start")).
        then().
                statusCode(200);

        assertThat(client.get().getConnectionManager().getSchemeRegistry().getScheme("https").getDefaultPort()).isEqualTo(443);
    }

    @Test
    void plain_http_request_is_not_affected_by_an_ssl_config_that_cannot_be_loaded() {
        // The SSL configuration is only materialized when an https connection is actually opened
        given().
                config(config().sslConfig(sslConfig().trustStore("does-not-exist.p12", "whatever"))).
        when().
                get(httpUrl("/plain")).
        then().
                statusCode(200).
                body(equalTo("plain"));
    }

    private String httpUrl(String path) {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort() + path;
    }

    private static HttpClient capture(AtomicReference<DefaultHttpClient> holder) {
        DefaultHttpClient client = new DefaultHttpClient();
        holder.set(client);
        return client;
    }

    private static SSLContext serverSslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = SSLConfigOnHttpToHttpsRedirectTest.class.getClassLoader().getResourceAsStream(SELF_SIGNED_KEYSTORE)) {
            keyStore.load(in, KEYSTORE_PASSWORD.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private static void send(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}

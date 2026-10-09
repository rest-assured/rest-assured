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

package io.restassured;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import io.restassured.builder.RequestSpecBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Verifies that certificate authentication sends the client certificate of the given key store to a server that
 * requires one (mutual TLS), and that the key store is also used as trust store (issues #1158 and #1325).
 * <p>
 * <code>client_certificate_auth.p12</code> (password <code>changeit</code>) holds the private key and self-signed
 * certificate of the client (alias <code>client</code>) and, as a trusted certificate (alias <code>server</code>), the
 * certificate of the server's key store <code>self_signed_localhost.p12</code>. It was created with:
 * <pre>
 * keytool -genkeypair -alias client -keyalg RSA -keysize 2048 -validity 36500 \
 *   -dname "CN=Rest Assured Test Client, OU=Rest Assured Test, O=Rest Assured" -ext EKU=clientAuth \
 *   -storetype PKCS12 -keystore client_certificate_auth.p12 -storepass changeit
 * keytool -exportcert -alias localhost -keystore self_signed_localhost.p12 -storepass changeit -file server.cer
 * keytool -importcert -noprompt -alias server -file server.cer -storetype PKCS12 \
 *   -keystore client_certificate_auth.p12 -storepass changeit
 * </pre>
 */
class CertificateAuthMutualTlsTest {

    private static final String CLIENT_KEYSTORE = "client_certificate_auth.p12";
    private static final String SERVER_KEYSTORE = "self_signed_localhost.p12";
    private static final String PASSWORD = "changeit";

    private static HttpsServer httpsServer;

    @BeforeAll
    static void startServerThatRequiresAClientCertificate() throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(loadKeyStore(SERVER_KEYSTORE), PASSWORD.toCharArray());
        // The server only trusts the certificate of the client
        KeyStore trustedClients = KeyStore.getInstance("PKCS12");
        trustedClients.load(null, null);
        trustedClients.setCertificateEntry("client", loadKeyStore(CLIENT_KEYSTORE).getCertificate("client"));
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustedClients);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        httpsServer = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters sslParameters = getSSLContext().getDefaultSSLParameters();
                sslParameters.setNeedClientAuth(true);
                params.setSSLParameters(sslParameters);
            }
        });
        httpsServer.createContext("/hello", exchange -> {
            byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        httpsServer.start();
    }

    @AfterAll
    static void stopServer() {
        httpsServer.stop(0);
    }

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    @Test
    void server_rejects_request_without_client_certificate() {
        assertThatThrownBy(() ->
                given().
                        config(config().sslConfig(sslConfig().trustStore(CLIENT_KEYSTORE, PASSWORD).trustStoreType("PKCS12"))).
                when().
                        get(url()))
                .isInstanceOf(IOException.class);
    }

    @Test
    void certificate_sends_the_client_certificate_of_the_key_store() {
        given().
                auth().certificate(CLIENT_KEYSTORE, PASSWORD).
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void certificate_with_settings_sends_the_client_certificate_of_the_key_store() {
        given().
                auth().certificate(CLIENT_KEYSTORE, PASSWORD, certAuthSettings().keyStoreType("PKCS12").trustStoreType("PKCS12")).
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void statically_configured_certificate_sends_the_client_certificate_of_the_key_store() {
        RestAssured.authentication = RestAssured.certificate(CLIENT_KEYSTORE, PASSWORD);

        given().
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void certificate_with_settings_in_request_spec_builder_sends_the_client_certificate_of_the_key_store() {
        given().
                spec(new RequestSpecBuilder().setAuth(RestAssured.certificate(CLIENT_KEYSTORE, PASSWORD, certAuthSettings())).build()).
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void certificate_with_trust_store_in_settings_uses_that_trust_store_and_the_key_store_of_the_path() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("server", loadKeyStore(SERVER_KEYSTORE).getCertificate("localhost"));

        given().
                auth().certificate(CLIENT_KEYSTORE, PASSWORD, certAuthSettings().trustStore(trustStore)).
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void certificate_with_trust_store_in_settings_doesnt_use_the_key_store_of_the_path_as_trust_store() throws Exception {
        // A trust store that doesn't trust the server, so the request fails although the key store of the path does
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("client", loadKeyStore(CLIENT_KEYSTORE).getCertificate("client"));

        assertThatThrownBy(() ->
                given().
                        auth().certificate(CLIENT_KEYSTORE, PASSWORD, certAuthSettings().trustStore(trustStore)).
                when().
                        get(url()))
                .hasStackTraceContaining("PKIX");
    }

    @Test
    void certificate_with_key_store_in_settings_uses_the_password_for_the_key_store() throws Exception {
        given().
                auth().certificate(CLIENT_KEYSTORE, PASSWORD, certAuthSettings().keyStore(loadKeyStore(CLIENT_KEYSTORE))).
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void certificate_with_empty_trust_store_path_uses_the_jvm_default_trust_store() {
        RestAssured.authentication = RestAssured.certificate("", "", CLIENT_KEYSTORE, PASSWORD, certAuthSettings());

        // The JVM's default trust store doesn't trust the self-signed server certificate
        assertThatThrownBy(() -> given().when().get(url()))
                .hasStackTraceContaining("PKIX path building failed");
    }

    private static String url() {
        return "https://localhost:" + httpsServer.getAddress().getPort() + "/hello";
    }

    private static KeyStore loadKeyStore(String resource) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = CertificateAuthMutualTlsTest.class.getClassLoader().getResourceAsStream(resource)) {
            keyStore.load(is, PASSWORD.toCharArray());
        }
        return keyStore;
    }
}

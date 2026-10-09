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
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.Locale;

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
 * The JCEKS and JKS files of the tests are made from these in {@link #createStore(String, String, boolean)}.
 */
class CertificateAuthMutualTlsTest {

    private static final String CLIENT_KEYSTORE = "client_certificate_auth.p12";
    private static final String SERVER_KEYSTORE = "self_signed_localhost.p12";
    private static final String PASSWORD = "changeit";

    private static HttpsServer httpsServer;
    private static HttpsServer oneWayHttpsServer;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void startServers() throws Exception {
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

        httpsServer = startServer(sslContext, true);
        // A server that doesn't ask for a client certificate
        oneWayHttpsServer = startServer(sslContext, false);
    }

    private static HttpsServer startServer(SSLContext sslContext, boolean needClientAuth) throws IOException {
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters sslParameters = getSSLContext().getDefaultSSLParameters();
                sslParameters.setNeedClientAuth(needClientAuth);
                params.setSSLParameters(sslParameters);
            }
        });
        server.createContext("/hello", exchange -> {
            byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return server;
    }

    @AfterAll
    static void stopServers() {
        httpsServer.stop(0);
        oneWayHttpsServer.stop(0);
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

    @Test
    void certificate_reads_a_trust_store_only_file_with_the_trust_store_type() throws Exception {
        String trustStoreOnly = createStore("JCEKS", PASSWORD, false);

        given().
                auth().certificate(trustStoreOnly, PASSWORD, certAuthSettings().trustStoreType("JCEKS")).
        when().
                get(oneWayUrl()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void statically_configured_certificate_reads_a_trust_store_only_file_with_the_trust_store_type_of_the_ssl_config() throws Exception {
        String trustStoreOnly = createStore("JCEKS", PASSWORD, false);
        RestAssured.config = config().sslConfig(sslConfig().trustStoreType("JCEKS"));
        RestAssured.authentication = RestAssured.certificate(trustStoreOnly, PASSWORD);

        given().
        when().
                get(oneWayUrl()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void certificate_sends_the_client_certificate_of_a_key_store_read_with_the_trust_store_type() throws Exception {
        String keyStore = createStore("JCEKS", PASSWORD, true);

        given().
                auth().certificate(keyStore, PASSWORD, certAuthSettings().trustStoreType("JCEKS")).
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    @Test
    void certificate_explains_that_the_private_key_must_have_the_password_of_the_file() throws Exception {
        String keyStore = createStore("JKS", "another password", true);

        assertThatThrownBy(() ->
                given().
                        auth().certificate(keyStore, PASSWORD, certAuthSettings().trustStoreType("JKS")).
                when().
                        get(url()))
                .isInstanceOf(UnrecoverableKeyException.class)
                .hasMessage("The private key in " + keyStore + " can't be read with the given password. The file given to " +
                        "certificate(..) is used as key store as well as trust store, so its private key must have the password " +
                        "of the file. Use RestAssured.certificate(trustStorePath, trustStorePassword, keyStorePath, keyStorePassword, " +
                        "CertificateAuthSettings) to use separate files, or an empty key store path to use the file only as trust store.");
    }

    @Test
    void statically_configured_certificate_reads_the_key_store_of_the_ssl_config_with_its_password() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream is = Files.newInputStream(Paths.get(createStore("JKS", "key password", true)))) {
            keyStore.load(is, PASSWORD.toCharArray());
        }
        RestAssured.config = config().sslConfig(sslConfig().keyStore(keyStore).keyStore("key password"));
        RestAssured.authentication = RestAssured.certificate(CLIENT_KEYSTORE, PASSWORD);

        given().
        when().
                get(url()).
        then().
                statusCode(200).
                body(equalTo("hello"));
    }

    private static String url() {
        return "https://localhost:" + httpsServer.getAddress().getPort() + "/hello";
    }

    private static String oneWayUrl() {
        return "https://localhost:" + oneWayHttpsServer.getAddress().getPort() + "/hello";
    }

    /**
     * Create a key store file of the given type, with store password {@value #PASSWORD}, that trusts the server's
     * certificate and, if <code>withClientKey</code>, holds the client's private key with the given key password.
     */
    private static String createStore(String type, String keyPassword, boolean withClientKey) throws Exception {
        KeyStore clientKeyStore = loadKeyStore(CLIENT_KEYSTORE);
        KeyStore store = KeyStore.getInstance(type);
        store.load(null, null);
        store.setCertificateEntry("server", clientKeyStore.getCertificate("server"));
        if (withClientKey) {
            store.setKeyEntry("client", clientKeyStore.getKey("client", PASSWORD.toCharArray()), keyPassword.toCharArray(),
                    clientKeyStore.getCertificateChain("client"));
        }
        Path file = Files.createTempFile(tempDir, "store", "." + type.toLowerCase(Locale.ROOT));
        try (OutputStream os = Files.newOutputStream(file)) {
            store.store(os, PASSWORD.toCharArray());
        }
        return file.toAbsolutePath().toString();
    }

    private static KeyStore loadKeyStore(String resource) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = CertificateAuthMutualTlsTest.class.getClassLoader().getResourceAsStream(resource)) {
            keyStore.load(is, PASSWORD.toCharArray());
        }
        return keyStore;
    }
}

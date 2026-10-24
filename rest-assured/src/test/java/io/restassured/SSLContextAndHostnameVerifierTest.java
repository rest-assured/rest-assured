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
import com.sun.net.httpserver.HttpsServer;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.config.SSLConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * The JSSE based SSL settings added in 6.3.0 as replacements for the HttpClient 4 typed ones.
 */
@SuppressWarnings("deprecation")
class SSLContextAndHostnameVerifierTest {

    // Self-signed certificate for localhost/127.0.0.1, not trusted by the JVM default trust store.
    private static final String SELF_SIGNED_KEYSTORE = "self_signed_localhost.p12";
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final HostnameVerifier REJECT_ALL = (host, session) -> false;

    private HttpsServer httpsServer;

    @BeforeEach
    void startServer() throws Exception {
        httpsServer = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(serverSslContext()));
        httpsServer.createContext("/target", exchange -> {
            byte[] bytes = "secure".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        httpsServer.start();
    }

    @AfterEach
    void stopServer() {
        RestAssured.reset();
        httpsServer.stop(0);
    }

    @Test
    void ssl_context_in_ssl_config_is_used_to_connect() throws Exception {
        given().
                config(config().sslConfig(sslConfig().sslContext(clientSslContextTrustingTheServer()))).
        when().
                get(httpsUrl()).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void static_certificate_authentication_keeps_ssl_context_of_static_ssl_config() throws Exception {
        SSLContext sslContext = clientSslContextTrustingTheServer();
        RestAssured.config = config().sslConfig(sslConfig().sslContext(sslContext));

        CertAuthScheme certificate = (CertAuthScheme) RestAssured.certificate(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD);

        assertThat(certificate.getSslContext()).isSameAs(sslContext);
    }

    @Test
    void ssl_context_in_static_ssl_config_is_used_by_static_certificate_authentication() throws Exception {
        RestAssured.config = config().sslConfig(sslConfig().sslContext(clientSslContextTrustingTheServer()));
        RestAssured.authentication = RestAssured.certificate(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD);

        given().
        when().
                get(httpsUrl()).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void hostname_verifier_in_ssl_config_is_asked_about_the_host() throws Exception {
        List<String> verifiedHosts = new ArrayList<>();

        given().
                config(config().sslConfig(sslConfig().sslContext(clientSslContextTrustingTheServer()).hostnameVerifier((host, session) -> verifiedHosts.add(host)))).
        when().
                get(httpsUrl()).
        then().
                statusCode(200);

        assertThat(verifiedHosts).containsExactly("localhost");
    }

    @Test
    void hostname_verifier_in_ssl_config_that_rejects_the_host_fails_the_request() throws Exception {
        assertThatThrownBy(() ->
                given().
                        config(config().sslConfig(sslConfig().hostnameVerifier(REJECT_ALL).sslContext(clientSslContextTrustingTheServer()))).
                when().
                        get(httpsUrl()))
                .satisfies(e -> assertThat(hostNameRejection(e)).hasMessageContaining("Host name 'localhost' was rejected by the configured HostnameVerifier"));
    }

    // JDK differences table, hostname checks: the JDK client reports the SSLPeerUnverifiedException as the cause of an SSLException
    private static Throwable hostNameRejection(Throwable e) {
        assertThat(e).isInstanceOf(SSLException.class);
        Throwable rejection = e;
        while (!(rejection instanceof SSLPeerUnverifiedException) && rejection.getCause() != null) {
            rejection = rejection.getCause();
        }
        assertThat(rejection).isInstanceOf(SSLPeerUnverifiedException.class);
        return rejection;
    }

    @Test
    void hostname_verifier_in_ssl_config_is_also_used_with_a_trust_store() {
        assertThatThrownBy(() ->
                given().
                        config(config().sslConfig(sslConfig().trustStore(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD).trustStoreType("PKCS12").hostnameVerifier(REJECT_ALL))).
                when().
                        get(httpsUrl()))
                .satisfies(SSLContextAndHostnameVerifierTest::hostNameRejection);
    }

    @Test
    void ssl_context_in_certificate_auth_settings_is_used_to_connect() throws Exception {
        given().
                auth().certificate(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD, certAuthSettings().sslContext(clientSslContextTrustingTheServer())).
        when().
                get(httpsUrl()).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void certificate_scheme_used_through_an_authentication_context_connects() throws Exception {
        AuthenticationScheme certificate = RestAssured.certificate(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD, certAuthSettings().sslContext(clientSslContextTrustingTheServer()));
        RestAssured.authentication = certificate::authenticate;

        given().
        when().
                get(httpsUrl()).
        then().
                statusCode(200).
                body(equalTo("secure"));
    }

    @Test
    void hostname_verifier_in_certificate_auth_settings_that_rejects_the_host_fails_the_request() throws Exception {
        assertThatThrownBy(() ->
                given().
                        auth().certificate(SELF_SIGNED_KEYSTORE, KEYSTORE_PASSWORD, certAuthSettings().sslContext(clientSslContextTrustingTheServer()).hostnameVerifier(REJECT_ALL)).
                when().
                        get(httpsUrl()))
                .satisfies(SSLContextAndHostnameVerifierTest::hostNameRejection);
    }

    @Test
    void ssl_config_returns_what_was_set() throws Exception {
        SSLContext sslContext = clientSslContextTrustingTheServer();

        SSLConfig sslConfig = sslConfig().sslContext(sslContext).hostnameVerifier(REJECT_ALL);

        assertThat(sslConfig.getSSLContext()).isSameAs(sslContext);
        assertThat(sslConfig.getHostnameVerifier()).isSameAs(REJECT_ALL);
    }

    @Test
    void ssl_config_defaults_to_strict_host_name_verification_and_no_ssl_context() {
        assertThat(sslConfig().getHostnameVerifier()).isSameAs(sslConfig().strictHostnames().getHostnameVerifier());
        assertThat(sslConfig().allowAllHostnames().getHostnameVerifier()).isNotSameAs(sslConfig().getHostnameVerifier());
        assertThat(sslConfig().getSSLContext()).isNull();
    }

    @Test
    void certificate_auth_settings_return_what_was_set() throws Exception {
        SSLContext sslContext = clientSslContextTrustingTheServer();

        CertificateAuthSettings settings = certAuthSettings().sslContext(sslContext).hostnameVerifier(REJECT_ALL);

        assertThat(settings.getSSLContext()).isSameAs(sslContext);
        assertThat(settings.getHostnameVerifier()).isSameAs(REJECT_ALL);
        assertThat(certAuthSettings().getHostnameVerifier()).isSameAs(certAuthSettings().strictHostnames().getHostnameVerifier());
    }

    @Test
    void cert_auth_scheme_returns_what_was_set() throws Exception {
        SSLContext sslContext = clientSslContextTrustingTheServer();
        CertAuthScheme scheme = new CertAuthScheme();

        scheme.setSslContext(sslContext);
        scheme.setHostnameVerifier(REJECT_ALL);

        assertThat(scheme.getSslContext()).isSameAs(sslContext);
        assertThat(scheme.getHostnameVerifier()).isSameAs(REJECT_ALL);
    }

    private String httpsUrl() {
        return "https://localhost:" + httpsServer.getAddress().getPort() + "/target";
    }

    private static KeyStore selfSignedKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = SSLContextAndHostnameVerifierTest.class.getClassLoader().getResourceAsStream(SELF_SIGNED_KEYSTORE)) {
            keyStore.load(in, KEYSTORE_PASSWORD.toCharArray());
        }
        return keyStore;
    }

    private static SSLContext serverSslContext() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(selfSignedKeyStore(), KEYSTORE_PASSWORD.toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private static SSLContext clientSslContextTrustingTheServer() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(selfSignedKeyStore());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
}

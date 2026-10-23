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

package io.restassured.itest.java.wire;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.HttpConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SSLConfig.sslConfig;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * What REST Assured sends over HTTPS and which certificates and host names it accepts.
 * <p>
 * {@code localhost_valid_cert_server.p12} holds a certificate for {@code localhost} and {@code 127.0.0.1} that
 * {@code localhost_valid_cert_truststore.p12} trusts and the JVM doesn't. {@code wire_mismatched_name_server.p12}
 * holds a certificate for {@code not-this-host.example} that {@code wire_mismatched_name_truststore.p12} trusts. They
 * were made with (JDK 17 keytool, in src/test/resources):
 * <pre>
 * keytool -genkeypair -alias wire -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=not-this-host.example" -ext "SAN=dns:not-this-host.example" -storetype PKCS12 -keystore wire_mismatched_name_server.p12 -storepass test1234 -keypass test1234
 * keytool -exportcert -alias wire -storetype PKCS12 -keystore wire_mismatched_name_server.p12 -storepass test1234 -file wire_mismatched_name.crt
 * keytool -importcert -noprompt -alias wire -file wire_mismatched_name.crt -storetype PKCS12 -keystore wire_mismatched_name_truststore.p12 -storepass test1234
 * </pre>
 * (the exported {@code wire_mismatched_name.crt} isn't kept).
 */
public class WireSslITest extends WireTest {
    private static final String PASSWORD = "test1234";
    private static final String VALID_TRUST_STORE = "localhost_valid_cert_truststore.p12";
    private static final String MISMATCHED_NAME_TRUST_STORE = "wire_mismatched_name_truststore.p12";
    private static final String TLS_HEADER = "X-Tls-Protocol";
    private static final String REMOTE_PORT_HEADER = "X-Remote-Port";

    private WireServer mismatchedNameServer;

    @Override
    protected WireServer startServer() throws Exception {
        return WireServer.startWithTls("localhost_valid_cert_server.p12", PASSWORD, "PKCS12");
    }

    @BeforeAll
    void startMismatchedNameServer() throws Exception {
        mismatchedNameServer = WireServer.startWithTls("wire_mismatched_name_server.p12", PASSWORD, "PKCS12");
    }

    @AfterAll
    void stopMismatchedNameServer() throws Exception {
        mismatchedNameServer.close();
    }

    @BeforeEach
    void answerWithTheConnectionDetails() {
        mismatchedNameServer.reset();
        server.respond("/tls", connectionDetails());
        mismatchedNameServer.respond("/tls", connectionDetails());
    }

    private String validUrl() {
        return server.httpsUrl("localhost", "/tls");
    }

    private String mismatchedNameUrl() {
        return mismatchedNameServer.httpsUrl("localhost", "/tls");
    }

    private static RequestSpecification with(SSLConfig sslConfig) {
        return given().config(config().sslConfig(sslConfig));
    }

    // Untrusted certificates

    @Test public void
    an_untrusted_certificate_fails_the_handshake_by_default() {
        assertThatThrownBy(() -> given().get(validUrl())).isExactlyInstanceOf(SSLHandshakeException.class);
        assertThat(server.requests()).isEmpty();
    }

    @Test public void
    a_trust_store_in_the_ssl_config_makes_the_certificate_trusted() {
        with(sslConfig().trustStore(VALID_TRUST_STORE, PASSWORD)).
        when().
                get(validUrl()).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().localPort()).isEqualTo(server.httpsPort());
    }

    @Test public void
    a_trust_store_in_the_request_specification_makes_the_certificate_trusted() {
        given().trustStore(VALID_TRUST_STORE, PASSWORD).when().get(validUrl()).then().statusCode(200);
    }

    @Test public void
    a_trust_store_given_as_key_store_makes_the_certificate_trusted() throws Exception {
        given().trustStore(keyStore(VALID_TRUST_STORE)).when().get(validUrl()).then().statusCode(200);
    }

    @Test public void
    a_trust_store_set_statically_makes_the_certificate_trusted() {
        RestAssured.trustStore(VALID_TRUST_STORE, PASSWORD);
        try {
            given().get(validUrl()).then().statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    the_ip_address_of_the_server_matches_an_ip_address_in_the_certificate() {
        with(sslConfig().trustStore(VALID_TRUST_STORE, PASSWORD)).when().get(server.httpsUrl("127.0.0.1", "/tls")).then().statusCode(200);
    }

    // Certificates whose name doesn't match the host

    @Test public void
    a_trusted_certificate_whose_name_doesnt_match_fails_by_default() {
        assertThatThrownBy(() -> with(sslConfig().trustStore(MISMATCHED_NAME_TRUST_STORE, PASSWORD)).get(mismatchedNameUrl()))
                .isExactlyInstanceOf(SSLException.class);
        assertThat(mismatchedNameServer.requests()).isEmpty();
    }

    @Test public void
    allow_all_hostnames_accepts_a_trusted_certificate_whose_name_doesnt_match() {
        with(sslConfig().trustStore(MISMATCHED_NAME_TRUST_STORE, PASSWORD).allowAllHostnames()).
        when().
                get(mismatchedNameUrl()).
        then().
                statusCode(200);
    }

    @Test public void
    strict_hostnames_undoes_allow_all_hostnames() {
        assertThatThrownBy(() -> with(sslConfig().trustStore(MISMATCHED_NAME_TRUST_STORE, PASSWORD).allowAllHostnames().strictHostnames()).get(mismatchedNameUrl()))
                .isExactlyInstanceOf(SSLException.class);
    }

    @Test public void
    a_trust_store_path_in_the_request_specification_also_allows_all_hostnames() {
        // Today's behavior: given().trustStore(path, password) calls allowAllHostnames() too, for backward compatibility
        given().trustStore(MISMATCHED_NAME_TRUST_STORE, PASSWORD).when().get(mismatchedNameUrl()).then().statusCode(200);
    }

    @Test public void
    a_trust_store_key_store_in_the_request_specification_verifies_host_names() throws Exception {
        assertThatThrownBy(() -> given().trustStore(keyStore(MISMATCHED_NAME_TRUST_STORE)).get(mismatchedNameUrl()))
                .isExactlyInstanceOf(SSLException.class);
    }

    // Relaxed HTTPS validation

    @Test public void
    relaxed_https_validation_accepts_an_untrusted_certificate() {
        given().relaxedHTTPSValidation().when().get(validUrl()).then().statusCode(200);
    }

    @Test public void
    relaxed_https_validation_accepts_a_certificate_whose_name_doesnt_match() {
        given().relaxedHTTPSValidation().when().get(mismatchedNameUrl()).then().statusCode(200);
    }

    @Test public void
    relaxed_https_validation_in_the_ssl_config_accepts_an_untrusted_certificate() {
        with(sslConfig().relaxedHTTPSValidation()).when().get(validUrl()).then().statusCode(200);
    }

    @Test public void
    relaxed_https_validation_negotiates_the_newest_protocol_by_default() {
        given().relaxedHTTPSValidation().when().get(validUrl()).then().header(TLS_HEADER, "TLSv1.3");
    }

    @Test public void
    relaxed_https_validation_with_a_protocol_negotiates_that_protocol() {
        given().relaxedHTTPSValidation("TLSv1.2").when().get(mismatchedNameUrl()).then().statusCode(200).header(TLS_HEADER, "TLSv1.2");
    }

    @Test public void
    relaxed_https_validation_ignores_the_hostname_verifier() {
        // Documented: relaxedHTTPSValidation() sets a socket factory that verifies host names itself, by allowing all
        with(sslConfig().relaxedHTTPSValidation().hostnameVerifier((host, session) -> false)).
        when().
                get(mismatchedNameUrl()).
        then().
                statusCode(200);
    }

    // SSLConfig.sslContext(SSLContext) and hostnameVerifier(HostnameVerifier)

    @Test public void
    an_ssl_context_decides_which_certificates_are_trusted() throws Exception {
        with(sslConfig().sslContext(sslContextTrusting(VALID_TRUST_STORE))).when().get(validUrl()).then().statusCode(200);
    }

    @Test public void
    an_ssl_context_that_doesnt_trust_the_certificate_fails_the_handshake() throws Exception {
        assertThatThrownBy(() -> with(sslConfig().sslContext(sslContextTrusting(MISMATCHED_NAME_TRUST_STORE))).get(validUrl()))
                .isExactlyInstanceOf(SSLHandshakeException.class);
    }

    @Test public void
    an_ssl_context_overrides_the_trust_store() throws Exception {
        assertThatThrownBy(() -> with(sslConfig().trustStore(VALID_TRUST_STORE, PASSWORD).sslContext(sslContextTrusting(MISMATCHED_NAME_TRUST_STORE))).get(validUrl()))
                .isExactlyInstanceOf(SSLHandshakeException.class);
    }

    @Test public void
    host_names_are_still_verified_with_an_ssl_context() throws Exception {
        assertThatThrownBy(() -> with(sslConfig().sslContext(sslContextTrusting(MISMATCHED_NAME_TRUST_STORE))).get(mismatchedNameUrl()))
                .isExactlyInstanceOf(SSLException.class);
    }

    @Test public void
    allow_all_hostnames_applies_to_an_ssl_context() throws Exception {
        with(sslConfig().sslContext(sslContextTrusting(MISMATCHED_NAME_TRUST_STORE)).allowAllHostnames()).
        when().
                get(mismatchedNameUrl()).
        then().
                statusCode(200);
    }

    @Test public void
    a_hostname_verifier_gets_the_host_of_the_url_and_can_accept_a_name_that_doesnt_match() throws Exception {
        List<String> verifiedHosts = new CopyOnWriteArrayList<>();

        with(sslConfig().sslContext(sslContextTrusting(MISMATCHED_NAME_TRUST_STORE)).hostnameVerifier((host, session) -> verifiedHosts.add(host))).
        when().
                get(mismatchedNameUrl()).
        then().
                statusCode(200);

        assertThat(verifiedHosts).containsExactly("localhost");
    }

    @Test public void
    a_hostname_verifier_applies_to_a_trust_store() {
        with(sslConfig().trustStore(MISMATCHED_NAME_TRUST_STORE, PASSWORD).hostnameVerifier((host, session) -> true)).
        when().
                get(mismatchedNameUrl()).
        then().
                statusCode(200);
    }

    @Test public void
    a_hostname_verifier_that_rejects_the_host_fails_with_ssl_peer_unverified_exception() {
        assertThatThrownBy(() -> with(sslConfig().trustStore(VALID_TRUST_STORE, PASSWORD).hostnameVerifier((host, session) -> false)).get(validUrl()))
                .isExactlyInstanceOf(SSLPeerUnverifiedException.class);
        assertThat(server.requests()).isEmpty();
    }

    // The request itself

    @Test public void
    the_request_over_https_is_the_same_as_over_http() {
        RecordedRequest overHttp = sendPost(server.url("/p/a b"));
        server.reset();
        RecordedRequest overHttps = sendPost(server.httpsUrl("localhost", "/p/a b"));

        assertThat(overHttps.localPort()).isEqualTo(server.httpsPort());
        assertThat(overHttp.localPort()).isEqualTo(server.port());
        for (RecordedRequest request : List.of(overHttp, overHttps)) {
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.target()).isEqualTo("/p/a%20b?q=a%20b");
            assertThat(request.protocol()).isEqualTo("HTTP/1.1");
            assertThat(request.header("Accept")).isEqualTo("*/*");
            assertThat(request.header("x-Mixed-Case")).isEqualTo("1");
            assertThat(request.header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");
            assertThat(request.header("Content-Length")).isEqualTo("5");
            assertThat(request.bodyAsString(ISO_8859_1)).isEqualTo("hällo");
        }
        assertThat(overHttps.headers("x-Mixed-Case")).isEqualTo(overHttp.headers("x-Mixed-Case"));
    }

    private RecordedRequest sendPost(String url) {
        given().
                trustStore(VALID_TRUST_STORE, PASSWORD).
                header("x-Mixed-Case", "1").
                queryParam("q", "a b").
                contentType("text/plain").
                body("hällo").
        when().
                post(url).
        then().
                statusCode(200).
                body(equalTo("ok"));
        return server.onlyRequest();
    }

    @Test public void
    https_connections_with_ssl_settings_arent_reused_even_when_the_http_client_instance_is() {
        // Intentional: a connection made with request specific SSL settings mustn't serve a later request with other ones
        RestAssuredConfig config = config().sslConfig(sslConfig().trustStore(VALID_TRUST_STORE, PASSWORD)).httpClient(httpClientConfig().reuseHttpClientInstance());

        String first = remotePortOfReadResponse(config);
        String second = remotePortOfReadResponse(config);

        assertThat(second).isNotEqualTo(first);
    }

    private String remotePortOfReadResponse(RestAssuredConfig config) {
        Response response = given().config(config).get(validUrl());
        response.asString();
        return response.header(REMOTE_PORT_HEADER);
    }

    private static Responder connectionDetails() {
        return (request, response) -> {
            EndPoint endPoint = HttpConnection.getCurrentConnection().getEndPoint();
            response.setHeader(REMOTE_PORT_HEADER, String.valueOf(((InetSocketAddress) endPoint.getRemoteAddress()).getPort()));
            if (endPoint instanceof SslConnection.DecryptedEndPoint) {
                response.setHeader(TLS_HEADER, ((SslConnection.DecryptedEndPoint) endPoint).getSslConnection().getSSLEngine().getSession().getProtocol());
            }
            Responder.ok().respond(request, response);
        };
    }

    private static KeyStore keyStore(String resource) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = WireSslITest.class.getClassLoader().getResourceAsStream(resource)) {
            keyStore.load(in, PASSWORD.toCharArray());
        }
        return keyStore;
    }

    private static SSLContext sslContextTrusting(String trustStoreResource) throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore(trustStoreResource));
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }
}

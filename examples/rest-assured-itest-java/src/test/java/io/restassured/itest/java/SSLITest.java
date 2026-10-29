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

package io.restassured.itest.java;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import io.restassured.RestAssured;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.itest.java.support.TestClient;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.config.ConnectionConfig.connectionConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class SSLITest {

    public static ResponseSpecification helloWorldSpec() {
        return new ResponseSpecBuilder().
                expectBody("hello", equalTo("Hello Scalatra")).
                expectStatusCode(200).build();
    }

    @Nested
    public class OneWaySSLTest extends WithJetty {

        @Test
        public void throwsSSLExceptionWhenHostnameInCertDoesntMatch() {
            assertThatThrownBy(() -> RestAssured.get("https://localhost:8443/hello"))
                .isInstanceOf(javax.net.ssl.SSLException.class);
        }

        @Test
        public void givenTrustStoreDefinedStaticallyWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() {
            // The certificate of the server is issued for org.restassured, not localhost
            RestAssured.config = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
            RestAssured.trustStore("jetty_localhost_client.jks", "test1234");
            try {
                RestAssured.expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void whenEnablingAllowAllHostNamesVerifierWithoutActivatingAKeyStore() {
            RestAssured.config = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
            try {
                assertThatThrownBy(() -> RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec()))
                    .isExactlyInstanceOf(javax.net.ssl.SSLHandshakeException.class);
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void usingStaticallyConfiguredCertificateAuthenticationWorks() {
            RestAssured.authentication = RestAssured.certificate("jetty_localhost_client.jks", "test1234", CertificateAuthSettings.certAuthSettings().allowAllHostnames());
            try {
                RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec());
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertDoesntWork() {
            RestAssured.authentication = RestAssured.certificate("truststore_mjvmobile.jks", "test4321");
            try {
                assertThatThrownBy(() -> RestAssured.get("https://localhost:8443/hello").then().body(containsString("eurosport")))
                    .isInstanceOf(javax.net.ssl.SSLException.class);
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertWorksWhenSSLConfigIsConfiguredToAllowAllHostNames() {
            RestAssured.config = RestAssuredConfig.newConfig().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
            RestAssured.authentication = RestAssured.certificate("jetty_localhost_client.jks", "test1234");
            try {
                RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec());
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void givenKeystoreDefinedUsingGivenWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() {
            RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames())).trustStore("/jetty_localhost_client.jks", "test1234").then().expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
        }

        @Test
        public void throwsIOExceptionWhenPasswordIsIncorrect() {
            assertThatThrownBy(() ->
                RestAssured.given().
                        auth().certificate("jetty_localhost_client.jks", "test4333").
                        when().
                        get("https://localhost:8443/hello").
                        then().
                        body(containsString("eurosport"))
            )
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Keystore was tampered with, or password was incorrect");
        }

        @Test
        public void certificateAuthenticationWorks() {
            RestAssured.given().
                    auth().certificate("jetty_localhost_client.jks", "test1234", CertificateAuthSettings.certAuthSettings().allowAllHostnames()).
                    when().
                    get("https://localhost:8443/hello").
                    then().
                    spec(helloWorldSpec());
        }

        @Test
        public void
        allows_specifying_trust_store_in_dsl() throws Exception {
            InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jetty_localhost_client.jks");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keyStoreStream, "test1234".toCharArray());

            RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames())).trustStore(keyStore).when().get("https://localhost:8443/hello").then().statusCode(200);
        }

        @Disabled("Temporary ignored but I think this ought to work. Perhaps some issues with config merging?")
        @Test
        public void
        allows_specifying_trust_store_statically() throws Exception {
            InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jetty_localhost_client.jks");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keyStoreStream, "test1234".toCharArray());

            RestAssured.trustStore(keyStore);

            try {
                RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames())).when().get("https://localhost:8443/hello").then().statusCode(200);
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void
        allows_specifying_trust_store_and_allow_all_host_names_in_config_using_dsl() throws Exception {
            InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jetty_localhost_client.jks");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keyStoreStream, "test1234".toCharArray());

            RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().trustStore(keyStore).and().allowAllHostnames())).when().get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        }

        @Test
        public void
        relaxed_https_validation_works_using_instance_config() {
            RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())).when().get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        }

        // The certificate of the server is issued for org.restassured, not localhost, and no trust store trusts it.
        // Relaxed validation accepts it all the same.
        @Test
        public void
        relaxed_https_validation_works_using_instance_dsl() {
            RestAssured.given().relaxedHTTPSValidation().when().get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        }

        @Test
        public void
        relaxed_https_validation_works_when_defined_statically() {
            RestAssured.useRelaxedHTTPSValidation();

            try {
                RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec());
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void
        relaxed_https_validation_works_when_defined_statically_with_base_uri() {
            RestAssured.useRelaxedHTTPSValidation();
            RestAssured.baseURI = "https://localhost:8443";

            try {
                RestAssured.get("/hello").then().spec(helloWorldSpec());
            } finally {
                RestAssured.reset();
            }
        }

        @Test
        public void
        allow_all_hostnames_accepts_a_trusted_certificate_issued_for_another_host_using_instance_dsl() {
            // The certificate of the server is issued for org.restassured, not localhost
            RestAssured.given().
                    config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().trustStore("jetty_localhost_client.jks", "test1234").allowAllHostnames())).
                    when().
                    get("https://localhost:8443/hello").
                    then().
                    spec(helloWorldSpec());
        }

        @Test
        public void
        a_trusted_certificate_issued_for_another_host_is_rejected_without_allow_all_hostnames() {
            RestAssuredConfig config = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().trustStore("jetty_localhost_client.jks", "test1234"));

            assertThatThrownBy(() -> RestAssured.given().config(config).get("https://localhost:8443/hello"))
                    .isInstanceOf(javax.net.ssl.SSLException.class);
        }

        @Test
        public void
        truststore_works_with_static_base_uri() {
            RestAssured.baseURI = "https://localhost:8443/hello";

            try {
                RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames())).trustStore("/jetty_localhost_client.jks", "test1234").when().get().then().spec(helloWorldSpec());
            } finally {
                RestAssured.reset();
            }
        }

        @Disabled("Temporary ignored since site has changed")
        @Test
        public void truststrore_works_with_static_base_uri() throws Exception {
            RestAssured.baseURI = "https://bunny.cloudamqp.com/";

            InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("truststore_cloudamqp.jks");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keyStoreStream, "cloud1234".toCharArray());

            try {
                RestAssured.given().trustStore(keyStore).when().get("/api/").then().statusCode(200);
            } finally {
                RestAssured.reset();
            }
        }


        @Test
        public void
        can_make_request_to_sites_that_with_valid_ssl_cert() throws Exception {
            // A server whose certificate is valid for localhost, trusted through the JVM's default trust store
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("localhost_valid_cert_server.p12")) {
                keyStore.load(keyStoreStream, "test1234".toCharArray());
            }
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "test1234".toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            server.createContext("/", exchange -> {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            server.start();

            String trustStore = Paths.get(Thread.currentThread().getContextClassLoader().getResource("localhost_valid_cert_truststore.p12").toURI()).toString();
            Map<String, String> previousProperties = new HashMap<>();
            for (String property : asList("javax.net.ssl.trustStore", "javax.net.ssl.trustStoreType", "javax.net.ssl.trustStorePassword")) {
                previousProperties.put(property, System.getProperty(property));
            }
            try {
                System.setProperty("javax.net.ssl.trustStore", trustStore);
                System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
                System.setProperty("javax.net.ssl.trustStorePassword", "test1234");
                // JDK differences table, connection reuse: a client reads the trust store properties when it's created, and
                // the shared JDK client may have been created by an earlier test, so this request gets a client of its own
                RestAssured.given().config(RestAssuredConfig.config().connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse())).
                        get("https://localhost:" + server.getAddress().getPort() + "/").then().statusCode(200);
            } finally {
                previousProperties.forEach((property, value) -> {
                    if (value == null) {
                        System.clearProperty(property);
                    } else {
                        System.setProperty(property, value);
                    }
                });
                server.stop(0);
            }
        }

        @Test
        public void
        allows_specifying_trust_store_statically_with_request_builder() throws Exception {
            // Load the trust store
            InputStream trustStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jetty_localhost_client.jks");
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(trustStoreStream, "test1234".toCharArray());

            // Set the truststore on the global config
            RestAssured.config = RestAssured.config().sslConfig(SSLConfig.sslConfig().trustStore(trustStore).and().allowAllHostnames());

            final RequestSpecification spec = new RequestSpecBuilder().build();
            RestAssured.given().spec(spec).get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        }

        @Test
        public void
        supports_setting_truststore_in_request_specification() {
            final RequestSpecification spec = new RequestSpecBuilder().setConfig(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames())).setTrustStore("/jetty_localhost_client.jks", "test1234").build();
            RestAssured.given().spec(spec).expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
        }

        @Test
        public void
        supports_overriding_truststore_in_request_specification() {
            final RequestSpecification spec = new RequestSpecBuilder().setConfig(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames())).setTrustStore("/jetty_localhost_client.jks", "wrong pw").build();
            RestAssured.given().spec(spec).trustStore("/jetty_localhost_client.jks", "test1234").expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
        }
    }

    @Nested
    public class TwoWaySSLTest extends WithJetty {
        @BeforeAll
        public void startJetty() throws Exception {
            startJettyTwoWaySSL();
        }

        @Test
        public void relaxed_https_validation_throws_exception_without_keystore() {
            RestAssuredConfig sslConfig = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
            Throwable thrown = catchThrowable(() -> RestAssured.given().config(sslConfig).when().get("https://localhost:8443/hello").then().spec(helloWorldSpec()));

            if (TestClient.isJdk()) {
                assertThat(thrown).isInstanceOf(javax.net.ssl.SSLException.class);
            } else {
                // With TLS 1.3, the server rejects the missing client certificate after the client has finished the
                // handshake. Apache HttpClient 5 then writes the request on the blocking socket, and fails with the
                // server's alert, or with a broken pipe or a reset when the server has already closed the connection.
                assertThat(thrown).isInstanceOfAny(javax.net.ssl.SSLException.class, SocketException.class);
            }
        }

        @Test
        public void relaxed_https_validation_works_using_instance_config() {
            RestAssuredConfig sslConfig = RestAssuredConfig.config().sslConfig(
                    SSLConfig.sslConfig()
                            .keyStore("keystore.p12", "test1234")
                            .keystoreType("PKCS12")
                            .relaxedHTTPSValidation()
            );
            RestAssured.given().config(sslConfig).when().get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        }

        @Test
        public void certificate_authentication_sends_the_client_certificate_of_the_key_store() {
            // keystore.p12 holds the private key and certificate that the server also uses, so it's trusted as trust store as well
            RestAssured.given().
                    auth().certificate("keystore.p12", "test1234", CertificateAuthSettings.certAuthSettings().keyStoreType("PKCS12").trustStoreType("PKCS12").allowAllHostnames()).
                    when().
                    get("https://localhost:8443/hello").
                    then().
                    spec(helloWorldSpec());
        }

        @Test
        public void statically_configured_certificate_authentication_sends_the_client_certificate_of_the_key_store() {
            RestAssured.authentication = RestAssured.certificate("keystore.p12", "test1234", CertificateAuthSettings.certAuthSettings().keyStoreType("PKCS12").trustStoreType("PKCS12").allowAllHostnames());
            try {
                RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec());
            } finally {
                RestAssured.reset();
            }
        }
    }
}

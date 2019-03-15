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

import io.restassured.RestAssured;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class SSLITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public static ResponseSpecification helloWorldSpec() {
        return new ResponseSpecBuilder().
                expectBody("hello", equalTo("Hello Scalatra")).
                expectStatusCode(200).build();
    }

    @Test(expected = SSLException.class)
    public void throwsSSLExceptionWhenHostnameInCertDoesntMatch() throws Exception {
        RestAssured.get("https://localhost:8443/hello");
    }

    @Test
    public void givenTrustStoreDefinedStaticallyWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        RestAssured.trustStore("jetty_localhost_client.jks", "test1234");
        try {
            RestAssured.expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test(expected = SSLHandshakeException.class)
    public void whenEnablingAllowAllHostNamesVerifierWithoutActivatingAKeyStore() throws Exception {
        RestAssured.config = RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
        try {
            RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingStaticallyConfiguredCertificateAuthenticationWorks() throws Exception {
        RestAssured.authentication = RestAssured.certificate("jetty_localhost_client.jks", "test1234", CertificateAuthSettings.certAuthSettings().allowAllHostnames());
        try {
            RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        } finally {
            RestAssured.reset();
        }
    }

    @Test(expected = SSLException.class)
    public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertDoesntWork() throws Exception {
        RestAssured.authentication = RestAssured.certificate("truststore_mjvmobile.jks", "test4321");
        try {
            RestAssured.get("https://localhost:8443/hello").then().body(containsString("eurosport"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertWorksWhenSSLConfigIsConfiguredToAllowAllHostNames() throws Exception {
        RestAssured.config = RestAssuredConfig.newConfig().sslConfig(SSLConfig.sslConfig().allowAllHostnames());
        RestAssured.authentication = RestAssured.certificate("jetty_localhost_client.jks", "test1234");
        try {
            RestAssured.get("https://localhost:8443/hello").then().spec(helloWorldSpec());
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void givenKeystoreDefinedUsingGivenWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        RestAssured.given().trustStore("/jetty_localhost_client.jks", "test1234").then().expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
    }

    @Test
    public void throwsIOExceptionWhenPasswordIsIncorrect() throws Exception {
        exception.expect(IOException.class);
        exception.expectMessage("Keystore was tampered with, or password was incorrect");

        RestAssured.given().
                auth().certificate("jetty_localhost_client.jks", "test4333").
        when().
                get("https://localhost:8443/hello").
        then().
                body(containsString("eurosport"));
    }

    @Test
    public void certificateAuthenticationWorks() throws Exception {
        RestAssured.given().
                auth().certificate("jetty_localhost_client.jks", "test1234", CertificateAuthSettings.certAuthSettings().allowAllHostnames()).
        when().
                get("https://localhost:8443/hello").
        then().
                spec(helloWorldSpec());
    }

    @Test public void
    allows_specifying_trust_store_in_dsl() throws Exception {
        InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jetty_localhost_client.jks");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(keyStoreStream, "test1234".toCharArray());

        RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().allowAllHostnames())).trustStore(keyStore).when().get("https://localhost:8443/hello").then().statusCode(200);
    }

    @Ignore("Temporary ignored but I think this ought to work. Perhaps some issues with config merging?")
    @Test public void
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

    @Test public void
    allows_specifying_trust_store_and_allow_all_host_names_in_config_using_dsl() throws Exception {
        InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("jetty_localhost_client.jks");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(keyStoreStream, "test1234".toCharArray());

        RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().trustStore(keyStore).and().allowAllHostnames())).when().get("https://localhost:8443/hello").then().spec(helloWorldSpec());
    }

    @Test public void
    relaxed_https_validation_works_using_instance_config() {
        RestAssured.given().config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())).when().get("https://localhost:8443/hello").then().spec(helloWorldSpec());
    }

    @Test public void
    relaxed_https_validation_works_using_instance_dsl() {
        RestAssured.given().relaxedHTTPSValidation().when().get("https://bunny.cloudamqp.com/api/").then().statusCode(200);
    }

    @Test public void
    relaxed_https_validation_works_when_defined_statically() {
        RestAssured.useRelaxedHTTPSValidation();

        try {
            RestAssured.get("https://bunny.cloudamqp.com/api/").then().statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    relaxed_https_validation_works_when_defined_statically_with_base_uri() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = "https://bunny.cloudamqp.com";

        try {
            RestAssured.get("/api/").then().statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    truststore_works_with_static_base_uri() {
        RestAssured.baseURI = "https://localhost:8443/hello";

        try {
            RestAssured.given().trustStore("/jetty_localhost_client.jks", "test1234").when().get().then().spec(helloWorldSpec());
        } finally {
            RestAssured.reset();
        }
    }

    @Ignore("Temporary ignored since site has changed") @Test public void
    truststrore_works_with_static_base_uri() throws Exception{
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


    @Test public void
    can_make_request_to_sites_that_with_valid_ssl_cert() {
        RestAssured.get("https://duckduckgo.com/").then().statusCode(200);
    }

    @Test public void
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

    @Test public void
    supports_setting_truststore_in_request_specification() {
        final RequestSpecification spec = new RequestSpecBuilder().setTrustStore("/jetty_localhost_client.jks", "test1234").build();
        RestAssured.given().spec(spec).expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
    }

    @Test public void
    supports_overriding_truststore_in_request_specification() {
        final RequestSpecification spec = new RequestSpecBuilder().setTrustStore("/jetty_localhost_client.jks", "wrong pw").build();
        RestAssured.given().spec(spec).trustStore("/jetty_localhost_client.jks", "test1234").expect().spec(helloWorldSpec()).when().get("https://localhost:8443/hello");
    }
}

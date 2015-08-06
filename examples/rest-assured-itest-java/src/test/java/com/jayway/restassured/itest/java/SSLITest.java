/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static com.jayway.restassured.config.SSLConfig.sslConfig;
import static com.jayway.restassured.http.ContentType.HTML;
import static org.hamcrest.Matchers.containsString;

@Ignore
public class SSLITest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    public static ResponseSpecification eurosportSpec() {
        return new ResponseSpecBuilder().
                expectBody(containsString("An error occurred while processing your request.")).
                expectContentType(HTML).
                expectStatusCode(500).build();
    }

    @Test(expected = SSLException.class)
    public void throwsSSLExceptionWhenHostnameInCertDoesntMatch() throws Exception {
        get("https://tv.eurosport.com/");
    }

    @Test
    public void givenKeystoreDefinedStaticallyWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        RestAssured.keystore("truststore_eurosport.jks", "test4321");
        try {
            expect().spec(eurosportSpec()).get("https://tv.eurosport.com/");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenEnablingAllowAllHostNamesVerifierWithoutActivatingAKeyStoreTheCallTo() throws Exception {
        RestAssured.config = config().sslConfig(sslConfig().allowAllHostnames());
        try {
            get("https://tv.eurosport.com/").then().spec(eurosportSpec());
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingStaticallyConfiguredCertificateAuthenticationWorks() throws Exception {
        RestAssured.authentication = certificate("truststore_eurosport.jks", "test4321", certAuthSettings().allowAllHostnames());
        try {
            get("https://tv.eurosport.com/").then().spec(eurosportSpec());
        } finally {
            RestAssured.reset();
        }
    }

    @Test(expected = SSLException.class)
    public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertDoesntWork() throws Exception {
        RestAssured.authentication = certificate("truststore_mjvmobile.jks", "test4321");
        try {
            get("https://tv.eurosport.com/").then().body(containsString("eurosport"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertWorksWhenSSLConfigIsConfiguredToAllowAllHostNames() throws Exception {
        RestAssured.config = newConfig().sslConfig(sslConfig().allowAllHostnames());
        RestAssured.authentication = certificate("truststore_eurosport.jks", "test4321");
        try {
            get("https://tv.eurosport.com/").then().spec(eurosportSpec());
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void givenKeystoreDefinedUsingGivenWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        given().keystore("/truststore_eurosport.jks", "test4321").then().expect().spec(eurosportSpec()).get("https://tv.eurosport.com/");
    }

    @Test
    public void throwsIOExceptionWhenPasswordIsIncorrect() throws Exception {
        exception.expect(IOException.class);
        exception.expectMessage("Keystore was tampered with, or password was incorrect");

        given().
                auth().certificate("truststore_eurosport.jks", "test4333").
        when().
                get("https://tv.eurosport.com/").
        then().
                body(containsString("eurosport"));
    }

    @Test
    public void certificateAuthenticationWorks() throws Exception {
        given().
                auth().certificate("truststore_eurosport.jks", "test4321", certAuthSettings().allowAllHostnames()).
        when().
                get("https://tv.eurosport.com/").
        then().
                spec(eurosportSpec());
    }

    @Ignore("Temporary ignored since site has changed") @Test public void
    allows_specifying_trust_store_in_dsl() throws Exception {
        InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("truststore_cloudamqp.jks");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(keyStoreStream, "cloud1234".toCharArray());

        given().trustStore(keyStore).then().get("https://bunny.cloudamqp.com/api/").then().statusCode(200);
    }

    @Ignore("Temporary ignored since site has changed") @Test public void
    allows_specifying_trust_store_statically() throws Exception {

        InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("truststore_cloudamqp.jks");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(keyStoreStream, "cloud1234".toCharArray());

        RestAssured.trustStore(keyStore);

        try {
            get("https://bunny.cloudamqp.com/api/").then().statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    allows_specifying_trust_store_and_allow_all_host_names_in_config_using_dsl() throws Exception {
        InputStream keyStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("truststore_eurosport.jks");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(keyStoreStream, "test4321".toCharArray());

        given().config(config().sslConfig(sslConfig().trustStore(keyStore).and().allowAllHostnames())).then().get("https://tv.eurosport.com/").then().spec(eurosportSpec());
    }

    @Test public void
    relaxed_https_validation_works_using_instance_config() {
        given().config(config().sslConfig(sslConfig().relaxedHTTPSValidation())).then().get("https://tv.eurosport.com/").then().spec(eurosportSpec());
    }

    @Test public void
    relaxed_https_validation_works_using_instance_dsl() {
        given().relaxedHTTPSValidation().then().get("https://bunny.cloudamqp.com/api/").then().statusCode(200);
    }

    @Test public void
    relaxed_https_validation_works_when_defined_statically() {
        RestAssured.useRelaxedHTTPSValidation();

        try {
            get("https://bunny.cloudamqp.com/api/").then().statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    relaxed_https_validation_works_when_defined_statically_with_base_uri() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = "https://bunny.cloudamqp.com";

        try {
            get("/api/").then().statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    keystore_works_with_static_base_uri() {
        RestAssured.baseURI = "https://tv.eurosport.com/";

        try {
            given().keystore("/truststore_eurosport.jks", "test4321").when().get().then().spec(eurosportSpec());
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
            given().trustStore(keyStore).when().get("/api/").then().statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }


    @Test public void
    can_make_request_to_sites_that_with_valid_ssl_cert() {
        get("https://duckduckgo.com/").then().statusCode(200);
    }

    @Test public void
    allows_specifying_trust_store_statically_with_request_builder() throws Exception {
        // Load the trust store
        InputStream trustStoreStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("truststore_eurosport.jks");
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(trustStoreStream, "test4321".toCharArray());

        // Set the truststore on the global config
        RestAssured.config = RestAssured.config().sslConfig(sslConfig().trustStore(trustStore).and().allowAllHostnames());

        final RequestSpecification spec = new RequestSpecBuilder().build();
        given().spec(spec).get("https://tv.eurosport.com/").then().spec(eurosportSpec());
    }

}

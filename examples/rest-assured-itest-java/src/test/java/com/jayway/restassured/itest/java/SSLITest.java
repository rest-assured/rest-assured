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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.net.ssl.SSLException;
import java.io.IOException;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static com.jayway.restassured.config.SSLConfig.sslConfig;
import static org.hamcrest.Matchers.containsString;

public class SSLITest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test(expected = SSLException.class)
    public void throwsSSLExceptionWhenHostnameInCertDoesntMatch() throws Exception {
        get("https://tv.eurosport.com/");
    }

    @Test
    public void givenKeystoreDefinedStaticallyWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        RestAssured.keystore("truststore_eurosport.jks", "test4321");
        try {
            expect().body(containsString("EUROSPORT PLAYER")).get("https://tv.eurosport.com/");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void givenKeystoreDefinedStaticallyWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseOtherSSLDomains() throws Exception {
        RestAssured.keystore("/truststore_mjvmobile.jks", "test4321");
        try {
            expect().body(containsString("EUROSPORT PLAYER")).get("https://tv.eurosport.com/");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void whenEnablingAllowAllHostNamesVerifierWithoutActivatingAKeyStoreTheCallTo() throws Exception {
        RestAssured.config = config().sslConfig(sslConfig().allowAllHostnames());
        try {
            get("https://tv.eurosport.com/").then().body(containsString("EUROSPORT PLAYER"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingStaticallyConfiguredCertificateAuthenticationWorks() throws Exception {
        RestAssured.authentication = certificate("truststore_eurosport.jks", "test4321", certAuthSettings().allowAllHostnames());
        try {
            get("https://tv.eurosport.com/").then().body(containsString("EUROSPORT PLAYER"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test(expected = SSLException.class)
    public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertDoesntWork() throws Exception {
        RestAssured.authentication = certificate("truststore_mjvmobile.jks", "test4321");
        try {
            get("https://tv.eurosport.com/").then().body(containsString("EUROSPORT PLAYER"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void usingStaticallyConfiguredCertificateAuthenticationWithIllegalHostNameInCertWorksWhenSSLConfigIsConfiguredToAllowAllHostNames() throws Exception {
        RestAssured.config = newConfig().sslConfig(sslConfig().allowAllHostnames());
        RestAssured.authentication = certificate("truststore_eurosport.jks", "test4321");
        try {
            get("https://tv.eurosport.com/").then().body(containsString("EUROSPORT PLAYER"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void givenKeystoreDefinedUsingGivenWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        given().keystore("/truststore_eurosport.jks", "test4321").then().expect().body(containsString("EUROSPORT PLAYER")).get("https://tv.eurosport.com/");
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
                body(containsString("EUROSPORT PLAYER"));
    }

    @Test
    public void certificateAuthenticationWorks() throws Exception {
        given().
                auth().certificate("truststore_eurosport.jks", "test4321", certAuthSettings().allowAllHostnames()).
        when().
                get("https://tv.eurosport.com/").
        then().
                body(containsString("EUROSPORT PLAYER"));
    }
}

/*
 * Copyright 2011 the original author or authors.
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
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLException;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.containsString;

@Ignore("https://dev.java.net/ is down atm")
public class SSLTest {

    @Test(expected = SSLException.class)
    public void throwsSSLExceptionWhenHostnameInCertDoesntMatch() throws Exception {
        get("https://dev.java.net/");
    }

    @Test
    public void givenKeystoreDefinedStaticallyWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        RestAssured.keystore("/truststore.jks", "test1234");
        try {
            expect().body(containsString("The Source for Java Technology Collaboration")).get("https://dev.java.net/");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void givenKeystoreDefinedUsingGivenWhenSpecifyingJksKeyStoreFileWithCorrectPasswordAllowsToUseSSL() throws Exception {
        given().keystore("/truststore.jks", "test1234").then().expect().body(containsString("The Source for Java Technology Collaboration")).get("https://dev.java.net/");
    }
}

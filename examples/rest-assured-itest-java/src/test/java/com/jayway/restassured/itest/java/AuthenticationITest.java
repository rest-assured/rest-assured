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
import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.authentication.FormAuthConfig.springSecurity;
import static org.hamcrest.Matchers.equalTo;

public class AuthenticationITest extends WithJetty {

    @Test
    public void basicAuthentication() throws Exception {
        given().auth().basic("jetty", "jetty").expect().statusCode(200).when().get("/secured/hello");
    }

    @Test
    public void basicAuthenticationUsingDefault() throws Exception {
        authentication = basic("jetty", "jetty");
        try {
            expect().statusCode(200).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void explicitExcludeOfBasicAuthenticationWhenUsingDefault() throws Exception {
        authentication = basic("jetty", "jetty");
        try {
            given().auth().none().and().expect().statusCode(401).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void supportsExpectingStatusCodeWhenAuthenticationError() throws Exception {
        given().auth().basic("abcd", "abCD1").expect().statusCode(401).when().get("/secured/hello");
    }

    @Test
    public void supportsPreemptiveBasicAuthentication() throws Exception {
        given().auth().preemptive().basic("jetty", "jetty").expect().statusCode(200).when().get("/secured/hello");
    }

    @Test
    public void supportsExpectingStatusCodeWhenPreemptiveBasicAuthenticationError() throws Exception {
        given().auth().preemptive().basic("jetty", "bad password").expect().statusCode(401).when().get("/secured/hello");
    }

    @Test
    public void preemptiveBasicAuthenticationUsingDefault() throws Exception {
        authentication = preemptive().basic("jetty", "jetty");
        try {
            expect().statusCode(200).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void explicitExcludeOfPreemptiveBasicAuthenticationWhenUsingDefault() throws Exception {
        authentication = preemptive().basic("jetty", "jetty");
        try {
            given().auth().none().and().expect().statusCode(401).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void formAuthenticationUsingSpringAuthConf() throws Exception {
        given().
                auth().form("John", "Doe", springSecurity()).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void formAuthenticationWithLoginPageParsing() throws Exception {
        given().
                auth().form("John", "Doe").
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void formAuthenticationUsingDefaultWithLoginPageParsing() throws Exception {
        RestAssured.authentication = form("John", "Doe");

        try {
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void formAuthenticationUsingDefaultWithSpringAuthConf() throws Exception {
        RestAssured.authentication = form("John", "Doe", springSecurity());

        try {
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
        } finally {
            RestAssured.reset();
        }
    }

    /**
     * Asserts that <a href="http://code.google.com/p/rest-assured/issues/detail?id=95">issue 95</a> is resolved.
     */
    @Test
    public void canSpecifyPortWhenUsingFormAuth() throws Exception {
        RestAssured.port = 8091; // Specify an unused port

        try {
            given().
                    auth().form("John", "Doe", springSecurity()).
                    port(8080).
            expect().
                    statusCode(200).
                    body(equalTo("OK")).
            when().
                    get("/formAuth");
        } finally {
            RestAssured.port = 8080;
        }
    }
}

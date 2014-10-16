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
import com.jayway.restassured.authentication.FormAuthConfig;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.filter.session.SessionFilter;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.authentication.FormAuthConfig.formAuthConfig;
import static com.jayway.restassured.authentication.FormAuthConfig.springSecurity;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static com.jayway.restassured.config.SessionConfig.sessionConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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
    public void formAuthenticationWithAutoFormDetailsAndAutoCsrfDetection() throws Exception {
        given().
                auth().form("John", "Doe", formAuthConfig().withAutoDetectionOfCsrf()).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithDefinedCsrfField() throws Exception {
        given().
                auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf", "j_username", "j_password").withCsrfFieldName("_csrf")).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithDefinedCsrfFieldAsHeader() throws Exception {
        given().
                auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf_header", "j_username", "j_password").withCsrfFieldName("_csrf").sendCsrfTokenAsHeader()).
        when().
                get("/formAuthCsrfInHeader").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithCsrfAutoDetectionButSpecifiedFormDetails() throws Exception {
        given().
                auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf", "j_username", "j_password").withAutoDetectionOfCsrf()).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationUsingLogging() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                auth().form("John", "Doe", springSecurity().withLoggingEnabled(new LogConfig(captor, true))).
        when().
                get("/formAuth").
        then().
                statusCode(200).
                body(equalTo("OK"));

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/j_spring_security_check\nProxy:\t\t\t<none>\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\tj_username=John\n\t\t\t\tj_password=Doe\nPath params:\t<none>\nMultiparts:\t\t<none>\nHeaders:\t\tContent-Type=*/*; charset="+ RestAssured.config().getEncoderConfig().defaultContentCharset()+"\nCookies:\t\t<none>\nBody:\t\t\t<none>\nHTTP/1.1 200 OK\nContent-Type: text/plain; charset=utf-8\nSet-Cookie: jsessionid=1234\nContent-Length: 0\nServer: Jetty(6.1.14)\n"));
    }

    @Test
    public void formAuthenticationUsingLoggingWithLogDetailEqualToParams() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                auth().form("John", "Doe", springSecurity().withLoggingEnabled(LogDetail.PARAMS, new LogConfig(captor, true))).
        when().
                get("/formAuth").
        then().
                statusCode(200).
                body(equalTo("OK"));

        assertThat(writer.toString(), equalTo("Request params:\t<none>\nQuery params:\t<none>\nForm params:\tj_username=John\n\t\t\t\tj_password=Doe\nPath params:\t<none>\nMultiparts:\t\t<none>\n"));
    }

    @Test
    public void formAuthenticationUsingLoggingWithLogDetailEqualToStatus() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                auth().form("John", "Doe", springSecurity().withLoggingEnabled(LogDetail.STATUS, new LogConfig(captor, true))).
        when().
                get("/formAuth").
        then().
                statusCode(200).
                body(equalTo("OK"));

        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\n"));
    }

    @Test
    public void formAuthenticationUsingSpringAuthConfDefinedInRequestSpec() throws Exception {
        final RequestSpecification specification = new RequestSpecBuilder().setAuth(form("John", "Doe", springSecurity())).build();

        given().
                spec(specification).
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
    public void sessionFilterRecordsAndProvidesTheSessionId() throws Exception {
        final SessionFilter sessionFilter = new SessionFilter();

        given().
                auth().form("John", "Doe").
                filter(sessionFilter).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");

        assertThat(sessionFilter.getSessionId(), equalTo("1234"));
    }

    @Test
    public void reusingSameSessionFilterInDifferentRequestsAppliesTheSessionIdToTheNewRequest() throws Exception {
        final SessionFilter sessionFilter = new SessionFilter();

        given().
                auth().form("John", "Doe").
                filter(sessionFilter).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");

        given().
                filter(sessionFilter).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void sessionFilterRecordsAndProvidesTheSessionIdWhenSessionNameIsNotDefault() throws Exception {
        final SessionFilter sessionFilter = new SessionFilter();

        given().
                config(newConfig().sessionConfig(sessionConfig().sessionIdName("phpsessionid"))).
                auth().form("John", "Doe", new FormAuthConfig("/j_spring_security_check_phpsessionid", "j_username", "j_password")).
                filter(sessionFilter).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");

        assertThat(sessionFilter.getSessionId(), equalTo("1234"));
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

    /**
     * Asserts that <a href="http://code.google.com/p/rest-assured/issues/detail?id=233">issue 233</a> is resolved.
     */
    @Test
    public void canOverridePreemptiveBasicAuthFromStaticConfiguration() throws Exception {
        RestAssured.authentication = preemptive().basic("invalid", "password");

        try {
            given().
                     auth().preemptive().basic("jetty", "jetty").
            expect().
                     statusCode(200).
            when().
                    get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }
}

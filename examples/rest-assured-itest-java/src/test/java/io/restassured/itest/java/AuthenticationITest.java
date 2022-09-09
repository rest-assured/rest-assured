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
import io.restassured.authentication.FormAuthConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.*;
import static io.restassured.authentication.FormAuthConfig.formAuthConfig;
import static io.restassured.config.CsrfConfig.csrfConfig;
import static io.restassured.config.SessionConfig.sessionConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AuthenticationITest extends WithJetty {

    @Test
    public void basicAuthentication() {
        given().auth().basic("jetty", "jetty").expect().statusCode(200).when().get("/secured/hello");
    }

    @Test
    public void basicAuthenticationUsingDefault() {
        authentication = basic("jetty", "jetty");
        try {
            expect().statusCode(200).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void explicitExcludeOfBasicAuthenticationWhenUsingDefault() {
        authentication = basic("jetty", "jetty");
        try {
            given().auth().none().and().expect().statusCode(401).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void supportsExpectingStatusCodeWhenAuthenticationError() {
        given().auth().basic("abcd", "abCD1").expect().statusCode(401).when().get("/secured/hello");
    }

    @Test
    public void supportsPreemptiveBasicAuthentication() {
        given().auth().preemptive().basic("jetty", "jetty").expect().statusCode(200).when().get("/secured/hello");
    }

    @Test
    public void supportsExpectingStatusCodeWhenPreemptiveBasicAuthenticationError() {
        given().auth().preemptive().basic("jetty", "bad password").expect().statusCode(401).when().get("/secured/hello");
    }

    @Test
    public void preemptiveBasicAuthenticationUsingDefault() {
        authentication = preemptive().basic("jetty", "jetty");
        try {
            expect().statusCode(200).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void explicitExcludeOfPreemptiveBasicAuthenticationWhenUsingDefault() {
        authentication = preemptive().basic("jetty", "jetty");
        try {
            given().auth().none().and().expect().statusCode(401).when().get("/secured/hello");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void formAuthenticationUsingSpringAuthConf() {
        given().
                auth().form("John", "Doe", FormAuthConfig.springSecurity()).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void formAuthenticationWithAutoFormDetailsAndAutoCsrfDetectionDefinedInRequestConfig() {
        given().
                config(config().csrfConfig(csrfConfig().with().csrfTokenPath("/formAuthCsrf").and().autoDetectCsrfInputFieldName())).
                auth().form("John", "Doe", formAuthConfig()).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithDefinedCsrfFieldDefinedInRequestConfig() {
        given().
                config(config().csrfConfig(csrfConfig().csrfTokenPath("/formAuthCsrf").csrfInputFieldName("_csrf"))).
                auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf", "j_username", "j_password")).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithAutoFormDetailsAndAutoCsrfDetectionDefinedInStaticRequestConfig() {
        RestAssured.config = config().csrfConfig(csrfConfig().csrfTokenPath("/formAuthCsrf").autoDetectCsrfInputFieldName());

        try {
            given().
                    auth().form("John", "Doe", formAuthConfig()).
            when().
                    get("/formAuthCsrf").
            then().
                    statusCode(200).
                    body(equalTo("OK"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void formAuthenticationWithDefinedCsrfFieldDefinedInStaticRequestConfig() {
        RestAssured.config = config().csrfConfig(csrfConfig().csrfTokenPath("/formAuthCsrf").csrfInputFieldName("_csrf"));

        try {
            given().
                    auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf", "j_username", "j_password")).
            when().
                    get("/formAuthCsrf").
            then().
                    statusCode(200).
                    body(equalTo("OK"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void formAuthenticationWithAutoFormDetailsAndAutoCsrfDetectionDefinedInDSL() {
        given().
                csrf("/formAuthCsrf").
                auth().form("John", "Doe", formAuthConfig()).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithDefinedCsrfFieldDefinedDefinedInDSL() {
        given().
                csrf("/formAuthCsrf", "_csrf").
                auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf", "j_username", "j_password")).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithDefinedCsrfFieldAsHeader() {
        given().
                config(config().csrfConfig(csrfConfig().csrfTokenPath("/formAuthCsrf").csrfInputFieldName("_csrf").and().sendCsrfTokenAsHeader())).
                auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf_header", "j_username", "j_password")).
        when().
                get("/formAuthCsrfInHeader").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithAdditionalFields() {
        given().
                auth().form("John", "Doe", formAuthConfig().withAdditionalFields("smquerydata", "smauthreason", "smagentname").withAdditionalField("postpreservationdata")).
        when().
                get("/formAuthAdditionalFields").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationWithCsrfAutoDetectionButSpecifiedFormDetails() {
        given().
                config(config().csrfConfig(csrfConfig().autoDetectCsrfInputFieldName())).
                auth().form("John", "Doe", new FormAuthConfig("j_spring_security_check_with_csrf", "j_username", "j_password")).
        when().
                get("/formAuthCsrf").
        then().
                statusCode(200).
                body(equalTo("OK"));
    }

    @Test
    public void formAuthenticationUsingLogging() {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer, StandardCharsets.UTF_8), true);

        given().
                auth().form("John", "Doe", FormAuthConfig.springSecurity().withLoggingEnabled(new LogConfig(captor, true))).
        when().
                get("/formAuth").
        then().
                statusCode(200).
                body(equalTo("OK"));

        assertThat(writer.toString(), equalTo(String.format("Request method:\tPOST%n" +
                        "Request URI:\thttp://localhost:8080/j_spring_security_check%n" +
                        "Proxy:\t\t\t<none>%n" +
                        "Request params:\t<none>%n" +
                        "Query params:\t<none>%n" +
                        "Form params:\tj_username=John%n" +
                        "\t\t\t\tj_password=Doe%n" +
                        "Path params:\t<none>%n" +
                        "Headers:\t\tAccept=*/*%n" +
                        "\t\t\t\tContent-Type=application/x-www-form-urlencoded; charset=%s%n" +
                        "Cookies:\t\t<none>%n" +
                        "Multiparts:\t\t<none>%n" +
                        "Body:\t\t\t<none>%n" +
                        "HTTP/1.1 200 OK%n" +
                        "Content-Type: text/plain;charset=utf-8%n" +
                        "Set-Cookie: jsessionid=1234%n" +
                        "Content-Length: 2%n" +
                        "Server: Jetty(9.4.34.v20201102)%n%nNO%n",
                RestAssured.config().getEncoderConfig().defaultContentCharset())));
    }

    @Test
    public void formAuthenticationUsingLoggingWithLogDetailEqualToParams() {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer, StandardCharsets.UTF_8), true);

        given().
                auth().form("John", "Doe", FormAuthConfig.springSecurity().withLoggingEnabled(LogDetail.PARAMS, new LogConfig(captor, true))).
        when().
                get("/formAuth").
        then().
                statusCode(200).
                body(equalTo("OK"));

        assertThat(writer.toString(), equalTo(String.format("Request params:\t<none>%n" +
                "Query params:\t<none>%n" +
                "Form params:\tj_username=John%n" +
                "\t\t\t\tj_password=Doe%n" +
                "Path params:\t<none>%n" +
                "Multiparts:\t\t<none>%n")));
    }

    @Test
    public void formAuthenticationUsingLoggingWithLogDetailEqualToStatus() {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                auth().form("John", "Doe", FormAuthConfig.springSecurity().withLoggingEnabled(LogDetail.STATUS, new LogConfig(captor, true))).
        when().
                get("/formAuth").
        then().
                statusCode(200).
                body(equalTo("OK"));

        assertThat(writer.toString(), equalTo(String.format("HTTP/1.1 200 OK%n")));
    }

    @Test
    public void formAuthenticationUsingSpringAuthConfDefinedInRequestSpec() {
        final RequestSpecification specification = new RequestSpecBuilder().setAuth(form("John", "Doe", FormAuthConfig.springSecurity())).build();

        given().
                spec(specification).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void formAuthenticationWithLoginPageParsing() {
        given().
                auth().form("John", "Doe").
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void sessionFilterRecordsAndProvidesTheSessionId() {
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
    public void reusingSameSessionFilterInDifferentRequestsAppliesTheSessionIdToTheNewRequest() {
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
    public void sessionFilterRecordsAndProvidesTheSessionIdWhenSessionNameIsNotDefault() {
        final SessionFilter sessionFilter = new SessionFilter();

        given().
                config(RestAssuredConfig.newConfig().sessionConfig(sessionConfig().sessionIdName("phpsessionid"))).
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
    public void formAuthenticationUsingDefaultWithLoginPageParsing() {
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
    public void formAuthenticationUsingDefaultWithSpringAuthConf() {
        RestAssured.authentication = form("John", "Doe", FormAuthConfig.springSecurity());

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
    public void canSpecifyPortWhenUsingFormAuth() {
        RestAssured.port = 8091; // Specify an unused port

        try {
            given().
                    auth().form("John", "Doe", FormAuthConfig.springSecurity()).
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
    public void canOverridePreemptiveBasicAuthFromStaticConfiguration() {
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

/*
 * Copyright 2014 the original author or authors.
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
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.LogConfig.logConfig;
import static com.jayway.restassured.config.RestAssuredConfig.config;
import static com.jayway.restassured.filter.log.LogDetail.BODY;
import static com.jayway.restassured.filter.log.LogDetail.HEADERS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LogIfValidationFailsITest extends WithJetty {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Before
    public void setup() throws Exception {
        RestAssured.config = config().logConfig(logConfig().enablePrettyPrinting(false));
    }

    @After
    public void teardown() throws Exception {
        RestAssured.reset();
    }

    @Test
    public void worksForRequestSpecificationsUsingGivenWhenThenSyntax() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        try {
            given().
                    config(RestAssured.config().logConfig(logConfig().defaultStream(captor))).
                    log().ifValidationFails().
                    param("firstName", "John").
                    param("lastName", "Doe").
                    header("Content-type", "application/json").
            when().
                    get("/greet").
            then().
                    statusCode(400);

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo("Request method:\tGET"+LINE_SEPARATOR+"Request path:\thttp://localhost:8080/greet?firstName=John&lastName=Doe"+LINE_SEPARATOR+"Request params:\tfirstName=John"+LINE_SEPARATOR+"\t\t\t\tlastName=Doe"+LINE_SEPARATOR+"Query params:\t<none>"+LINE_SEPARATOR+"Form params:\t<none>"+LINE_SEPARATOR+"Path params:\t<none>"+LINE_SEPARATOR+"Headers:\t\tContent-Type=application/json"+LINE_SEPARATOR+"Cookies:\t\t<none>"+LINE_SEPARATOR+"Body:\t\t\t<none>" + LINE_SEPARATOR));
        }
    }

    @Test
    public void worksForRequestSpecificationsUsingLegacySyntax() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        try {
            given().
                    config(RestAssured.config().logConfig(logConfig().defaultStream(captor))).
                    log().ifValidationFails().
                    param("firstName", "John").
                    param("lastName", "Doe").
                    header("Content-type", "application/json").
            expect().
                    statusCode(400).
            when().
                    get("/greet");

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo("Request method:\tGET"+LINE_SEPARATOR+"Request path:\thttp://localhost:8080/greet?firstName=John&lastName=Doe"+LINE_SEPARATOR+"Request params:\tfirstName=John"+LINE_SEPARATOR+"\t\t\t\tlastName=Doe"+LINE_SEPARATOR+"Query params:\t<none>"+LINE_SEPARATOR+"Form params:\t<none>"+LINE_SEPARATOR+"Path params:\t<none>"+LINE_SEPARATOR+"Headers:\t\tContent-Type=application/json"+LINE_SEPARATOR+"Cookies:\t\t<none>"+LINE_SEPARATOR+"Body:\t\t\t<none>" + LINE_SEPARATOR));
        }
    }

    @Test
    public void doesntLogRequestSpecificationsUsingGivenWhenThenSyntaxWhenValidationSucceeds() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(RestAssured.config().logConfig(logConfig().defaultStream(captor))).
                log().ifValidationFails().
                param("firstName", "John").
                param("lastName", "Doe").
                header("Content-type", "application/json").
        when().
                get("/greet").
        then().
                statusCode(200);

        assertThat(writer.toString(), isEmptyString());
    }

    @Test
    public void doesntLogRequestSpecificationsUsingLegacySyntaxWhenValidationSucceeds() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(RestAssured.config().logConfig(logConfig().defaultStream(captor))).
                log().ifValidationFails().
                param("firstName", "John").
                param("lastName", "Doe").
                header("Content-type", "application/json").
        expect().
                statusCode(200).
        when().
                get("/greet");

        assertThat(writer.toString(), isEmptyString());
    }

    @Test
    public void worksForResponseSpecificationsUsingGivenWhenThenSyntax() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        try {
            given().
                    config(config().logConfig(new LogConfig(captor, false))).
                    pathParam("firstName", "John").
                    pathParam("lastName", "Doe").
            when().
                    get("/{firstName}/{lastName}").
            then().
                    log().ifValidationFails(BODY).
                    body("fullName", equalTo("John Doe2"));

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"fullName\":\"John Doe\"}" + LINE_SEPARATOR));
        }
    }

    @Test
    public void worksForResponseSpecificationsUsingLegacySyntax() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        try {
            given().
                    config(config().logConfig(new LogConfig(captor, false))).
                    pathParam("firstName", "John").
                    pathParam("lastName", "Doe").
            expect().
                    log().ifValidationFails(BODY).
                    body("fullName", equalTo("John Doe2")).
            when().
                    get("/{firstName}/{lastName}");

            fail("Should throw AssertionError");
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"fullName\":\"John Doe\"}" + LINE_SEPARATOR));
        }
    }

    @Test
    public void doesntLogUsingGivenWhenThenSyntaxWhenValidationSucceeds() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, false))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        when().
                get("/{firstName}/{lastName}").
        then().
                log().ifValidationFails(BODY).
                body("fullName", equalTo("John Doe"));
        assertThat(writer.toString(), isEmptyString());
    }

    @Test
    public void doesntLogResponseSpecUsingLegacySyntaxWhenValidationSucceeds() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, false))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().ifValidationFails(BODY).
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");

        assertThat(writer.toString(), isEmptyString());
    }

    @Test
    public void configuringLogConfigToEnableLoggingOfRequestAndResponseIfValidationFailsWorksAsExpected() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        try {
            given().
                    config(RestAssured.config().logConfig(logConfig().defaultStream(captor).and().enableLoggingOfRequestAndResponseIfValidationFails())).
                    param("firstName", "John").
                    param("lastName", "Doe").
                    header("Content-type", "application/json").
            when().
                    get("/greet").
            then().
                    statusCode(400);

              fail("Should throw AssertionError");
          } catch (AssertionError e) {
              assertThat(writer.toString(), equalTo("Request method:\tGET"+LINE_SEPARATOR+"Request path:\thttp://localhost:8080/greet?firstName=John&lastName=Doe"+LINE_SEPARATOR+"Request params:\tfirstName=John"+LINE_SEPARATOR+"\t\t\t\tlastName=Doe"+LINE_SEPARATOR+"Query params:\t<none>"+LINE_SEPARATOR+"Form params:\t<none>"+LINE_SEPARATOR+"Path params:\t<none>"+LINE_SEPARATOR+"Headers:\t\tContent-Type=application/json"+LINE_SEPARATOR+"Cookies:\t\t<none>"+LINE_SEPARATOR+"Body:\t\t\t<none>" + LINE_SEPARATOR+
                      "HTTP/1.1 200 OK"+LINE_SEPARATOR+"Content-Type: application/json; charset=UTF-8"+LINE_SEPARATOR+"Content-Length: 33"+LINE_SEPARATOR+"Server: Jetty(6.1.14)"+LINE_SEPARATOR+""+LINE_SEPARATOR+"{"+LINE_SEPARATOR+"    \"greeting\": \"Greetings John Doe\""+LINE_SEPARATOR+"}"+LINE_SEPARATOR+""));
          }
    }

    @Test
    public void configuringLogConfigToEnableLoggingOfRequestAndResponseIfValidationFailsWorksAsExpectedWhenSpecifyingLogDetail() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        try {
            given().
                    config(RestAssured.config().logConfig(logConfig().defaultStream(captor).and().enableLoggingOfRequestAndResponseIfValidationFails(HEADERS))).
                    param("firstName", "John").
                    param("lastName", "Doe").
                    header("Content-type", "application/json").
            when().
                    get("/greet").
            then().
                    statusCode(400);

              fail("Should throw AssertionError");
          } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo("Headers:\t\tContent-Type=application/json"+LINE_SEPARATOR+
                    "Content-Type: application/json; charset=UTF-8"+LINE_SEPARATOR+"Content-Length: 33"+LINE_SEPARATOR+"Server: Jetty(6.1.14)"+LINE_SEPARATOR));
          }
    }
}
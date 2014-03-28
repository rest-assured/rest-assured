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
import static com.jayway.restassured.filter.log.ErrorLoggingFilter.logErrorsTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo("Request method:\tGET\nRequest path:\thttp://localhost:8080/greet?firstName=John&lastName=Doe\nRequest params:\tfirstName=John\n\t\t\t\tlastName=Doe\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=application/json\nCookies:\t\t<none>\nBody:\t\t\t<none>" + LINE_SEPARATOR));
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
        } catch (AssertionError e) {
            assertThat(writer.toString(), equalTo("Request method:\tGET\nRequest path:\thttp://localhost:8080/greet?firstName=John&lastName=Doe\nRequest params:\tfirstName=John\n\t\t\t\tlastName=Doe\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=application/json\nCookies:\t\t<none>\nBody:\t\t\t<none>" + LINE_SEPARATOR));
        }
    }
}
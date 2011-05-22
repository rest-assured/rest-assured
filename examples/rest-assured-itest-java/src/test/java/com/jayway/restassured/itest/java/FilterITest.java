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
import com.jayway.restassured.internal.filter.FormAuthFilter;
import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.filter.log.ErrorLoggingFilter.logErrorsTo;
import static com.jayway.restassured.filter.log.ResponseLoggingFilter.logResponseTo;
import static com.jayway.restassured.filter.log.ResponseLoggingFilter.logResponseToIfMatches;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FilterITest extends WithJetty {

    @Test
    public void filterWorks() throws Exception {
        final FormAuthFilter filter = new FormAuthFilter();
        filter.setUserName("John");
        filter.setPassword("Doe");

        given().
                filter(filter).
                expect().
                statusCode(200).
                body(equalTo("OK")).
                when().
                get("/formAuth");
    }

    @Test
    public void errorLoggingFilterWorks() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logErrorsTo(captor)).and().expect().body(equalTo("ERROR")).when().get("/409");
        assertThat(writer.toString(), containsString("ERROR"));
    }

    @Test
    public void loggingFilterLogsErrors() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseTo(captor)).and().expect().body(equalTo("ERROR")).when().get("/409");
        assertThat(writer.toString(), containsString("ERROR"));
    }

    @Test
    public void loggingFilterLogsNonErrors() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseTo(captor)).expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
        assertThat(writer.toString(), containsString("{\"greeting\": \"Greetings John Doe\"}"));
    }

    @Test
    public void loggingFilterLogsToSpecifiedWriterWhenMatcherIsFulfilled() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseToIfMatches(captor, equalTo(200))).expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
        assertThat(writer.toString(), containsString("{\"greeting\": \"Greetings John Doe\"}"));
    }

    @Test
    public void loggingFilterDoesntLogWhenSpecifiedMatcherIsNotFulfilled() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseToIfMatches(captor, equalTo(400))).expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
        assertThat(writer.toString(), is(""));
    }

    @Test
    public void supportsSpecifyingDefaultFilters() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.filters(asList(logErrorsTo(captor), logResponseTo(captor)));
        try {
            expect().body(equalTo("ERROR")).when().get("/409");
        }  finally {
            RestAssured.reset();
        }
        assertThat(writer.toString(), is("ERROR\nERROR\n"));
    }
}

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
import com.jayway.restassured.builder.ResponseBuilder;
import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.itest.java.objects.Greeting;
import com.jayway.restassured.itest.java.objects.Message;
import com.jayway.restassured.itest.java.objects.ScalatraObject;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.LogConfig.logConfig;
import static com.jayway.restassured.config.RestAssuredConfig.config;
import static com.jayway.restassured.filter.log.ErrorLoggingFilter.logErrorsTo;
import static com.jayway.restassured.filter.log.LogDetail.COOKIES;
import static com.jayway.restassured.filter.log.ResponseLoggingFilter.logResponseTo;
import static com.jayway.restassured.filter.log.ResponseLoggingFilter.logResponseToIfMatches;
import static com.jayway.restassured.parsing.Parser.JSON;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LoggingITest extends WithJetty {

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
    public void errorLoggingFilterWorks() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logErrorsTo(captor)).and().expect().body(equalTo("ERROR")).when().get("/409");
        assertThat(writer.toString(), containsString("ERROR"));
    }

    @Test
    public void logErrorsUsingRequestSpec() throws Exception {
        expect().log().ifError().body(equalTo("ERROR")).when().get("/409");
    }

    @Test
    public void logUsingRequestSpec() throws Exception {
        given().log().everything().and().expect().body(equalTo("ERROR")).when().get("/409");
    }

    @Test
    public void logUsingResponseSpec() throws Exception {
        expect().log().everything().body(equalTo("ERROR")).when().get("/409");
    }

    @Test
    public void logResponseThatHasCookiesWithLogDetailAll() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseTo(captor)).and().expect().body(equalTo("OK")).when().get("/multiCookie");
        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\nContent-Type: text/plain; charset=utf-8\nExpires: Thu, 01 Jan 1970 00:00:00 GMT\nSet-Cookie: cookie1=cookieValue1;Domain=localhost\nSet-Cookie: cookie1=cookieValue2;Version=1;Comment=\"My Purpose\";Path=/;Domain=localhost;Max-Age=1234567;Secure\nContent-Length: 2\nServer: Jetty(6.1.14)\n\nOK" + LINE_SEPARATOR));
    }

    @Test
    public void logResponseThatHasCookiesWithLogDetailCookies() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseTo(captor, COOKIES)).and().expect().body(equalTo("OK")).when().get("/multiCookie");
        assertThat(writer.toString(), equalTo("cookie1=cookieValue1;Domain=localhost\ncookie1=cookieValue2;Comment=\"My Purpose\";Path=/;Domain=localhost;Max-Age=1234567;Secure;Version=1" + LINE_SEPARATOR));
    }

    @Test
    public void loggingResponseFilterLogsErrors() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseTo(captor)).and().expect().body(equalTo("ERROR")).when().get("/409");
        assertThat(writer.toString(), containsString("ERROR"));
    }

    @Test
    public void loggingResponseFilterLogsNonErrors() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseTo(captor)).expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
        assertThat(writer.toString(), containsString("{\"greeting\":\"Greetings John Doe\"}"));
    }

    @Test
    public void loggingResponseFilterLogsToSpecifiedWriterWhenMatcherIsFulfilled() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseToIfMatches(captor, equalTo(200))).expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
        assertThat(writer.toString(), containsString("{\"greeting\":\"Greetings John Doe\"}"));
    }

    @Test
    public void loggingResponseFilterDoesntLogWhenSpecifiedMatcherIsNotFulfilled() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().filter(logResponseToIfMatches(captor, equalTo(400))).expect().body("greeting", equalTo("Greetings John Doe")).when().get("/greet?firstName=John&lastName=Doe");
        assertThat(writer.toString(), is(""));
    }

    @Test
    public void loggingResponseFilterLogsWhenExpectationsFail() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        try {
            given().filter(logResponseTo(captor)).expect().body("greeting", equalTo("Greetings John Do")).when().get("/greet?firstName=John&lastName=Doe");
            fail("Should throw exception");
        } catch (AssertionError e) {
            assertThat(writer.toString(), containsString("{\"greeting\":\"Greetings John Doe\"}"));
        }
    }

    @Test
    public void loggingRequestFilterWithParamsCookiesAndHeaders() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().
                filter(new RequestLoggingFilter(captor)).
                formParam("firstName", "John").
                formParam("lastName", "Doe").
                queryParam("something1", "else1").
                queryParam("something2", "else2").
                queryParam("something3", "else3").
                param("hello1", "world1").
                param("hello2", "world2").
                param("multiParam", "multi1", "multi2").
                cookie("multiCookie", "value1", "value2").
                cookie("standardCookie", "standard value").
                header("multiHeader", "headerValue1", "headerValue2").
                header("standardHeader", "standard header value").
        expect().
                body("greeting", equalTo("Greetings John Doe")).
        when().
                post("/greet");

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/greet?something1=else1&something2=else2&something3=else3\nRequest params:\thello1=world1\n\t\t\t\thello2=world2\n\t\t\t\tmultiParam=[multi1, multi2]\nQuery params:\tsomething1=else1\n\t\t\t\tsomething2=else2\n\t\t\t\tsomething3=else3\nForm params:\tfirstName=John\n\t\t\t\tlastName=Doe\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\n\t\t\t\tmultiHeader=headerValue1\n\t\t\t\tmultiHeader=headerValue2\n\t\t\t\tstandardHeader=standard header value\nCookies:\t\tmultiCookie=value1\n\t\t\t\tmultiCookie=value2\n\t\t\t\tstandardCookie=standard value\nBody:\t\t\t<none>" + LINE_SEPARATOR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void loggingRequestFilterDoesntAcceptStatusAsLogDetail() throws Exception {
        new RequestLoggingFilter(LogDetail.STATUS);
    }

    @Test
    public void loggingRequestFilterWithExplicitContentType() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().
                filter(new RequestLoggingFilter(captor)).
                param("firstName", "John").
                param("lastName", "Doe").
                header("Content-type", "application/json").
        expect().
                body("greeting", equalTo("Greetings John Doe")).
        when().
                get("/greet");

        assertThat(writer.toString(), equalTo("Request method:\tGET\nRequest path:\thttp://localhost:8080/greet?firstName=John&lastName=Doe\nRequest params:\tfirstName=John\n\t\t\t\tlastName=Doe\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-type=application/json\nCookies:\t\t<none>\nBody:\t\t\t<none>" + LINE_SEPARATOR));
    }

    @Test
    public void loggingRequestFilterPathParams() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                filter(new RequestLoggingFilter(captor)).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");

        assertThat(writer.toString(), equalTo("Request method:\tGET\nRequest path:\thttp://localhost:8080/John/Doe\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\tfirstName=John\n\t\t\t\tlastName=Doe\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\t\t\t<none>" + LINE_SEPARATOR));
    }

    @Test
    public void loggingRequestFilterWithBody() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        final ScalatraObject object = new ScalatraObject();
        object.setHello("Hello world");
        given().filter(new RequestLoggingFilter(captor)).expect().defaultParser(JSON).given().body(object).when().post("/reflect");

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/reflect\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\n{\"hello\":\"Hello world\"}" + LINE_SEPARATOR));
    }

    @Test
    public void loggingRequestAndResponseAtTheSameTimeWhenRequestFilterIsAddedBeforeResponseFilter() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        final ScalatraObject object = new ScalatraObject();
        object.setHello("Hello world");
        given().
                filters(new RequestLoggingFilter(captor), new ResponseLoggingFilter(captor)).
                body(object).
        expect().
                defaultParser(JSON).
        when().
                post("/reflect");

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/reflect\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\n{\"hello\":\"Hello world\"}" + LINE_SEPARATOR + "HTTP/1.1 200 OK\nContent-Type: text/plain; charset=iso-8859-1\nContent-Length: 23\nServer: Jetty(6.1.14)\n\n{\"hello\":\"Hello world\"}" + LINE_SEPARATOR));
    }

    @Test
    public void loggingRequestAndResponseAtTheSameTimeWhenResponseFilterIsAddedBeforeRequestFilter() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        final ScalatraObject object = new ScalatraObject();
        object.setHello("Hello world");
        given().
                filters(new ResponseLoggingFilter(captor), new RequestLoggingFilter(captor)).
                body(object).
        expect().
                defaultParser(JSON).
        when().
                post("/reflect");

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/reflect\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\n{\"hello\":\"Hello world\"}" + LINE_SEPARATOR + "HTTP/1.1 200 OK\nContent-Type: text/plain; charset=iso-8859-1\nContent-Length: 23\nServer: Jetty(6.1.14)\n\n{\"hello\":\"Hello world\"}" + LINE_SEPARATOR));
    }

    @Test
    public void logEverythingResponseUsingLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(logConfig().defaultStream(captor).and().enablePrettyPrinting(false))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().all().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");

        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\nContent-Type: application/json; charset=UTF-8\nContent-Length: 59\nServer: Jetty(6.1.14)\n\n{\"firstName\":\"John\",\"lastName\":\"Doe\",\"fullName\":\"John Doe\"}" + LINE_SEPARATOR));
    }

    @Test
    public void logIfStatusCodeIsEqualToResponseUsingLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(logConfig().defaultStream(captor))).
                expect().
                log().ifStatusCodeIsEqualTo(409).
                when().
                get("/409");

        assertThat(writer.toString(), equalTo("HTTP/1.1 409 Conflict\nContent-Type: text/plain; charset=utf-8\nContent-Length: 5\nServer: Jetty(6.1.14)\n\nERROR" + LINE_SEPARATOR));
    }

    @Test
    public void doesntLogIfStatusCodeIsNotEqualToResponseUsingLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(logConfig().defaultStream(captor))).
                expect().
                log().ifStatusCodeIsEqualTo(200).
                when().
                get("/409");

        assertThat(writer.toString(), equalTo(""));
    }

    @Test
    public void logIfStatusCodeMatchesResponseUsingLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(logConfig().defaultStream(captor))).
        expect().
                log().ifStatusCodeMatches(greaterThan(200)).
        when().
                get("/409");

        assertThat(writer.toString(), equalTo("HTTP/1.1 409 Conflict\nContent-Type: text/plain; charset=utf-8\nContent-Length: 5\nServer: Jetty(6.1.14)\n\nERROR" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyBodyUsingResponseUsingLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, false))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().body().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");


        assertThat(writer.toString(), equalTo("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"fullName\":\"John Doe\"}" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyResponseBodyWithPrettyPrintingWhenJson() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().body().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");


        assertThat(writer.toString(), equalTo("{\n    \"firstName\": \"John\",\n    \"lastName\": \"Doe\",\n    \"fullName\": \"John Doe\"\n}" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyResponseBodyWithPrettyPrintingWhenXml() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
        expect().
                log().body().
                body("videos.music[0].title", is("Video Title 1")).
        when().
                get("/videos-not-formatted");

        assertThat(writer.toString(), equalTo("<videos>\n  <music>\n    <title>\n      Video Title 1\n    </title>\n    <artist>\n      Artist 1\n    </artist>\n  </music>\n  <music>\n    <title>\n      Video Title 2\n    </title>\n    <artist>\n      Artist 2\n    </artist>\n    <artist>\n      Artist 3\n    </artist>\n  </music>\n</videos>" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyResponseBodyWithPrettyPrintingWhenHtml() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
        expect().
                log().body().
                body("html.head.title", is("my title")).
        when().
                get("/textHTML-not-formatted");

        assertThat(writer.toString(), equalTo("<html>\n  <head>\n    <title>\n      my title\n    </title>\n  </head>\n  <body>\n    <p>\n      paragraph 1\n    </p>\n    <p>\n      paragraph 2\n    </p>\n  </body>\n</html>" + LINE_SEPARATOR));
    }

    @Test
    public void logAllWithPrettyPrintingWhenJson() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().all().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");


        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\nContent-Type: application/json; charset=UTF-8\nContent-Length: 59\nServer: Jetty(6.1.14)\n\n{\n    \"firstName\": \"John\",\n    \"lastName\": \"Doe\",\n    \"fullName\": \"John Doe\"\n}" + LINE_SEPARATOR));
    }

    @Test
    public void logAllWithPrettyPrintingUsingDSLWhenJson() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, false))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().all(true).
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");


        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\nContent-Type: application/json; charset=UTF-8\nContent-Length: 59\nServer: Jetty(6.1.14)\n\n{\n    \"firstName\": \"John\",\n    \"lastName\": \"Doe\",\n    \"fullName\": \"John Doe\"\n}" + LINE_SEPARATOR));
    }

    @Test
    public void logAllWithNoPrettyPrintingUsingDSLWhenJson() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().all(false).
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");


        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\nContent-Type: application/json; charset=UTF-8\nContent-Length: 59\nServer: Jetty(6.1.14)\n\n{\"firstName\":\"John\",\"lastName\":\"Doe\",\"fullName\":\"John Doe\"}" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyResponseBodyWithPrettyPrintingUsingDSLWhenXml() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, false))).
        expect().
                log().body(true).
                body("videos.music[0].title", is("Video Title 1")).
        when().
                get("/videos-not-formatted");

        assertThat(writer.toString(), equalTo("<videos>\n  <music>\n    <title>\n      Video Title 1\n    </title>\n    <artist>\n      Artist 1\n    </artist>\n  </music>\n  <music>\n    <title>\n      Video Title 2\n    </title>\n    <artist>\n      Artist 2\n    </artist>\n    <artist>\n      Artist 3\n    </artist>\n  </music>\n</videos>" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyResponseBodyWithNoPrettyPrintingUsingDSLWhenXml() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
        expect().
                log().body(false).
                body("videos.music[0].title", is("Video Title 1")).
        when().
                get("/videos-not-formatted");

        assertThat(writer.toString(), equalTo("<videos><music><title>Video Title 1</title><artist>Artist 1</artist></music><music><title>Video Title 2</title><artist>Artist 2</artist><artist>Artist 3</artist></music></videos>" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyStatusUsingResponseUsingLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().status().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");


        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyHeadersUsingResponseUsingLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        expect().
                log().headers().
                body("fullName", equalTo("John Doe")).
        when().
                get("/{firstName}/{lastName}");


        assertThat(writer.toString(), equalTo("Content-Type: application/json; charset=UTF-8\nContent-Length: 59\nServer: Jetty(6.1.14)" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyHeadersUsingResponseUsingLogSpecWhenMultiHeaders() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
        expect().
                log().headers().
        when().
                get("/multiValueHeader");


        assertThat(writer.toString(), equalTo("Content-Type: text/plain; charset=utf-8\nMultiHeader: Value 1\nMultiHeader: Value 2\nContent-Length: 0\nServer: Jetty(6.1.14)" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyCookiesUsingResponseLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
        expect().
                log().cookies().
        when().
                get("/multiCookie");

        assertThat(writer.toString(), equalTo("cookie1=cookieValue1;Domain=localhost\ncookie1=cookieValue2;Comment=\"My Purpose\";Path=/;Domain=localhost;Max-Age=1234567;Secure;Version=1" + LINE_SEPARATOR));
    }

    @Test
    public void logBodyPrettyPrintedUsingResponseLogSpecWhenContentTypeDoesntMatchContent() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
        expect().
                log().everything().
                body(equalTo("This is not a valid JSON document")).
        when().
                get("/contentTypeJsonButContentIsNotJson");

        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\nContent-Type: application/json; charset=UTF-8\nContent-Length: 33\nServer: Jetty(6.1.14)\n\nThis is not a valid JSON document" + LINE_SEPARATOR));
    }

    @Test
    public void logAllUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().everything().
                param("firstName", "John").
                queryParam("lastName", "Doe").
        when().
                get("/greetJSON");

        assertThat(writer.toString(), equalTo("Request method:\tGET\nRequest path:\thttp://localhost:8080/greetJSON?firstName=John&lastName=Doe\nRequest params:\tfirstName=John\nQuery params:\tlastName=Doe\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\t\t\t<none>" + LINE_SEPARATOR));
    }

    @Test
    public void logParamsUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().parameters().
                param("firstName", "John").
                queryParam("lastName", "Doe").
        when().
                get("/greetJSON");

        assertThat(writer.toString(), equalTo("Request params:\tfirstName=John\nQuery params:\tlastName=Doe\nForm params:\t<none>\nPath params:\t<none>" + LINE_SEPARATOR));
    }


   @Test
    public void logNoValueParamsUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().parameters().
                formParam("formParam").
                queryParam("queryParam").
        when().
                post("/noValueParam");

        assertThat(writer.toString(), equalTo("Request params:\t<none>\nQuery params:\tqueryParam\nForm params:\tformParam\nPath params:\t<none>" + LINE_SEPARATOR));
    }

    @Test
    public void logBodyUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().body().
                param("firstName", "John").
                queryParam("lastName", "Doe").
        when().
                get("/greetJSON");

        assertThat(writer.toString(), equalTo("Body:\t\t\t<none>" + LINE_SEPARATOR));
    }

    @Test
    public void logBodyWithPrettyPrintingUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                contentType(ContentType.JSON).
                config(config().logConfig(new LogConfig(captor, true))).
                log().body().
                body("{ \"something\" : \"else\" }").
        when().
                post("/body");

        assertThat(writer.toString(), equalTo("Body:\n{\n    \"something\": \"else\"\n}" + LINE_SEPARATOR));
    }

    @Test
    public void logBodyWithPrettyPrintingUsingDslAndRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                contentType(ContentType.JSON).
                config(config().logConfig(new LogConfig(captor, false))).
                log().body(true).
                body("{ \"something\" : \"else\" }").
        when().
                post("/body");

        assertThat(writer.toString(), equalTo("Body:\n{\n    \"something\": \"else\"\n}" + LINE_SEPARATOR));
    }

    @Test
    public void logBodyWithPrettyPrintingUsingRequestLogSpecAndObjectMapping() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        final Message message = new Message();
        message.setMessage("My message");
        given().
                contentType(ContentType.JSON).
                config(config().logConfig(new LogConfig(captor, true))).
                log().body().
                body(message).
        when().
                post("/body");

        assertThat(writer.toString(), equalTo("Body:\n{\n    \"message\": \"My message\"\n}" + LINE_SEPARATOR));
    }

    @Test
    public void logBodyWithPrettyPrintingUsingRequestLogSpecAndObjectMappingWhenXML() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        final Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");
        given().
                contentType(ContentType.XML).
                config(config().logConfig(new LogConfig(captor, true))).
                log().body().
                body(greeting).
        when().
                post("/body");

        assertThat(writer.toString(), equalTo("Body:\n<greeting>\n  <firstName>\n    John\n  </firstName>\n  <lastName>\n    Doe\n  </lastName>\n</greeting>" + LINE_SEPARATOR));
    }

    @Test
    public void logCookiesUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().cookies().
                cookie("myCookie1", "myCookieValue1").
                cookie("myCookie2", "myCookieValue2").
                cookie("myMultiCookie", "myMultiCookieValue1", "myMultiCookieValue2").
        when().
                post("/reflect");

        assertThat(writer.toString(), equalTo("Cookies:\t\tmyCookie1=myCookieValue1\n\t\t\t\tmyCookie2=myCookieValue2\n\t\t\t\tmyMultiCookie=myMultiCookieValue1\n\t\t\t\tmyMultiCookie=myMultiCookieValue2" + LINE_SEPARATOR));
    }

    @Test
    public void logHeadersUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().headers().
                header("myHeader1", "myHeaderValue1").
                header("myHeader2", "myHeaderValue2").
                header("myMultiHeader", "myMultiHeaderValue1", "myMultiHeaderValue2").
        when().
                get("/multiHeaderReflect");

        assertThat(writer.toString(), equalTo("Headers:\t\tContent-Type=*/*\n\t\t\t\tmyHeader1=myHeaderValue1\n\t\t\t\tmyHeader2=myHeaderValue2\n\t\t\t\tmyMultiHeader=myMultiHeaderValue1\n\t\t\t\tmyMultiHeader=myMultiHeaderValue2" + LINE_SEPARATOR));
    }

    @Test
    public void logBodyPrettyPrintedUsingRequestLogSpecWhenContentTypeDoesntMatchContent() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().everything().
                contentType("application/json").
                body("This is not JSON").
        when().
                post("/reflect");

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/reflect\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=application/json\nCookies:\t\t<none>\nBody:\nThis is not JSON" + LINE_SEPARATOR));
    }

    @Test
    public void logAllWhenBasePathIsDefinedUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.basePath = "/reflect";

        try {
        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().all().
                body("hello").
        when().
                post("/");
        } finally {
            RestAssured.reset();
        }

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/reflect\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\nhello" + LINE_SEPARATOR));
    }

    @Test
    public void logAllWhenBaseURIIsDefinedUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.baseURI = "http://localhost:8080/reflect";

        try {
        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().all().
                body("hello").
        when().
                post("/");
        } finally {
            RestAssured.reset();
        }

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/reflect\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\nhello" + LINE_SEPARATOR));
    }

    @Test
    public void logAllWhenBasePathAndBasePortAndBaseURIIsDefinedUsingRequestLogSpec() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        RestAssured.basePath = "/reflect";

        try {
        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().all().
                body("hello").
        when().
                post("/");
        } finally {
            RestAssured.reset();
        }

        assertThat(writer.toString(), equalTo("Request method:\tPOST\nRequest path:\thttp://localhost:8080/reflect\nRequest params:\t<none>\nQuery params:\t<none>\nForm params:\t<none>\nPath params:\t<none>\nHeaders:\t\tContent-Type=*/*\nCookies:\t\t<none>\nBody:\nhello" + LINE_SEPARATOR));
    }

    @Test
    public void logsFullyQualifiedUrlsAreLoggedCorrectly() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                log().all().
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        return new ResponseBuilder().setStatusCode(200).setBody("changed").build();
                    }
                }).get("http://www.beijingchina.net.cn/transportation/train/train-to-shanghai.html");

        assertThat(writer.toString(), startsWith("Request method:\tGET\nRequest path:\thttp://www.beijingchina.net.cn/transportation/train/train-to-shanghai.html"));
    }

    @Test
    public void logsXmlNamespacesCorrectly() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                filter(logResponseTo(captor)).
        expect().
                statusCode(200).
        when().
                get("/namespace-example");

        assertThat(writer.toString(), containsString("foo xmlns:ns=\"http://localhost/\">\n      <bar>sudo </bar>\n      <ns:bar>make me a sandwich!</ns:bar>\n    </foo>"));
    }
}
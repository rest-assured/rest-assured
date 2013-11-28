package com.jayway.restassured.itest.java;

import com.jayway.restassured.config.LogConfig;
import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.Test;

import java.io.PrintStream;
import java.io.StringWriter;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.LogConfig.logConfig;
import static com.jayway.restassured.config.RestAssuredConfig.config;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class GivenWhenThenLoggingITest extends WithJetty {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Test
    public void logsEverythingResponseUsingGivenWhenThenSyntax() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(logConfig().defaultStream(captor).and().enablePrettyPrinting(false))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        when().
                get("/{firstName}/{lastName}").
        then().
                log().all().
                body("fullName", equalTo("John Doe"));

        assertThat(writer.toString(), equalTo("HTTP/1.1 200 OK\nContent-Type: application/json; charset=UTF-8\nContent-Length: 59\nServer: Jetty(6.1.14)\n\n{\"firstName\":\"John\",\"lastName\":\"Doe\",\"fullName\":\"John Doe\"}" + LINE_SEPARATOR));
    }

    @Test
    public void logResponseThatHasCookiesWithLogDetailCookiesUsingGivenWhenThenSyntax() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        given().
                config(config().logConfig(logConfig().defaultStream(captor).and().enablePrettyPrinting(false))).
        when().
                get("/multiCookie").
        then().
                log().cookies().
                body(equalTo("OK"));
        assertThat(writer.toString(), equalTo("cookie1=cookieValue1;Domain=localhost\ncookie1=cookieValue2;Comment=\"My Purpose\";Path=/;Domain=localhost;Max-Age=1234567;Secure;Version=1" + LINE_SEPARATOR));
    }

    @Test
    public void logOnlyHeadersUsingResponseUsingLogSpecWithGivenWhenThenSyntax() throws Exception {
        final StringWriter writer = new StringWriter();
        final PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);

        given().
                config(config().logConfig(new LogConfig(captor, true))).
                pathParam("firstName", "John").
                pathParam("lastName", "Doe").
        when().
                get("/{firstName}/{lastName}").
        then().
                log().headers().
                body("fullName", equalTo("John Doe"));


        assertThat(writer.toString(), equalTo("Content-Type: application/json; charset=UTF-8\nContent-Length: 59\nServer: Jetty(6.1.14)" + LINE_SEPARATOR));
    }
}

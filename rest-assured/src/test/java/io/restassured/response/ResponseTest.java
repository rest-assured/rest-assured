package io.restassured.response;

import org.junit.Assert;
import org.junit.Test;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class ResponseTest {
    @Test
    public void prettyPrintHeadersPrintsExpectedValue() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        given().
                header("name", "value").
                get("https://httpbin.org/headers").
                prettyPrintHeaders();
        System.out.flush();
        System.setOut(old);
        // I assumed there are always the Date and Content-Type headers.
        Assert.assertTrue(Pattern.matches("^Headers:\\t\\t.+=.+\\R(?:\\t\\t\\t\\t.+=.+\\R)+\\R\\R$", baos.toString()));
    }

    @Test
    public void prettyPrintHeadersReturnsExpectedValue() {
        String headersString = given().
                header("name", "value").
                get("https://httpbin.org/headers").
                prettyPrintHeaders();
        // I assumed there are always the Date and Content-Type headers.
        Assert.assertTrue(Pattern.matches("^Headers:\\t\\t.+=.+\\n(?:\\t\\t\\t\\t.+=.+\\n)+$", headersString));
    }
}

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.parsing.Parser.JSON;
import static org.hamcrest.Matchers.equalTo;

public class GivenWhenThenDefaultParserITest extends WithJetty {

    @Test public void
    statically_defined_default_parser_works_for_given_when_then_statements() {
        RestAssured.defaultParser = JSON;
        try {
            get("/noContentTypeJsonCompatible").then().body("message", equalTo("It works"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    non_statically_defined_default_parser_works_for_given_when_then_statements() {
        get("/noContentTypeJsonCompatible").then().using().defaultParser(JSON).assertThat().body("message", equalTo("It works"));
    }
}

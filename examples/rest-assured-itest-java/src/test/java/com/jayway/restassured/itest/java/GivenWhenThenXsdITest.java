package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.matcher.RestAssuredMatchers.matchesXsd;
import static org.hamcrest.Matchers.equalTo;

public class GivenWhenThenXsdITest extends WithJetty {

    @Test
    public void validatesXsdString() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd");
        final String xsd = IOUtils.toString(inputstream);

        get("/carRecords").then().body(matchesXsd(xsd));
    }

    @Test
    public void validatesXsdInputStream() throws Exception {
        InputStream inputstream = Thread.currentThread().getContextClassLoader().getResourceAsStream("car-records.xsd");

        get("/carRecords").then().body(matchesXsd(inputstream));
    }

    @Test
    public void validatesXsdStringAndPath() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd");
        final String xsd = IOUtils.toString(inputstream);

        get("/carRecords").then().body(matchesXsd(xsd)).and().body("records.car.find { it.@name == 'HSV Maloo' }.@year", equalTo("2006"));
    }
}
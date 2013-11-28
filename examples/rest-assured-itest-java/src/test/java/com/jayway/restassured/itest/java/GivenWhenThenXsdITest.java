package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.matcher.RestAssuredMatchers.matchesXsd;

public class GivenWhenThenXsdITest extends WithJetty {

    @Test
    public void validatesXsdString() throws Exception {
        final InputStream inputstream = getClass().getResourceAsStream("/car-records.xsd");
        final String xsd = IOUtils.toString(inputstream);

        get("/carRecords").then().body(matchesXsd(xsd));
    }
}
package com.jayway.restassured.itest.java.support;

import org.apache.commons.lang3.StringUtils;

import java.io.StringWriter;

public class RequestPathFromLogExtractor {

    public static String loggedRequestPathIn(StringWriter writer) {
        return StringUtils.substringBetween(writer.toString(), "Request path:", "\n").trim();
    }
}

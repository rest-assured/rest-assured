package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.specification.RequestSender;

public interface MockMvcRequestSpecification {

    /**
     * A slightly shorter version of .
     *
     * @param parameterName   The parameter name
     * @param parameterValues Parameter values, one to many if you want to specify multiple values for the same parameter.
     * @return The request specification
     */
    MockMvcRequestSpecification param(String parameterName, Object... parameterValues);

    RequestSender when();
}

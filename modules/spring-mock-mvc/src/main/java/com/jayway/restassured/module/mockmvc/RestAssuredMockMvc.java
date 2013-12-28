package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.config.RestAssuredConfigMockMvc;
import com.jayway.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

public class RestAssuredMockMvc {
    public static MockMvc mockMvc = null;
    public static RestAssuredConfigMockMvc config;
    private static List<ResultHandler> resultHandlers = new ArrayList<ResultHandler>();

    public static MockMvcRequestSpecification given() {
        return new MockMvcRequestSpecificationImpl(mockMvc, config, resultHandlers);
    }

    public static void standaloneSetup(Object... controllers) {
        mockMvc = MockMvcBuilders.standaloneSetup(controllers).build();
    }

    public static void webAppContextSetup(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    public static void resultHandlers(ResultHandler resultHandler, ResultHandler... resultHandlers) {
        notNull(resultHandler, ResultHandler.class);
        RestAssuredMockMvc.resultHandlers.add(resultHandler);
        if (resultHandlers != null && resultHandlers.length >= 1) {
            Collections.addAll(RestAssuredMockMvc.resultHandlers, resultHandlers);
        }
    }

    public static void reset() {
        mockMvc = null;
        config = null;
        resultHandlers.clear();
    }
}

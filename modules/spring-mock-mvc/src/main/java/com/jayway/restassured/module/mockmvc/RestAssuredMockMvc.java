package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class RestAssuredMockMvc {
    public static MockMvc mockMvc = null;
    public static RestAssuredConfig config;

    public static MockMvcRequestSpecification given() {
        return new MockMvcRequestSpecificationImpl(mockMvc, config);
    }

    public static void standaloneSetup(Object... controllers) {
        mockMvc = MockMvcBuilders.standaloneSetup(controllers).build();
    }

    public static void webAppContextSetup(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    public static void reset() {
        mockMvc = null;
        config = null;
    }
}

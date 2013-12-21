package com.jayway.restassured.module.mockmvc;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class RestAssuredMockMvc {
    public static MockMvc mockMvc = null;

    public static MockMvcRequestSpecification given() {
        return new MockMvcRequestSpecificationImpl(mockMvc);
    }

    public static void standaloneSetup(Object... controllers) {
        mockMvc = MockMvcBuilders.standaloneSetup(controllers).build();
    }

    public static void webAppContextSetup(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    public static void reset() {
        mockMvc = null;
    }
}

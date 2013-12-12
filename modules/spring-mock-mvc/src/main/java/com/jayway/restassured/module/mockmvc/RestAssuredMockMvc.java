package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.specification.RequestSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

public class RestAssuredMockMvc implements MockMvcRequestSpecification {

    public static MockMvc mockMvc = null;

    private final MockMvc instanceMockMvc;

    private final MultiValueMap<String, Object> params = new LinkedMultiValueMap<String, Object>();

    private RestAssuredMockMvc(MockMvc mockMvc) {
        this.instanceMockMvc = mockMvc;
    }

    public static RestAssuredMockMvc given() {
        return new RestAssuredMockMvc(mockMvc);
    }

    public MockMvcRequestSpecification mockMvc(MockMvc mockMvc) {
        return new RestAssuredMockMvc(mockMvc);
    }

    public MockMvcRequestSpecification standaloneSetup(Object... controllers) {
        return new RestAssuredMockMvc(MockMvcBuilders.standaloneSetup(controllers).build());
    }

    public MockMvcRequestSpecification webAppContextSetup(WebApplicationContext context) {
        return new RestAssuredMockMvc(MockMvcBuilders.webAppContextSetup(context).build());
    }

    public MockMvcRequestSpecification param(String parameterName, Object... parameterValues) {
        for (Object parameterValue : parameterValues) {
            params.add(parameterName, parameterValue);
        }
        return this;
    }

    public RequestSender when() {
        return new MockMvcRequestSender(instanceMockMvc, params);
    }

    public static void reset() {
        mockMvc = null;
    }
}

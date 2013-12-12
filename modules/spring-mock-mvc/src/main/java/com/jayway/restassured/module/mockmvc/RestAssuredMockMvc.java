package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.http.CharsetExtractor;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.internal.mapping.ObjectMapperSerializationContextImpl;
import com.jayway.restassured.internal.mapping.ObjectMapping;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.specification.RequestSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;
import static com.jayway.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;

public class RestAssuredMockMvc implements MockMvcRequestSpecification {

    public static MockMvc mockMvc = null;

    private final MockMvc instanceMockMvc;

    private final MultiValueMap<String, Object> params = new LinkedMultiValueMap<String, Object>();

    private Object requestBody = null;
    private String requestContentType;

    private RestAssuredConfig restAssuredConfig;

    private RestAssuredMockMvc(MockMvc mockMvc) {
        this.instanceMockMvc = mockMvc;
        restAssuredConfig = new RestAssuredConfig();
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

    public MockMvcRequestSpecification contentType(ContentType contentType) {
        notNull(contentType, "contentType");
        this.requestContentType = contentType.toString();
        return this;
    }

    public MockMvcRequestSpecification contentType(String contentType) {
        notNull(contentType, "contentType");
        this.requestContentType = contentType;
        return this;
    }

    public MockMvcRequestSpecification param(String parameterName, Object... parameterValues) {
        for (Object parameterValue : parameterValues) {
            params.add(parameterName, parameterValue);
        }
        return this;
    }

    public MockMvcRequestSpecification body(String body) {
        this.requestBody = body;
        return this;
    }

    public MockMvcRequestSpecification body(byte[] body) {
        this.requestBody = body;
        return this;
    }

    public MockMvcRequestSpecification body(Object object) {
        notNull(object, "object");
        if (!isSerializableCandidate(object)) {
            return body(object.toString());
        }

        this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), null, restAssuredConfig.getObjectMapperConfig());
        return this;
    }

    public MockMvcRequestSpecification body(Object object, ObjectMapper mapper) {
        notNull(object, "object");
        notNull(mapper, "Object mapper");
        ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
        ctx.setObject(object);
        ctx.setCharset(findEncoderCharsetOrReturnDefault(requestContentType));
        ctx.setContentType(requestContentType);
        this.requestBody = mapper.serialize(ctx);
        return this;
    }

    public MockMvcRequestSpecification body(Object object, ObjectMapperType mapperType) {
        notNull(object, "object");
        notNull(mapperType, "Object mapper type");
        this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), mapperType, restAssuredConfig.getObjectMapperConfig());
        return this;
    }

    public RequestSender when() {
        return new MockMvcRequestSender(instanceMockMvc, params, restAssuredConfig, requestBody, requestContentType);
    }

    public static void reset() {
        mockMvc = null;
    }

    private String findEncoderCharsetOrReturnDefault(String contentType) {
        String charset = CharsetExtractor.getCharsetFromContentType(contentType);
        if (charset == null) {
            charset = restAssuredConfig.getEncoderConfig().defaultContentCharset();
        }
        return charset;
    }

}

package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.MapCreator;
import com.jayway.restassured.internal.http.CharsetExtractor;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.internal.mapping.ObjectMapperSerializationContextImpl;
import com.jayway.restassured.internal.mapping.ObjectMapping;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.specification.RequestSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;
import static com.jayway.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;
import static java.util.Arrays.asList;

public class RestAssuredMockMvc implements MockMvcRequestSpecification {
    public static MockMvc mockMvc = null;

    private static final String CONTENT_TYPE = "content-type";

    private final MockMvc instanceMockMvc;

    private final MultiValueMap<String, Object> params = new LinkedMultiValueMap<String, Object>();

    private Object requestBody = null;
    private String requestContentType;

    private RestAssuredConfig restAssuredConfig;

    private Headers requestHeaders = new Headers();

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

    public MockMvcRequestSpecification headers(String firstHeaderName, Object firstHeaderValue, Object... headerNameValuePairs) {
        return headers(MapCreator.createMapFromParams(firstHeaderName, firstHeaderValue, headerNameValuePairs));
    }

    public MockMvcRequestSpecification headers(Map<String, ?> headers) {
        notNull(headers, "headers");
        List<Header> headerList = new ArrayList<Header>();
        if (this.requestHeaders.exist()) {
            for (Header requestHeader : this.requestHeaders) {
                headerList.add(requestHeader);
            }
        }

        for (Map.Entry<String, ?> stringEntry : headers.entrySet()) {
            headerList.add(new Header(stringEntry.getKey(), serializeIfNeeded(stringEntry.getValue())));
        }

        filterContentTypeHeader(headerList);
        this.requestHeaders = new Headers(headerList);
        return this;
    }

    public MockMvcRequestSpecification headers(Headers headers) {
        notNull(headers, "Headers");
        if (headers.exist()) {
            List<Header> headerList = new ArrayList<Header>();
            if (this.requestHeaders.exist()) {
                for (Header requestHeader : this.requestHeaders) {
                    headerList.add(requestHeader);
                }
            }

            for (Header requestHeader : headers) {
                headerList.add(requestHeader);
            }

            filterContentTypeHeader(headerList);
            this.requestHeaders = new Headers(headerList);
        }
        return this;
    }

    public MockMvcRequestSpecification header(final String headerName, final Object headerValue, Object... additionalHeaderValues) {
        notNull(headerName, "Header name");
        notNull(headerValue, "Header value");

        if (CONTENT_TYPE.equalsIgnoreCase(headerName)) {
            return contentType(headerValue.toString());
        }

        List<Header> headerList = new ArrayList<Header>() {{
            add(new Header(headerName, serializeIfNeeded(headerValue)));
        }};

        if (additionalHeaderValues != null) {
            for (Object additionalHeaderValue : additionalHeaderValues) {
                headerList.add(new Header(headerName, serializeIfNeeded(additionalHeaderValue)));
            }
        }

        return headers(new Headers(headerList));
    }

    public MockMvcRequestSpecification header(Header header) {
        notNull(header, "Header");

        if (CONTENT_TYPE.equalsIgnoreCase(header.getName())) {
            return contentType(header.getName());
        }

        return headers(new Headers(asList(header)));
    }

    private void filterContentTypeHeader(List<Header> headerList) {
        ListIterator<Header> headerListIterator = headerList.listIterator();
        while (headerListIterator.hasNext()) {
            Header header = headerListIterator.next();
            if (CONTENT_TYPE.equalsIgnoreCase(header.getName())) {
                contentType(header.getValue());
                headerListIterator.remove();
            }
        }
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
        return new MockMvcRequestSender(instanceMockMvc, params, restAssuredConfig, requestBody, requestContentType, requestHeaders);
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

    private String serializeIfNeeded(Object object) {
        return serializeIfNeeded(object, requestContentType);
    }

    private String serializeIfNeeded(Object object, String contentType) {
        return isSerializableCandidate(object) ? ObjectMapping.serialize(object, contentType, findEncoderCharsetOrReturnDefault(contentType), null, restAssuredConfig.getObjectMapperConfig()) : object.toString();
    }


}

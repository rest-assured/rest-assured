package com.jayway.restassured.module.mockmvc.internal;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.MapCreator;
import com.jayway.restassured.internal.http.CharsetExtractor;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.internal.mapping.ObjectMapperSerializationContextImpl;
import com.jayway.restassured.internal.mapping.ObjectMapping;
import com.jayway.restassured.internal.support.ParameterAppender;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.module.mockmvc.config.RestAssuredConfigMockMvc;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestLogSpecification;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import com.jayway.restassured.response.*;
import com.jayway.restassured.specification.RequestSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;
import static com.jayway.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;
import static com.jayway.restassured.module.mockmvc.internal.ConfigConverter.convertToRestAssuredConfig;
import static java.util.Arrays.asList;

public class MockMvcRequestSpecificationImpl implements MockMvcRequestSpecification {

    private static final String CONTENT_TYPE = "content-type";

    private MockMvc instanceMockMvc;

    private final Map<String, Object> params = new LinkedHashMap<String, Object>();
    private final Map<String, Object> queryParams = new LinkedHashMap<String, Object>();

    private Object requestBody = null;

    private RestAssuredConfigMockMvc restAssuredConfigMockMvc;

    private Headers requestHeaders = new Headers();

    private Cookies cookies = new Cookies();

    private String requestContentType;

    private List<MockMvcMultiPart> multiParts = new ArrayList<MockMvcMultiPart>();

    private RequestLoggingFilter requestLoggingFilter;

    private ParameterAppender parameterAppender = new ParameterAppender(new ParameterAppender.Serializer() {
        public String serializeIfNeeded(Object value) {
            return MockMvcRequestSpecificationImpl.this.serializeIfNeeded(value);
        }
    });

    public MockMvcRequestSpecificationImpl(MockMvc mockMvc, RestAssuredConfigMockMvc config) {
        this.instanceMockMvc = mockMvc;
        restAssuredConfigMockMvc = config == null ? new RestAssuredConfigMockMvc() : config;
    }

    public MockMvcRequestSpecification mockMvc(MockMvc mockMvc) {
        notNull(mockMvc, MockMvc.class);
        return changeMockMvcInstanceTo(mockMvc);
    }

    public MockMvcRequestSpecification standaloneSetup(Object... controllers) {
        return changeMockMvcInstanceTo(MockMvcBuilders.standaloneSetup(controllers).build());
    }

    public MockMvcRequestSpecification webAppContextSetup(WebApplicationContext context) {
        return changeMockMvcInstanceTo(MockMvcBuilders.webAppContextSetup(context).build());
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

    public MockMvcRequestLogSpecification log() {
        return new MockMvcRequestLogSpecificationImpl(this);
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
        notNull(parameterName, "parameterName");
        parameterAppender.appendZeroToManyParameters(params, parameterName, parameterValues);
        return this;
    }

    public MockMvcRequestSpecification queryParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        notNull(firstParameterName, "firstParameterName");
        notNull(firstParameterValue, "firstParameterValue");
        return queryParams(MapCreator.createMapFromParams(firstParameterName, firstParameterValue, parameterNameValuePairs));
    }

    public MockMvcRequestSpecification queryParams(Map<String, ?> parametersMap) {
        notNull(parametersMap, "parametersMap");
        parameterAppender.appendParameters((Map<String, Object>) parametersMap, queryParams);
        return this;
    }

    public MockMvcRequestSpecification queryParam(String parameterName, Object... parameterValues) {
        notNull(parameterName, "parameterName");
        parameterAppender.appendZeroToManyParameters(queryParams, parameterName, parameterValues);
        return this;
    }

    public MockMvcRequestSpecification queryParam(String parameterName, Collection<?> parameterValues) {
        notNull(parameterName, "parameterName");
        notNull(parameterValues, "parameterValues");
        parameterAppender.appendCollectionParameter(queryParams, parameterName, (Collection<Object>) parameterValues);
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

        this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), null, restAssuredConfigMockMvc.getObjectMapperConfig());
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
        this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), mapperType, restAssuredConfigMockMvc.getObjectMapperConfig());
        return this;
    }

    public MockMvcRequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs) {
        return cookies(MapCreator.createMapFromParams(firstCookieName, firstCookieValue, cookieNameValuePairs));
    }

    public MockMvcRequestSpecification cookies(Map<String, ?> cookies) {
        notNull(cookies, "cookies");
        List<Cookie> cookieList = new ArrayList<Cookie>();
        if (this.cookies.exist()) {
            for (Cookie requestCookie : this.cookies) {
                cookieList.add(requestCookie);
            }
        }

        for (Map.Entry<String, ?> stringEntry : cookies.entrySet()) {
            cookieList.add(new Cookie.Builder(stringEntry.getKey(), serializeIfNeeded(stringEntry.getValue())).build());
        }

        this.cookies = new Cookies(cookieList);
        return this;
    }

    public MockMvcRequestSpecification cookies(Cookies cookies) {
        notNull(cookies, "Cookies");
        if (cookies.exist()) {
            List<Cookie> cookieList = new ArrayList<Cookie>();
            if (this.cookies.exist()) {
                for (Cookie cookie : this.cookies) {
                    cookieList.add(cookie);
                }
            }

            for (Cookie cookie : cookies) {
                cookieList.add(cookie);
            }

            this.cookies = new Cookies(cookieList);
        }
        return this;
    }

    public MockMvcRequestSpecification cookie(final String cookieName, final Object cookieValue, Object... additionalValues) {
        notNull(cookieName, "Cookie name");
        notNull(cookieValue, "Cookie value");

        if (CONTENT_TYPE.equalsIgnoreCase(cookieName)) {
            return contentType(cookieValue.toString());
        }

        List<Cookie> cookieList = new ArrayList<Cookie>() {{
            add(new Cookie.Builder(cookieName, serializeIfNeeded(cookieValue)).build());
        }};

        if (additionalValues != null) {
            for (Object additionalCookieValue : additionalValues) {
                cookieList.add(new Cookie.Builder(cookieName, serializeIfNeeded(additionalCookieValue)).build());
            }
        }

        return cookies(new Cookies(cookieList));
    }

    public MockMvcRequestSpecification cookie(Cookie cookie) {
        notNull(cookie, "Cookie");
        return cookies(new Cookies(asList(cookie)));
    }

    public MockMvcRequestSpecification multiPart(File file) {
        multiParts.add(new MockMvcMultiPart(file));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, File file) {
        multiParts.add(new MockMvcMultiPart(controlName, file));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, File file, String mimeType) {
        multiParts.add(new MockMvcMultiPart(controlName, file, mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, Object object) {
        multiParts.add(new MockMvcMultiPart(controlName, serializeIfNeeded(object)));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, Object object, String mimeType) {
        multiParts.add(new MockMvcMultiPart(controlName, serializeIfNeeded(object, mimeType), mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, byte[] bytes) {
        multiParts.add(new MockMvcMultiPart(controlName, fileName, bytes));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, byte[] bytes, String mimeType) {
        multiParts.add(new MockMvcMultiPart(controlName, fileName, bytes, mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, InputStream stream) {
        multiParts.add(new MockMvcMultiPart(controlName, fileName, stream));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, InputStream stream, String mimeType) {
        multiParts.add(new MockMvcMultiPart(controlName, fileName, stream, mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String contentBody) {
        multiParts.add(new MockMvcMultiPart(controlName, contentBody));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String contentBody, String mimeType) {
        multiParts.add(new MockMvcMultiPart(controlName, contentBody, mimeType));
        return this;
    }

    public MockMvcRequestSpecification config(RestAssuredConfigMockMvc config) {
        this.restAssuredConfigMockMvc = config == null ? new RestAssuredConfigMockMvc() : config;
        return this;
    }

    public RequestSender when() {
        return new MockMvcRequestSender(instanceMockMvc, params, queryParams, restAssuredConfigMockMvc, requestBody, requestContentType, requestHeaders, cookies, multiParts, requestLoggingFilter);
    }

    private String findEncoderCharsetOrReturnDefault(String contentType) {
        String charset = CharsetExtractor.getCharsetFromContentType(contentType);
        if (charset == null) {
            charset = restAssuredConfigMockMvc.getEncoderConfig().defaultContentCharset();
        }
        return charset;
    }

    private String serializeIfNeeded(Object object) {
        return serializeIfNeeded(object, requestContentType);
    }

    private String serializeIfNeeded(Object object, String contentType) {
        return isSerializableCandidate(object) ? ObjectMapping.serialize(object, contentType, findEncoderCharsetOrReturnDefault(contentType), null, restAssuredConfigMockMvc.getObjectMapperConfig()) : object.toString();
    }

    public Response get(String path, Object... pathParams) {
        return when().get(path, pathParams);
    }

    public Response get(String path, Map<String, ?> pathParams) {
        return when().get(path, pathParams);
    }

    public Response post(String path, Object... pathParams) {
        return when().post(path, pathParams);
    }

    public Response post(String path, Map<String, ?> pathParams) {
        return when().post(path, pathParams);
    }

    public Response put(String path, Object... pathParams) {
        return when().put(path, pathParams);
    }

    public Response put(String path, Map<String, ?> pathParams) {
        return when().put(path, pathParams);
    }

    public Response delete(String path, Object... pathParams) {
        return when().delete(path, pathParams);
    }

    public Response delete(String path, Map<String, ?> pathParams) {
        return when().delete(path, pathParams);
    }

    public Response head(String path, Object... pathParams) {
        return when().head(path, pathParams);
    }

    public Response head(String path, Map<String, ?> pathParams) {
        return when().head(path, pathParams);
    }

    public Response patch(String path, Object... pathParams) {
        return when().patch(path, pathParams);
    }

    public Response patch(String path, Map<String, ?> pathParams) {
        return when().patch(path, pathParams);
    }

    public Response options(String path, Object... pathParams) {
        return when().options(path, pathParams);
    }

    public Response options(String path, Map<String, ?> pathParams) {
        return when().options(path, pathParams);
    }

    public Response get(URI uri) {
        return when().get(uri);
    }

    public Response post(URI uri) {
        return when().post(uri);
    }

    public Response put(URI uri) {
        return when().put(uri);
    }

    public Response delete(URI uri) {
        return when().delete(uri);
    }

    public Response head(URI uri) {
        return when().head(uri);
    }

    public Response patch(URI uri) {
        return when().patch(uri);
    }

    public Response options(URI uri) {
        return when().options(uri);
    }

    public Response get(URL url) {
        return when().get(url);
    }

    public Response post(URL url) {
        return when().post(url);
    }

    public Response put(URL url) {
        return when().put(url);
    }

    public Response delete(URL url) {
        return when().delete(url);
    }

    public Response head(URL url) {
        return when().head(url);
    }

    public Response patch(URL url) {
        return when().patch(url);
    }

    public Response options(URL url) {
        return when().options(url);
    }

    public Response get() {
        return when().get();
    }

    public Response post() {
        return when().post();
    }

    public Response put() {
        return when().put();
    }

    public Response delete() {
        return when().delete();
    }

    public Response head() {
        return when().head();
    }

    public Response patch() {
        return when().patch();
    }

    public Response options() {
        return when().options();
    }

    public RestAssuredConfig getRestAssuredConfig() {
        return convertToRestAssuredConfig(restAssuredConfigMockMvc);
    }

    public void setRequestLoggingFilter(RequestLoggingFilter requestLoggingFilter) {
        this.requestLoggingFilter = requestLoggingFilter;
    }

    private MockMvcRequestSpecification changeMockMvcInstanceTo(MockMvc mockMvc) {
        this.instanceMockMvc = mockMvc;
        return this;
    }
}

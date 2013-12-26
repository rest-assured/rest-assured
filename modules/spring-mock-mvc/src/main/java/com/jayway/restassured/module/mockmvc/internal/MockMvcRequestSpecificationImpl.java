package com.jayway.restassured.module.mockmvc.internal;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.MapCreator;
import com.jayway.restassured.internal.http.CharsetExtractor;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.internal.mapping.ObjectMapperSerializationContextImpl;
import com.jayway.restassured.internal.mapping.ObjectMapping;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.module.mockmvc.MockMvcRequestSpecification;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.specification.RequestSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;
import static com.jayway.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;
import static java.util.Arrays.asList;

public class MockMvcRequestSpecificationImpl implements MockMvcRequestSpecification {

    private static final String CONTENT_TYPE = "content-type";

    private final MockMvc instanceMockMvc;

    private final MultiValueMap<String, Object> params = new LinkedMultiValueMap<String, Object>();

    private Object requestBody = null;

    private RestAssuredConfig restAssuredConfig;

    private Headers requestHeaders = new Headers();

    private Cookies cookies = new Cookies();

    private String requestContentType;

    private List<MvcMultiPart> multiParts = new ArrayList<MvcMultiPart>();

    public MockMvcRequestSpecificationImpl(MockMvc mockMvc) {
        this.instanceMockMvc = mockMvc;
        restAssuredConfig = new RestAssuredConfig();
    }

    public MockMvcRequestSpecification mockMvc(MockMvc mockMvc) {
        notNull(mockMvc, MockMvc.class);
        return new MockMvcRequestSpecificationImpl(mockMvc);
    }

    public MockMvcRequestSpecification standaloneSetup(Object... controllers) {
        return new MockMvcRequestSpecificationImpl(MockMvcBuilders.standaloneSetup(controllers).build());
    }

    public MockMvcRequestSpecification webAppContextSetup(WebApplicationContext context) {
        return new MockMvcRequestSpecificationImpl(MockMvcBuilders.webAppContextSetup(context).build());
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
        multiParts.add(new MvcMultiPart(file));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, File file) {
        multiParts.add(new MvcMultiPart(controlName, file));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, File file, String mimeType) {
        multiParts.add(new MvcMultiPart(controlName, file, mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, Object object) {
        multiParts.add(new MvcMultiPart(controlName, serializeIfNeeded(object)));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, Object object, String mimeType) {
        multiParts.add(new MvcMultiPart(controlName, serializeIfNeeded(object, mimeType), mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, byte[] bytes) {
        multiParts.add(new MvcMultiPart(controlName, fileName, bytes));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, byte[] bytes, String mimeType) {
        multiParts.add(new MvcMultiPart(controlName, fileName, bytes, mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, InputStream stream) {
        multiParts.add(new MvcMultiPart(controlName, fileName, stream));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String fileName, InputStream stream, String mimeType) {
        multiParts.add(new MvcMultiPart(controlName, fileName, stream, mimeType));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String contentBody) {
        multiParts.add(new MvcMultiPart(controlName, contentBody));
        return this;
    }

    public MockMvcRequestSpecification multiPart(String controlName, String contentBody, String mimeType) {
        multiParts.add(new MvcMultiPart(controlName, contentBody, mimeType));
        return this;
    }

    public RequestSender when() {
        return new MockMvcRequestSender(instanceMockMvc, params, restAssuredConfig, requestBody, requestContentType, requestHeaders, cookies, multiParts);
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

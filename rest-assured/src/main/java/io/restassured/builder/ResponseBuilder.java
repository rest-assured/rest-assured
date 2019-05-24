/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.builder;

import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

/**
 * A builder to make it easier to create new {@link Response} implementations. This is useful if you're working with {@link Filter}s and want to
 * change the response you get from the sever somehow.
 */
public class ResponseBuilder {

    private RestAssuredResponseImpl restAssuredResponse = new RestAssuredResponseImpl();

    /**
     * Clone an already existing response.
     *
     * @return Builder.
     */
    public ResponseBuilder clone(Response response) {
        if (isRestAssuredResponse(response)) {
            final RestAssuredResponseImpl raResponse = raResponse(response);
            restAssuredResponse.setContent(raResponse.getContent());
            restAssuredResponse.setHasExpectations(raResponse.getHasExpectations());
            restAssuredResponse.setDefaultContentType(raResponse.getDefaultContentType());
            restAssuredResponse.setDecoderConfig(raResponse.getDecoderConfig());
            restAssuredResponse.setSessionIdName(raResponse.getSessionIdName());
            restAssuredResponse.setConnectionManager(raResponse.getConnectionManager());
            restAssuredResponse.setConfig(raResponse.getConfig());
            restAssuredResponse.setRpr(raResponse.getRpr());
            restAssuredResponse.setLogRepository(raResponse.getLogRepository());
            restAssuredResponse.setFilterContextProperties(raResponse.getFilterContextProperties());
        } else {
            restAssuredResponse.setContent(response.asInputStream());
        }
        restAssuredResponse.setContentType(response.getContentType());
        restAssuredResponse.setCookies(response.getDetailedCookies());
        restAssuredResponse.setResponseHeaders(response.getHeaders());
        restAssuredResponse.setStatusCode(response.getStatusCode());
        restAssuredResponse.setStatusLine(response.getStatusLine());
        return this;
    }

    /**
     * Set the response body to a String
     *
     * @return Builder.
     */
    public ResponseBuilder setBody(String stringBody) {
        notNull(stringBody, "Response body");
        restAssuredResponse.setContent(stringBody);
        return this;
    }


    /**
     * Set the response body to an inputstream
     *
     * @return Builder.
     */
    public ResponseBuilder setBody(InputStream inputStream) {
        notNull(inputStream, "Response body");
        restAssuredResponse.setContent(inputStream);
        return this;
    }

    /**
     * Set the response body to an array of bytes
     *
     * @return Builder.
     */
    public ResponseBuilder setBody(byte[] bytes) {
        notNull(bytes, "Response body");
        restAssuredResponse.setContent(bytes);
        return this;
    }

    /**
     * Set response headers,  e.g:
     * <pre>
     * Header first = new Header("headerName1", "headerValue1");
     * Header second = new Header("headerName2", "headerValue2");
     * Headers headers = new Header(first, second);
     * </pre>
     *
     * @return The builder
     * @see Headers
     */
    public ResponseBuilder setHeaders(Headers headers) {
        notNull(headers, "Headers");
        restAssuredResponse.setResponseHeaders(headers);
        return this;
    }

    /**
     * Set some cookies that will be available in the response. To create cookies you can do:
     * <pre>
     * Cookie cookie1 = Cookie.Builder("username", "John").setComment("comment 1").build();
     * Cookie cookie2 = Cookie.Builder("token", 1234).setComment("comment 2").build();
     * Cookies cookies = new Cookies(cookie1, cookie2);
     * </pre>
     *
     * @return The Builder
     */
    public ResponseBuilder setCookies(Cookies cookies) {
        notNull(cookies, "Cookies");
        restAssuredResponse.setCookies(cookies);
        return this;
    }

    /**
     * Set the content type of the response
     *
     * @return The builder
     */
    public ResponseBuilder setContentType(String contentType) {
        notNull(contentType, "Content type");
        restAssuredResponse.setContentType(contentType);
        setHeader(CONTENT_TYPE, contentType);
        return this;
    }

    /**
     * Set the content type of the response
     *
     * @return The builder
     */
    public ResponseBuilder setContentType(ContentType contentType) {
        notNull(contentType, ContentType.class);
        return setContentType(contentType.toString());
    }

    /**
     * Set a specific header
     *
     * @return The builder
     */
    public ResponseBuilder setHeader(String name, String value) {
        notNull(name, "Header name");
        notNull(value, "Header value");

        List<Header> newHeaders = new ArrayList<Header>(restAssuredResponse.headers().asList());
        newHeaders.add(new Header(name, value));
        restAssuredResponse.setResponseHeaders(new Headers(newHeaders));

        if (trim(name).equalsIgnoreCase(CONTENT_TYPE)) {
            restAssuredResponse.setContentType(value);
        }
        return this;
    }

    /**
     * Set the status line of the response.
     *
     * @return The builder
     */
    public ResponseBuilder setStatusLine(String statusLine) {
        notNull(statusLine, "Status line");
        restAssuredResponse.setStatusLine(statusLine);
        return this;
    }

    /**
     * Set the status code of the response.
     *
     * @return The builder
     */
    public ResponseBuilder setStatusCode(int statusCode) {
        restAssuredResponse.setStatusCode(statusCode);
        return this;
    }

    /**
     * Build the actual response
     *
     * @return The response object
     */
    public Response build() {
        final int statusCode = restAssuredResponse.statusCode();

        if (statusCode < 100 || statusCode >= 600) {
            throw new IllegalArgumentException(format("Status code must be greater than 100 and less than 600, was %d.", statusCode));
        }

        if (StringUtils.isBlank(restAssuredResponse.statusLine())) {
            restAssuredResponse.setStatusLine(restAssuredResponse.statusCode());
        }

        // Set a default config
        if (restAssuredResponse.getConfig() == null) {
            restAssuredResponse.setConfig(RestAssuredConfig.config());
        }
        restAssuredResponse.setRpr(new ResponseParserRegistrar());
        return restAssuredResponse;
    }

    private boolean isRestAssuredResponse(Response response) {
        return response instanceof RestAssuredResponseImpl;
    }

    private RestAssuredResponseImpl raResponse(Response response) {
        return (RestAssuredResponseImpl) response;
    }
}

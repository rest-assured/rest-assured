/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.builder;

import com.jayway.restassured.authentication.AuthenticationScheme;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.internal.RequestSpecificationImpl;
import com.jayway.restassured.internal.SpecificationMerger;
import com.jayway.restassured.specification.RequestSpecification;
import groovyx.net.http.ContentType;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.*;

/**
 * You can use the builder to construct a request specification. The specification can be used as e.g.
 * <pre>
 * ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
 * RequestSpecification requestSpec = new RequestSpecBuilder().addParam("parameter1", "value1").build();
 *
 * given(responseSpec, requestSpec).post("/something");
 * </pre>
 *
 * or
 * <pre>
 * RequestSpecification requestSpec = new RequestSpecBuilder().addParameter("parameter1", "value1").build();
 *
 * given().
 *         spec(requestSpec).
 * expect().
 *         body("x.y.z", equalTo("something")).
 * when().
 *        get("/something");
 * </pre>
 */
public class RequestSpecBuilder {

    private RequestSpecification spec;

    public RequestSpecBuilder() {
        this.spec = new RequestSpecificationImpl(baseURI, port, basePath, authentication, filters(), requestContentType());
    }

    /**
     * Specify a String request body (such as e.g. JSON or XML) to be sent with the request. This works for the
     * POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     *
     * <p>
     * Note that {@link #setBody(String)} and {@link #setContent(String)} are the same except for the syntactic difference.
     * </p>
     *
     * @param body The body to send.
     * @return The request specification builder
     */
    public RequestSpecBuilder setBody(String body) {
        spec.body(body);
        return this;
    }

    /**
     * Specify a byte array request body to be sent with the request. This only works for the
     * POST http method. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Note that {@link #setBody(byte[])} and {@link #setContent(byte[])} are the same except for the syntactic difference.
     * </p>
     *
     * @param body The body to send.
     * @return The request specification builder
     */
    public RequestSpecBuilder setBody(byte[] body) {
        spec.body(body);
        return this;
    }

    /**
     * Specify a String request content (such as e.g. JSON or XML) to be sent with the request. This works for the
     * POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Note that {@link #setBody(String)} and {@link #setContent(String)} are the same except for the syntactic difference.
     * </p>
     *
     * @param content The content to send.
     * @return The request specification builder
     */
    public RequestSpecBuilder setContent(String content) {
        spec.content(content);
        return this;
    }

    /**
     * Specify a byte array request content to be sent with the request. This only works for the
     * POST http method. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Note that {@link #setBody(byte[])} and {@link #setContent(byte[])} are the same except for the syntactic difference.
     * </p>
     *
     * @param content The content to send.
     * @return The request specification builder
     */
    public RequestSpecBuilder setContent(byte[] content) {
        spec.content(content);
        return this;
    }

    /**
     * Add cookies to be sent with the request as Map e.g:
     *
     * @param cookies The Map containing the cookie names and their values to set in the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addCookies(Map<String, String> cookies) {
        spec.cookies(cookies);
        return this;
    }

    /**
     * Add a cookie to be sent with the request.
     *
     * @param key The cookie key
     * @param value The cookie value
     * @return The request specification builder
     */
    public RequestSpecBuilder addCookie(String key, String value) {
        spec.cookie(key, value);
        return this;
    }

    /**
     * Add a cookie without value to be sent with the request.
     *
     * @param key The cookie key
     * @return The request specification builder
     */
    public RequestSpecBuilder addCookie(String key) {
        spec.cookie(key);
        return this;
    }

    /**
     * Add a filter that will be used in the request.
     *
     * @param filter The filter to add
     * @return the request specification builder
     */
    public RequestSpecBuilder addFilter(Filter filter) {
        spec.filter(filter);
        return this;
    }

    /**
     * Add filters that will be used in the request.
     *
     * @param filters The filters to add
     * @return the request specification builder
     */
    public RequestSpecBuilder addFilters(List<Filter> filters) {
        spec.filters(filters);
        return this;
    }

    /**
     * Add parameters to be sent with the request as Map.

     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addParameters(Map<String, String> parametersMap) {
        spec.parameters(parametersMap);
        return this;
    }

    /**
     * Add a parameter to be sent with the request.
     *
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification builder
     */
    public RequestSpecBuilder addParameter(String parameterName, String parameterValue, String... additionalParameterValues) {
        spec.parameter(parameterName, parameterValue, additionalParameterValues);
        return this;
    }

    /**
     * Add a multi-value parameter to be sent with the request.
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addParameter(String parameterName, List<String> parameterValues) {
        spec.parameter(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addParameters(Map)}.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addParams(Map<String, String> parametersMap) {
        spec.params(parametersMap);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addParameter(String, String, String...) }.
     *
     * @see #addParameter(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification builder
     */
    public RequestSpecBuilder addParam(String parameterName, String parameterValue, String... additionalParameterValues) {
        spec.param(parameterName, parameterValue, additionalParameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addParameter(String, java.util.List)}.
     *
     * @see #addParameter(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addParam(String parameterName, List<String> parameterValues) {
        spec.param(parameterName, parameterValues);
        return this;
    }

    /**
     * Add query parameters to be sent with the request as a Map. This method is the same as {@link #addParameters(java.util.Map)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.

     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParameters(Map<String, String> parametersMap) {
        spec.queryParameters(parametersMap);
        return this;
    }

    /**
     * Add a query parameter to be sent with the request. This method is the same as {@link #addParameter(String, String, String...)} )}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     *
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParameter(String parameterName, String parameterValue, String... additionalParameterValues) {
        spec.queryParameter(parameterName, parameterValue, additionalParameterValues);
        return this;
    }

    /**
     * Add a query parameter to be sent with the request. This method is the same as {@link #addParameter(String, java.util.List)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParameter(String parameterName, List<String> parameterValues) {
        spec.parameter(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addQueryParameter(String, java.util.List)}.
     *
     * @see #addQueryParam(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParam(String parameterName, List<String> parameterValues) {
        spec.queryParam(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addQueryParameters(Map)}.
     *
     * @see #addQueryParameters(java.util.Map)
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParams(Map<String, String> parametersMap) {
        spec.queryParams(parametersMap);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addQueryParameter(String, String, String...)}.
     *
     * @see #addQueryParam(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParam(String parameterName, String parameterValue, String... additionalParameterValues) {
        spec.queryParam(parameterName, parameterValue, additionalParameterValues);
        return this;
    }

    /**
     * Add headers to be sent with the request as Map.
     *
     * @param headers The Map containing the header names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addHeaders(Map<String, String> headers) {
        spec.headers(headers);
        return this;
    }

    /**
     * Add a header to be sent with the request e.g:
     *
     * @param headerName The header name
     * @param headerValue The header value
     * @return The request specification builder
     */
    public RequestSpecBuilder addHeader(String headerName, String headerValue) {
        spec.header(headerName, headerValue);
        return this;
    }

    /**
     * Specify the content type of the request.
     *
     * @see groovyx.net.http.ContentType
     * @param contentType The content type of the request
     * @return The request specification builder
     */
    public RequestSpecBuilder setContentType(ContentType contentType) {
        spec.contentType(contentType);
        return this;
    }

    /**
     * Specify the content type of the request as string.
     *
     * @param contentType The content type of the request
     * @return The request specification builder
     */
    public RequestSpecBuilder setContentType(String contentType) {
        spec.contentType(contentType);
        return this;
    }

    /**
     * If you need to specify some credentials when performing a request.
     *
     * @return The request specification builder
     */
    public RequestSpecBuilder setAuthentication(AuthenticationScheme auth) {
        ((RequestSpecificationImpl) spec).setAuthenticationScheme(auth);
        return this;
    }

    /**
     * A slightly short version of {@link #setAuthentication(com.jayway.restassured.authentication.AuthenticationScheme)} )}.
     *
     * @see #setAuthentication(com.jayway.restassured.authentication.AuthenticationScheme)
     * @return The request specification builder
     */
    public RequestSpecBuilder setAuth(AuthenticationScheme auth) {
        return setAuthentication(auth);
    }

    /**
     * Specify the port.
     *
     * @param port The port of URI
     * @return The request specification builder
     */
    public RequestSpecBuilder setPort(int port) {
        spec.port(port);
        return this;
    }

    /**
     * Merge this builder with settings from another specification. Note that the supplied specification
     * can overwrite data in the current specification. The following settings are overwritten:
     * <ul>
     *     <li>Port</li>
     *     <li>Authentication scheme</
     *     <li>Content type</li>
     *     <li>Request body</li>
     * </ul>
     * The following settings are merged:
     * <ul>
     *     <li>Parameters</li>
     *     <li>Cookies</li>
     *     <li>Headers</li>
     * </ul>
     * @param specification The specification to add
     * @return The request specification builder
     */
    public RequestSpecBuilder addRequestSpecification(RequestSpecification specification) {
        if(!(specification instanceof RequestSpecification)) {
            throw new IllegalArgumentException("specification must be of type "+RequestSpecification.class.getClass()+".");
        }

        RequestSpecificationImpl rs = (RequestSpecificationImpl) specification;
        SpecificationMerger.merge((RequestSpecificationImpl) spec, rs);
        return this;
    }

    /**
     * Build the request specification.
     *
     * @return The assembled request specification
     */
    public RequestSpecification build() {
        return spec;
    }
}

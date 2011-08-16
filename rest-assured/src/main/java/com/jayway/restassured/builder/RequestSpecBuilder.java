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
        this.spec = new RequestSpecificationImpl(baseURI, port, basePath, authentication, filters(), keystore(), requestContentType(), requestSpecification, urlEncodingEnabled);
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
     * Add query parameters to be sent with the request as a Map. This method is the same as {@link #addParameters(java.util.Map)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.

     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParameters(Map<String, String> parametersMap) {
        spec.queryParameters(parametersMap);
        return this;
    }

    /**
     * Add a form parameter to be sent with the request. This method is the same as {@link #addParameter(String, String, String...)} )}
     * for all HTTP methods except PUT where this method can be used to differentiate between form and query params.
     *
     *
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParameter(String parameterName, String parameterValue, String... additionalParameterValues) {
        spec.queryParameter(parameterName, parameterValue, additionalParameterValues);
        return this;
    }

    /**
     * Add a form parameter to be sent with the request. This method is the same as {@link #addParameter(String, java.util.List)}
     * for all HTTP methods except PUT where this method can be used to differentiate between form and query params.
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParameter(String parameterName, List<String> parameterValues) {
        spec.parameter(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addFormParameter(String, java.util.List)}.
     *
     * @see #addFormParam(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParam(String parameterName, List<String> parameterValues) {
        spec.queryParam(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addFormParameters(Map)}.
     *
     * @see #addFormParameters(java.util.Map)
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParams(Map<String, String> parametersMap) {
        spec.queryParams(parametersMap);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addFormParameter(String, String, String...)}.
     *
     * @see #addFormParam(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParam(String parameterName, String parameterValue, String... additionalParameterValues) {
        spec.queryParam(parameterName, parameterValue, additionalParameterValues);
        return this;
    }

    /**
     * Specify a path parameter. Path parameters are used to improve readability of the request path. E.g. instead
     * of writing:
     * <pre>
     * expect().statusCode(200).when().get("/item/"+myItem.getItemNumber()+"/buy/"+2);
     * </pre>
     * you can write:
     * <pre>
     * given().
     *         pathParameter("itemNumber", myItem.getItemNumber()).
     *         pathParameter("amount", 2).
     * expect().
     *          statusCode(200).
     * when().
     *        get("/item/{itemNumber}/buy/{amount}");
     * </pre>
     *
     * which improves readability and allows the path to be reusable in many tests. Another alternative is to use:
     * <pre>
     * expect().statusCode(200).when().get("/item/{itemNumber}/buy/{amount}", myItem.getItemNumber(), 2);
     * </pre>
     *
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @return The request specification
     */
    public RequestSpecBuilder addPathParameter(String parameterName, Object parameterValue) {
        spec.pathParameter(parameterName, parameterValue);
        return this;
    }

    /**
     * Specify multiple path parameter name-value pairs. Path parameters are used to improve readability of the request path. E.g. instead
     * of writing:
     * <pre>
     * expect().statusCode(200).when().get("/item/"+myItem.getItemNumber()+"/buy/"+2);
     * </pre>
     * you can write:
     * <pre>
     * given().
     *         pathParameters("itemNumber", myItem.getItemNumber(), "amount", 2).
     * expect().
     *          statusCode(200).
     * when().
     *        get("/item/{itemNumber}/buy/{amount}");
     * </pre>
     *
     * which improves readability and allows the path to be reusable in many tests. Another alternative is to use:
     * <pre>
     * expect().statusCode(200).when().get("/item/{itemNumber}/buy/{amount}", myItem.getItemNumber(), 2);
     * </pre>
     *
     * @param parameterName The parameter key
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    public RequestSpecBuilder addPathParameters(String parameterName, Object...parameterNameValuePairs) {
        spec.pathParameters(parameterName, parameterNameValuePairs);
        return this;
    }

    /**
     * Specify multiple path parameter name-value pairs. Path parameters are used to improve readability of the request path. E.g. instead
     * of writing:
     * <pre>
     * expect().statusCode(200).when().get("/item/"+myItem.getItemNumber()+"/buy/"+2);
     * </pre>
     * you can write:
     * <pre>
     * Map&lt;String,Object&gt; pathParams = new HashMap&lt;String,Object&gt;();
     * pathParams.add("itemNumber",myItem.getItemNumber());
     * pathParams.add("amount",2);
     *
     * given().
     *         pathParameters(pathParams).
     * expect().
     *          statusCode(200).
     * when().
     *        get("/item/{itemNumber}/buy/{amount}");
     * </pre>
     *
     * which improves readability and allows the path to be reusable in many tests. Another alternative is to use:
     * <pre>
     * expect().statusCode(200).when().get("/item/{itemNumber}/buy/{amount}", myItem.getItemNumber(), 2);
     * </pre>
     *
     * @param parameterNameValuePairs A map containing the path parameters.
     * @return The request specification
     */
    public RequestSpecBuilder addPathParameters(Map<String, Object> parameterNameValuePairs) {
        spec.pathParameters(parameterNameValuePairs);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addPathParameter(String, Object)}.
     *
     * @see #addPathParameter(String, Object)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @return The request specification
     */
    public RequestSpecBuilder addPathParam(String parameterName, Object parameterValue) {
        spec.pathParam(parameterName, parameterValue);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addPathParameters(String, Object...)}.
     *
     * @see #addPathParameters(String, Object...)
     * @param parameterName The parameter key
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    public RequestSpecBuilder addPathParams(String parameterName, Object...parameterNameValuePairs) {
        spec.pathParams(parameterName, parameterNameValuePairs);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addPathParameters(java.util.Map)}.
     *
     * @see #addPathParameters(java.util.Map)
     * @param parameterNameValuePairs A map containing the path parameters.
     * @return The request specification
     */
    public RequestSpecBuilder addPathParams(Map<String, Object> parameterNameValuePairs) {
        spec.pathParams(parameterNameValuePairs);
        return this;
    }

    /**
     * The following documentation is taken from <a href="HTTP Builder">http://groovy.codehaus.org/modules/http-builder/doc/ssl.html</a>:
     * <p>
     *     <h1>SSL Configuration</h1>
     *
     * SSL should, for the most part, "just work." There are a few situations where it is not completely intuitive. You can follow the example below, or see HttpClient's SSLSocketFactory documentation for more information.
     *
     * <h1>SSLPeerUnverifiedException</h1>
     *
     * If you can't connect to an SSL website, it is likely because the certificate chain is not trusted. This is an Apache HttpClient issue, but explained here for convenience. To correct the untrusted certificate, you need to import a certificate into an SSL truststore.
     *
     * First, export a certificate from the website using your browser. For example, if you go to https://dev.java.net in Firefox, you will probably get a warning in your browser. Choose "Add Exception," "Get Certificate," "View," "Details tab." Choose a certificate in the chain and export it as a PEM file. You can view the details of the exported certificate like so:
     * <pre>
     * $ keytool -printcert -file EquifaxSecureGlobaleBusinessCA-1.crt
     * Owner: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Issuer: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Serial number: 1
     * Valid from: Mon Jun 21 00:00:00 EDT 1999 until: Sun Jun 21 00:00:00 EDT 2020
     * Certificate fingerprints:
     * MD5:  8F:5D:77:06:27:C4:98:3C:5B:93:78:E7:D7:7D:9B:CC
     * SHA1: 7E:78:4A:10:1C:82:65:CC:2D:E1:F1:6D:47:B4:40:CA:D9:0A:19:45
     * Signature algorithm name: MD5withRSA
     * Version: 3
     * ....
     * </pre>
     * Now, import that into a Java keystore file:
     *<pre>
     * $ keytool -importcert -alias "equifax-ca" -file EquifaxSecureGlobaleBusinessCA-1.crt -keystore truststore.jks -storepass test1234
     * Owner: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Issuer: CN=Equifax Secure Global eBusiness CA-1, O=Equifax Secure Inc., C=US
     * Serial number: 1
     * Valid from: Mon Jun 21 00:00:00 EDT 1999 until: Sun Jun 21 00:00:00 EDT 2020
     * Certificate fingerprints:
     * MD5:  8F:5D:77:06:27:C4:98:3C:5B:93:78:E7:D7:7D:9B:CC
     * SHA1: 7E:78:4A:10:1C:82:65:CC:2D:E1:F1:6D:47:B4:40:CA:D9:0A:19:45
     * Signature algorithm name: MD5withRSA
     * Version: 3
     * ...
     * Trust this certificate? [no]:  yes
     * Certificate was added to keystore
     * </pre>
     * Now you want to use this truststore in your client:
     * <pre>
     * RestAssured.keystore("/truststore.jks", "test1234");
     * </pre>
     * or
     * <pre>
     * given().keystore("/truststore.jks", "test1234"). ..
     * </pre>
     * </p>
     * @param pathToJks The path to the JKS
     * @param password The store pass
     */
    public RequestSpecBuilder setKeystore(String pathToJks, String password) {
        spec.keystore(pathToJks, password);
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
     * Specifies if Rest Assured should url encode the URL automatically. Usually this is a recommended but in some cases
     * e.g. the query parameters are already be encoded before you provide them to Rest Assured then it's useful to disable
     * URL encoding.
     * @param isEnabled Specify whether or not URL encoding should be enabled or disabled.
     * @return The request specification builder
     */
    public RequestSpecBuilder setUrlEncodingEnabled(boolean isEnabled) {
        spec.urlEncodingEnabled(isEnabled);
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

/*
 * Copyright 2013 the original author or authors.
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
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.RequestSpecificationImpl;
import com.jayway.restassured.internal.SpecificationMerger;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.specification.MultiPartSpecification;
import com.jayway.restassured.specification.RequestSpecification;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
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
 * <p/>
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
        this.spec = new RequestSpecificationImpl(baseURI, port, basePath, authentication, filters(), keystore(),
                requestContentType(), requestSpecification, urlEncodingEnabled, config);
    }

    /**
     * Specify a String request body (such as e.g. JSON or XML) to be sent with the request. This works for the
     * POST, PUT and PATCH methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p/>
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
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request.
     * If the object is a primitive or <a href="http://download.oracle.com/javase/6/docs/api/java/lang/Number.html">Number</a> the object will
     * be converted to a String and put in the request body. This works for the POST, PUT and PATCH methods only.
     * Trying to do this for the other http methods will cause an exception to be thrown.
     * <p/>
     * <p>
     * Note that {@link #setBody(Object)}  and {@link #setContent(Object)} are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @return The request specification
     */
    public RequestSpecBuilder setBody(Object object) {
        spec.body(object);
        return this;
    }

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper.
     * This works for the POST, PATCH and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Note that {@link #setBody(Object, com.jayway.restassured.mapper.ObjectMapper)}  and {@link #setContent(Object, com.jayway.restassured.mapper.ObjectMapper)}
     * are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @param mapper The object mapper
     * @return The request specification
     */
    public RequestSpecBuilder setBody(Object object, ObjectMapper mapper) {
        spec.body(object, mapper);
        return this;
    }

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper type.
     * This works for the POST, PATCH and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * Message message = new Message();
     * message.setMessage("My beautiful message");
     *
     * given().
     *         body(message, ObjectMapper.GSON).
     * expect().
     *         content(equalTo("Response to a beautiful message")).
     * when().
     *         post("/beautiful-message");
     * </pre>
     * </p>
     * Note that {@link #setBody(Object, com.jayway.restassured.internal.mapper.ObjectMapperType)}  and {@link #setContent(Object, com.jayway.restassured.internal.mapper.ObjectMapperType)}
     * are the same except for the syntactic difference.
     * </p>
     *
     * @param object     The object to serialize and send with the request
     * @param mapperType The object mapper type to be used
     * @return The request specification
     */
    public RequestSpecBuilder setBody(Object object, ObjectMapperType mapperType) {
        spec.body(object, mapperType);
        return this;
    }

    /**
     * Specify a String request content (such as e.g. JSON or XML) to be sent with the request. This works for the
     * POST, PUT and PATCH methods only. Trying to do this for the other http methods will cause an exception to be thrown.
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
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request.
     * If the object is a primitive or <a href="http://download.oracle.com/javase/6/docs/api/java/lang/Number.html">Number</a> the object will
     * be converted to a String and put in the request body. This works for the POST, PUT and PATCH methods only.
     * Trying to do this for the other http methods will cause an exception to be thrown.
     * <p/>
     * <p>
     * Note that {@link #setBody(Object)}  and {@link #setContent(Object)} are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @return The request specification
     */
    public RequestSpecBuilder setContent(Object object) {
        spec.body(object);
        return this;
    }

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper.
     * This works for the POST, PATCH and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Note that {@link #setBody(Object, com.jayway.restassured.mapper.ObjectMapper)}  and {@link #setContent(Object, com.jayway.restassured.mapper.ObjectMapper)}
     * are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @param mapper The object mapper
     * @return The request specification
     */
    public RequestSpecBuilder setContent(Object object, ObjectMapper mapper) {
        spec.body(object, mapper);
        return this;
    }

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper type.
     * This works for the POST, PATCH and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * Message message = new Message();
     * message.setMessage("My beautiful message");
     *
     * given().
     *         body(message, ObjectMapper.GSON).
     * expect().
     *         content(equalTo("Response to a beautiful message")).
     * when().
     *         post("/beautiful-message");
     * </pre>
     * </p>
     * Note that {@link #setBody(Object, com.jayway.restassured.internal.mapper.ObjectMapperType)}  and {@link #setContent(Object, com.jayway.restassured.internal.mapper.ObjectMapperType)}
     * are the same except for the syntactic difference.
     * </p>
     *
     * @param object     The object to serialize and send with the request
     * @param mapperType The object mapper type to be used
     * @return The request specification
     */
    public RequestSpecBuilder setContent(Object object, ObjectMapperType mapperType) {
        spec.body(object, mapperType);
        return this;
    }

    /**
     * Add cookies to be sent with the request as Map e.g:
     *
     * @param cookies The Map containing the cookie names and their values to set in the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addCookies(Map<String, ?> cookies) {
        spec.cookies(cookies);
        return this;
    }

    /**
     * Add a detailed cookie
     *
     * @param cookie The cookie to add.
     * @return The request specification builder
     */
    public RequestSpecBuilder addCookie(Cookie cookie) {
        spec.cookie(cookie);
        return this;
    }

    /**
     * Add a cookie to be sent with the request.
     *
     * @param key                  The cookie key
     * @param value                The cookie value
     * @param cookieNameValuePairs Additional cookies values. This will actually create two cookies with the same name but with different values.
     * @return The request specification builder
     */
    public RequestSpecBuilder addCookie(String key, Object value, Object... cookieNameValuePairs) {
        spec.cookie(key, value, cookieNameValuePairs);
        return this;
    }

    /**
     * Add a cookie without value to be sent with the request.
     *
     * @param name The cookie name
     * @return The request specification builder
     */
    public RequestSpecBuilder addCookie(String name) {
        spec.cookie(name);
        return this;
    }

    /**
     * Specify multiple detailed cookies that'll be sent with the request.
     *
     * @param cookies The cookies to set in the request.
     * @return The request specification builder
     * @see RequestSpecification#cookies(com.jayway.restassured.response.Cookies)
     */
    public RequestSpecBuilder addCookies(Cookies cookies) {
        spec.cookies(cookies);
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
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addParameters(Map<String, ?> parametersMap) {
        spec.parameters(parametersMap);
        return this;
    }

    /**
     * Add a parameter to be sent with the request.
     *
     * @param parameterName   The parameter name
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     */
    public RequestSpecBuilder addParameter(String parameterName, Object... parameterValues) {
        spec.parameter(parameterName, parameterValues);
        return this;
    }

    /**
     * Add a multi-value parameter to be sent with the request.
     *
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addParameter(String parameterName, Collection<?> parameterValues) {
        spec.parameter(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addParameters(Map)}.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addParams(Map<String, ?> parametersMap) {
        spec.params(parametersMap);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addParameter(String, Object...) }.
     *
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     * @see #addParameter(String, Object...)
     */
    public RequestSpecBuilder addParam(String parameterName, Object... parameterValues) {
        spec.param(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addParameter(String, java.util.Collection}.
     *
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     * @see #addParameter(String, Object...)
     */
    public RequestSpecBuilder addParam(String parameterName, Collection<?> parameterValues) {
        spec.param(parameterName, parameterValues);
        return this;
    }

    /**
     * Add query parameters to be sent with the request as a Map. This method is the same as {@link #addParameters(java.util.Map)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParameters(Map<String, ?> parametersMap) {
        spec.queryParameters(parametersMap);
        return this;
    }

    /**
     * Add a query parameter to be sent with the request. This method is the same as {@link #addParameter(String, Object...)} )}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parameterName   The parameter key
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParameter(String parameterName, Object... parameterValues) {
        spec.queryParameter(parameterName, parameterValues);
        return this;
    }

    /**
     * Add a query parameter to be sent with the request. This method is the same as {@link #addParameter(String, java.util.Collection)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParameter(String parameterName, Collection<?> parameterValues) {
        spec.parameter(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addQueryParameter(String, java.util.Collection)}.
     *
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     * @see #addQueryParam(String, Object...)
     */
    public RequestSpecBuilder addQueryParam(String parameterName, Collection<?> parameterValues) {
        spec.queryParam(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addQueryParameters(Map)}.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     * @see #addQueryParameters(java.util.Map)
     */
    public RequestSpecBuilder addQueryParams(Map<String, ?> parametersMap) {
        spec.queryParams(parametersMap);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addQueryParameter(String, Object...)}.
     *
     * @param parameterName   The parameter key
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     * @see #addQueryParameter(String, Object...)
     */
    public RequestSpecBuilder addQueryParam(String parameterName, Object... parameterValues) {
        spec.queryParam(parameterName, parameterValues);
        return this;
    }

    /**
     * Add query parameters to be sent with the request as a Map. This method is the same as {@link #addParameters(java.util.Map)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParameters(Map<String, ?> parametersMap) {
        spec.formParameters(parametersMap);
        return this;
    }

    /**
     * Add a form parameter to be sent with the request. This method is the same as {@link #addParameter(String, Object...)} )}
     * for all HTTP methods except PUT where this method can be used to differentiate between form and query params.
     *
     * @param parameterName   The parameter key
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParameter(String parameterName, Object... parameterValues) {
        spec.formParameter(parameterName, parameterValues);
        return this;
    }

    /**
     * Add a form parameter to be sent with the request. This method is the same as {@link #addParameter(String, java.util.Collection)}
     * for all HTTP methods except PUT where this method can be used to differentiate between form and query params.
     *
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParameter(String parameterName, Collection<?> parameterValues) {
        spec.formParam(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addFormParameter(String, java.util.Collection}.
     *
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     * @see #addFormParam(String, Object...)
     */
    public RequestSpecBuilder addFormParam(String parameterName, Collection<?> parameterValues) {
        spec.formParam(parameterName, parameterValues);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addFormParameters(Map)}.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     * @see #addFormParameters(java.util.Map)
     */
    public RequestSpecBuilder addFormParams(Map<String, ?> parametersMap) {
        spec.formParams(parametersMap);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addFormParameter(String, Object...)}.
     *
     * @param parameterName   The parameter name
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     * @see #addFormParam(String, Object...)
     */
    public RequestSpecBuilder addFormParam(String parameterName, Object... parameterValues) {
        spec.formParam(parameterName, parameterValues);
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
     * <p/>
     * which improves readability and allows the path to be reusable in many tests. Another alternative is to use:
     * <pre>
     * expect().statusCode(200).when().get("/item/{itemNumber}/buy/{amount}", myItem.getItemNumber(), 2);
     * </pre>
     *
     * @param parameterName  The parameter key
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
     * <p/>
     * which improves readability and allows the path to be reusable in many tests. Another alternative is to use:
     * <pre>
     * expect().statusCode(200).when().get("/item/{itemNumber}/buy/{amount}", myItem.getItemNumber(), 2);
     * </pre>
     *
     * @param firstParameterName      The name of the first parameter
     * @param firstParameterValue     The value of the first parameter
     * @param parameterNameValuePairs Additional parameters in name-value pairs.
     * @return The request specification
     */
    public RequestSpecBuilder addPathParameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        spec.pathParameters(firstParameterName, firstParameterValue, parameterNameValuePairs);
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
     * <p/>
     * which improves readability and allows the path to be reusable in many tests. Another alternative is to use:
     * <pre>
     * expect().statusCode(200).when().get("/item/{itemNumber}/buy/{amount}", myItem.getItemNumber(), 2);
     * </pre>
     *
     * @param parameterNameValuePairs A map containing the path parameters.
     * @return The request specification
     */
    public RequestSpecBuilder addPathParameters(Map<String, ?> parameterNameValuePairs) {
        spec.pathParameters(parameterNameValuePairs);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addPathParameter(String, Object)}.
     *
     * @param parameterName  The parameter key
     * @param parameterValue The parameter value
     * @return The request specification
     * @see #addPathParameter(String, Object)
     */
    public RequestSpecBuilder addPathParam(String parameterName, Object parameterValue) {
        spec.pathParam(parameterName, parameterValue);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addPathParameters(String, Object, Object...)}.
     *
     * @param firstParameterName      The name of the first parameter
     * @param firstParameterValue     The value of the first parameter
     * @param parameterNameValuePairs Additional parameters in name-value pairs.
     * @return The request specification
     * @see #addPathParameters(String, Object, Object...)
     */
    public RequestSpecBuilder addPathParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        spec.pathParams(firstParameterName, firstParameterValue, parameterNameValuePairs);
        return this;
    }

    /**
     * A slightly shorter version of {@link #addPathParameters(java.util.Map)}.
     *
     * @param parameterNameValuePairs A map containing the path parameters.
     * @return The request specification
     * @see #addPathParameters(java.util.Map)
     */
    public RequestSpecBuilder addPathParams(Map<String, ?> parameterNameValuePairs) {
        spec.pathParams(parameterNameValuePairs);
        return this;
    }

    /**
     * The following documentation is taken from <a href="HTTP Builder">http://groovy.codehaus.org/modules/http-builder/doc/ssl.html</a>:
     * <p>
     * <h1>SSL Configuration</h1>
     * <p/>
     * SSL should, for the most part, "just work." There are a few situations where it is not completely intuitive. You can follow the example below, or see HttpClient's SSLSocketFactory documentation for more information.
     * <p/>
     * <h1>SSLPeerUnverifiedException</h1>
     * <p/>
     * If you can't connect to an SSL website, it is likely because the certificate chain is not trusted. This is an Apache HttpClient issue, but explained here for convenience. To correct the untrusted certificate, you need to import a certificate into an SSL truststore.
     * <p/>
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
     * <pre>
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
     *
     * @param pathToJks The path to the JKS
     * @param password  The store pass
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
     * @param headerName  The header name
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
     * @param contentType The content type of the request
     * @return The request specification builder
     * @see com.jayway.restassured.http.ContentType
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
     * Specify a multi-part specification. Use this method if you need to specify content-type etc.
     *
     * @param multiPartSpecification Multipart specification
     * @return The request specification
     */
    RequestSpecBuilder addMultiPart(MultiPartSpecification multiPartSpecification) {
        spec.multiPart(multiPartSpecification);
        return this;
    }

    /**
     * Specify a file to upload to the server using multi-part form data uploading.
     * It will assume that the control name is <tt>file</tt> and the content-type is <tt>application/octet-stream</tt>.
     * If this is not what you want please use an overloaded method.
     *
     * @param file The file to upload
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(File file) {
        spec.multiPart(file);
        return this;
    }

    /**
     * Specify a file to upload to the server using multi-part form data uploading with a specific
     * control name. It will use the content-type <tt>application/octet-stream</tt>.
     * If this is not what you want please use an overloaded method.
     *
     * @param file        The file to upload
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, File file) {
        spec.multiPart(controlName, file);
        return this;
    }

    /**
     * Specify a file to upload to the server using multi-part form data uploading with a specific
     * control name and content-type.
     *
     * @param file        The file to upload
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param mimeType    The content-type
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, File file, String mimeType) {
        spec.multiPart(controlName, file, mimeType);
        return this;
    }

    /**
     * Specify a byte-array to upload to the server using multi-part form data.
     * It will use the content-type <tt>application/octet-stream</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param bytes       The bytes you want to send
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, String fileName, byte[] bytes) {
        spec.multiPart(controlName, fileName, bytes);
        return this;
    }

    /**
     * Specify a byte-array to upload to the server using multi-part form data.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param bytes       The bytes you want to send
     * @param mimeType    The content-type
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, String fileName, byte[] bytes, String mimeType) {
        spec.multiPart(controlName, fileName, bytes, mimeType);
        return this;
    }

    /**
     * Specify an inputstream to upload to the server using multi-part form data.
     * It will use the content-type <tt>application/octet-stream</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param stream      The stream you want to send
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, String fileName, InputStream stream) {
        spec.multiPart(controlName, fileName, stream);
        return this;
    }

    /**
     * Specify an inputstream to upload to the server using multi-part form data.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param stream      The stream you want to send
     * @param mimeType    The content-type
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, String fileName, InputStream stream, String mimeType) {
        spec.multiPart(controlName, fileName, stream, mimeType);
        return this;
    }

    /**
     * Specify a string to send to the server using multi-part form data.
     * It will use the content-type <tt>text/plain</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param contentBody The string to send
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, String contentBody) {
        spec.multiPart(controlName, contentBody);
        return this;
    }

    /**
     * Specify a string to send to the server using multi-part form data with a specific mime-type.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param contentBody The string to send
     * @param mimeType    The mime-type
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(String controlName, String contentBody, String mimeType) {
        spec.multiPart(controlName, mimeType);
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
     * @return The request specification builder
     * @see #setAuthentication(com.jayway.restassured.authentication.AuthenticationScheme)
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
     *
     * @param isEnabled Specify whether or not URL encoding should be enabled or disabled.
     * @return The request specification builder
     */
    public RequestSpecBuilder setUrlEncodingEnabled(boolean isEnabled) {
        spec.urlEncodingEnabled(isEnabled);
        return this;
    }

    /**
     * Set the session id for this request. It will use the configured session id name from the configuration (by default this is {@value com.jayway.restassured.config.SessionConfig#DEFAULT_SESSION_ID_NAME}).
     * You can configure the session id name by using:
     * <pre>
     *     RestAssured.config = newConfig().sessionConfig(new SessionConfig().sessionIdName(&lt;sessionIdName&gt;));
     * </pre>
     * or you can use the {@link #setSessionId(String, String)} method to set it for this request only.
     *
     * @param sessionIdValue The session id value.
     * @return The request specification
     */
    public RequestSpecBuilder setSessionId(String sessionIdValue) {
        spec.sessionId(sessionIdValue);
        return this;
    }

    /**
     * Set the session id name and value for this request. It'll override the default session id name from the configuration (by default this is {@value com.jayway.restassured.config.SessionConfig#DEFAULT_SESSION_ID_NAME}).
     * You can configure the default session id name by using:
     * <pre>
     *     RestAssured.config = newConfig().sessionConfig(new SessionConfig().sessionIdName(&lt;sessionIdName&gt;));
     * </pre>
     * and then you can use the {@link RequestSpecBuilder#setSessionId(String)} method to set the session id value without specifying the name for each request.
     *
     * @param sessionIdName  The session id name
     * @param sessionIdValue The session id value.
     * @return The request specification
     */
    public RequestSpecBuilder setSessionId(String sessionIdName, String sessionIdValue) {
        spec.sessionId(sessionIdName, sessionIdValue);
        return this;
    }

    /**
     * Merge this builder with settings from another specification. Note that the supplied specification
     * can overwrite data in the current specification. The following settings are overwritten:
     * <ul>
     * <li>Port</li>
     * <li>Authentication scheme</
     * <li>Content type</li>
     * <li>Request body</li>
     * </ul>
     * The following settings are merged:
     * <ul>
     * <li>Parameters</li>
     * <li>Cookies</li>
     * <li>Headers</li>
     * <li>Filters</li>
     * </ul>
     *
     * @param specification The specification to add
     * @return The request specification builder
     */
    public RequestSpecBuilder addRequestSpecification(RequestSpecification specification) {
        if (!(specification instanceof RequestSpecificationImpl)) {
            throw new IllegalArgumentException("Specification must be of type " + RequestSpecificationImpl.class.getClass() + ".");
        }

        RequestSpecificationImpl rs = (RequestSpecificationImpl) specification;
        SpecificationMerger.merge((RequestSpecificationImpl) spec, rs);
        return this;
    }

    /**
     * Define a configuration for redirection settings and http client parameters.
     *
     * @param config The configuration to use for this request. If <code>null</code> no config will be used.
     * @return The request specification builder
     */
    public RequestSpecBuilder setConfig(RestAssuredConfig config) {
        spec.config(config);
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

    /**
     * Adding the baseUri Property from the RequestSpecBuilder.
     * instead of using static field RestAssured.baseURI
     * <p/>
     * <pre>
     * RequestSpecBuilder builder = new RequestSpecBuilder();
     * builder.setBaseUri("http://example.com");
     * RequestSpecification specs = builder.build();
     * given().specification(specs)
     * </pre>
     *
     * @param uri
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setBaseUri(String uri) {
        spec.baseUri(uri);
        return this;
    }
}

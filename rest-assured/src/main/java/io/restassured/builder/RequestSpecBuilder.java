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

import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.SpecificationMerger;
import io.restassured.internal.log.LogRepository;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.*;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

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

    private static final String SSL = "SSL";
    private RequestSpecificationImpl spec;

    public RequestSpecBuilder() {
        this.spec = (RequestSpecificationImpl) new RequestSpecificationImpl(baseURI, port, basePath, authentication, filters(),
                requestSpecification, urlEncodingEnabled, config, new LogRepository(), proxy).config(RestAssured.config());
    }

    /**
     * Specify a String request body (such as e.g. JSON or XML) to be sent with the request. This works for the
     * POST, PUT and PATCH methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p/>
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
     * @see RequestSpecification#cookies(Cookies)
     */
    public RequestSpecBuilder addCookies(Cookies cookies) {
        spec.cookies(cookies);
        return this;
    }


    /**
     * Add a filter that will be used in the request.
     *
     * @param filter The filter to add
     * @return RequestSpecBuilder builder
     */
    public RequestSpecBuilder addFilter(Filter filter) {
        spec.filter(filter);
        return this;
    }

    /**
     * Add filters that will be used in the request.
     *
     * @param filters The filters to add
     * @return RequestSpecBuilder builder
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
    public RequestSpecBuilder addParams(Map<String, ?> parametersMap) {
        spec.params(parametersMap);
        return this;
    }

    /**
     * Add a parameter to be sent with the request.
     *
     * @param parameterName   The parameter name
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     */
    public RequestSpecBuilder addParam(String parameterName, Object... parameterValues) {
        spec.param(parameterName, parameterValues);
        return this;
    }

    /**
     * Add a multi-value parameter to be sent with the request.
     *
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public RequestSpecBuilder addParam(String parameterName, Collection<?> parameterValues) {
        spec.param(parameterName, parameterValues);
        return this;
    }

    /**
     * Method to remove parameter added with {@link #addParam(String, Object...)} from map.
     * Removes all values of this parameter
     *
     * @param parameterName The parameter key
     * @return The request specification builder
     */
    public RequestSpecBuilder removeParam(String parameterName) {
        spec.removeParam(parameterName);
        return this;
    }

    /**
     * Add a query parameter to be sent with the request. This method is the same as {@link #addParam(String, java.util.Collection)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
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
     * Add query parameters to be sent with the request as a Map. This method is the same as {@link #addParams(java.util.Map)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParams(Map<String, ?> parametersMap) {
        spec.queryParams(parametersMap);
        return this;
    }

    /**
     * Add a query parameter to be sent with the request. This method is the same as {@link #addParam(String, Object...)} )}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parameterName   The parameter key
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     */
    public RequestSpecBuilder addQueryParam(String parameterName, Object... parameterValues) {
        spec.queryParam(parameterName, parameterValues);
        return this;
    }

    /**
     * Method to remove parameter added with from map.
     * Removes all values of this parameter
     *
     * @param parameterName The parameter key
     * @return The request specification builder
     */
    public RequestSpecBuilder removeQueryParam(String parameterName) {
        spec.removeQueryParam(parameterName);
        return this;
    }

    /**
     * Add a form parameter to be sent with the request. This method is the same as {@link #addParam(String, java.util.Collection)}
     * for all HTTP methods except PUT where this method can be used to differentiate between form and query params.
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
     * Add query parameters to be sent with the request as a Map. This method is the same as {@link #addParams(java.util.Map)}
     * for all HTTP methods except POST where this method can be used to differentiate between form and query params.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public RequestSpecBuilder addFormParams(Map<String, ?> parametersMap) {
        spec.formParams(parametersMap);
        return this;
    }

    /**
     * Add a form parameter to be sent with the request. This method is the same as {@link #addParam(String, Object...)} )}
     * for all HTTP methods except PUT where this method can be used to differentiate between form and query params.
     *
     * @param parameterName   The parameter key
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     * @see #addFormParam(String, Object...)
     */
    public RequestSpecBuilder addFormParam(String parameterName, Object... parameterValues) {
        spec.formParam(parameterName, parameterValues);
        return this;
    }

    /**
     * Method to remove parameter added with {@link #addFormParam(String, Object...)} from map.
     * Removes all values of this parameter
     *
     * @param parameterName The parameter key
     * @return The request specification builder
     */
    public RequestSpecBuilder removeFormParam(String parameterName) {
        spec.removeFormParam(parameterName);
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
     *         pathParam("itemNumber", myItem.getItemNumber()).
     *         pathParam("amount", 2).
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
    public RequestSpecBuilder addPathParam(String parameterName, Object parameterValue) {
        spec.pathParam(parameterName, parameterValue);
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
     *         pathParam("itemNumber", myItem.getItemNumber(), "amount", 2).
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
    public RequestSpecBuilder addPathParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
        spec.pathParams(firstParameterName, firstParameterValue, parameterNameValuePairs);
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
    public RequestSpecBuilder addPathParams(Map<String, ?> parameterNameValuePairs) {
        spec.pathParams(parameterNameValuePairs);
        return this;
    }

    /**
     * Method to remove parameter added with {@link #addPathParam(String, Object)} from map.
     * Removes all values of this parameter.
     *
     * @param parameterName The parameter key
     * @return The request specification builder
     */
    public RequestSpecBuilder removePathParam(String parameterName) {
        spec.removePathParam(parameterName);
        return this;
    }

    /**
     * Specify a keystore.
     * <pre>
     * RestAssured.keyStore("/truststore_javanet.jks", "test1234");
     * </pre>
     * or
     * <pre>
     * given().keyStore("/truststore_javanet.jks", "test1234"). ..
     * </pre>
     * </p>
     *
     * @param pathToJks The path to the JKS
     * @param password  The store pass
     */
    public RequestSpecBuilder setKeyStore(String pathToJks, String password) {
        spec.keyStore(pathToJks, password);
        return this;
    }

    /**
     * The following documentation is taken from <a href="HTTP Builder">https://github.com/jgritman/httpbuilder/wiki/SSL</a>:
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
     * $ keytool -importcert -alias "equifax-ca" -file EquifaxSecureGlobaleBusinessCA-1.crt -keystore truststore_javanet.jks -storepass test1234
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
     * RestAssured.trustStore("/truststore_javanet.jks", "test1234");
     * </pre>
     * or
     * <pre>
     * given().trustStore("/truststore_javanet.jks", "test1234"). ..
     * </pre>
     * </p>
     *
     * @param pathToJks The path to the JKS
     * @param password  The store pass
     */
    public RequestSpecBuilder setTrustStore(String pathToJks, String password) {
        spec.trustStore(pathToJks, password);
        return this;
    }

    /**
     * The following documentation is taken from <a href="HTTP Builder">https://github.com/jgritman/httpbuilder/wiki/SSL</a>:
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
     * $ keytool -importcert -alias "equifax-ca" -file EquifaxSecureGlobaleBusinessCA-1.crt -keystore truststore_javanet.jks -storepass test1234
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
     * RestAssured.trustStore("/truststore_javanet.jks", "test1234");
     * </pre>
     * or
     * <pre>
     * given().trustStore("/truststore_javanet.jks", "test1234"). ..
     * </pre>
     * </p>
     *
     * @param pathToJks The path to the JKS
     * @param password  The store pass
     */
    public RequestSpecBuilder setTrustStore(File pathToJks, String password) {
        spec.trustStore(pathToJks, password);
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
     * @see ContentType
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
     * Specify the accept header of the request. This just a shortcut for:
     * <pre>
     * addHeader("Accept", contentType);
     * </pre>
     *
     * @param contentType The content type whose accept header {@link ContentType#getAcceptHeader()} will be used as Accept header in the request.
     * @return The request specification
     * @see ContentType
     * @see #addHeader(String, String)
     */
    public RequestSpecBuilder setAccept(ContentType contentType) {
        spec.accept(contentType);
        return this;
    }

    /**
     * Specify the accept header of the request. This just a shortcut for:
     * <pre>
     * header("Accept", contentType);
     * </pre>
     *
     * @param mediaTypes The media type(s) that will be used as Accept header in the request.
     * @return The request specification
     * @see ContentType
     * @see #addHeader(String, String)
     */
    public RequestSpecBuilder setAccept(String mediaTypes) {
        spec.accept(mediaTypes);
        return this;
    }

    /**
     * Specify a multi-part specification. Use this method if you need to specify content-type etc.
     *
     * @param multiPartSpecification Multipart specification
     * @return The request specification
     */
    public RequestSpecBuilder addMultiPart(MultiPartSpecification multiPartSpecification) {
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
        spec.multiPart(controlName, contentBody, mimeType);
        return this;
    }

    /**
     * If you need to specify some credentials when performing a request.
     *
     * @return The request specification builder
     */
    public RequestSpecBuilder setAuth(AuthenticationScheme auth) {
        spec.setAuthenticationScheme(auth);
        return this;
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
     * Set the session id for this request. It will use the configured session id name from the configuration (by default this is {@value SessionConfig#DEFAULT_SESSION_ID_NAME}).
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
     * Set the session id name and value for this request. It'll override the default session id name from the configuration (by default this is {@value SessionConfig#DEFAULT_SESSION_ID_NAME}).
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
     * Build RequestSpecBuilder.
     *
     * @return The assembled request specification
     */
    public RequestSpecification build() {
        return spec;
    }

    /**
     * Add the baseUri property from the RequestSpecBuilder instead of using static field RestAssured.baseURI.
     * <p/>
     * <pre>
     * RequestSpecBuilder builder = new RequestSpecBuilder();
     * builder.setBaseUri("http://example.com");
     * RequestSpecification specs = builder.build();
     * given().specification(specs)
     * </pre>
     *
     * @param uri The URI
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setBaseUri(String uri) {
        spec.baseUri(uri);
        return this;
    }

    /**
     * Add the baseUri property from the RequestSpecBuilder instead of using static field RestAssured.baseURI.
     * <p/>
     * <pre>
     * RequestSpecification specs = new RequestSpecBuilder()
     *                                  .setBaseUri(URI.create("http://example.com"))
     *                                  .build();
     * given().specification(specs)
     * </pre>
     * uses {@link #setBaseUri(String)}
     *
     * @param uri The URI
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setBaseUri(URI uri) {
        return setBaseUri(notNull(uri, "Base URI").toString());
    }

    /**
     * Set the base path that's prepended to each path by REST assured when making requests. E.g. let's say that
     * the base uri is <code>http://localhost</code> and <code>basePath</code> is <code>/resource</code>
     * then
     * <p/>
     * <pre>
     * ..when().get("/something");
     * </pre>
     * <p/>
     * will make a request to <code>http://localhost/resource</code>.
     *
     * @param path The base path to set.
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setBasePath(String path) {
        spec.basePath(path);
        return this;
    }

    /**
     * Enabled logging with the specified log detail. Set a {@link LogConfig} to configure the print stream and pretty printing options.
     *
     * @param logDetail The log detail.
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder log(LogDetail logDetail) {
        notNull(logDetail, LogDetail.class);
        RestAssuredConfig restAssuredConfig = spec.getConfig();
        LogConfig logConfig;
        if (restAssuredConfig == null) {
            logConfig = new RestAssuredConfig().getLogConfig();
        } else {
            logConfig = restAssuredConfig.getLogConfig();
        }
        PrintStream printStream = logConfig.defaultStream();
        boolean prettyPrintingEnabled = logConfig.isPrettyPrintingEnabled();
        boolean shouldUrlEncodeRequestUri = logConfig.shouldUrlEncodeRequestUri();
        Set<String> blacklistedHeaders = logConfig.blacklistedHeaders();

        spec.filter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri, blacklistedHeaders));
        return this;
    }

    /**
     * Use the supplied truststore for HTTPS requests. Shortcut for:
     * <p>
     * <pre>
     * given().config(RestAssured.config().sslConfig(sslConfig().trustStore(truststore));
     * </pre>
     * </p>
     * <p/>
     *
     * @param trustStore The truststore.
     * @return RequestSpecBuilder
     * @see #setKeyStore(String, String)
     */
    public RequestSpecBuilder setTrustStore(KeyStore trustStore) {
        spec.trustStore(trustStore);
        return this;
    }

    /**
     * Use the supplied keystore for HTTPS requests. Shortcut for:
     * <p>
     * <pre>
     * given().config(RestAssured.config().sslConfig(sslConfig().keyStore(keystore));
     * </pre>
     * </p>
     * <p/>
     *
     * @param keyStore The truststore.
     * @return RequestSpecBuilder
     * @see #setKeyStore(String, String)
     */
    public RequestSpecBuilder setKeyStore(KeyStore keyStore) {
        spec.keyStore(keyStore);
        return this;
    }

    /**
     * Use relaxed HTTP validation with SSLContext protocol {@value #SSL}. This means that you'll trust all hosts regardless if the SSL certificate is invalid. By using this
     * method you don't need to specify a keystore (see {@link #setKeyStore(String, String)} or trust store (see {@link #setTrustStore(java.security.KeyStore)}.
     * <p>
     * This is just a shortcut for:
     * </p>
     * <pre>
     * given().config(RestAssured.config().sslConfig(sslConfig().relaxedHTTPSValidation())). ..;
     * </pre>
     *
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setRelaxedHTTPSValidation() {
        return setRelaxedHTTPSValidation(SSL);
    }

    /**
     * Use relaxed HTTP validation with a given SSLContext protocol. This means that you'll trust all hosts regardless if the SSL certificate is invalid. By using this
     * method you don't need to specify a keystore (see {@link #setKeyStore(String, String)} or trust store (see {@link #setTrustStore(java.security.KeyStore)}.
     * <p>
     * This is just a shortcut for:
     * </p>
     * <pre>
     * given().config(RestAssured.config().sslConfig(sslConfig().relaxedHTTPSValidation())). ..;
     * </pre>
     *
     * @param protocol The standard name of the requested protocol. See the SSLContext section in the <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext">Java Cryptography Architecture Standard Algorithm Name Documentation</a> for information about standard protocol names.
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setRelaxedHTTPSValidation(String protocol) {
        spec.relaxedHTTPSValidation(protocol);
        return this;
    }

    /**
     * Instruct REST Assured to connect to a proxy on the specified host and port.
     *
     * @param host The hostname of the proxy to connect to (for example <code>127.0.0.1</code>)
     * @param port The port of the proxy to connect to (for example <code>8888</code>)
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setProxy(String host, int port) {
        spec.proxy(host, port);
        return this;
    }

    /**
     * Instruct REST Assured to connect to a proxy on the specified host on port <code>8888</code>.
     *
     * @param host The hostname of the proxy to connect to (for example <code>127.0.0.1</code>). Can also be a URI represented as a String.
     * @return RequestSpecBuilder
     * @see #setProxy(String)
     */
    public RequestSpecBuilder setProxy(String host) {
        spec.proxy(host);
        return this;
    }

    /**
     * Instruct REST Assured to connect to a proxy on the specified port on localhost.
     *
     * @param port The port of the proxy to connect to (for example <code>8888</code>)
     * @return RequestSpecBuilder
     * @see #setProxy(int)
     */
    public RequestSpecBuilder setProxy(int port) {
        spec.proxy(port);
        return this;
    }

    /**
     * Instruct REST Assured to connect to a proxy on the specified port on localhost with a specific scheme.
     *
     * @param host   The hostname of the proxy to connect to (for example <code>127.0.0.1</code>)
     * @param port   The port of the proxy to connect to (for example <code>8888</code>)
     * @param scheme The http scheme (http or https)
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setProxy(String host, int port, String scheme) {
        spec.proxy(host, port, scheme);
        return this;
    }

    /**
     * Instruct REST Assured to connect to a proxy using a URI.
     *
     * @param uri The URI of the proxy
     * @return RequestSpecBuilder
     */
    public RequestSpecBuilder setProxy(URI uri) {
        spec.proxy(uri);
        return this;
    }

    /**
     * Instruct REST Assured to connect to a proxy using a {@link ProxySpecification}.
     *
     * @param proxySpecification The proxy specification to use.
     * @return RequestSpecBuilder
     * @see RequestSpecification#proxy(ProxySpecification)
     */
    public RequestSpecBuilder setProxy(ProxySpecification proxySpecification) {
        spec.proxy(proxySpecification);
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return the same RequestSpecBuilder instance
     */
    public RequestSpecBuilder and() {
        return this;
    }
}

/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.specification;

import groovyx.net.http.ContentType;

import java.util.List;
import java.util.Map;

/**
 * Allows you to specify how the request will look like.
 */
public interface RequestSpecification extends RequestSender {

    /**
     * Specify a String request body (such as e.g. JSON or XML) that'll be sent with the request. This works for the
     * POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * given().body("{ \"message\" : \"hello world\"}").then().expect().body(equalTo("hello world")).when().post("/json");
     * </pre>
     * This will POST a request containing JSON to "/json" and expect that the response body equals to "hello world".
     * </p>
     *
     * <p>
     * Note that {@link #body(String)} and {@link #content(String)} are the same except for the syntactic difference.
     * </p>
     *
     * @param body The body to send.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification body(String body);

    /**
     * Specify a byte array request body that'll be sent with the request. This only works for the
     * POST http method. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * byte[] someBytes = ..
     * given().body(someBytes).then().expect().body(equalTo("hello world")).when().post("/json");
     * </pre>
     * This will POST a request containing <code>someBytes</code> to "/json" and expect that the response body equals to "hello world".
     * </p>
     *
     * <p>
     * Note that {@link #body(byte[])} and {@link #content(byte[])} are the same except for the syntactic difference.
     * </p>
     *
     * @param body The body to send.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification body(byte[] body);

    /**
     * Specify a String request content (such as e.g. JSON or XML) that'll be sent with the request. This works for the
     * POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * given().content("{ \"message\" : \"hello world\"}").then().expect().content(equalTo("hello world")).when().post("/json");
     * </pre>
     * This will POST a request containing JSON to "/json" and expect that the response content equals to "hello world".
     * </p>
     *
     * <p>
     * Note that {@link #body(String)} and {@link #content(String)} are the same except for the syntactic difference.
     * </p>
     *
     * @param content The content to send.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification content(String content);

    /**
     * Specify a byte array request content that'll be sent with the request. This only works for the
     * POST http method. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * byte[] someBytes = ..
     * given().content(someBytes).then().expect().content(equalTo("hello world")).when().post("/json");
     * </pre>
     * This will POST a request containing <code>someBytes</code> to "/json" and expect that the response content equals to "hello world".
     * </p>
     *
     * <p>
     * Note that {@link #body(byte[])} and {@link #content(byte[])} are the same except for the syntactic difference.
     * </p>
     *
     * @param content The content to send.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification content(byte[] content);

    /**
     * Specify the cookies that'll be sent with the request. This is done by specifying the cookies in name-value pairs, e.g:
     * <pre>
     * given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().get("/cookie");
     * </pre>
     *
     * This will send a GET request to "/cookie" with two cookies:
     * <ol>
     *   <li>username=John</li>
     *   <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param cookieName The name of the first cookie
     * @param cookieNameValuePairs The value of the first cookie followed by additional cookies in name-value pairs.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification cookies(String cookieName, String...cookieNameValuePairs);

    /**
     * Specify the cookies that'll be sent with the request as Map e.g:
     * <pre>
     * Map&lt;String, String&gt; cookies = new HashMap&lt;String, String&gt;();
     * cookies.put("username", "John");
     * cookies.put("token", "1234");
     * given().cookies(cookies).then().expect().body(equalTo("username, token")).when().get("/cookie");
     * </pre>
     *
     * This will send a GET request to "/cookie" with two cookies:
     * <ol>
     *   <li>username=John</li>
     *   <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param cookies The Map containing the cookie names and their values to set in the request.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification cookies(Map<String, String> cookies);

    /**
     * Specify a cookie that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().cookie("username", "John").and().expect().body(equalTo("username")).when().get("/cookie");
     * </pre>
     * This will set the cookie <code>username=John</code> in the GET request to "/cookie".
     * </p>
     *
     * <p>
     * You can also specify several cookies like this:
     * <pre>
     * given().cookie("username", "John").and().cookie("password", "1234").and().expect().body(equalTo("username")).when().get("/cookie");
     * </pre>
     * </p>
     *
     * @see #cookies(String, String...)
     * @param key The cookie key
     * @param value The cookie value
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification cookie(String key, String value);

    /**
     * Specify the parameters that'll be sent with the request. This is done by specifying the parameters in name-value pairs, e.g:
     * <pre>
     * given().parameters("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().get("/parameters");
     * </pre>
     *
     * This will send a GET request to "/parameters" with two parameters:
     * <ol>
     *   <li>username=John</li>
     *   <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param parameterName The name of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification parameters(String parameterName, String...parameterNameValuePairs);
    /**
     * Specify the parameters that'll be sent with the request as Map e.g:
     * <pre>
     * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
     * parameters.put("username", "John");
     * parameters.put("token", "1234");
     * given().parameters(parameters).then().expect().body(equalTo("username, token")).when().get("/cookie");
     * </pre>
     *
     * This will send a GET request to "/cookie" with two parameters:
     * <ol>
     *   <li>username=John</li>
     *   <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification parameters(Map<String, String> parametersMap);

    /**
     * Specify a parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().parameter("username", "John").and().expect().body(equalTo("username")).when().get("/cookie");
     * </pre>
     * This will set the parameter <code>username=John</code> in the GET request to "/cookie".
     * </p>
     *
     * <p>
     * You can also specify several parameters like this:
     * <pre>
     * given().parameter("username", "John").and().parameter("password", "1234").and().expect().body(equalTo("username")).when().get("/cookie");
     * </pre>
     * </p>
     *
     * @see #parameters(String, String...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification parameter(String parameterName, String parameterValue, String... additionalParameterValues);

    /**
     * Specify a multi-value parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().parameter("cars", asList("Volvo", "Saab"))..;
     * </pre>
     * This will set the parameter <code>cars=Volvo</code> and <code>cars=Saab</code>.
     * </p>
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification parameter(String parameterName, List<String> parameterValues);

    /**
     * A slightly shorter version of {@link #parameters(String, String...)}.
     *
     * @see #parameters(String, String...)
     * @param parameterName The name of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification params(String parameterName, String...parameterNameValuePairs);

    /**
     * A slightly shorter version of {@link #parameters(Map)}.
     *
     * @see #parameters(Map)
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification params(Map<String, String> parametersMap);

    /**
     * A slightly shorter version of {@link #parameter(String, String, String...) }.
     *
     * @see #parameter(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification param(String parameterName, String parameterValue, String... additionalParameterValues);

    /**
     * A slightly shorter version of {@link #parameter(String, java.util.List)}  }.
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification param(String parameterName, List<String> parameterValues);

    /**
     * Specify the query parameters that'll be sent with the request. Note that this method is the same as {@link #parameters(String, String...)}
     * for all http methods except for POST where {@link #parameters(String, String...)} sets the form parameters and this method sets the
     * query parameters.
     *
     * @param parameterName The name of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParameters(String parameterName, String...parameterNameValuePairs);

    /**
     * Specify the query parameters that'll be sent with the request. Note that this method is the same as {@link #parameters(Map)}
     * for all http methods except for POST where {@link #parameters(Map)} sets the form parameters and this method sets the
     * query parameters.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParameters(Map<String, String> parametersMap);

    /**
     * Specify a query parameter that'll be sent with the request. Note that this method is the same as {@link #parameter(String, String, String...)}
     * for all http methods except for POST where {@link #parameter(String, String, String...)} adds a form parameter and this method sets a
     * query parameter.
     *
     * @see #parameter(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParameter(String parameterName, String parameterValue, String... additionalParameterValues);

    /**
     * Specify a multi-value query parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().queryParameter("cars", asList("Volvo", "Saab"))..;
     * </pre>
     * This will set the parameter <code>cars=Volvo</code> and <code>cars=Saab</code>.
     * </p>
     *
     * Note that this method is the same as {@link #parameter(String, java.util.List)}
     * for all http methods except for POST where {@link #parameter(String, java.util.List)} adds a form parameter and
     * this method sets a query parameter.
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParameter(String parameterName, List<String> parameterValues);

    /**
     * A slightly shorter version of {@link #queryParameters(String, String...)}.
     *
     * @see #queryParameters(String, String...)
     * @param parameterName The name of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParams(String parameterName, String...parameterNameValuePairs);

    /**
     * A slightly shorter version of {@link #queryParams(java.util.Map)}.
     *
     * @see #queryParams(java.util.Map)
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParams(Map<String, String> parametersMap);

    /**
     * A slightly shorter version of {@link #queryParameter(String, String, String...)}.
     *
     * @see #parameter(String, String, String...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParam(String parameterName, String parameterValue, String... additionalParameterValues);

    /**
     * A slightly shorter version of {@link #queryParameter(String, java.util.List)}.
     *
     * @see #queryParam(String, java.util.List)
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification queryParam(String parameterName, List<String> parameterValues);

    /**
     * Specify the headers that'll be sent with the request. This is done by specifying the headers in name-value pairs, e.g:
     * <pre>
     * given().headers("headerName1", "headerValue1", "headerName2", "headerValue2").then().expect().body(equalTo("something")).when().get("/headers");
     * </pre>
     *
     * This will send a GET request to "/headers" with two headers:
     * <ol>
     *   <li>headerName1=headerValue1</li>
     *   <li>headerName2=headerValue2</li>
     * </ol>
     * and expect that the response body is equal to "something".
     *
     * @param headerName The name of the first header
     * @param headerNameValuePairs The value of the first header followed by additional headers in name-value pairs.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification headers(String headerName, String ... headerNameValuePairs);

    /**
     * Specify the headers that'll be sent with the request as Map e.g:
     * <pre>
     * Map&lt;String, String&gt; headers = new HashMap&lt;String, String&gt;();
     * parameters.put("headerName1", "headerValue1");
     * parameters.put("headerName2", "headerValue2");
     * given().headers(headers).then().expect().body(equalTo("something")).when().get("/headers");
     * </pre>
     *
     * This will send a GET request to "/headers" with two headers:
     * <ol>
     *   <li>headerName1=headerValue1</li>
     *   <li>headerName2=headerValue2</li>
     * </ol>
     * and expect that the response body is equal to "something".
     *
     * @param headers The Map containing the header names and their values to send with the request.
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification headers(Map<String, String> headers);

    /**
     * Specify a header that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().header("username", "John").and().expect().body(equalTo("something")).when().get("/header");
     * </pre>
     * This will set the header <code>username=John</code> in the GET request to "/header".
     * </p>
     *
     * <p>
     * You can also specify several headers like this:
     * <pre>
     * given().header("username", "John").and().header("zipCode", "12345").and().expect().body(equalTo("something")).when().get("/header");
     * </pre>
     * </p>
     *
     * @see #headers(String, String...)
     * @param headerName The header name
     * @param headerValue The header value
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification header(String headerName, String headerValue);

    /**
     * Specify the content type of the request.
     *
     * @see ContentType
     * @param contentType The content type of the request
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification contentType(ContentType contentType);

    /**
     * If you need to specify some credentials when performing a request.
     *
     * @see com.jayway.restassured.specification.AuthenticationSpecification
     * @return The authentication com.jayway.restassured.specification
     */
    AuthenticationSpecification authentication();

    /**
     * A slightly short version of {@link #authentication()}.
     *
     * @see #authentication()
     * @see com.jayway.restassured.specification.AuthenticationSpecification
     * @return The authentication com.jayway.restassured.specification
     */
    AuthenticationSpecification auth();

    /**
     * Specify the port of the URI. E.g.
     * <p>
     * <pre>
     * given().port(8081).and().expect().statusCode(200).when().get("/something");
     * </pre>
     * will perform a GET request to <tt>http;//localhost:8081/something</tt>. It will override the default port of
     * REST assured for this request only.
     * </p>
     * <p>
     * Note that it's also possible to specify the port like this:
     * <pre>
     * expect().statusCode(200).when().get("http://localhost:8081/something");
     * </pre>
     * </p>
     *
     * @param port The port of URI
     * @return The request com.jayway.restassured.specification
     */
    RequestSpecification port(int port);


    /**
     * Add request data from a pre-defined specification. E.g.
     * <pre>
     * RequestSpecification requestSpec = new RequestSpecBuilder().addParam("parameter1", "value1").build();
     *
     * given().
     *         spec(requestSpec).
     *         param("parameter2", "value2").
     * when().
     *        get("/something");
     * </pre>
     *
     * This is useful when you want to reuse an entire specification across multiple requests.
     * <p>
     * The specification passed to this method is merged with the current specification. Note that the supplied specification
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
     *
     * This method is the same as {@link #specification(RequestSpecification)} but the name is a bit shorter.
     *
     * @param requestSpecificationToMerge The specification to merge with.
     * @return the request specification
     */
    RequestSpecification spec(RequestSpecification requestSpecificationToMerge);

    /**
     * Add request data from a pre-defined specification. E.g.
     * <pre>
     * RequestSpecification requestSpec = new RequestSpecBuilder().addParam("parameter1", "value1").build();
     *
     * given().
     *         spec(requestSpec).
     *         param("parameter2", "value2").
     * when().
     *        get("/something");
     * </pre>
     *
     * This is useful when you want to reuse an entire specification across multiple requests.
     * <p>
     * The specification passed to this method is merged with the current specification. Note that the supplied specification
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
     *
     * This method is the same as {@link #specification(RequestSpecification)} but the name is a bit shorter.
     *
     * @param requestSpecificationToMerge The specification to merge with.
     * @return the request specification
     */
    RequestSpecification specification(RequestSpecification requestSpecificationToMerge);

    /**
     * Returns the response com.jayway.restassured.specification so that you can setup the expectations on the response. E.g.
     * <pre>
     * given().param("name", "value").then().response().body(equalTo("something")).when().get("/something");
     * </pre>
     *
     * @return the response com.jayway.restassured.specification
     */
    ResponseSpecification response();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().body(containsString("OK")).and().body(containsString("something else")).when().get("/something");
     * </pre>
     *
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).body(containsString("something else")).when().get("/something");
     * </pre>
     *
     * @return the request com.jayway.restassured.specification
     */
    RequestSpecification and();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().body(containsString("OK")).and().with().request().parameters("param1", "value1").get("/something");
     * </pre>
     *
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).and().request().parameters("param1", "value1").get("/something");
     * </pre>
     *
     * @return the request com.jayway.restassured.specification
     */
    RequestSpecification with();

    /**
     * Returns the response com.jayway.restassured.specification so that you can setup the expectations on the response. E.g.
     * <pre>
     * given().param("name", "value").then().body(equalTo("something")).when().get("/something");
     * </pre>
     *
     * @return the response com.jayway.restassured.specification
     */
    ResponseSpecification then();

    /**
     * Returns the response com.jayway.restassured.specification so that you can setup the expectations on the response. E.g.
     * <pre>
     * given().param("name", "value").and().expect().body(equalTo("something")).when().get("/something");
     * </pre>
     *
     * @return the response com.jayway.restassured.specification
     */
    ResponseSpecification expect();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().body(containsString("OK")).when().get("/something");
     * </pre>
     *
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).get("/something");
     * </pre>
     *
     * @return the request com.jayway.restassured.specification
     */
    RequestSpecification when();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * given().param("name1", "value1").and().given().param("name2", "value2").when().get("/something");
     * </pre>
     *
     * is that same as:
     * <pre>
     * given().param("name1", "value1").and().param("name2", "value2").when().get("/something");
     * </pre>
     *
     * @return the request com.jayway.restassured.specification
     */
    RequestSpecification given();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * expect().that().body(containsString("OK")).when().get("/something");
     * </pre>
     *
     * is that same as:
     * <pre>
     * expect().body(containsString("OK")).get("/something");
     * </pre>
     *
     * @return the request com.jayway.restassured.specification
     */
    RequestSpecification that();

    /**
     * Syntactic sugar, e.g.
     * <pre>
     * given().request().param("name", "John").then().expect().body(containsString("OK")).when().get("/something");
     * </pre>
     *
     * is that same as:
     * <pre>
     * given().param("name", "John").then().expect().body(containsString("OK")).when().get("/something");
     * </pre>
     *
     * @return the request com.jayway.restassured.specification
     */
    RequestSpecification request();
}
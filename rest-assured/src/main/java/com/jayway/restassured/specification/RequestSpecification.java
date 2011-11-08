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

import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import groovyx.net.http.ContentType;

import java.io.File;
import java.io.InputStream;
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
     * @return The request specification
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
     * @return The request specification
     */
    RequestSpecification body(byte[] body);

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request.
     * If the object is a primitive or <a href="http://download.oracle.com/javase/6/docs/api/java/lang/Number.html">Number</a> the object will
     * be converted to a String and put in the request body. This works for the POST and PUT methods only.
     * Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * Message message = new Message();
     * message.setMessage("My beautiful message");
     *
     * given().
     *         contentType("application/json").
     *         body(message).
     * expect().
     *         content(equalTo("Response to a beautiful message")).
     * when().
     *         post("/beautiful-message");
     * </pre>
     * </p>
     * Since the content-type is "application/json" then REST Assured will automatically try to serialize the object using
     * <a href="http://jackson.codehaus.org/">Jackson</a> or <a href="http://code.google.com/p/google-gson/">Gson</a> if they are
     * available in the classpath. If any of these frameworks are not in the classpath then an exception is thrown.
     * <br />
     * If the content-type is "application/xml" then REST Assured will automatically try to serialize the object using <a href="http://jaxb.java.net/">JAXB</a>
     * if it's available in the classpath. Otherwise an exception will be thrown.
     * <br />
     * If no request content-type is specified then REST Assured determine the parser in the following order:
     * <ol>
     *     <li>Jackson</li>
     *     <li>Gson</li>
     *     <li>JAXB</li>
     * </ol>
     * <p>
     * Note that {@link #body(Object)}  and {@link #content(Object)} are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @return The request specification
     */
    RequestSpecification body(Object object);

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper.
     * This works for the POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
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
     * Note that {@link #body(Object, ObjectMapper)}  and {@link #content(Object, ObjectMapper)} are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @return The request specification
     */
    RequestSpecification body(Object object, ObjectMapper mapper);

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
     * @return The request specification
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
     * @return The request specification
     */
    RequestSpecification content(byte[] content);

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request.
     * If the object is a primitive or <a href="http://download.oracle.com/javase/6/docs/api/java/lang/Number.html">Number</a> the object will
     * be converted to a String and put in the request body. This works for the POST and PUT methods only.
     * Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * Message message = new Message();
     * message.setMessage("My beautiful message");
     *
     * given().
     *         contentType("application/json").
     *         content(message).
     * expect().
     *         content(equalTo("Response to a beautiful message")).
     * when().
     *         post("/beautiful-message");
     * </pre>
     * </p>
     * Since the content-type is "application/json" then REST Assured will automatically try to serialize the object using
     * <a href="http://jackson.codehaus.org/">Jackson</a> or <a href="http://code.google.com/p/google-gson/">Gson</a> if they are
     * available in the classpath. If any of these frameworks are not in the classpath then an exception is thrown.
     * <br />
     * If the content-type is "application/xml" then REST Assured will automatically try to serialize the object using <a href="http://jaxb.java.net/">JAXB</a>
     * if it's available in the classpath. Otherwise an exception will be thrown.
     * <br />
     * If no request content-type is specified then REST Assured determine the parser in the following order:
     * <ol>
     *     <li>Jackson</li>
     *     <li>Gson</li>
     *     <li>JAXB</li>
     * </ol>
     * <p>
     * Note that {@link #body(Object)}  and {@link #content(Object)} are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @return The request specification
     */
    RequestSpecification content(Object object);

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper.
     * This works for the POST and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * Message message = new Message();
     * message.setMessage("My beautiful message");
     *
     * given().
     *         content(message, ObjectMapper.GSON).
     * expect().
     *         content(equalTo("Response to a beautiful message")).
     * when().
     *         post("/beautiful-message");
     * </pre>
     * </p>
     * Note that {@link #body(Object, ObjectMapper)}  and {@link #content(Object, ObjectMapper)} are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @return The request specification
     */
    RequestSpecification content(Object object, ObjectMapper mapper);

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
     *
     * @param firstCookieName The name of the first cookie
     * @param firstCookieValue The value of the first cookie
     * @param cookieNameValuePairs Additional cookies in name-value pairs.
     * @return The request specification
     */
    RequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs);

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
     *
     * @param cookies The Map containing the cookie names and their values to set in the request.
     * @return The request specification
     */
    RequestSpecification cookies(Map<String, ?> cookies);

    /**
     * Specify the cookies that'll be sent with the request as {@link Cookies}:
     * <pre>
     * Cookie cookie1 =  Cookie.Builder("username", "John").setComment("comment 1").build();
     * Cookie cookie2 =  Cookie.Builder("token", 1234).setComment("comment 2").build();
     * Cookies cookies = new Cookies(cookie1, cookie2);
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
     * @param cookies The cookies to set in the request.
     * @return The request specification
     */
    RequestSpecification cookies(Cookies cookies);

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
     * If you specify <code>additionalValues</code> then the Cookie will be a multi-value cookie. This means that you'll create several cookies with the
     * same name but with different values.
     *
     * @see #cookies(String, Object, Object...)
     * @param cookieName The cookie cookieName
     * @param value The cookie value
     * @param additionalValues Additional cookies values. This will actually create two cookies with the same name but with different values.
     * @return The request specification
     */
    RequestSpecification cookie(String cookieName, Object value, Object...additionalValues);

    /**
     * Specify a cookie with no value that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().cookie("some_cookie"").and().expect().body(equalTo("x")).when().get("/cookie");
     * </pre>
     * This will set the cookie <code>some_cookie</code> in the GET request to "/cookie".
     * </p>
     *
     * @see #cookies(String, Object, Object...)
     * @param cookieName The cookie cookieName
     * @return The request specification
     */
    RequestSpecification cookie(String cookieName);

    /**
     * Specify  a {@link Cookie} to send with the request.
     * <p>
     * <pre>
     * Cookie someCookie = new Cookie.Builder("some_cookie"", "some_value").setSecured(true).build();
     * given().cookie(someCookie).and().expect().body(equalTo("x")).when().get("/cookie");
     * </pre>
     * This will set the cookie <code>someCookie</code> in the GET request to "/cookie".
     * </p>
     *
     * @see #cookies(com.jayway.restassured.response.Cookies)
     * @param cookie The cookie to add to the request
     * @return The request specification
     */
    RequestSpecification cookie(Cookie cookie);

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
     *
     *
     *
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs Additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification parameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

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
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    RequestSpecification parameters(Map<String, ?> parametersMap);

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
     * @see #parameters(String, Object, Object...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification
     */
    RequestSpecification parameter(String parameterName, Object parameterValue, Object... additionalParameterValues);

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
     * @return The request specification
     */
    RequestSpecification parameter(String parameterName, List<?> parameterValues);

    /**
     * A slightly shorter version of {@link #parameters(String, Object, Object...)}
     *
     * @see #parameters(String, Object, Object...)
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs Additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification params(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * A slightly shorter version of {@link #parameters(Map)}.
     *
     * @see #parameters(Map)
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    RequestSpecification params(Map<String, ?> parametersMap);

    /**
     * A slightly shorter version of {@link #parameter(String, Object, Object...) }.
     *
     * @see #parameter(String, Object, Object...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification
     */
    RequestSpecification param(String parameterName, Object parameterValue, Object... additionalParameterValues);

    /**
     * A slightly shorter version of {@link #parameter(String, java.util.List)}  }.
     *
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification
     */
    RequestSpecification param(String parameterName, List<?> parameterValues);

    /**
     * Specify the query parameters that'll be sent with the request. Note that this method is the same as {@link #parameters(String, Object, Object...)}
     * for all http methods except for POST where {@link #parameters(String, Object, Object...)} sets the form parameters and this method sets the
     * query parameters.
     *
     *
     *
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification queryParameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * Specify the query parameters that'll be sent with the request. Note that this method is the same as {@link #parameters(Map)}
     * for all http methods except for POST where {@link #parameters(Map)} sets the form parameters and this method sets the
     * query parameters.
     *
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    RequestSpecification queryParameters(Map<String, ?> parametersMap);

    /**
     * Specify a query parameter that'll be sent with the request. Note that this method is the same as {@link #parameter(String, Object, Object...)}
     * for all http methods except for POST where {@link #parameter(String, Object, Object...)} adds a form parameter and this method sets a
     * query parameter.
     *
     * @see #parameter(String, Object, Object...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification
     */
    RequestSpecification queryParameter(String parameterName, Object parameterValue, Object... additionalParameterValues);

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
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification
     */
    RequestSpecification queryParameter(String parameterName, List<?> parameterValues);

    /**
     * A slightly shorter version of {@link #queryParameters(String, Object, Object...)}.
     *
     * @see #queryParameters(String, Object, Object...)
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification queryParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * A slightly shorter version of {@link #queryParams(java.util.Map)}.
     *
     * @see #queryParams(java.util.Map)
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    RequestSpecification queryParams(Map<String, ?> parametersMap);

    /**
     * A slightly shorter version of {@link #queryParameter(String, Object, Object...)}.
     *
     * @see #parameter(String, Object, Object...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification
     */
    RequestSpecification queryParam(String parameterName, Object parameterValue, Object... additionalParameterValues);

    /**
     * A slightly shorter version of {@link #queryParameter(String, java.util.List)}.
     *
     * @see #queryParam(String, java.util.List)
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification
     */
    RequestSpecification queryParam(String parameterName, List<?> parameterValues);

    /**
     * Specify the form parameters that'll be sent with the request. Note that this method is the same as {@link #parameters(String, Object, Object...)}
     * for all http methods except for PUT where {@link #parameters(String, Object, Object...)} sets the query parameters and this method sets the
     * form parameters.
     *
     *
     *
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification formParameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * Specify the form parameters that'll be sent with the request. Note that this method is the same as {@link #parameters(Map)}
     * for all http methods except for PUT where {@link #parameters(Map)} sets the query parameters and this method sets the
     * form parameters.
     *
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    RequestSpecification formParameters(Map<String, ?> parametersMap);

    /**
     * Specify a form parameter that'll be sent with the request. Note that this method is the same as {@link #parameter(String, Object, Object...)}
     * for all http methods except for PUT where {@link #parameter(String, Object, Object...)} adds a query parameter and this method sets a
     * form parameter.
     *
     * @see #parameter(String, Object, Object...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification
     */
    RequestSpecification formParameter(String parameterName, Object parameterValue, Object... additionalParameterValues);

    /**
     * Specify a multi-value form parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().formParameter("cars", asList("Volvo", "Saab"))..;
     * </pre>
     * This will set the parameter <code>cars=Volvo</code> and <code>cars=Saab</code>.
     * </p>
     *
     * Note that this method is the same as {@link #parameter(String, java.util.List)}
     * for all http methods except for PUT where {@link #parameter(String, java.util.List)} adds a query parameter and
     * this method sets a form parameter.
     *
     *
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification
     */
    RequestSpecification formParameter(String parameterName, List<?> parameterValues);

    /**
     * A slightly shorter version of {@link #formParameters(String, Object, Object...)}.
     *
     * @see #formParameters(String, Object, Object...)
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification formParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * A slightly shorter version of {@link #formParams(java.util.Map)}.
     *
     * @see #formParams(java.util.Map)
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    RequestSpecification formParams(Map<String, ?> parametersMap);

    /**
     * A slightly shorter version of {@link #formParameter(String, Object, Object...)}.
     *
     * @see #parameter(String, Object, Object...)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @param additionalParameterValues Additional parameter values if you want to specify multiple values for the same parameter
     * @return The request specification
     */
    RequestSpecification formParam(String parameterName, Object parameterValue, Object... additionalParameterValues);

    /**
     * A slightly shorter version of {@link #formParameter(String, java.util.List)}.
     *
     * @see #formParam(String, java.util.List)
     * @param parameterName The parameter key
     * @param parameterValues The parameter values
     * @return The request specification
     */
    RequestSpecification formParam(String parameterName, List<?> parameterValues);

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
    RequestSpecification pathParameter(String parameterName, Object parameterValue);

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
     *
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs Additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification pathParameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

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
    RequestSpecification pathParameters(Map<String, ?> parameterNameValuePairs);

    /**
     * A slightly shorter version of {@link #pathParameter(String, Object)}.
     *
     * @see #pathParameter(String, Object)
     * @param parameterName The parameter key
     * @param parameterValue The parameter value
     * @return The request specification
     */
    RequestSpecification pathParam(String parameterName, Object parameterValue);

    /**
     * A slightly shorter version of {@link #pathParameters(String, Object, Object...)}.
     *
     * @see #pathParameters(String, Object, Object...)
     * @param firstParameterName The name of the first parameter
     * @param firstParameterValue The value of the first parameter
     * @param parameterNameValuePairs Additional parameters in name-value pairs.
     * @return The request specification
     */
    RequestSpecification pathParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * A slightly shorter version of {@link #pathParameters(java.util.Map)}.
     *
     * @see #pathParameters(java.util.Map)
     * @param parameterNameValuePairs A map containing the path parameters.
     * @return The request specification
     */
    RequestSpecification pathParams(Map<String, ?> parameterNameValuePairs);

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
    RequestSpecification keystore(String pathToJks, String password);

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
     *
     * @param firstHeaderName The name of the first header
     * @param firstHeaderValue The value of the first header
     * @param headerNameValuePairs Additional headers in name-value pairs.
     * @return The request specification
     */
    RequestSpecification headers(String firstHeaderName, Object firstHeaderValue, Object... headerNameValuePairs);

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
     *
     * @param headers The Map containing the header names and their values to send with the request.
     * @return The request specification
     */
    RequestSpecification headers(Map<String, ?> headers);

    /**
     * Specify the headers that'll be sent with the request as {@link Headers}, e.g:
     * <pre>
     * Header first = new Header("headerName1", "headerValue1");
     * Header second = new Header("headerName2", "headerValue2");
     * Headers headers = new Header(first, second);
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
     * @param headers The headers to use in the request
     * @return The request specification
     */
    RequestSpecification headers(Headers headers);

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
     * If you specify <code>additionalHeaderValues</code> then the Header will be a multi-value header. This means that you'll create several headers with the
     * same name but with different values.
     *
     *
     * @see #headers(String, Object, Object...)
     * @param headerName The header name
     * @param headerValue The header value
     * @param additionalHeaderValues Additional header values. This will actually create two headers with the same name but with different values.
     * @return The request specification
     */
    RequestSpecification header(String headerName, Object headerValue, Object...additionalHeaderValues);

    /**
     * Specify  a {@link Header} to send with the request.
     * <p>
     * <pre>
     * Header someHeader = new Header("some_name"", "some_value");
     * given().header(someHeader).and().expect().body(equalTo("x")).when().get("/header");
     * </pre>
     * This will set the header <code>some_name=some_value</code> in the GET request to "/header".
     * </p>
     *
     * @see #headers(com.jayway.restassured.response.Headers)
     * @param header The header to add to the request
     * @return The request specification
     */
    RequestSpecification header(Header header);

    /**
     * Specify the content type of the request.
     *
     * @see ContentType
     * @param contentType The content type of the request
     * @return The request specification
     */
    RequestSpecification contentType(ContentType contentType);

    /**
     * Specify the content type of the request.
     *
     * @see ContentType
     * @param contentType The content type of the request
     * @return The request specification
     */
    RequestSpecification contentType(String contentType);

    /**
     * Specify a file to upload to the server using multi-part form data uploading.
     * It will assume that the control name is <tt>file</tt> and the mime-type is <tt>application/octet-stream</tt>.
     * If this is not what you want please use an overloaded method.
     *
     * @param file The file to upload
     * @return The request specification
     */
    RequestSpecification multiPart(File file);

    /**
     * Specify a file to upload to the server using multi-part form data uploading with a specific
     * control name. It will use the mime-type <tt>application/octet-stream</tt>.
     * If this is not what you want please use an overloaded method.
     *
     * @param file The file to upload
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, File file);

    /**
     * Specify a file to upload to the server using multi-part form data uploading with a specific
     * control name and mime-type.
     *
     * @param file The file to upload
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param mimeType The mime-type
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, File file, String mimeType);

    /**
     * Specify a byte-array to upload to the server using multi-part form data.
     * It will use the mime-type <tt>application/octet-stream</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName The name of the content you're uploading
     * @param bytes The bytes you want to send
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, String fileName, byte[] bytes);

    /**
     * Specify a byte-array to upload to the server using multi-part form data.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName The name of the content you're uploading
     * @param bytes The bytes you want to send
     * @param mimeType The mime-type
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, String fileName, byte[] bytes, String mimeType);

    /**
     * Specify an inputstream to upload to the server using multi-part form data.
     * It will use the mime-type <tt>application/octet-stream</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName The name of the content you're uploading
     * @param stream The stream you want to send
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, String fileName, InputStream stream);

    /**
     * Specify an inputstream to upload to the server using multi-part form data.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName The name of the content you're uploading
     * @param stream The stream you want to send
     * @param mimeType The mime-type
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, String fileName, InputStream stream, String mimeType);

    /**
     * Specify a string to send to the server using multi-part form data.
     * It will use the mime-type <tt>text/plain</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param contentBody The string to send
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, String contentBody);

    /**
     * Specify a string to send to the server using multi-part form data with a specific mime-type.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param contentBody The string to send
     * @param mimeType The mime-type
     * @return The request specification
     */
    RequestSpecification multiPart(String controlName, String contentBody, String mimeType);

    /**
     * If you need to specify some credentials when performing a request.
     *
     * @see AuthenticationSpecification
     * @return The authentication specification
     */
    AuthenticationSpecification authentication();

    /**
     * A slightly short version of {@link #authentication()}.
     *
     * @see #authentication()
     * @see AuthenticationSpecification
     * @return The authentication specification
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
     * @return The request specification
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
     * Specifies if Rest Assured should url encode the URL automatically. Usually this is a recommended but in some cases
     * e.g. the query parameters are already be encoded before you provide them to Rest Assured then it's useful to disable
     * URL encoding.
     * @param isEnabled Specify whether or not URL encoding should be enabled or disabled.
     * @return the request specification
     */
    RequestSpecification urlEncodingEnabled(boolean isEnabled);

    /**
     * Add a filter that will be used in the request.
     *
     * @param filter The filter to add
     * @return the request specification
     */
    RequestSpecification filter(Filter filter);

    /**
     * Add filters that will be used in the request.
     *
     * @param filters The filters to add
     * @return the request specification
     */
    RequestSpecification filters(List<Filter> filters);

    /**
     * Log (i.e. print to system out) the response body to system out. This is mainly useful for debug purposes when writing
     * your tests. A shortcut for:
     * <pre>
     * given().filter(ResponseLoggingFilter.responseLogger()). ..
     * </pre>
     * @return the request specification
     */
    RequestSpecification log();

    /**
     * Log (i.e. print to system out) the response body to system out if an error occurs. This is mainly useful for debug purposes when writing
     * your tests. A shortcut for:
     * <pre>
     * given().filter(ErrorLoggingFilter.errorLogger()). ..
     * </pre>
     * @return the request specification
     */
    RequestSpecification logOnError();

    /**
     * Returns the response specification so that you can setup the expectations on the response. E.g.
     * <pre>
     * given().param("name", "value").then().response().body(equalTo("something")).when().get("/something");
     * </pre>
     *
     * @return the response specification
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
     * @return the request specification
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
     * @return the request specification
     */
    RequestSpecification with();

    /**
     * Returns the response specification so that you can setup the expectations on the response. E.g.
     * <pre>
     * given().param("name", "value").then().body(equalTo("something")).when().get("/something");
     * </pre>
     *
     * @return the response specification
     */
    ResponseSpecification then();

    /**
     * Returns the response specification so that you can setup the expectations on the response. E.g.
     * <pre>
     * given().param("name", "value").and().expect().body(equalTo("something")).when().get("/something");
     * </pre>
     *
     * @return the response specification
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
     * @return the request specification
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
     * @return the request specification
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
     * @return the request specification
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
     * @return the request specification
     */
    RequestSpecification request();
}
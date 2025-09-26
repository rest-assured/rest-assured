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

package io.restassured.module.mockmvc.specification;

import io.restassured.config.SessionConfig;
import io.restassured.http.*;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.intercept.MockHttpServletRequestBuilderInterceptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Allows you to specify how the request will look like.
 */
public interface MockMvcRequestSpecification extends MockMvcRequestSender {

    /**
     * Specify authentication details that'll be used in the request.
     *
     * @return A {@link MockMvcAuthenticationSpecification}.
     */
    MockMvcAuthenticationSpecification auth();

    /**
     * Specify the content type of the request.
     *
     * @param contentType The content type of the request
     * @return The request specification
     * @see ContentType
     */
    MockMvcRequestSpecification contentType(ContentType contentType);

    /**
     * Specify the content type of the request.
     *
     * @param mediaType The content type of the request
     * @return The request specification
     * @see ContentType
     * @see MediaType
     */
    MockMvcRequestSpecification contentType(MediaType mediaType);

    /**
     * Specify the content type of the request.
     *
     * @param contentType The content type of the request
     * @return The request specification
     * @see ContentType
     */
    MockMvcRequestSpecification contentType(String contentType);

    /**
     * Specify the accept header of the request. This just a shortcut for:
     * <pre>
     * header("Accept", contentType);
     * </pre>
     *
     * @param contentType The content type whose accept header {@link ContentType#getAcceptHeader()} will be used as Accept header in the request.
     * @return The request specification
     * @see ContentType
     * @see #header(String, Object, Object...)
     */
    MockMvcRequestSpecification accept(ContentType contentType);

    /**
     * Specify the accept header of the request. This just a shortcut for:
     * <pre>
     * header("Accept", contentType);
     * </pre>
     *
     * @param mediaTypes The media type(s) that will be used as Accept header in the request.
     * @return The request specification
     * @see ContentType
     * @see #header(String, Object, Object...)
     */
    MockMvcRequestSpecification accept(MediaType... mediaTypes);

    /**
     * Specify the accept header of the request. This just a shortcut for:
     * <pre>
     * header("Accept", contentType);
     * </pre>
     *
     * @param mediaTypes The media type(s) that will be used as Accept header in the request.
     * @return The request specification
     * @see ContentType
     * @see #header(String, Object, Object...)
     */
    MockMvcRequestSpecification accept(String mediaTypes);

    /**
     * Specify the headers that'll be sent with the request. This is done by specifying the headers in name-value pairs, e.g:
     * <pre>
     * given().headers("headerName1", "headerValue1", "headerName2", "headerValue2").then().expect().body(equalTo("something")).when().get("/headers");
     * </pre>
     * <p/>
     * This will send a GET request to "/headers" with two headers:
     * <ol>
     * <li>headerName1=headerValue1</li>
     * <li>headerName2=headerValue2</li>
     * </ol>
     * and expect that the response body is equal to "something".
     *
     * @param firstHeaderName      The name of the first header
     * @param firstHeaderValue     The value of the first header
     * @param headerNameValuePairs Additional headers in name-value pairs.
     * @return The request specification
     */
    MockMvcRequestSpecification headers(String firstHeaderName, Object firstHeaderValue, Object... headerNameValuePairs);

    /**
     * Specify the headers that'll be sent with the request as Map e.g:
     * <pre>
     * Map&lt;String, String&gt; headers = new HashMap&lt;String, String&gt;();
     * parameters.put("headerName1", "headerValue1");
     * parameters.put("headerName2", "headerValue2");
     * given().headers(headers).then().expect().body(equalTo("something")).when().get("/headers");
     * </pre>
     * <p/>
     * This will send a GET request to "/headers" with two headers:
     * <ol>
     * <li>headerName1=headerValue1</li>
     * <li>headerName2=headerValue2</li>
     * </ol>
     * and expect that the response body is equal to "something".
     *
     * @param headers The Map containing the header names and their values to send with the request.
     * @return The request specification
     */
    MockMvcRequestSpecification headers(Map<String, ?> headers);

    /**
     * Specify the headers that'll be sent with the request as {@link Headers}, e.g:
     * <pre>
     * Header first = new Header("headerName1", "headerValue1");
     * Header second = new Header("headerName2", "headerValue2");
     * Headers headers = new Header(first, second);
     * given().headers(headers).then().expect().body(equalTo("something")).when().get("/headers");
     * </pre>
     * <p/>
     * This will send a GET request to "/headers" with two headers:
     * <ol>
     * <li>headerName1=headerValue1</li>
     * <li>headerName2=headerValue2</li>
     * </ol>
     * and expect that the response body is equal to "something".
     *
     * @param headers The headers to use in the request
     * @return The request specification
     */
    MockMvcRequestSpecification headers(Headers headers);

    /**
     * Specify a header that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().header("username", "John").and().expect().body(equalTo("something")).when().get("/header");
     * </pre>
     * This will set the header <code>username=John</code> in the GET request to "/header".
     * </p>
     * <p/>
     * <p>
     * You can also specify several headers like this:
     * <pre>
     * given().header("username", "John").and().header("zipCode", "12345").and().expect().body(equalTo("something")).when().get("/header");
     * </pre>
     * </p>
     * <p/>
     * If you specify <code>additionalHeaderValues</code> then the Header will be a multi-value header. This means that you'll create several headers with the
     * same name but with different values.
     *
     * @param headerName             The header name
     * @param headerValue            The header value
     * @param additionalHeaderValues Additional header values. This will actually create two headers with the same name but with different values.
     * @return The request specification
     * @see #headers(String, Object, Object...)
     */
    MockMvcRequestSpecification header(String headerName, Object headerValue, Object... additionalHeaderValues);

    /**
     * Specify  a {@link Header} to send with the request.
     * <p>
     * <pre>
     * Header someHeader = new Header("some_name", "some_value");
     * given().header(someHeader).and().expect().body(equalTo("x")).when().get("/header");
     * </pre>
     * This will set the header <code>some_name=some_value</code> in the GET request to "/header".
     * </p>
     *
     * @param header The header to add to the request
     * @return The request specification
     * @see #headers(Headers)
     */
    MockMvcRequestSpecification header(Header header);

    /**
     * Returns the {@link MockMvcRequestLogSpecification} that allows you to log different parts of the {@link MockMvcRequestSpecification}.
     * This is mainly useful for debug purposes when writing your tests.
     *
     * @return the request log specification
     */
    MockMvcRequestLogSpecification log();

    /**
     * Specify the parameters that'll be sent with the request. This is done by specifying the parameters in name-value pairs, e.g:
     * <pre>
     * given().params("username", "John", "token", "1234").when().get("/parameters").then().assertThat().body(equalTo("username, token"));
     * </pre>
     * <p/>
     * This will send a GET request to "/parameters" with two parameters:
     * <ol>
     * <li>username=John</li>
     * <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param firstParameterName      The name of the first parameter
     * @param firstParameterValue     The value of the first parameter
     * @param parameterNameValuePairs Additional parameters in name-value pairs.
     * @return The request specification
     */
    MockMvcRequestSpecification params(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * Specify the parameters that'll be sent with the request as Map e.g:
     * <pre>
     * Map&lt;String, String&gt; parameters = new HashMap&lt;String, String&gt;();
     * parameters.put("username", "John");
     * parameters.put("token", "1234");
     * given().params(parameters).when().get("/cookie").then().assertThat().body(equalTo("username, token")).;
     * </pre>
     * <p/>
     * This will send a GET request to "/cookie" with two parameters:
     * <ol>
     * <li>username=John</li>
     * <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    MockMvcRequestSpecification params(Map<String, ?> parametersMap);

    /**
     * Specify a parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().parameter("username", "John").and().expect().body(equalTo("username")).when().get("/cookie");
     * </pre>
     * This will set the parameter <code>username=John</code> in the GET request to "/cookie".
     * </p>
     * <p/>
     * <p>
     * You can also specify several parameters like this:
     * <pre>
     * given().param("username", "John").and().param("password", "1234").when().get("/cookie").then().assertThat().body(equalTo("username")).;
     * </pre>
     * </p>
     *
     * @param parameterName   The parameter name
     * @param parameterValues Zero to many parameter values for this parameter name
     * @return The request specification
     * @see #params(String, Object, Object...)
     */
    MockMvcRequestSpecification param(String parameterName, Object... parameterValues);

    /**
     * Specify a multi-value parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().param("cars", asList("Volvo", "Saab"))..;
     * </pre>
     * This will set the parameter <code>cars=Volvo</code> and <code>cars=Saab</code>.
     * </p>
     *
     * @param parameterName   The parameter name
     * @param parameterValues The parameter values
     * @return The request specification
     */
    MockMvcRequestSpecification param(String parameterName, Collection<?> parameterValues);

    /**
     * Specify the query parameters that'll be sent with the request. Note that this method is the same as {@link #params(String, Object, Object...)}
     * for all http methods except for POST where {@link #params(String, Object, Object...)} sets the form parameters and this method sets the
     * query parameters.
     *
     * @param firstParameterName      The name of the first parameter
     * @param firstParameterValue     The value of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    MockMvcRequestSpecification queryParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * Specify the query parameters that'll be sent with the request. Note that this method is the same as {@link #params(Map)}
     * for all http methods except for POST where {@link #params(Map)} sets the form parameters and this method sets the
     * query parameters.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    MockMvcRequestSpecification queryParams(Map<String, ?> parametersMap);

    /**
     * Specify a query parameter that'll be sent with the request. Note that this method is the same as {@link #param(String, Object...)}
     * for all http methods except for POST where {@link #param(String, Object...)} adds a form parameter and this method sets a
     * query parameter.
     *
     * @param parameterName   The parameter name
     * @param parameterValues Zero to many parameter values, i.e. you can specify multiple values for the same parameter
     * @return The request specification
     * @see #param(String, Object...)
     */
    MockMvcRequestSpecification queryParam(String parameterName, Object... parameterValues);

    /**
     * Specify a multi-value query parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().queryParam("cars", asList("Volvo", "Saab"))..;
     * </pre>
     * This will set the parameter <code>cars=Volvo</code> and <code>cars=Saab</code>.
     * </p>
     * <p/>
     * Note that this method is the same as {@link #param(String, java.util.Collection)}
     * for all http methods except for POST where {@link #param(String, java.util.Collection)} adds a form parameter and
     * this method sets a query parameter.
     *
     * @param parameterName   The parameter name
     * @param parameterValues The parameter values
     * @return The request specification
     */
    MockMvcRequestSpecification queryParam(String parameterName, Collection<?> parameterValues);

    /**
     * Adds an optional query parameter to the request if the parameter value is not {@code null}.
     * The parameter name must not be null and must pass the {@code notNull} validation.
     *
     * @param parameterName   The parameter name
     * @param parameterValue The value of the query parameter as an object. If this value is {@code null}, no query parameter is added.
     *                       If not null, the query parameter is added with the given parameter name and value.
     */
    MockMvcRequestSpecification optionalQueryParam(String parameterName, Object parameterValue);

    /**
     * Specify the path parameters that'll be sent with the request.
     *
     * @param firstParameterName      The name of the first parameter
     * @param firstParameterValue     The value of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    MockMvcRequestSpecification pathParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * Specify the path parameters that'll be sent with the request.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    MockMvcRequestSpecification pathParams(Map<String, Object> parametersMap);

    /**
     * Specify a path parameter. Path parameters are used to improve readability of the request path. E.g. instead
     * of writing:
     * <pre>
     * when().
     *        get("/item/"+myItem.getItemNumber()+"/buy/"+2).
     * then().
     *        statusCode(200);
     * </pre>
     * you can write:
     * <pre>
     * given().
     *         pathParam("itemNumber", myItem.getItemNumber()).
     *         pathParam("amount", 2).
     * when().
     *        get("/item/{itemNumber}/buy/{amount}").
     * then().
     *          statusCode(200);
     * </pre>
     * <p/>
     * which improves readability and allows the path to be reusable in many tests. Another alternative is to use:
     * <pre>
     * when().get("/item/{itemNumber}/buy/{amount}", myItem.getItemNumber(), 2).then().statusCode(200).;
     * </pre>
     *
     * @param parameterName  The parameter name
     * @param parameterValue The parameter value
     * @return The request specification
     */
    MockMvcRequestSpecification pathParam(String parameterName, Object parameterValue);

    /**
     * Specify the form parameters that'll be sent with the request. Note that this method is the same as {@link #params(String, Object, Object...)}
     * for all http methods except for POST where {@link #params(String, Object, Object...)} sets the form parameters and this method sets the
     * form parameters.
     *
     * @param firstParameterName      The name of the first parameter
     * @param firstParameterValue     The value of the first parameter
     * @param parameterNameValuePairs The value of the first parameter followed by additional parameters in name-value pairs.
     * @return The request specification
     */
    MockMvcRequestSpecification formParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs);

    /**
     * Specify the form parameters that'll be sent with the request. Note that this method is the same as {@link #params(Map)}
     * for all http methods except for POST where {@link #params(Map)} sets the form parameters and this method sets the
     * form parameters.
     *
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification
     */
    MockMvcRequestSpecification formParams(Map<String, ?> parametersMap);

    /**
     * Specify a form parameter that'll be sent with the request. Note that this method is the same as {@link #param(String, Object...)}
     * for all http methods except for POST where {@link #param(String, Object...)} adds a form parameter and this method sets a
     * form parameter.
     *
     * @param parameterName   The parameter name
     * @param parameterValues Zero to many parameter values, i.e. you can specify multiple values for the same parameter
     * @return The request specification
     * @see #param(String, Object...)
     */
    MockMvcRequestSpecification formParam(String parameterName, Object... parameterValues);

    /**
     * Specify a multi-value form parameter that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().formParam("cars", asList("Volvo", "Saab"))..;
     * </pre>
     * This will set the parameter <code>cars=Volvo</code> and <code>cars=Saab</code>.
     * </p>
     * <p/>
     * Note that this method is the same as {@link #param(String, java.util.Collection)}
     * for all http methods except for POST where {@link #param(String, java.util.Collection)} adds a form parameter and
     * this method sets a form parameter.
     *
     * @param parameterName   The parameter name
     * @param parameterValues The parameter values
     * @return The request specification
     */
    MockMvcRequestSpecification formParam(String parameterName, Collection<?> parameterValues);

    /**
     * Specify a single-value request attribute
     *
     * @param attributeName  The attribute name
     * @param attributeValue The attribute value
     * @return The request specification
     */
    MockMvcRequestSpecification attribute(String attributeName, Object attributeValue);

    /**
     * Specify request attributes as a map
     *
     * @param attributesMap The Map containing the request attribute names and their values
     * @return The request specification
     */
    MockMvcRequestSpecification attributes(Map<String, ?> attributesMap);

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
     * <p/>
     *
     * @param body The body to send.
     * @return The request specification
     */
    MockMvcRequestSpecification body(String body);

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
     * <p/>
     *
     * @param body The body to send.
     * @return The request specification
     */
    MockMvcRequestSpecification body(byte[] body);

    /**
     * Specify file content that'll be sent with the request. This only works for the
     * POST, PATCH and PUT http method. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Example of use:
     * <pre>
     * File myFile = ..
     * given().content(myFile).when().post("/json").then().content(equalTo("hello world"));
     * </pre>
     * This will POST a request containing <code>myFile</code> to "/json" and expect that the response content equals to "hello world".
     * </p>
     * <p/>
     *
     * @param body The content to send.
     * @return The request specification
     */
    MockMvcRequestSpecification body(File body);

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
     * <a href="https://github.com/FasterXML/jackson">Jackson</a> or <a href="https://github.com/google/gson">Gson</a> if they are
     * available in the classpath. If any of these frameworks are not in the classpath then an exception is thrown.
     * <br />
     * If the content-type is "application/xml" then REST Assured will automatically try to serialize the object using <a href="http://jaxb.java.net/">JAXB</a>
     * if it's available in the classpath. Otherwise an exception will be thrown.
     * <br />
     * If no request content-type is specified then REST Assured determine the parser in the following order:
     * <ol>
     * <li>Jackson</li>
     * <li>Gson</li>
     * <li>JAXB</li>
     * </ol>
     *
     * @param object The object to serialize and send with the request
     * @return The request specification
     */
    MockMvcRequestSpecification body(Object object);

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper.
     * This works for the POST, PATCH and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p/>
     * Example of use:
     * <pre>
     * Message message = new Message();
     * message.setMessage("My beautiful message");
     *
     * given().
     *         body(message, new MyObjectMapper()).
     * expect().
     *         content(equalTo("Response to a beautiful message")).
     * when().
     *         post("/beautiful-message");
     * </pre>
     *
     * @param object The object to serialize and send with the request
     * @param mapper The object mapper
     * @return The request specification
     */
    MockMvcRequestSpecification body(Object object, ObjectMapper mapper);

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper type.
     * This works for the POST, PATCH and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p/>
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
     *
     * @param object     The object to serialize and send with the request
     * @param mapperType The object mapper type to be used
     * @return The request specification
     */
    MockMvcRequestSpecification body(Object object, ObjectMapperType mapperType);

    /**
     * Specify the cookies that'll be sent with the request. This is done by specifying the cookies in name-value pairs, e.g:
     * <pre>
     * given().cookies("username", "John", "token", "1234").then().expect().body(equalTo("username, token")).when().get("/cookie");
     * </pre>
     * <p/>
     * This will send a GET request to "/cookie" with two cookies:
     * <ol>
     * <li>username=John</li>
     * <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param firstCookieName      The name of the first cookie
     * @param firstCookieValue     The value of the first cookie
     * @param cookieNameValuePairs Additional cookies in name-value pairs.
     * @return The request specification
     */
    MockMvcRequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs);

    /**
     * Specify the cookies that'll be sent with the request as Map e.g:
     * <pre>
     * Map&lt;String, String&gt; cookies = new HashMap&lt;String, String&gt;();
     * cookies.put("username", "John");
     * cookies.put("token", "1234");
     * given().cookies(cookies).then().expect().body(equalTo("username, token")).when().get("/cookie");
     * </pre>
     * <p/>
     * This will send a GET request to "/cookie" with two cookies:
     * <ol>
     * <li>username=John</li>
     * <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param cookies The Map containing the cookie names and their values to set in the request.
     * @return The request specification
     */
    MockMvcRequestSpecification cookies(Map<String, ?> cookies);

    /**
     * Specify the cookies that'll be sent with the request as {@link Cookies}:
     * <pre>
     * Cookie cookie1 = Cookie.Builder("username", "John").setComment("comment 1").build();
     * Cookie cookie2 = Cookie.Builder("token", 1234).setComment("comment 2").build();
     * Cookies cookies = new Cookies(cookie1, cookie2);
     * given().cookies(cookies).then().expect().body(equalTo("username, token")).when().get("/cookie");
     * </pre>
     * <p/>
     * This will send a GET request to "/cookie" with two cookies:
     * <ol>
     * <li>username=John</li>
     * <li>token=1234</li>
     * </ol>
     * and expect that the response body is equal to "username, token".
     *
     * @param cookies The cookies to set in the request.
     * @return The request specification
     */
    MockMvcRequestSpecification cookies(Cookies cookies);

    /**
     * Specify a cookie that'll be sent with the request e.g:
     * <p>
     * <pre>
     * given().cookie("username", "John").and().expect().body(equalTo("username")).when().get("/cookie");
     * </pre>
     * This will set the cookie <code>username=John</code> in the GET request to "/cookie".
     * </p>
     * <p/>
     * <p>
     * You can also specify several cookies like this:
     * <pre>
     * given().cookie("username", "John").and().cookie("password", "1234").and().expect().body(equalTo("username")).when().get("/cookie");
     * </pre>
     * </p>
     * <p/>
     * If you specify <code>additionalValues</code> then the Cookie will be a multi-value cookie. This means that you'll create several cookies with the
     * same name but with different values.
     *
     * @param cookieName       The cookie cookieName
     * @param value            The cookie value
     * @param additionalValues Additional cookies values. This will actually create two cookies with the same name but with different values.
     * @return The request specification
     * @see #cookies(String, Object, Object...)
     */
    MockMvcRequestSpecification cookie(String cookieName, Object value, Object... additionalValues);

    /**
     * Specify  a {@link Cookie} to send with the request.
     * <p>
     * <pre>
     * Cookie someCookie = new Cookie.Builder("some_cookie", "some_value").setSecured(true).build();
     * given().cookie(someCookie).and().expect().body(equalTo("x")).when().get("/cookie");
     * </pre>
     * This will set the cookie <code>someCookie</code> in the GET request to "/cookie".
     * </p>
     *
     * @param cookie The cookie to add to the request
     * @return The request specification
     * @see #cookies(Cookies)
     */
    MockMvcRequestSpecification cookie(Cookie cookie);

    /**
     * Specify a file to upload to the server using multi-part form data uploading.
     * It will assume that the control name is <tt>file</tt> and the mime-type is <tt>application/octet-stream</tt>.
     * If this is not what you want please use an overloaded method.
     *
     * @param file The file to upload
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(File file);

    /**
     * Specify a file to upload to the server using multi-part form data uploading with a specific
     * control name. It will use the mime-type <tt>application/octet-stream</tt>.
     * If this is not what you want please use an overloaded method.
     *
     * @param file        The file to upload
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, File file);

    /**
     * Specify a file to upload to the server using multi-part form data uploading with a specific
     * control name and mime-type.
     *
     * @param file        The file to upload
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param mimeType    The mime-type
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, File file, String mimeType);

    /**
     * Specify an object that will be serialized to JSON and uploaded to the server using multi-part form data
     * uploading with a specific control name. It will use mime-type <tt>application/json</tt>.
     * If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param object      The object to serialize to JSON or XML and send to the server
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, Object object);

    /**
     * Specify an object that will be serialized and uploaded to the server using multi-part form data
     * uploading with a specific control name.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param object      The object to serialize to JSON or XML and send to the server
     * @param mimeType    The mime-type
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, Object object, String mimeType);

    /**
     * Specify an object that will be serialized and uploaded to the server using multi-part form data
     * uploading with a specific control name.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param filename    The name of the content you're uploading
     * @param object      The object to serialize to JSON or XML and send to the server
     * @param mimeType    The mime-type
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, String filename, Object object, String mimeType);

    /**
     * Specify a byte-array to upload to the server using multi-part form data.
     * It will use the mime-type <tt>application/octet-stream</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param bytes       The bytes you want to send
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, String fileName, byte[] bytes);

    /**
     * Specify a byte-array to upload to the server using multi-part form data.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param bytes       The bytes you want to send
     * @param mimeType    The mime-type
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, String fileName, byte[] bytes, String mimeType);

    /**
     * Specify an inputstream to upload to the server using multi-part form data.
     * It will use the mime-type <tt>application/octet-stream</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param stream      The stream you want to send
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, String fileName, InputStream stream);

    /**
     * Specify an inputstream to upload to the server using multi-part form data.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param fileName    The name of the content you're uploading
     * @param stream      The stream you want to send
     * @param mimeType    The mime-type
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, String fileName, InputStream stream, String mimeType);

    /**
     * Specify a string to send to the server using multi-part form data.
     * It will use the mime-type <tt>text/plain</tt>. If this is not what you want please use an overloaded method.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param contentBody The string to send
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, String contentBody);

    /**
     * Specify a string to send to the server using multi-part form data with a specific mime-type.
     *
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param contentBody The string to send
     * @param mimeType    The mime-type
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, String contentBody, String mimeType);

    /**
     * Define a REST Assured Mock Mvc configuration. E.g.
     * <pre>
     * given().config(newConfig().logConfig(new LogConfig(captor, true))). ..
     * </pre>
     * <p/>
     * <code>newConfig()</code> can be statically imported from {@link RestAssuredMockMvcConfig}.
     *
     * @param config The configuration to use for this request. If <code>null</code> no config will be used.
     * @return The request specification
     */
    MockMvcRequestSpecification config(RestAssuredMockMvcConfig config);

    /**
     * Add request data from a pre-defined specification. E.g.
     * <pre>
     * MockMvcRequestSpecification requestSpec = new MockMvcRequestSpecBuilder().addParam("parameter1", "value1").build();
     *
     * given().
     *         spec(requestSpec).
     *         param("parameter2", "value2").
     * when().
     *        get("/something");
     * </pre>
     * <p/>
     * This is useful when you want to reuse an entire specification across multiple requests.
     * <p/>
     * The specification passed to this method is merged with the current specification. Note that the supplied specification
     * can overwrite data in the current specification. The following settings are overwritten:
     * <ul>
     * <li>Content type</li>
     * <li>Request body</li>
     * <li>Interceptions</li>
     * <li>Log (if defined in <code>requestSpecificationToMerge</code>)</li>
     * <li>Config</li>
     * </ul>
     * The following settings are merged:
     * <ul>
     * <li>Parameters</li>
     * <li>Attributes</li>
     * <li>Cookies</li>
     * <li>Headers</li>
     * </ul>
     * <p/>
     *
     * @param requestSpecificationToMerge The specification to merge with.
     * @return the request specification
     */
    MockMvcRequestSpecification spec(MockMvcRequestSpecification requestSpecificationToMerge);

    /**
     * Set the session id for this request. It will use the configured session id name from the configuration (by default this is {@value SessionConfig#DEFAULT_SESSION_ID_NAME}).
     * You can configure the session id name by using:
     * <pre>
     *     RestAssured.config = newConfig().sessionConfig(new SessionConfig().sessionIdName(&lt;sessionIdName&gt;));
     * </pre>
     * or you can use the {@link #sessionId(String, String)} method to set it for this request only.
     *
     * @param sessionIdValue The session id value.
     * @return The request specification
     */
    MockMvcRequestSpecification sessionId(String sessionIdValue);

    /**
     * Set the session id name and value for this request. It'll override the default session id name from the configuration (by default this is {@value SessionConfig#DEFAULT_SESSION_ID_NAME}).
     * You can configure the default session id name by using:
     * <pre>
     *     RestAssured.config = newConfig().sessionConfig(new SessionConfig().sessionIdName(&lt;sessionIdName&gt;));
     * </pre>
     * and then you can use the {@link #sessionId(String)} method to set the session id value without specifying the name for each request.
     *
     * @param sessionIdName  The session id name
     * @param sessionIdValue The session id value.
     * @return The request specification
     */
    MockMvcRequestSpecification sessionId(String sessionIdName, String sessionIdValue);

    /**
     * Set session attributes.
     *
     * @param sessionAttributes the session attributes
     */
    MockMvcRequestSpecification sessionAttrs(Map<String, Object> sessionAttributes);

    /**
     * Set a session attribute.
     *
     * @param name  the session attribute name
     * @param value the session attribute value
     */
    MockMvcRequestSpecification sessionAttr(String name, Object value);

    /**
     * Call this method when you're done setting up the request specification.
     *
     * @return The {@link MockMvcRequestSender} that let's you send the request.
     */
    MockMvcRequestAsyncSender when();

    /**
     * Build a {@link MockMvc} by registering one or more {@code @Controller}'s
     * instances and configuring Spring MVC infrastructure programmatically.
     * This allows full control over the instantiation and initialization of
     * controllerOrMockMvcConfigurer, and their dependencies, similar to plain unit tests while
     * also making it possible to test one controller at a time.
     * <p/>
     * <p>When this option is used, the minimum infrastructure required by the
     * {@link org.springframework.web.servlet.DispatcherServlet} to serve requests with annotated controllerOrMockMvcConfigurer is
     * automatically created, and can be customized, resulting in configuration
     * that is equivalent to what the MVC Java configuration provides except
     * using builder style methods.
     * <p/>
     * <p>If the Spring MVC configuration of an application is relatively
     * straight-forward, for example when using the MVC namespace or the MVC
     * Java config, then using this builder might be a good option for testing
     * a majority of controllers. A much smaller number of tests can be used
     * to focus on testing and verifying the actual Spring MVC configuration.
     *
     * @param controllerOrMockMvcConfigurer one or more {@link org.springframework.stereotype.Controller @Controller}'s to test
     *                                      or a combination of controllers and {@link MockMvcConfigurer}
     */
    MockMvcRequestSpecification standaloneSetup(Object... controllerOrMockMvcConfigurer);

    /**
     * Build a {@link MockMvc} by using a provided {@code AbstractMockMvcBuilder}
     * for configuring Spring MVC infrastructure programmatically.
     * This allows full control over the instantiation and initialization of
     * controllers, and their dependencies, similar to plain unit tests while
     * also making it possible to test one controller at a time.
     * <p/>
     * <p>If the Spring MVC configuration of an application is relatively
     * straight-forward, for example when using the MVC namespace or the MVC
     * Java config, then using this builder might be a good option for testing
     * a majority of controllers. A much smaller number of tests can be used
     * to focus on testing and verifying the actual Spring MVC configuration.
     *
     * @param builder {@link org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder} to build the MVC mock
     */
    MockMvcRequestSpecification standaloneSetup(MockMvcBuilder builder);

    /**
     * Provide a {@link org.springframework.test.web.servlet.MockMvc} instance to that REST Assured will use when making this request.
     *
     * @param mockMvc The mock mvc instance to use.
     * @return The request specification
     */
    MockMvcRequestSpecification mockMvc(MockMvc mockMvc);

    /**
     * Build a {@link MockMvc} using the given, fully initialized, i.e.
     * refreshed, {@link WebApplicationContext} and assign it to REST Assured.
     * The {@link org.springframework.web.servlet.DispatcherServlet}
     * will use the context to discover Spring MVC infrastructure and
     * application controllers in it. The context must have been configured with
     * a {@link javax.servlet.ServletContext}.
     *
     * @param context            The web application context to use
     * @param mockMvcConfigurers {@link MockMvcConfigurer}'s to be applied when creating a {@link MockMvc} instance of this WebApplicationContext (optional)
     */
    MockMvcRequestSpecification webAppContextSetup(WebApplicationContext context, MockMvcConfigurer... mockMvcConfigurers);

    /**
     * Intercept the {@link org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder} created by REST Assured before it's
     * used to perform the request.
     *
     * @param interceptor The interceptor
     * @return The request specification
     */
    MockMvcRequestSpecification interceptor(MockHttpServletRequestBuilderInterceptor interceptor);

    /**
     * Syntactic sugar
     *
     * @return The same {@link MockMvcRequestSpecification} instance.
     */
    MockMvcRequestSpecification and();

    /**
     * An extension point for further initialization of {@link MockHttpServletRequest}
     * in ways not built directly into the {@code MockHttpServletRequestBuilder}.
     * Implementation of this interface can have builder-style methods themselves
     * and be made accessible through static factory methods.
     * <p>
     * Note that it's recommended to use {@link MockMvcAuthenticationSpecification#with(RequestPostProcessor, RequestPostProcessor...)} instead of this method when setting authentication/authorization based RequestPostProcessors.
     * For example:
     * <pre>
     * given().auth().with(httpBasic("username", "password")). ..
     * </pre>
     * </p>
     *
     * @param postProcessor            a post-processor to add
     * @param additionalPostProcessors Additional post-processors to add
     * @see MockHttpServletRequestBuilder#with(RequestPostProcessor)
     */
    MockMvcRequestSpecification postProcessors(RequestPostProcessor postProcessor, RequestPostProcessor... additionalPostProcessors);
}

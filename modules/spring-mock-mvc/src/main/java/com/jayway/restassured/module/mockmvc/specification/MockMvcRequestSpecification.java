package com.jayway.restassured.module.mockmvc.specification;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.module.mockmvc.config.RestAssuredConfigMockMvc;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.specification.RequestSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

public interface MockMvcRequestSpecification extends RequestSender {
    /**
     * Specify the content type of the request.
     *
     * @param contentType The content type of the request
     * @return The request specification
     * @see com.jayway.restassured.http.ContentType
     */
    MockMvcRequestSpecification contentType(ContentType contentType);

    /**
     * Specify the content type of the request.
     *
     * @param contentType The content type of the request
     * @return The request specification
     * @see ContentType
     */
    MockMvcRequestSpecification contentType(String contentType);

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
     * Specify the headers that'll be sent with the request as {@link com.jayway.restassured.response.Headers}, e.g:
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
     * Specify  a {@link com.jayway.restassured.response.Header} to send with the request.
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
     * @see #headers(com.jayway.restassured.response.Headers)
     */
    MockMvcRequestSpecification header(Header header);

    /**
     * Returns the {@link com.jayway.restassured.module.mockmvc.specification.MockMvcRequestLogSpecification} that allows you to log different parts of the {@link MockMvcRequestSpecification}.
     * This is mainly useful for debug purposes when writing your tests.
     *
     * @return the request log specification
     */
    MockMvcRequestLogSpecification log();

    /**
     * A slightly shorter version of .
     *
     * @param parameterName   The parameter name
     * @param parameterValues Parameter values, one to many if you want to specify multiple values for the same parameter.
     * @return The request specification
     */
    MockMvcRequestSpecification param(String parameterName, Object... parameterValues);

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
     * Specify the cookies that'll be sent with the request as {@link com.jayway.restassured.response.Cookies}:
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
     * Specify  a {@link com.jayway.restassured.response.Cookie} to send with the request.
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
     * @see #cookies(com.jayway.restassured.response.Cookies)
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
     * @param object      The object to serialize to JSON or XML and send to the server
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, Object object);

    /**
     * Specify an object that will be serialized and uploaded to the server using multi-part form data
     * uploading with a specific control name.
     *
     * @param object      The object to serialize to JSON or XML and send to the server
     * @param controlName Defines the control name of the body part. In HTML this is the attribute name of the input tag.
     * @param mimeType    The mime-type
     * @return The request specification
     */
    MockMvcRequestSpecification multiPart(String controlName, Object object, String mimeType);

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
     * <code>newConfig()</code> can be statically imported from {@link com.jayway.restassured.module.mockmvc.config.RestAssuredConfigMockMvc}.
     *
     * @param config The configuration to use for this request. If <code>null</code> no config will be used.
     * @return The request specification
     */
    MockMvcRequestSpecification config(RestAssuredConfigMockMvc config);

    MockMvcRequestSpecification resultHandlers(ResultHandler resultHandler, ResultHandler... resultHandlers);

    RequestSender when();

    MockMvcRequestSpecification standaloneSetup(Object... controllers);

    MockMvcRequestSpecification mockMvc(MockMvc mockMvc);

    MockMvcRequestSpecification webAppContextSetup(WebApplicationContext context);
}

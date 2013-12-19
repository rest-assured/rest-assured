package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.specification.RequestSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

public interface MockMvcRequestSpecification {
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
     * A slightly shorter version of .
     *
     * @param parameterName   The parameter name
     * @param parameterValues Parameter values, one to many if you want to specify multiple values for the same parameter.
     * @return The request specification
     */
    MockMvcRequestSpecification param(String parameterName, Object... parameterValues);

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


    RequestSender when();

    MockMvcRequestSpecification standaloneSetup(Object... controllers);

    MockMvcRequestSpecification mockMvc(MockMvc mockMvc);

    MockMvcRequestSpecification webAppContextSetup(WebApplicationContext context);
}

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

import io.restassured.config.LogConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.intercept.MockHttpServletRequestBuilderInterceptor;
import io.restassured.module.mockmvc.internal.MockMvcFactory;
import io.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * You can use the builder to construct a request specification. The specification can be used as e.g.
 * <pre>
 * MockMvcRequestSpecification requestSpec = new MockMvcRequestSpecBuilder().addParameter("parameter1", "value1").build();
 *
 * given().
 *         spec(requestSpec).
 * when().
 *        get("/something").
 * then().
 *         body("x.y.z", equalTo("something")).
 * </pre>
 */
public class MockMvcRequestSpecBuilder {

    private MockMvcRequestSpecificationImpl spec;

    public MockMvcRequestSpecBuilder() {
        this.spec = (MockMvcRequestSpecificationImpl) new MockMvcRequestSpecificationImpl(getConfiguredMockMvcFactory(), RestAssuredMockMvc.config,
                RestAssuredMockMvc.resultHandlers(), RestAssuredMockMvc.postProcessors(), RestAssuredMockMvc.basePath, RestAssuredMockMvc.requestSpecification,
                RestAssuredMockMvc.responseSpecification, RestAssuredMockMvc.authentication).config(RestAssuredMockMvc.config);
    }

    /**
     * If you need to specify some credentials when performing a request.
     *
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder setAuth(MockMvcAuthenticationScheme auth) {
        auth.authenticate(spec);
        return this;
    }

    /**
     * Set the post processors for this request.
     *
     * @param postProcessors The post processors to use
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder setPostProcessors(RequestPostProcessor postProcessors) {
        spec.postProcessors(postProcessors);
        return this;
    }

    /**
     * Specify a String request body (such as e.g. JSON or XML) to be sent with the request. This works for the
     * POST, PUT and PATCH methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p/>
     *
     * @param body The body to send.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder setBody(String body) {
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
    public MockMvcRequestSpecBuilder setBody(byte[] body) {
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
    public MockMvcRequestSpecBuilder setBody(Object object) {
        spec.body(object);
        return this;
    }

    /**
     * Specify an Object request content that will automatically be serialized to JSON or XML and sent with the request using a specific object mapper.
     * This works for the POST, PATCH and PUT methods only. Trying to do this for the other http methods will cause an exception to be thrown.
     * <p>
     * Note that {@link #setBody(Object, ObjectMapper)}
     * are the same except for the syntactic difference.
     * </p>
     *
     * @param object The object to serialize and send with the request
     * @param mapper The object mapper
     * @return The request specification
     */
    public MockMvcRequestSpecBuilder setBody(Object object, ObjectMapper mapper) {
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
     * Note that {@link #setBody(Object, ObjectMapperType)}
     * are the same except for the syntactic difference.
     * </p>
     *
     * @param object     The object to serialize and send with the request
     * @param mapperType The object mapper type to be used
     * @return The request specification
     */
    public MockMvcRequestSpecBuilder setBody(Object object, ObjectMapperType mapperType) {
        spec.body(object, mapperType);
        return this;
    }

    /**
     * Set session attributes.
     *
     * @param sessionAttributes the session attributes
     */
    MockMvcRequestSpecBuilder addSessionAttrs(Map<String, Object> sessionAttributes) {
        spec.sessionAttrs(sessionAttributes);
        return this;
    }

    /**
     * Set a session attribute.
     *
     * @param name  the session attribute name
     * @param value the session attribute value
     */
    MockMvcRequestSpecBuilder addSessionAttr(String name, Object value) {
        spec.sessionAttr(name, value);
        return this;
    }


    /**
     * Add cookies to be sent with the request as Map e.g:
     *
     * @param cookies The Map containing the cookie names and their values to set in the request.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addCookies(Map<String, ?> cookies) {
        spec.cookies(cookies);
        return this;
    }

    /**
     * Add a detailed cookie
     *
     * @param cookie The cookie to add.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addCookie(Cookie cookie) {
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
    public MockMvcRequestSpecBuilder addCookie(String key, Object value, Object... cookieNameValuePairs) {
        spec.cookie(key, value, cookieNameValuePairs);
        return this;
    }

    /**
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addParams(Map<String, ?> parametersMap) {
        spec.params(parametersMap);
        return this;
    }

    /**
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addParam(String parameterName, Object... parameterValues) {
        spec.param(parameterName, parameterValues);
        return this;
    }

    /**
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addParam(String parameterName, Collection<?> parameterValues) {
        spec.param(parameterName, parameterValues);
        return this;
    }


    /**
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     * @see #addQueryParam(String, Object...)
     */
    public MockMvcRequestSpecBuilder addQueryParam(String parameterName, Collection<?> parameterValues) {
        spec.queryParam(parameterName, parameterValues);
        return this;
    }

    /**
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addQueryParams(Map<String, ?> parametersMap) {
        spec.queryParams(parametersMap);
        return this;
    }

    /**
     * @param parameterName   The parameter key
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addQueryParam(String parameterName, Object... parameterValues) {
        spec.queryParam(parameterName, parameterValues);
        return this;
    }


    /**
     * @param parameterName   The parameter key
     * @param parameterValues The parameter values
     * @return The request specification builder
     * @see #addFormParam(String, Object...)
     */
    public MockMvcRequestSpecBuilder addFormParam(String parameterName, Collection<?> parameterValues) {
        spec.formParam(parameterName, parameterValues);
        return this;
    }

    /**
     * @param parametersMap The Map containing the parameter names and their values to send with the request.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addFormParams(Map<String, ?> parametersMap) {
        spec.formParams(parametersMap);
        return this;
    }

    /**
     * @param parameterName   The parameter name
     * @param parameterValues Zero to many parameter values for this parameter name.
     * @return The request specification builder
     * @see #addFormParam(String, Object...)
     */
    public MockMvcRequestSpecBuilder addFormParam(String parameterName, Object... parameterValues) {
        spec.formParam(parameterName, parameterValues);
        return this;
    }

    /**
     * Add request attribute
     *
     * @param attributeName  The attribute name
     * @param attributeValue The attribute value
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addAttribute(String attributeName, Object attributeValue) {
        spec.attribute(attributeName, attributeValue);
        return this;
    }

    /**
     * Add request attributes
     *
     * @param attributesMap The Map containing the request attribute names and their values
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addAttributes(Map<String, ?> attributesMap) {
        spec.attributes(attributesMap);
        return this;
    }

    /**
     * Add headers to be sent with the request as Map.
     *
     * @param headers The Map containing the header names and their values to send with the request.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addHeaders(Map<String, String> headers) {
        spec.headers(headers);
        return this;
    }

    /**
     * Add a header to be sent with the request
     *
     * @param headerName  The header name
     * @param headerValue The header value
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addHeader(String headerName, String headerValue) {
        spec.header(headerName, headerValue);
        return this;
    }

    /**
     * Add a header to be sent with the request.
     *
     * @param header The header
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addHeader(Header header) {
        spec.header(header);
        return this;
    }

    /**
     * Specify the content type of the request.
     *
     * @param contentType The content type of the request
     * @return The request specification builder
     * @see ContentType
     */
    public MockMvcRequestSpecBuilder setContentType(ContentType contentType) {
        spec.contentType(contentType);
        return this;
    }

    /**
     * Specify the content type of the request as string.
     *
     * @param contentType The content type of the request
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder setContentType(String contentType) {
        spec.contentType(contentType);
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
    public MockMvcRequestSpecBuilder addMultiPart(File file) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, File file) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, File file, String mimeType) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, String fileName, byte[] bytes) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, String fileName, byte[] bytes, String mimeType) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, String fileName, InputStream stream) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, String fileName, InputStream stream, String mimeType) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, String contentBody) {
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
    public MockMvcRequestSpecBuilder addMultiPart(String controlName, String contentBody, String mimeType) {
        spec.multiPart(controlName, mimeType);
        return this;
    }

    /**
     * Set the session id for this request. It will use the configured session id name from the configuration (by default this is {@value SessionConfig#DEFAULT_SESSION_ID_NAME}).
     * You can configure the session id name by using:
     * <pre>
     *     RestAssuredMockMvc.config = newConfig().sessionConfig(new SessionConfig().sessionIdName(&lt;sessionIdName&gt;));
     * </pre>
     * or you can use the {@link #setSessionId(String, String)} method to set it for this request only.
     *
     * @param sessionIdValue The session id value.
     * @return The request specification
     */
    public MockMvcRequestSpecBuilder setSessionId(String sessionIdValue) {
        spec.sessionId(sessionIdValue);
        return this;
    }

    /**
     * Set the session id name and value for this request. It'll override the default session id name from the configuration (by default this is {@value SessionConfig#DEFAULT_SESSION_ID_NAME}).
     * You can configure the default session id name by using:
     * <pre>
     *     RestAssuredMockMvc.config = newConfig().sessionConfig(new SessionConfig().sessionIdName(&lt;sessionIdName&gt;));
     * </pre>
     * and then you can use the {@link MockMvcRequestSpecBuilder#setSessionId(String)} method to set the session id value without specifying the name for each request.
     *
     * @param sessionIdName  The session id name
     * @param sessionIdValue The session id value.
     * @return The request specification
     */
    public MockMvcRequestSpecBuilder setSessionId(String sessionIdName, String sessionIdValue) {
        spec.sessionId(sessionIdName, sessionIdValue);
        return this;
    }

    /**
     * Merge this builder with settings from another specification. Note that the supplied specification
     * can overwrite data in the current specification. The following settings are overwritten:
     * <ul>
     * <li>Content type</li>
     * <li>Request body</li>
     * <li>Interceptors</li>
     * <li>Config (if defined)</li>
     * </ul>
     * The following settings are merged:
     * <ul>
     * <li>Parameters</li>
     * <li>Cookies</li>
     * <li>Headers</li>
     * </ul>
     *
     * @param specification The specification to add
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder addMockMvcRequestSpecification(MockMvcRequestSpecification specification) {
        this.spec.spec(specification);
        return this;
    }

    /**
     * Define a configuration for redirection settings and http client parameters.
     *
     * @param config The configuration to use for this request. If <code>null</code> no config will be used.
     * @return The request specification builder
     */
    public MockMvcRequestSpecBuilder setConfig(RestAssuredMockMvcConfig config) {
        spec.config(config);
        return this;
    }

    /**
     * Build the request specification.
     *
     * @return The assembled request specification
     */
    public MockMvcRequestSpecification build() {
        return spec;
    }

    /**
     * Set the basePath property of the MockMvcRequestSpecBuilder instead of using static field RestAssuredMockMvc.basePath.
     * <p/>
     * <pre>
     * MockMvcRequestSpecBuilder builder = new MockMvcRequestSpecBuilder();
     * builder.setBasePath("/something");
     * MockMvcRequestSpecification specs = builder.build();
     * given().spec(specs)
     * </pre>
     *
     * @param basePath
     * @return MockMvcRequestSpecBuilder
     */
    public MockMvcRequestSpecBuilder setBasePath(String basePath) {
        spec.basePath(basePath);
        return this;
    }

    /**
     * The mock mvc instance to use.
     * <p/>
     * Note that this will override the any {@link MockMvc} instances configured by other setters.*
     *
     * @param mockMvc The mock mvc instance
     * @return MockMvcRequestSpecBuilder
     */
    public MockMvcRequestSpecBuilder setMockMvc(MockMvc mockMvc) {
        spec.mockMvc(mockMvc);
        return this;
    }

    /**
     * The standalone setup to be used by supplying a set of controllers.
     * <p/>
     * Note that this will override the any {@link MockMvc} instances configured by other setters.
     *
     * @param controllers The controllers to use
     * @return MockMvcRequestSpecBuilder
     * @see MockMvcRequestSpecification#standaloneSetup(Object...)
     */
    public MockMvcRequestSpecBuilder setStandaloneSetup(Object... controllers) {
        spec.standaloneSetup(controllers);
        return this;
    }

    /**
     * Initialize with a MockMvcBuilder that will be used to create the {@link MockMvc} instance.
     * <p/>
     * Note that this will override the any {@link MockMvc} instances configured by other setters.
     *
     * @param builder The builder to use
     * @return MockMvcRequestSpecBuilder
     * @see MockMvcRequestSpecification#standaloneSetup(MockMvcBuilder)
     */
    public MockMvcRequestSpecBuilder setStandaloneSetup(MockMvcBuilder builder) {
        spec.standaloneSetup(builder);
        return this;
    }

    /**
     * Initialize with a {@link WebApplicationContext} that will be used to create the {@link MockMvc} instance.
     * <p/>
     * Note that this will override the any {@link MockMvc} instances configured by other setters.
     *
     * @param context            The WebApplicationContext to use
     * @param mockMvcConfigurers {@link MockMvcConfigurer}'s to be applied when creating a {@link MockMvc} instance of this WebApplicationContext (optional)
     * @return MockMvcRequestSpecBuilder
     * @see MockMvcRequestSpecification#webAppContextSetup(WebApplicationContext, MockMvcConfigurer...)
     */
    public MockMvcRequestSpecBuilder setWebAppContextSetup(WebApplicationContext context, MockMvcConfigurer... mockMvcConfigurers) {
        spec.webAppContextSetup(context, mockMvcConfigurers);
        return this;
    }

    /**
     * The mock mvc instance to use.
     *
     * @param interceptor The interceptor
     * @return MockMvcRequestSpecBuilder
     */
    public MockMvcRequestSpecBuilder setMockHttpServletRequestBuilderInterceptor(MockHttpServletRequestBuilderInterceptor interceptor) {
        spec.interceptor(interceptor);
        return this;
    }

    /**
     * Add a result handler
     *
     * @param resultHandler The result handler
     * @return MockMvcRequestSpecBuilder
     */
    public MockMvcRequestSpecBuilder addResultHandlers(ResultHandler resultHandler, ResultHandler... additionalResultHandlers) {
        spec.resultHandlers(resultHandler, additionalResultHandlers);
        return this;
    }

    /**
     * Enabled logging with the specified log detail. Set a {@link LogConfig} to configure the print stream and pretty printing options.
     *
     * @param logDetail The log detail.
     * @return MockMvcRequestSpecBuilder
     */
    public MockMvcRequestSpecBuilder log(LogDetail logDetail) {
        notNull(logDetail, LogDetail.class);
        LogConfig logConfig = spec.getRestAssuredMockMvcConfig().getLogConfig();
        PrintStream printStream = logConfig.defaultStream();
        boolean prettyPrintingEnabled = logConfig.isPrettyPrintingEnabled();
        boolean shouldUrlEncodeRequestUri = logConfig.shouldUrlEncodeRequestUri();
        spec.setRequestLoggingFilter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri));
        return this;
    }

    /**
     * Returns the same MockMvcRequestSpecBuilder instance for syntactic sugar.
     *
     * @return MockMvcRequestSpecBuilder
     */
    public MockMvcRequestSpecBuilder and() {
        return this;
    }

    private static MockMvcFactory getConfiguredMockMvcFactory() {
        try {
            Field mockMvcFactory = RestAssuredMockMvc.class.getDeclaredField("mockMvcFactory");
            mockMvcFactory.setAccessible(true);
            Object instance = mockMvcFactory.get(RestAssuredMockMvc.class);
            mockMvcFactory.setAccessible(false);
            return (MockMvcFactory) instance;
        } catch (Exception e) {
            throw new RuntimeException("Internal error: Cannot find mockMvcFactory field in " + RestAssuredMockMvc.class.getName());
        }
    }
}

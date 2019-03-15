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

package io.restassured.module.mockmvc;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Method;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.internal.MockMvcFactory;
import io.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import io.restassured.module.mockmvc.internal.StandaloneMockMvcFactory;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.specification.*;
import io.restassured.specification.ResponseSpecification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * The Spring MVC module's equivalent of {@link RestAssured}. This is the starting point of the DSL.
 * <p>Note that some Javadoc is copied from Spring MVC's test documentation.</p>
 */
public class RestAssuredMockMvc {

    /**
     * Set a {@link org.springframework.test.web.servlet.MockMvc} instance that REST Assured will use when making requests unless overwritten
     * by a {@link MockMvcRequestSpecification}.
     *
     * @param mockMvc The MockMvc instance to use.
     */
    public static void mockMvc(MockMvc mockMvc) {
        RestAssuredMockMvc.mockMvcFactory = new MockMvcFactory(mockMvc);
    }

    /**
     * Define a REST Assured Mock Mvc configuration. E.g.
     * <pre>
     * given().config(newConfig().logConfig(new LogConfig(captor, true))). ..
     * </pre>
     * <p/>
     * <code>newConfig()</code> can be statically imported from {@link RestAssuredMockMvcConfig}.
     */
    public static RestAssuredMockMvcConfig config;
    /**
     * Specify a default request specification that will be sent with each request. E,g.
     * <pre>
     * RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().addParam("parameter1", "value1").build();
     * </pre>
     * <p/>
     * means that for each request by Rest Assured "parameter1" will be equal to "value1".
     */
    public static MockMvcRequestSpecification requestSpecification;

    /**
     * Specify a default response specification that will be sent with each request. E,g.
     * <pre>
     * RestAssuredMockMvc.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
     * </pre>
     * <p/>
     * means that for each response Rest Assured will assert that the status code is equal to 200.
     */
    public static ResponseSpecification responseSpecification = null;

    private static List<ResultHandler> resultHandlers = new ArrayList<ResultHandler>();

    private static List<RequestPostProcessor> requestPostProcessors = new ArrayList<RequestPostProcessor>();

    private static MockMvcFactory mockMvcFactory = null;

    /**
     * The base path that's used by REST assured when making requests. The base path is prepended to the request path.
     * Default value is <code>/</code>.
     */
    public static String basePath = "/";

    /**
     * Defines a global authentication scheme that'll be used for all requests (if not overridden). Usage example:
     * <pre>
     * RestAssured.authentication = principal(myPrincipal);
     * </pre>
     *
     * @see #principal(java.security.Principal)
     * @see #principal(Object)
     * @see #principalWithCredentials(Object, Object, String...)
     * @see #authentication(Object)
     */
    public static MockMvcAuthenticationScheme authentication;

    /**
     * This is usually the entry-point of the API if you need to specify parameters or a body in the request. For example:
     * <p/>
     * <pre>
     * given().
     *         param("x", "y").
     * when().
     *         get("/something").
     * then().
     *        statusCode(200).
     *        body("x.y", notNullValue());
     * </pre>
     * Note that this method is the same as {@link #with()} but with another syntax.
     *
     * @return A {@link MockMvcRequestSpecification}.
     */
    public static MockMvcRequestSpecification given() {
        return new MockMvcRequestSpecificationImpl(mockMvcFactory, config, resultHandlers, requestPostProcessors, basePath, requestSpecification, responseSpecification, authentication);
    }

    /**
     * This is usually the entry-point of the API if you need to specify parameters or a body in the request. For example:
     * <p/>
     * <pre>
     * given().
     *         param("x", "y").
     * when().
     *         get("/something").
     * then().
     *        statusCode(200).
     *        body("x.y", notNullValue());
     * </pre>
     * <p/>
     * Note that this method is the same as {@link #given()} but with another syntax.
     *
     * @return A {@link MockMvcRequestSpecification}.
     */
    public static MockMvcRequestSpecification with() {
        return given();
    }

    /**
     * This is usually the entry-point of the API if you need to specify parameters or a body in the request. For example:
     * <p/>
     * <pre>
     * when().
     *        get("/x").
     * then().
     *        body("x.y.z1", equalTo("Z1")).
     *        body("x.y.z2", equalTo("Z2"));
     * </pre>
     * <p>
     * Note that if you need to add parameters, headers, cookies or other request properties use the {@link #given()} method.
     * </p>
     *
     * @return A request sender interface that let's you call resources on the server
     */
    public static MockMvcRequestSender when() {
        return given().when();
    }

    /**
     * Build a {@link MockMvc} by registering one or more {@code @Controller}'s
     * instances and configuring Spring MVC infrastructure programmatically.
     * This allows full control over the instantiation and initialization of
     * controllers, and their dependencies, similar to plain unit tests while
     * also making it possible to test one controller at a time.
     * <p/>
     * <p>When this option is used, the minimum infrastructure required by the
     * {@link org.springframework.web.servlet.DispatcherServlet} to serve requests with annotated controllers is
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
     * @param controllersOrMockMvcConfigurers one or more {@link org.springframework.stereotype.Controller @Controller}'s to test as well
     *                                        as @{link MockMvcConfigurer}'s to apply
     */
    public static void standaloneSetup(Object... controllersOrMockMvcConfigurers) {
        mockMvcFactory = StandaloneMockMvcFactory.of(controllersOrMockMvcConfigurers);
    }

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
    public static void standaloneSetup(MockMvcBuilder builder) {
        mockMvcFactory = new MockMvcFactory(builder);
    }

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
    public static void webAppContextSetup(WebApplicationContext context, MockMvcConfigurer... mockMvcConfigurers) {
        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context);  // To avoid compile-time errors
        if (mockMvcConfigurers != null && mockMvcConfigurers.length > 0) {
            for (MockMvcConfigurer mockMvcConfigurer : mockMvcConfigurers) {
                builder.apply(mockMvcConfigurer);
            }
        }
        mockMvcFactory = new MockMvcFactory(builder);
    }

    /**
     * Assign one or more {@link org.springframework.test.web.servlet.ResultHandler} that'll be executes after a request has been made.
     *
     * @param resultHandler  The result handler
     * @param resultHandlers Additional result handlers (optional)
     */
    public static void resultHandlers(ResultHandler resultHandler, ResultHandler... resultHandlers) {
        notNull(resultHandler, ResultHandler.class);
        RestAssuredMockMvc.resultHandlers.add(resultHandler);
        if (resultHandlers != null && resultHandlers.length >= 1) {
            Collections.addAll(RestAssuredMockMvc.resultHandlers, resultHandlers);
        }
    }

    /**
     * @return The defined list of result handlers
     */
    public static List<ResultHandler> resultHandlers() {
        return Collections.unmodifiableList(resultHandlers);
    }

    /**
     * Assign one or more {@link org.springframework.test.web.servlet.ResultHandler} that'll be executes after a request has been made.
     * <p>
     * Note that it's recommended to use {@link #with(RequestPostProcessor, RequestPostProcessor...)} instead of this method when setting
     * authentication/authorization based RequestPostProcessors.
     * </p>
     *
     * @param postProcessor            a post-processor to add
     * @param additionalPostProcessors Additional post-processors to add
     * @see MockMvcRequestSpecification#postProcessors(RequestPostProcessor, RequestPostProcessor...)
     */
    public static void postProcessors(RequestPostProcessor postProcessor, RequestPostProcessor... additionalPostProcessors) {
        notNull(postProcessor, RequestPostProcessor.class);
        RestAssuredMockMvc.requestPostProcessors.add(postProcessor);
        if (additionalPostProcessors != null && additionalPostProcessors.length >= 1) {
            Collections.addAll(RestAssuredMockMvc.requestPostProcessors, additionalPostProcessors);
        }
    }

    /**
     * @return The defined list of request post processors
     */
    public static List<RequestPostProcessor> postProcessors() {
        return Collections.unmodifiableList(requestPostProcessors);
    }

    /**
     * Reset all static configurations to their default values.
     */
    public static void reset() {
        mockMvcFactory = null;
        config = null;
        basePath = "/";
        resultHandlers.clear();
        requestPostProcessors.clear();
        responseSpecification = null;
        requestSpecification = null;
        authentication = null;
    }

    /**
     * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>get("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the GET request.
     */
    public static MockMvcResponse get(String path, Object... pathParams) {
        return given().get(path, pathParams);
    }

    /**
     * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the GET request.
     */
    public static MockMvcResponse get(String path, Map<String, ?> pathParams) {
        return given().get(path, pathParams);
    }

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>post("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static MockMvcResponse post(String path, Object... pathParams) {
        return given().post(path, pathParams);
    }

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static MockMvcResponse post(String path, Map<String, ?> pathParams) {
        return given().post(path, pathParams);
    }

    /**
     * Perform a PUT request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>put("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static MockMvcResponse put(String path, Object... pathParams) {
        return given().put(path, pathParams);
    }

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>delete("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static MockMvcResponse delete(String path, Object... pathParams) {
        return given().delete(path, pathParams);
    }

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static MockMvcResponse delete(String path, Map<String, ?> pathParams) {
        return given().delete(path, pathParams);
    }

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static MockMvcResponse head(String path, Object... pathParams) {
        return given().head(path, pathParams);
    }

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static MockMvcResponse head(String path, Map<String, ?> pathParams) {
        return given().head(path, pathParams);
    }

    /**
     * Perform a PATCH request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static MockMvcResponse patch(String path, Object... pathParams) {
        return given().patch(path, pathParams);
    }

    /**
     * Perform a PATCH request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static MockMvcResponse patch(String path, Map<String, ?> pathParams) {
        return given().patch(path, pathParams);
    }

    /**
     * Perform a OPTIONS request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static MockMvcResponse options(String path, Object... pathParams) {
        return given().options(path, pathParams);
    }

    /**
     * Perform a OPTIONS request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static MockMvcResponse options(String path, Map<String, ?> pathParams) {
        return given().options(path, pathParams);
    }

    /**
     * Perform a GET request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the GET request.
     */
    public static MockMvcResponse get(URI uri) {
        return given().get(uri);
    }

    /**
     * Perform a POST request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse post(URI uri) {
        return given().post(uri);
    }

    /**
     * Perform a PUT request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse put(URI uri) {
        return given().put(uri);
    }

    /**
     * Perform a DELETE request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse delete(URI uri) {
        return given().delete(uri);
    }

    /**
     * Perform a HEAD request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse head(URI uri) {
        return given().head(uri);
    }

    /**
     * Perform a PATCH request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse patch(URI uri) {
        return given().patch(uri);
    }

    /**
     * Perform a OPTIONS request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse options(URI uri) {
        return given().options(uri);
    }

    /**
     * Perform a GET request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the GET request.
     */
    public static MockMvcResponse get(URL url) {
        return given().get(url);
    }

    /**
     * Perform a POST request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse post(URL url) {
        return given().post(url);
    }

    /**
     * Perform a PUT request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse put(URL url) {
        return given().put(url);
    }

    /**
     * Perform a DELETE request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse delete(URL url) {
        return given().delete(url);
    }

    /**
     * Perform a HEAD request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse head(URL url) {
        return given().head(url);
    }

    /**
     * Perform a PATCH request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse patch(URL url) {
        return given().patch(url);
    }

    /**
     * Perform a OPTIONS request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static MockMvcResponse options(URL url) {
        return given().options(url);
    }

    /**
     * Perform a GET request to the statically configured base path.
     *
     * @return The response of the GET request.
     */
    public static MockMvcResponse get() {
        return given().get();
    }

    /**
     * Perform a POST request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static MockMvcResponse post() {
        return given().post();
    }

    /**
     * Perform a PUT request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static MockMvcResponse put() {
        return given().put();
    }

    /**
     * Perform a DELETE request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static MockMvcResponse delete() {
        return given().delete();
    }

    /**
     * Perform a HEAD request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static MockMvcResponse head() {
        return given().head();
    }

    /**
     * Perform a PATCH request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static MockMvcResponse patch() {
        return given().patch();
    }

    /**
     * Perform a OPTIONS request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static MockMvcResponse options() {
        return given().options();
    }
    
    /**
         * Perform a request to the pre-configured path (by default <code>http://localhost:8080</code>).
         *
         * @param method The HTTP method to use
         * @return The response of the request.
         */
        public static MockMvcResponse request(Method method) {
            return given().request(method);
        }
    
        /**
         * Perform a custom HTTP request to the pre-configured path (by default <code>http://localhost:8080</code>).
         *
         * @param method The HTTP method to use
         * @return The response of the request.
         */
        public static MockMvcResponse request(String method) {
            return given().request(method);
        }
    
        /**
         * Perform a HTTP request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
         * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
         *
         * @param method     The HTTP method to use
         * @param path       The path to send the request to.
         * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>request(Method.TRACE,"/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
         * @return The response of the request.
         */
        public static MockMvcResponse request(Method method, String path, Object... pathParams) {
            return given().request(method, path, pathParams);
        }
    
        /**
         * Perform a custom HTTP request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
         * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
         *
         * @param method     The HTTP method to use
         * @param path       The path to send the request to.
         * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>request("method","/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
         * @return The response of the request.
         */
        public static MockMvcResponse request(String method, String path, Object... pathParams) {
            return given().request(method, path, pathParams);
        }
    
        /**
         * Perform a request to a <code>uri</code>.
         *
         * @param method The HTTP method to use
         * @param uri    The uri to send the request to.
         * @return The response of the GET request.
         */
        public static MockMvcResponse request(Method method, URI uri) {
            return given().request(method, uri);
        }
    
        /**
         * Perform a request to a <code>url</code>.
         *
         * @param method The HTTP method to use
         * @param url    The url to send the request to.
         * @return The response of the GET request.
         */
        public static MockMvcResponse request(Method method, URL url) {
            return given().request(method, url);
        }
    
        /**
         * Perform a custom HTTP request to a <code>uri</code>.
         *
         * @param method The HTTP method to use
         * @param uri    The uri to send the request to.
         * @return The response of the GET request.
         */
        public static MockMvcResponse request(String method, URI uri) {
            return given().request(method, uri);
        }
    
        /**
         * Perform a custom HTTP request to a <code>url</code>.
         *
         * @param method The HTTP method to use
         * @param url    The url to send the request to.
         * @return The response of the GET request.
         */
        public static MockMvcResponse request(String method, URL url) {
            return given().request(method, url);
        }

    /**
     * Authenticate using the given principal. Used as:
     * <pre>
     * RestAssured.authentication = principal(myPrincipal);
     * </pre>
     * or in a {@link MockMvcRequestSpecBuilder}:
     * <pre>
     * MockMvcRequestSpecification req = new MockMvcRequestSpecBuilder().setAuth(principal(myPrincipal)). ..
     * </pre>
     *
     * @param principal The principal to use.
     * @return A {@link MockMvcAuthenticationScheme} instance.
     * @see MockMvcAuthenticationSpecification#principal(java.security.Principal)
     */
    public static MockMvcAuthenticationScheme principal(final Principal principal) {
        return new MockMvcAuthenticationScheme() {
            public void authenticate(MockMvcRequestSpecification mockMvcRequestSpecification) {
                mockMvcRequestSpecification.auth().principal(principal);
            }
        };
    }

    /**
     * Authenticate using the given principal. Used as:
     * <pre>
     * RestAssured.authentication = principal(myPrincipal);
     * </pre>
     * or in a {@link MockMvcRequestSpecBuilder}:
     * <pre>
     * MockMvcRequestSpecification req = new MockMvcRequestSpecBuilder().setAuth(principal(myPrincipal)). ..
     * </pre>
     *
     * @param principal The principal to use.
     * @return A {@link MockMvcAuthenticationScheme} instance.
     * @see MockMvcAuthenticationSpecification#principal(Object)
     */
    public static MockMvcAuthenticationScheme principal(final Object principal) {
        return new MockMvcAuthenticationScheme() {
            public void authenticate(MockMvcRequestSpecification mockMvcRequestSpecification) {
                mockMvcRequestSpecification.auth().principal(principal);
            }
        };
    }

    /**
     * Authenticate using the given principal and credentials. Used as:
     * <pre>
     * RestAssured.authentication = principalWithCredentials(myPrincipal, myCredentials);
     * </pre>
     * or in a {@link MockMvcRequestSpecBuilder}:
     * <pre>
     * MockMvcRequestSpecification req = new MockMvcRequestSpecBuilder().setAuth(principalWithCredentials(myPrincipal, myCredentials)). ..
     * </pre>
     *
     * @param principal   The principal to use.
     * @param credentials The credentials to use
     * @param authorities Optional list of authorities
     * @return A {@link MockMvcAuthenticationScheme} instance.
     * @see MockMvcAuthenticationSpecification#principalWithCredentials(Object, Object, String...)
     */
    public static MockMvcAuthenticationScheme principalWithCredentials(final Object principal, final Object credentials, final String... authorities) {
        return new MockMvcAuthenticationScheme() {
            public void authenticate(MockMvcRequestSpecification mockMvcRequestSpecification) {
                mockMvcRequestSpecification.auth().principalWithCredentials(principal, credentials, authorities);
            }
        };
    }

    /**
     * Authenticate using the supplied authentication instance (<code>org.springframework.security.core.Authentication</code> from Spring Security). Used as:
     * <pre>
     * RestAssured.authentication = authentication(myAuth);
     * </pre>
     * or in a {@link MockMvcRequestSpecBuilder}:
     * <pre>
     * MockMvcRequestSpecification req = new MockMvcRequestSpecBuilder().setAuth(authentication(myAuth)). ..
     * </pre>
     *
     * @param authentication The authentication instance to use.
     * @return A {@link MockMvcAuthenticationScheme} instance.
     * @see MockMvcAuthenticationSpecification#authentication(Object)
     */
    public static MockMvcAuthenticationScheme authentication(final Object authentication) {
        return new MockMvcAuthenticationScheme() {
            public void authenticate(MockMvcRequestSpecification mockMvcRequestSpecification) {
                mockMvcRequestSpecification.auth().authentication(authentication);
            }
        };
    }

    /**
     * Authenticate using a {@link RequestPostProcessor}.
     * This is mainly useful when you have added the <code>spring-security-test</code> artifact to classpath. This allows
     * you to do for example:
     * <pre>
     * RestAssured.authentication = with(user("username").password("password"));
     * </pre>
     * where <code>user</code> is statically imported from <code>org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors</code>.
     *
     * @param requestPostProcessor           The first request post processor to be used for authentication
     * @param additionalRequestPostProcessor Additional request post processors to be used for authentication
     * @return A {@link MockMvcAuthenticationScheme} instance.
     */
    public static MockMvcAuthenticationScheme with(final RequestPostProcessor requestPostProcessor, final RequestPostProcessor... additionalRequestPostProcessor) {
        return new MockMvcAuthenticationScheme() {
            public void authenticate(MockMvcRequestSpecification mockMvcRequestSpecification) {
                mockMvcRequestSpecification.auth().with(requestPostProcessor, additionalRequestPostProcessor);
            }
        };
    }

    /**
     * Enable logging of both the request and the response if REST Assureds test validation fails with log detail equal to {@link LogDetail#ALL}.
     * <p/>
     * <p>
     * This is just a shortcut for:
     * </p>
     * <pre>
     * RestAssuredMockMvc.config = new RestAssuredMockMvcConfig().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
     * </pre>
     */
    public static void enableLoggingOfRequestAndResponseIfValidationFails() {
        enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
    }

    /**
     * Enable logging of both the request and the response if REST Assureds test validation fails with the specified log detail.
     * <p/>
     * <p>
     * This is just a shortcut for:
     * </p>
     * <pre>
     * RestAssured.config = new RestAssuredMockMvcConfig().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
     * </pre>
     *
     * @param logDetail The log detail to show in the log
     */
    public static void enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
        config = config == null ? new RestAssuredMockMvcConfig() : config;
        config = config.logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
        // Update request specification if already defined otherwise it'll override the configs.
        // Note that request spec also influence response spec when it comes to logging if validation fails due to the way filters work
        if (requestSpecification != null && requestSpecification instanceof MockMvcRequestSpecificationImpl) {
            RestAssuredMockMvcConfig restAssuredConfig = ((MockMvcRequestSpecificationImpl) requestSpecification).getRestAssuredMockMvcConfig();
            if (restAssuredConfig == null) {
                restAssuredConfig = config;
            } else {
                LogConfig logConfigForRequestSpec = restAssuredConfig.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail);
                restAssuredConfig = restAssuredConfig.logConfig(logConfigForRequestSpec);
            }
            requestSpecification.config(restAssuredConfig);
        }
    }

    /**
     * @return The assigned config or a new config is no config is assigned
     */
    public static RestAssuredMockMvcConfig config() {
        return config == null ? new RestAssuredMockMvcConfig() : config;
    }
}

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
package io.restassured.module.webtestclient;

import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Method;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.internal.*;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSender;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.config.LogConfig.logConfig;

/**
 * The Spring Web Test Client module's equivalent of {@link io.restassured.RestAssured}. This is the starting point of the DSL.
 */
public class RestAssuredWebTestClient {

    /**
     * Define a REST Assured WebTestClient configuration. E.g.
     * <pre>
     * given().config(newConfig().logConfig(new LogConfig(captor, true))). ..
     * </pre>
     * <p/>
     * <code>newConfig()</code> can be statically imported from {@link RestAssuredWebTestClientConfig}.
     */
    public static RestAssuredWebTestClientConfig config;

    /**
     * Specify a default request specification that will be sent with each request. E,g.
     * <pre>
     * RestAssuredWebTestClient.requestSpecification = new WebTestClientRequestSpecBuilder().addParam("parameter1", "value1").build();
     * </pre>
     * <p/>
     * means that for each request by Rest Assured "parameter1" will be equal to "value1".
     */
    public static WebTestClientRequestSpecification requestSpecification;

    /**
     * Specify a default response specification that will be sent with each request. E,g.
     * <pre>
     * RestAssuredWebTestClient.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
     * </pre>
     * <p/>
     * means that for each response Rest Assured will assert that the status code is equal to 200.
     */
    public static ResponseSpecification responseSpecification = null;

    /**
     * The base path that's used by REST assured when making requests. The base path is prepended to the request path.
     * Default value is <code>/</code>.
     */
    public static String basePath = "/";

    private static WebTestClientFactory webTestClientFactory = null;

    /**
     * Set a {@link WebTestClient} instance that REST Assured will use when making requests unless overwritten
     * by a {@link WebTestClientRequestSpecification}.
     *
     * @param webTestClient The WebTestClient instance to use.
     */
    public static void webTestClient(WebTestClient webTestClient) {
        RestAssuredWebTestClient.webTestClientFactory = new WrapperWebTestClientFactory(webTestClient);
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
     * Note that this method is the same as {@link #with()} but with another syntax.
     *
     * @return a {@link WebTestClientRequestSpecification}.
     */
    public static WebTestClientRequestSpecification given() {
        return new WebTestClientRequestSpecificationImpl(webTestClientFactory, config, basePath, requestSpecification,
                responseSpecification);
    }

    /**
     * This is usually the entry-point of the API if you need to specify parameters or a body in the request. For example:
     * Note that this method is the same as {@link #given()} but with another syntax.
     *
     * @return A {@link WebTestClientRequestSpecification}.
     */
    public static WebTestClientRequestSpecification with() {
        return given();
    }

    /**
     * Build a {@link WebTestClient} by registering one or more {@code @Controller}'s
     * instances and configuring WebTestClient programmatically.
     * This allows full control over the instantiation, configuration and initialization of
     * controllers, and their dependencies, similar to plain unit tests while
     * also making it possible to test one controller at a time.
     * <p/>
     * <p>It uses {@link WebTestClient#bindToController(Object...)} under the hood.
     * It also allows you to pass {@link WebTestClientConfigurer} and {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}
     * instances that are used to set up the {@link WebTestClient} instance.
     * <p/>
     *
     * @param controllersOrConfigurersOrExchangeFilterFunctions one or more {@link org.springframework.stereotype.Controller @Controller}s to test,
     *                                                          as well as {@link WebTestClientConfigurer}s
     *                                                          and {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}s to apply.
     */
    public static void standaloneSetup(Object... controllersOrConfigurersOrExchangeFilterFunctions) {
        webTestClientFactory = StandaloneWebTestClientFactory.of(controllersOrConfigurersOrExchangeFilterFunctions);
    }

    /**
     * Build a {@link WebTestClient} by using a provided {@link RouterFunction}
     * for configuring WebTestClient programmatically.
     * This allows full control over the instantiation, configuration and initialization of
     * router functions, and their dependencies, similar to plain unit tests while
     * also making it possible to test one router function at a time.
     * <p>
     * <p/>
     * It uses {@link WebTestClient#bindToRouterFunction(RouterFunction)} under the hood.
     * It also allows you to pass {@link WebTestClientConfigurer} and {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}
     * instances that are used to set up the {@link WebTestClient} instance
     * <p/>
     *
     * @param routerFunction                       {@link RouterFunction} to build WebTestClient.
     * @param configurersOrExchangeFilterFunctions {@link WebTestClientConfigurer}s and {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}s to apply.
     */
    public static void standaloneSetup(RouterFunction routerFunction, Object... configurersOrExchangeFilterFunctions) {
        webTestClientFactory = StandaloneWebTestClientFactory.of(routerFunction, configurersOrExchangeFilterFunctions);
    }

    /**
     * Build a {@link WebTestClient} by using a provided {@link org.springframework.test.web.reactive.server.WebTestClient.Builder}
     * for configuring WebTestClient programmatically.
     * This allows full control over the instantiation and initialization of
     * controllers, and their dependencies, similar to plain unit tests while
     * also making it possible to test one controller at a time.
     *
     * @param builder {@link org.springframework.test.web.reactive.server.WebTestClient.Builder} to build WebTestClient.
     */
    public static void standaloneSetup(WebTestClient.Builder builder) {
        webTestClientFactory = new BuilderBasedWebTestClientFactory(builder);
    }

    /**
     * Build a {@link WebTestClient} using the given, fully initialized, i.e.
     * refreshed, {@link WebApplicationContext} and assign it to REST Assured.
     * <p>
     * The passed {@link WebApplicationContext} will be used as {@link ApplicationContext}.
     *
     * @param context                              The web application context to use
     * @param configurersOrExchangeFilterFunctions {@link WebTestClientConfigurer}s and {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}s to apply.
     */
    public static void webAppContextSetup(WebApplicationContext context, Object... configurersOrExchangeFilterFunctions) {
        webTestClientFactory = StandaloneWebTestClientFactory.of(context, configurersOrExchangeFilterFunctions);
    }

    /**
     * Build a {@link WebTestClient} using the given, fully initialized, i.e.
     * refreshed, {@link ApplicationContext} and assign it to REST Assured.
     *
     * @param context                              The application context to use
     * @param configurersOrExchangeFilterFunctions {@link WebTestClientConfigurer}s and {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}s to apply.
     */
    public static void applicationContextSetup(ApplicationContext context,
                                               Object... configurersOrExchangeFilterFunctions) {
        webTestClientFactory = StandaloneWebTestClientFactory.of(context, configurersOrExchangeFilterFunctions);
    }

    /**
     * Reset all static configurations to their default values.
     */
    public static void reset() {
        webTestClientFactory = null;
        config = null;
        basePath = "/";
        responseSpecification = null;
        requestSpecification = null;
    }

    /**
     * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>get("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse get(String path, Object... pathParams) {
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
    public static WebTestClientResponse get(String path, Map<String, ?> pathParams) {
        return given().get(path, pathParams);
    }

    /**
     * Perform a GET request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse get(Function<UriBuilder, URI> uriFunction) {
        return given().get(uriFunction);
    }

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>post("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static WebTestClientResponse post(String path, Object... pathParams) {
        return given().post(path, pathParams);
    }

    /**
     * Perform a GET request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse get(URI uri) {
        return given().get(uri);
    }

    /**
     * Perform a GET request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse get(URL url) {
        return given().get(url);
    }

    /**
     * Perform a GET request to the statically configured base path.
     *
     * @return The response of the GET request.
     */
    public static WebTestClientResponse get() {
        return given().get();
    }

    /**
     * Perform a request to the pre-configured path (by default <code>http://localhost:8080</code>).
     *
     * @param method The HTTP method to use
     * @return The response of the request.
     */
    public static WebTestClientResponse request(Method method) {
        return given().request(method);
    }

    /**
     * Perform a custom HTTP request to the pre-configured path (by default <code>http://localhost:8080</code>).
     *
     * @param method The HTTP method to use
     * @return The response of the request.
     */
    public static WebTestClientResponse request(String method) {
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
    public static WebTestClientResponse request(Method method, String path, Object... pathParams) {
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
    public static WebTestClientResponse request(String method, String path, Object... pathParams) {
        return given().request(method, path, pathParams);
    }

    /**
     * Perform a request to a <code>uri</code>.
     *
     * @param method The HTTP method to use
     * @param uri    The uri to send the request to.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse request(Method method, URI uri) {
        return given().request(method, uri);
    }

    /**
     * Perform a request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param method The HTTP method to use
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse request(Method method, Function<UriBuilder, URI> uriFunction) {
        return given().request(method, uriFunction);
    }

    /**
     * Perform a request to a <code>url</code>.
     *
     * @param method The HTTP method to use
     * @param url    The url to send the request to.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse request(Method method, URL url) {
        return given().request(method, url);
    }

    /**
     * Perform a custom HTTP request to a <code>uri</code>.
     *
     * @param method The HTTP method to use
     * @param uri    The uri to send the request to.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse request(String method, URI uri) {
        return given().request(method, uri);
    }

    /**
     * Perform a request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param method The HTTP method to use
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse request(String method, Function<UriBuilder, URI> uriFunction) {
        return given().request(method, uriFunction);
    }

    /**
     * Perform a custom HTTP request to a <code>url</code>.
     *
     * @param method The HTTP method to use
     * @param url    The url to send the request to.
     * @return The response of the GET request.
     */
    public static WebTestClientResponse request(String method, URL url) {
        return given().request(method, url);
    }

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static WebTestClientResponse post(String path, Map<String, ?> pathParams) {
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
    public static WebTestClientResponse put(String path, Object... pathParams) {
        return given().put(path, pathParams);
    }

    /**
     * Perform a POST request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the POST request.
     */
    public static WebTestClientResponse post(Function<UriBuilder, URI> uriFunction) {
        return given().post(uriFunction);
    }

    /**
     * Perform a POST request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse post(URI uri) {
        return given().post(uri);
    }

    /**
     * Perform a POST request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse post(URL url) {
        return given().post(url);
    }

    /**
     * Perform a POST request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static WebTestClientResponse post() {
        return given().post();
    }

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>delete("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static WebTestClientResponse delete(String path, Object... pathParams) {
        return given().delete(path, pathParams);
    }

    public static WebTestClientResponse put(String path, Map<String, ?> pathParams) {
        return given().put(path, pathParams);
    }

    /**
     * Perform a PUT request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the PUT request.
     */
    public static WebTestClientResponse put(Function<UriBuilder, URI> uriFunction) {
        return given().put(uriFunction);
    }

    /**
     * Perform a PUT request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse put(URI uri) {
        return given().put(uri);
    }

    public static WebTestClientResponse put(URL url) {
        return given().put(url);
    }

    /**
     * Perform a PUT request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static WebTestClientResponse put() {
        return given().put();
    }

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static WebTestClientResponse delete(String path, Map<String, ?> pathParams) {
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
    public static WebTestClientResponse head(String path, Object... pathParams) {
        return given().head(path, pathParams);
    }

    /**
     * Perform a DELETE request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the DELETE request.
     */
    public static WebTestClientResponse delete(Function<UriBuilder, URI> uriFunction) {
        return given().delete(uriFunction);
    }

    /**
     * Perform a DELETE request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse delete(URI uri) {
        return given().delete(uri);
    }

    /**
     * Perform a DELETE request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse delete(URL url) {
        return given().delete(url);
    }

    /**
     * Perform a DELETE request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static WebTestClientResponse delete() {
        return given().delete();
    }

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static WebTestClientResponse head(String path, Map<String, ?> pathParams) {
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
    public static WebTestClientResponse patch(String path, Object... pathParams) {
        return given().patch(path, pathParams);
    }

    /**
     * Perform a HEAD request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the HEAD request.
     */
    public static WebTestClientResponse head(Function<UriBuilder, URI> uriFunction) {
        return given().head(uriFunction);
    }

    /**
     * Perform a HEAD request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse head(URI uri) {
        return given().head(uri);
    }

    /**
     * Perform a HEAD request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse head(URL url) {
        return given().head(url);
    }

    /**
     * Perform a HEAD request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static WebTestClientResponse head() {
        return given().head();
    }

    /**
     * Perform a PATCH request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    public static WebTestClientResponse patch(String path, Map<String, ?> pathParams) {
        return given().patch(path, pathParams);
    }

    /**
     * Perform a PATCH request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the PATCH request.
     */
    public static WebTestClientResponse patch(Function<UriBuilder, URI> uriFunction) {
        return given().patch(uriFunction);
    }

    /**
     * Perform a PATCH request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse patch(URI uri) {
        return given().patch(uri);
    }

    /**
     * Perform a PATCH request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse patch(URL url) {
        return given().patch(url);
    }

    /**
     * Perform a PATCH request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static WebTestClientResponse patch() {
        return given().patch();
    }

    /**
     * Perform a OPTIONS request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels R Us", 22);</code>.
     * @return The response of the request.
     */
    public static WebTestClientResponse options(String path, Object... pathParams) {
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
    public static WebTestClientResponse options(String path, Map<String, ?> pathParams) {
        return given().options(path, pathParams);
    }

    /**
     * Perform a OPTIONS request to a path generated from the provided {@link Function} <code>uriFunction</code>.
     *
     * @param uriFunction       The {@code Function<UriBuilder, URI>} used to generate the path to send the request to.
     * @return The response of the OPTIONS request.
     */
    public static WebTestClientResponse options(Function<UriBuilder, URI> uriFunction) {
        return given().options(uriFunction);
    }

    /**
     * Perform a OPTIONS request to a <code>uri</code>.
     *
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse options(URI uri) {
        return given().options(uri);
    }

    /**
     * Perform a OPTIONS request to a <code>url</code>.
     *
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    public static WebTestClientResponse options(URL url) {
        return given().options(url);
    }

    /**
     * Perform a OPTIONS request to the statically configured base path.
     *
     * @return The response of the request.
     */
    public static WebTestClientResponse options() {
        return given().options();
    }

    /**
     * Enable logging of both the request and the response if REST Assured test validation fails with log detail equal to {@link LogDetail#ALL}.
     * <p/>
     * <p>
     * This is just a shortcut for:
     * </p>
     * <pre>
     * RestAssuredWebTestClient.config = new RestAssuredWebTestClientConfig().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
     * </pre>
     */
    public static void enableLoggingOfRequestAndResponseIfValidationFails() {
        enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
    }

    /**
     * Enable logging of both the request and the response if REST Assured test validation fails with the specified log detail.
     * <p/>
     * <p>
     * This is just a shortcut for:
     * </p>
     * <pre>
     * RestAssured.config = new RestAssuredWebTestClientConfig().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
     * </pre>
     *
     * @param logDetail The log detail to show in the log
     */
    public static void enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
        config = config == null ? new RestAssuredWebTestClientConfig() : config;
        config = config.logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
        // Update request specification if already defined otherwise it'll override the configs.
        // Note that request spec also influence response spec when it comes to logging if validation fails
        // due to the way filters work
        if (requestSpecification instanceof WebTestClientRequestSpecificationImpl) {
            RestAssuredWebTestClientConfig restAssuredConfig = ((WebTestClientRequestSpecificationImpl) requestSpecification)
                    .getRestAssuredWebTestClientConfig();
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
     * @return A request sender interface that lets you call resources on the server.
     */
    public WebTestClientRequestSender when() {
        return given().when();
    }

    /**
     * @return The assigned config or a new config is no config is assigned
     */
    public static RestAssuredWebTestClientConfig config() {
        return config == null ? new RestAssuredWebTestClientConfig() : config;
    }
}

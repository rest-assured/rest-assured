package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import com.jayway.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import com.jayway.restassured.module.mockmvc.response.MockMvcResponse;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

public class RestAssuredMockMvc {
    public static MockMvc mockMvc = null;
    public static RestAssuredMockMvcConfig config;
    private static List<ResultHandler> resultHandlers = new ArrayList<ResultHandler>();
    /**
     * The base path that's used by REST assured when making requests. The base path is prepended to the request path.
     * Default value is <code>null</code> (which means no base path).
     */
    public static String basePath = null;

    public static MockMvcRequestSpecification given() {
        return new MockMvcRequestSpecificationImpl(mockMvc, config, resultHandlers, basePath);
    }

    public static void standaloneSetup(Object... controllers) {
        mockMvc = MockMvcBuilders.standaloneSetup(controllers).build();
    }

    public static void webAppContextSetup(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    public static void resultHandlers(ResultHandler resultHandler, ResultHandler... resultHandlers) {
        notNull(resultHandler, ResultHandler.class);
        RestAssuredMockMvc.resultHandlers.add(resultHandler);
        if (resultHandlers != null && resultHandlers.length >= 1) {
            Collections.addAll(RestAssuredMockMvc.resultHandlers, resultHandlers);
        }
    }

    public static void reset() {
        mockMvc = null;
        config = null;
        basePath = null;
        resultHandlers.clear();
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
}

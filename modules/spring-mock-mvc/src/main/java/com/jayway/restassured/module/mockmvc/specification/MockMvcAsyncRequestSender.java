package com.jayway.restassured.module.mockmvc.specification;

import com.jayway.restassured.module.mockmvc.response.MockMvcResponse;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface MockMvcAsyncRequestSender {

    /**
     * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>get("/book/{hotelName}/{roomNumber}", "Hotels MockMvcResponse Us", 22);</code>.
     * @return The response of the request.
     */
    MockMvcResponse get(Timeout withTimeout, String path, Object... pathParams);

    /**
     * Perform a GET request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    MockMvcResponse get(Timeout withTimeout, String path, Map<String, ?> pathParams);

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>post("/book/{hotelName}/{roomNumber}", "Hotels MockMvcResponse Us", 22);</code>.
     * @return The response of the request.
     */
    MockMvcResponse post(Timeout withTimeout, String path, Object... pathParams);

    /**
     * Perform a POST request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    MockMvcResponse post(Timeout withTimeout, String path, Map<String, ?> pathParams);

    /**
     * Perform a PUT request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>put("/book/{hotelName}/{roomNumber}", "Hotels MockMvcResponse Us", 22);</code>.
     * @return The response of the request.
     */
    MockMvcResponse put(Timeout withTimeout, String path, Object... pathParams);

    /**
     * Perform a PUT request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    MockMvcResponse put(Timeout withTimeout, String path, Map<String, ?> pathParams);

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>delete("/book/{hotelName}/{roomNumber}", "Hotels MockMvcResponse Us", 22);</code>.
     * @return The response of the request.
     */
    MockMvcResponse delete(Timeout withTimeout, String path, Object... pathParams);

    /**
     * Perform a DELETE request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    MockMvcResponse delete(Timeout withTimeout, String path, Map<String, ?> pathParams);

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels MockMvcResponse Us", 22);</code>.
     * @return The response of the request.
     */
    MockMvcResponse head(Timeout withTimeout, String path, Object... pathParams);

    /**
     * Perform a HEAD request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    MockMvcResponse head(Timeout withTimeout, String path, Map<String, ?> pathParams);

    /**
     * Perform a PATCH request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels MockMvcResponse Us", 22);</code>.
     * @return The response of the request.
     */
    MockMvcResponse patch(Timeout withTimeout, String path, Object... pathParams);

    /**
     * Perform a PATCH request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    MockMvcResponse patch(Timeout withTimeout, String path, Map<String, ?> pathParams);

    /**
     * Perform a OPTIONS request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters. E.g. if path is "/book/{hotelId}/{roomNumber}" you can do <code>head("/book/{hotelName}/{roomNumber}", "Hotels MockMvcResponse Us", 22);</code>.
     * @return The response of the request.
     */
    MockMvcResponse options(Timeout withTimeout, String path, Object... pathParams);


    /**
     * Perform a OPTIONS request to a <code>path</code>. Normally the path doesn't have to be fully-qualified e.g. you don't need to
     * specify the path as <tt>http://localhost:8080/path</tt>. In this case it's enough to use <tt>/path</tt>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param path       The path to send the request to.
     * @param pathParams The path parameters.
     * @return The response of the request.
     */
    MockMvcResponse options(Timeout withTimeout, String path, Map<String, ?> pathParams);

    /**
     * Perform a GET request to a <code>uri</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param uri The uri to send the request to.
     * @return The response of the GET request.
     */
    MockMvcResponse get(Timeout withTimeout, URI uri);

    /**
     * Perform a POST request to a <code>uri</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse post(Timeout withTimeout, URI uri);

    /**
     * Perform a PUT request to a <code>uri</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse put(Timeout withTimeout, URI uri);

    /**
     * Perform a DELETE request to a <code>uri</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse delete(Timeout withTimeout, URI uri);

    /**
     * Perform a HEAD request to a <code>uri</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse head(Timeout withTimeout, URI uri);

    /**
     * Perform a PATCH request to a <code>uri</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse patch(Timeout withTimeout, URI uri);

    /**
     * Perform a OPTIONS request to a <code>uri</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param uri The uri to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse options(Timeout withTimeout, URI uri);

    /**
     * Perform a GET request to a <code>url</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param url The url to send the request to.
     * @return The response of the GET request.
     */
    MockMvcResponse get(Timeout withTimeout, URL url);

    /**
     * Perform a POST request to a <code>url</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse post(Timeout withTimeout, URL url);

    /**
     * Perform a PUT request to a <code>url</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse put(Timeout withTimeout, URL url);

    /**
     * Perform a DELETE request to a <code>url</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse delete(Timeout withTimeout, URL url);

    /**
     * Perform a HEAD request to a <code>url</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse head(Timeout withTimeout, URL url);

    /**
     * Perform a PATCH request to a <code>url</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse patch(Timeout withTimeout, URL url);

    /**
     * Perform a OPTIONS request to a <code>url</code>.
     *
     * @param withTimeout   timeout to wait for async response
     * @param url The url to send the request to.
     * @return The response of the request.
     */
    MockMvcResponse options(Timeout withTimeout, URL url);

    /**
     * Perform a GET request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @param withTimeout   timeout to wait for async response
     * @return The response of the GET request.
     */
    MockMvcResponse get(Timeout withTimeout);

    /**
     * Perform a POST request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @param withTimeout   timeout to wait for async response
     * @return The response of the request.
     */
    MockMvcResponse post(Timeout withTimeout);

    /**
     * Perform a PUT request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @param withTimeout   timeout to wait for async response
     * @return The response of the request.
     */
    MockMvcResponse put(Timeout withTimeout);

    /**
     * Perform a DELETE request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @param withTimeout   timeout to wait for async response
     * @return The response of the request.
     */
    MockMvcResponse delete(Timeout withTimeout);

    /**
     * Perform a HEAD request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @param withTimeout   timeout to wait for async response
     * @return The response of the request.
     */
    MockMvcResponse head(Timeout withTimeout);

    /**
     * Perform a PATCH request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @param withTimeout   timeout to wait for async response
     * @return The response of the request.
     */
    MockMvcResponse patch(Timeout withTimeout);

    /**
     * Perform a OPTIONS request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @param withTimeout   timeout to wait for async response
     * @return The response of the request.
     */
    MockMvcResponse options(Timeout withTimeout);

    /**
     * Perform a GET request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @return The response of the GET request.
     */
    MockMvcResponse get();

    /**
     * Perform a POST request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @return The response of the request.
     */
    MockMvcResponse post();

    /**
     * Perform a PUT request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @return The response of the request.
     */
    MockMvcResponse put();

    /**
     * Perform a DELETE request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @return The response of the request.
     */
    MockMvcResponse delete();

    /**
     * Perform a HEAD request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @return The response of the request.
     */
    MockMvcResponse head();

    /**
     * Perform a PATCH request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @return The response of the request.
     */
    MockMvcResponse patch();

    /**
     * Perform a OPTIONS request to the statically configured path (by default <code>http://localhost:8080</code>).
     *
     * @return The response of the request.
     */
    MockMvcResponse options();


    class Timeout {

        private final long duration;
        private final TimeUnit timeUnit;

        public Timeout(long duration, TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = timeUnit;
        }

        public long getDuration() {
            return duration;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public long getTimeoutInMs() {
            return timeUnit.toMillis(duration);
        }

        public static Timeout withTimeout(long duration, TimeUnit timeUnit) {
            return new Timeout(duration, timeUnit);
        }
    }

}

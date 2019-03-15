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

package io.restassured.response;

import io.restassured.config.SessionConfig;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Headers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface ResponseOptions<T extends ResponseOptions<T>> {
    /**
     * Syntactic sugar, simply returns the same response instance.
     *
     * @return The same response instance.
     */
    T andReturn();

    /**
     * Syntactic sugar, simply returns the same response instance.
     *
     * @return The same response instance.
     */
    T thenReturn();

    /**
     * Returns the response body
     *
     * @return The response body.
     */
    ResponseBody body();

    /**
     * Returns the response body
     *
     * @return The response body.
     */
    ResponseBody getBody();

    /**
     * The response headers. If there are several response headers with the same name a list of
     * the response header values are returned.
     *
     * @return The response headers.
     */
    Headers headers();

    /**
     * The response headers. If there are several response headers with the same name a list of
     * the response header values are returned.
     *
     * @return The response headers.
     */
    Headers getHeaders();

    /**
     * Get a single header value associated with the given name. If the header is a multi-value header then you need to use
     * {@link Headers#getList(String)} in order to get all values.
     *
     * @return The header value or <code>null</code> if value was not found.
     */
    String header(String name);

    /**
     * Get a single header value associated with the given name. If the header is a multi-value header then you need to use
     * {@link Headers#getList(String)} in order to get all values..
     *
     * @return The header value or <code>null</code> if value was not found.
     */
    String getHeader(String name);

    /**
     * The response cookies as simple name/value pair. It assumes that no cookies have the same name. If two cookies should never the less
     * have the same name <i>the Last cookie value</i> is used. If you want to return ALL cookies including all the details such as Max-Age etc use
     * {@link #detailedCookies()}.
     *
     * @return The response cookies.
     */
    Map<String, String> cookies();

    /**
     * The response cookies with all the attributes. It also gives you the possibility to get multi-value cookies.
     *
     * @return The response cookies.
     */
    Cookies detailedCookies();

    /**
     * The response cookies as simple name/value pair. It assumes that no cookies have the same name. If two cookies should never the less
     * have the same name <i>the first cookie value</i> is used. If you want to return ALL cookies inlucding all the details such as Max-Age etc use
     * {@link #getDetailedCookies()}.
     *
     * @return The response cookies.
     */
    Map<String, String> getCookies();

    /**
     * The response cookies with all the attributes. It also gives you the possibility to get multi-value cookies.
     *
     * @return The response cookies.
     */
    Cookies getDetailedCookies();

    /**
     * Get a single cookie <i>value</i> associated with the given name. If you want more details than just
     * the value use {@link #detailedCookie(String)}.
     *
     * @return The cookie value or <code>null</code> if value was not found.
     */
    String cookie(String name);

    /**
     * Get a single cookie <i>value</i> associated with the given name. If you want more details than just
     * the value use {@link #getDetailedCookie(String)}.
     *
     * @return The cookie value or <code>null</code> if value was not found.
     */
    String getCookie(String name);

    /**
     * Get a single cookie including all attributes associated with the given name.
     *
     * @return The cookie value or <code>null</code> if value was not found.
     */
    Cookie detailedCookie(String name);

    /**
     * Get a single cookie including all attributes associated with the given name.
     *
     * @return The cookie value or <code>null</code> if value was not found.
     */
    Cookie getDetailedCookie(String name);

    /**
     * Get the content type of the response
     *
     * @return The content type value or <code>null</code> if not found.
     */
    String contentType();

    /**
     * Get the content type of the response
     *
     * @return The content type value or <code>null</code> if not found.
     */
    String getContentType();

    /**
     * Get the status line of the response.
     *
     * @return The status line of the response.
     */
    String statusLine();

    /**
     * Get the status line of the response.
     *
     * @return The status line of the response.
     */
    String getStatusLine();

    /**
     * Get the session id from the response. The session id name can be configured from the {@link SessionConfig}.
     *
     * @return The session id of the response or <code>null</code> if not defined.
     */
    String sessionId();

    /**
     * Get the session id from the response. The session id name can be configured from the {@link SessionConfig}.
     *
     * @return The session id of the response or <code>null</code> if not defined.
     */
    String getSessionId();

    /**
     * Get the status code of the response.
     *
     * @return The status code of the response.
     */
    int statusCode();

    /**
     * Get the status code of the response.
     *
     * @return The status code of the response.
     */
    int getStatusCode();

    /**
     * @return The response time in milliseconds (or -1 if no response time could be measured)
     */
    long time();

    /**
     * @return The response time in the given time unit (or -1 if no response time could be measured)
     */
    long timeIn(TimeUnit timeUnit);

    /**
     * @return The response time in milliseconds (or -1 if no response time could be measured)
     * @see #time()
     */
    long getTime();

    /**
     * @return The response time in the given time unit (or -1 if no response time could be measured)
     * @see #time()
     */
    long getTimeIn(TimeUnit timeUnit);
}

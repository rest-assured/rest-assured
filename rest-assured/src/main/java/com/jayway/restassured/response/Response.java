/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.response;

import java.util.Map;

/**
 * The response of a request made by REST Assured.
 * <p>
 * The response can only be returned if you don't use any expectations on the body. E.g.
 * <pre>
 *   expect().body(equalTo("my body")).when().get("/something").asString()
 * </pre>
 * will throw an {@link IllegalStateException} because of the <code>body</code> expectation matcher.
 *
 * </p>
 */
public interface Response extends ResponseBody {

    /**
     * Syntactic sugar, simply returns the same response instance.
     *
     * @return The same response instance.
     */
    Response andReturn();

    /**
     * Syntactic sugar, simply returns the same response instance.
     *
     * @return The same response instance.
     */
    Response thenReturn();

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
     * The response headers.
     *
     * @return The response headers.
     */
    Map<String, String> headers();

    /**
     * The response headers.
     *
     * @return The response headers.
     */
    Map<String, String> getHeaders();

    /**
     * Get a single header value associated with the given name.
     *
     * @return The header value or <code>null</code> if value was not found.
     */
    String header(String name);

    /**
     * Get a single header value associated with the given name.
     *
     * @return The header value or <code>null</code> if value was not found.
     */
    String getHeader(String name);

    /**
     * The response cookies.
     *
     * @return The response cookies.
     */
    Map<String, String> cookies();

    /**
     * The response cookies.
     *
     * @return The response cookies.
     */
    Map<String, String> getCookies();

    /**
     * Get a single cookie value associated with the given name.
     *
     * @return The cookie value or <code>null</code> if value was not found.
     */
    String cookie(String name);

    /**
     * Get a single cookie value associated with the given name.
     *
     * @return The cookie value or <code>null</code> if value was not found.
     */
    String getCookie(String name);
}

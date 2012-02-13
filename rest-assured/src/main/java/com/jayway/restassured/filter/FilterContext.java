/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.filter;

import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import com.jayway.restassured.specification.RequestSender;

/**
 * Provides the functionality to set properties, sending requests and continue the filter chain.
 */
public interface FilterContext {

    /**
     * Add a value that may be used be subsequent filters.
     *
     * @param name The name of the value
     * @param value The value itself
     */
    void setValue(String name, Object value);

    /**
     * Get a value

     * @param name The name of the value
     * @param <T> The type of the value
     * @return The value itself or <code>null</code> if no value was found for the supplied name.
     */
    <T> T getValue(String name);

    /**
     * Send a request to the same request path and with the same request method as the original request.
     *
     * @param requestSender The response or request specification.
     * @return The response.
     */
    Response send(RequestSender requestSender);

    /**
     * @return The request method of the request (E.g. POST, GET etc)
     */
    Method getRequestMethod();

    /**
     * @return The request path
     */
    String getRequestPath();

    /**
     * @return The complete request path. This is the fully-qualified path including port number and scheme.
     */
    String getCompleteRequestPath();

    /**
     * Continue to the next filter in the chain.
     *
     * @param request The request specification
     * @param response The response specification
     * @return The response of the request
     */
    Response next(FilterableRequestSpecification request, FilterableResponseSpecification response);
}
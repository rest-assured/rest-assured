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

package com.jayway.restassured.specification;

import com.jayway.restassured.authentication.AuthenticationScheme;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Headers;

import java.util.List;
import java.util.Map;

/**
 * A request specification that also supports getting the defined values. Intended for Filters.
 */
public interface FilterableRequestSpecification extends RequestSpecification {

    /**
     * @return The base URI defined in the request
     */
    String getBaseUri();

    /**
     * @return The base path defined in the request
     */
    String getBasePath();

    /**
     * @return The port defined in the request
     */
    int getPort();

    /**
     * @return The request content type defined in the request
     */
    String getRequestContentType();

    /**
     * @return The authentication scheme defined in the request
     */
    AuthenticationScheme getAuthenticationScheme();

    /**
     * @return The request parameters defined in the request
     */
    Map<String, String> getRequestParams();

    /**
     * @return The query parameters defined in the request
     */
    Map<String, String> getQueryParams();

    /**
     * @return The headers defined in the request
     */
    Headers getHeaders();

    /**
     * @return The cookies defined in the request
     */
    Cookies getCookies();

    /**
     * @return The request body
     */
    <T> T getBody();

    /**
     * @return The filters (unmodifiable)
     */
    List<Filter> getDefinedFilters();
}

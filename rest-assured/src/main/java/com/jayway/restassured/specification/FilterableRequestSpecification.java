/*
 * Copyright 2013 the original author or authors.
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
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Headers;
import org.apache.http.client.HttpClient;

import java.util.List;
import java.util.Map;

/**
 * A request specification that also supports getting the defined values. Intended for Filters.
 */
public interface FilterableRequestSpecification extends RequestSpecification {

    /**
     * @return The base URI defined in the request specification
     */
    String getBaseUri();

    /**
     * @return The base path defined in the request specification
     */
    String getBasePath();

    /**
     * @return The port defined in the request specification
     */
    int getPort();

    /**
     * @return The request content type defined in the request specification
     */
    String getRequestContentType();

    /**
     * @return The authentication scheme defined in the request specification
     */
    AuthenticationScheme getAuthenticationScheme();

    /**
     * @return The request parameters defined in the request specification
     */
    Map<String, ?> getRequestParams();

    /**
     * @return The form parameters defined in the request specification
     */
    Map<String, ?> getFormParams();

    /**
     * @return The (named) path parameters defined in the request specification
     */
    Map<String, ?> getPathParams();

    /**
     * @return The query parameters defined in the request specification
     */
    Map<String, ?> getQueryParams();

    /**
     * @return The headers defined in the request specification
     */
    Headers getHeaders();

    /**
     * @return The cookies defined in the request specification
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

    /**
     * @return The Rest Assured configurations
     */
    RestAssuredConfig getConfig();

    /**
     * @return The underlying http client. Only use this for advanced configuration which is not accessible from Rest Assured! By default an instance of {@link org.apache.http.impl.client.AbstractHttpClient} is used by REST Assured.
     */
    HttpClient getHttpClient();
}
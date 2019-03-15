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

package io.restassured.specification;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Headers;

/**
 * A request specification that also supports getting and changing the defined values. Intended for Filters.
 */
public interface FilterableRequestSpecification extends QueryableRequestSpecification, RequestSpecification {

    /**
     * Set the request path of the request specification. For example if the request was defined like this:
     * <p/>
     * <pre>
     * get("/x");
     * </pre>
     * <p/>
     * You can change to path to "/y" instead using this method. This will result in a request that looks like this:
     * <p/>
     * <pre>
     * get("/y");
     * </pre>
     *
     * @param path The path
     * @return the filterable request specification
     */
    FilterableRequestSpecification path(String path);

    /**
     * Remove a form parameter from the request.
     *
     * @param parameterName The parameter key
     * @return The {@link FilterableRequestSpecification} without the parameter
     */
    FilterableRequestSpecification removeFormParam(String parameterName);

    /**
     * Remove a path parameter from the request. It will remove both named and unnamed path parameters.
     *
     * @param parameterName The parameter key
     * @return The {@link FilterableRequestSpecification} without the parameter
     */
    FilterableRequestSpecification removePathParam(String parameterName);

    /**
     * Remove a named path parameter from the request. It will remove both named and unnamed path parameters.
     *
     * @param parameterName The parameter key
     * @return The {@link FilterableRequestSpecification} without the parameter
     */
    FilterableRequestSpecification removeNamedPathParam(String parameterName);

    /**
     * Remove an unnamed path parameter from the request.
     *
     * @param parameterName The parameter key
     * @return The {@link FilterableRequestSpecification} without the parameter
     */
    FilterableRequestSpecification removeUnnamedPathParam(String parameterName);

    /**
     * Remove the first unnamed path parameter from the request based on its value.
     *
     * @param parameterValue The parameter key
     * @return The {@link FilterableRequestSpecification} without the parameter
     */
    FilterableRequestSpecification removeUnnamedPathParamByValue(String parameterValue);

    /**
     * Remove a request parameter from the request.
     *
     * @param parameterName The parameter key
     * @return The {@link FilterableRequestSpecification} without the parameter
     */
    FilterableRequestSpecification removeParam(String parameterName);

    /**
     * Remove a query parameter from the request.
     *
     * @param parameterName The parameter key
     * @return The {@link FilterableRequestSpecification} without the parameter
     */
    FilterableRequestSpecification removeQueryParam(String parameterName);

    /**
     * Remove a header with the given name.
     *
     * @param headerName The header name
     * @return The {@link FilterableRequestSpecification} without the specified header
     */
    FilterableRequestSpecification removeHeader(String headerName);

    /**
     * Remove a cookie with the given name.
     *
     * @param cookieName The cookie name
     * @return The {@link FilterableRequestSpecification} without the specified cookie
     */
    FilterableRequestSpecification removeCookie(String cookieName);

    /**
     * Remove a cookie
     *
     * @param cookie The cookie
     * @return The {@link FilterableRequestSpecification} without the specified cookie
     */
    FilterableRequestSpecification removeCookie(Cookie cookie);

    /**
     * Replace a header with the new value. If the headerName doesn't exist the will be added.
     *
     * @param headerName The header name to replace
     * @return The {@link FilterableRequestSpecification} with the replaced header
     */
    FilterableRequestSpecification replaceHeader(String headerName, String newValue);

    /**
     * Replace a cookie with the given name. If the cookieName doesn't exist it will be added.
     *
     * @param cookieName The cookie name
     * @return The {@link FilterableRequestSpecification} with the replaced cookie
     */
    FilterableRequestSpecification replaceCookie(String cookieName, String value);

    /**
     * Replace a cookie, if it doesn't exist then it will be added.
     *
     * @param cookie The cookie
     * @return The {@link FilterableRequestSpecification} with the replaced cookie
     */
    FilterableRequestSpecification replaceCookie(Cookie cookie);

    /**
     * Replace all defined headers
     *
     * @param headers The new headers
     * @return The {@link FilterableRequestSpecification} with the replaced headers
     */
    FilterableRequestSpecification replaceHeaders(Headers headers);


    /**
     * Replace all defined cookies
     *
     * @param cookies The new cookies
     * @return The {@link FilterableRequestSpecification} with the replaced cookies
     */
    FilterableRequestSpecification replaceCookies(Cookies cookies);

    /**
     * Removes all defined headers
     *
     * @return The {@link FilterableRequestSpecification} without
     */
    FilterableRequestSpecification removeHeaders();


    /**
     * Removed all defined cookies
     *
     * @return The {@link FilterableRequestSpecification} with the replaced cookies
     */
    FilterableRequestSpecification removeCookies();
}
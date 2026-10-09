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

package io.restassured.internal.filter;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.util.GroovyStyleEquality;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSender;

import java.util.Iterator;
import java.util.Map;

public class FilterContextImpl implements FilterContext {
    private final Iterator<Filter> filters;
    private final String method;
    // The difference between internalRequestUri and requestUri is that query parameters defined outside the path is not included
    private final String internalRequestUri;
    private Object assertionClosure;
    private Map<String, Object> properties;

    /**
     * Only {@code internalRequestUri}, {@code method}, {@code assertionClosure}, {@code filters} and {@code properties}
     * are used by the filter context.
     *
     * @param requestUri         The full request uri including query params and encoding etc
     * @param fullOriginalPath   The original path without any path parameters applied (merged with base path)
     * @param fullSubstitutedPath The substituted path with path parameters applied (merged with base path)
     * @param internalRequestUri An internal request URI where query parameters not explicitly defined in the path are missing
     * @param userDefinedPath    The path that the user defined as a part of the request (for example if get("/x") then "/x" is the user defined path)
     * @param unnamedPathParams  the unnamed path parameters passed to the invocation method (for example if get("/{x}/{y}", "z", "w") then ["z", "w"] are the unnamed path params)
     * @param method             The method (e.g. GET, POST, PUT etc)
     * @param assertionClosure   (the assertions that should be performed after the request)
     * @param filters            The remaining filters to invoke
     * @param properties         The filter context properties
     */
    public FilterContextImpl(String requestUri, String fullOriginalPath, String fullSubstitutedPath, String internalRequestUri, String userDefinedPath,
                             Object[] unnamedPathParams, String method, Object assertionClosure, Iterator<Filter> filters, Map<String, Object> properties) {
        this.internalRequestUri = internalRequestUri;
        this.filters = filters;
        this.method = method;
        this.assertionClosure = assertionClosure;
        this.properties = properties;
    }

    @Override
    public Response next(FilterableRequestSpecification request, FilterableResponseSpecification response) {
        while (filters.hasNext()) {
            Filter nextFilter = filters.next();
            if (nextFilter != null) {
                FilterContext filterContext = (FilterContext) ((RequestSpecificationImpl) request).newFilterContext(assertionClosure, filters, properties);
                return nextFilter.filter(request, response, filterContext);
            }
        }
        return null;
    }

    // Used by SendRequestFilter
    public String getInternalRequestURI() {
        return internalRequestUri;
    }

    @Override
    public Response send(RequestSender requestSender) {
        return requestSender.request(method, internalRequestUri);
    }

    @Override
    public void setValue(String name, Object value) {
        properties.put(name, value);
    }

    @Override
    public boolean hasValue(String name) {
        return getValue(name) != null;
    }

    @Override
    public boolean hasValue(String name, Object value) {
        return hasValue(name) && GroovyStyleEquality.isEqual(getValue(name), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(String name) {
        return (T) properties.get(name);
    }

    public Object getAssertionClosure() {
        return assertionClosure;
    }

    public void setAssertionClosure(Object assertionClosure) {
        this.assertionClosure = assertionClosure;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}

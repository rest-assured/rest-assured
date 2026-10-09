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
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * The last filter of the filter chain, it sends the request.
 */
public class SendRequestFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification requestSpecification, FilterableResponseSpecification responseSpecification, FilterContext context) {
        FilterContextImpl filterContext = (FilterContextImpl) context;
        try {
            return ((RequestSpecificationImpl) requestSpecification).sendRequest(filterContext.getInternalRequestURI(), filterContext.getAssertionClosure(),
                    requestSpecification, filterContext.getProperties());
        } catch (IOException | URISyntaxException e) {
            // Thrown as is, for example a ConnectException
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }
}

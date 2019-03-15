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

package io.restassured.filter;

import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * A filter allows you to inspect and alter a request before it's actually committed and also inspect and alter the
 * response before it's returned to the expectations. You can regard it as an "around advice" in AOP terms.
 * Filters can be used to implement custom authentication schemes, session management, logging etc.
 *
 * If you need an ordered filter you should implement {@link OrderedFilter}.
 */
@FunctionalInterface
public interface Filter {

    /**
     * Filter the incoming request and response specifications and outgoing response.
     * You need to call {@link FilterContext#next(FilterableRequestSpecification, FilterableResponseSpecification)} when you're done otherwise the request will not be delivered.
     * It's of course possible to abort the filter chain execution by returning a {@link Response} directly.
     *
     *
     * @param requestSpec The incoming request spec
     * @param responseSpec The incoming response spec
     * @param ctx The filter context. You need to call {@link FilterContext#next(FilterableRequestSpecification, FilterableResponseSpecification)} when you're done otherwise the request will not be delivered.
     * @return The response
     *
     */
    Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx);
}

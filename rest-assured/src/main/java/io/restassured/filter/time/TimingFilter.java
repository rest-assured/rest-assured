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

package io.restassured.filter.time;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Filter that measures the response time and stores it in the {@link FilterContext} in key "{@value #RESPONSE_TIME_MILLISECONDS}".
 * <p/>
 * Note that this is the time it takes for REST Assured to perform the request and consume the response.
 * It's does <i>not</i> say how fast the server responds. Measurements can be misleading especially if the JVM is not warm. It's highly recommended
 * to warm up the JVM by running at least one REST Assured test prior to taking any measurements.
 */
public class TimingFilter implements Filter {
    public static final String RESPONSE_TIME_MILLISECONDS = "RA_RESPONSE_TIME_MILLIS";

    private final boolean shouldConsumeStream;

    /**
     * Constructs a new TimingFilter that doesn't consume the response body automatically if consuming an input stream.
     */
    public TimingFilter() {
        this(false);
    }

    /**
     * Constructs a new TimingFilter that optionally consumes the response body if consuming an input stream.
     *
     * @param consumeIfInputStream Consume response body and include how long it takes to consume the input stream in the response time measurement.
     */
    public TimingFilter(boolean consumeIfInputStream) {
        this.shouldConsumeStream = consumeIfInputStream;
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        long start = System.currentTimeMillis();
        Response response = ctx.next(requestSpec, responseSpec);
        if (shouldConsumeStream && response instanceof RestAssuredResponseImpl && ((RestAssuredResponseImpl) response).isInputStream()) {
            // Consume the body of the request (important if measure time also should include downloading of body)
            response.asByteArray();
        }

        long end = System.currentTimeMillis();
        long responseTime = end - start;
        ctx.setValue(RESPONSE_TIME_MILLISECONDS, responseTime);
        return response;
    }

    /**
     * Syntactic sugar for creating a new timing filter
     *
     * @return A new {@link TimingFilter}
     */
    public static TimingFilter measureTime() {
        return new TimingFilter();
    }
}

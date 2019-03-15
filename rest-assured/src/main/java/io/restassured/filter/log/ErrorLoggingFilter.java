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

package io.restassured.filter.log;

import io.restassured.filter.Filter;

import java.io.PrintStream;

import static org.hamcrest.Matchers.*;

/**
 * A filter that'll print the response body if an error occurred (status code is between 400 and 500).
 */
public class ErrorLoggingFilter extends StatusCodeBasedLoggingFilter {

    /**
     * Log to system out
     */
    public ErrorLoggingFilter() {
        this(System.out);
    }

    /**
     * Instantiate a error logger using a specific print stream
     * @param stream The stream to log errors to.
     */
    public ErrorLoggingFilter(PrintStream stream) {
        super(stream, allOf(greaterThanOrEqualTo(400), lessThanOrEqualTo(500)));
    }

    /**
     * Create a new error logging filter without using the "new" operator.
     * Will make the DSL look nicer.
     *
     * @return a new instance of the filter
     */
    public static Filter errorLogger() {
        return new ErrorLoggingFilter();
    }

    /**
     * Create a new error logging filter without using the "new" operator.
     * Will make the DSL look nicer.
     *
     * @param stream The print stream to log to
     * @return a new instance of the filter
     */
    public static Filter logErrorsTo(PrintStream stream) {
        return new ErrorLoggingFilter(stream);
    }
}

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

package com.jayway.restassured.filter.log;

import com.jayway.restassured.filter.Filter;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.io.PrintStream;

import static org.hamcrest.Matchers.equalTo;

/**
 * A filter that'll print the response body if it matches a given status code.
 */
public class ResponseLoggingFilter extends StatusCodeBasedLoggingFilter {

    /**
     * Log to system out for all status codes
     */
    public ResponseLoggingFilter() {
        this(System.out);
    }

    /**
     * Log to system out if response status code matches the given status code.
     *
     * @param statusCode The status code
     */
    public ResponseLoggingFilter(int statusCode) {
        this(System.out, equalTo(statusCode));
    }

    /**
     * Log to system out if response status code matches the given hamcrest matcher.
     *
     * @param matcher The hamcrest matcher
     */
    public ResponseLoggingFilter(Matcher<Integer> matcher) {
        this(System.out, matcher);
    }

    /**
     * Instantiate a logger using a specific print stream for all status codes
     *
     * @param stream The stream to log errors to.
     */
    public ResponseLoggingFilter(PrintStream stream) {
        this(stream, Matchers.<Integer>anything());
    }

    /**
     * Instantiate a logger using a specific print stream for status codes matching the supplied status code.
     *
     * @param stream The stream to log errors to.
     * @param statusCode The status code that must be present in the response if the response body is to be printed.
     */
    public ResponseLoggingFilter(PrintStream stream, int statusCode) {
        super(stream, equalTo(statusCode));
    }

    /**
     * Instantiate a logger using a specific print stream for status codes matching the supplied matcher.
     *
     * @param stream The stream to log errors to.
     * @param matcher The matcher that must be fulfilled if the response body is to be printed.
     */
    public ResponseLoggingFilter(PrintStream stream, Matcher<Integer> matcher) {
        super(stream, matcher);
    }

    /**
     * Create a new logging filter without using the "new" operator.
     * Will make the DSL look nicer.
     *
     * @return a new instance of the filter
     */
    public static Filter responseLogger() {
        return new ResponseLoggingFilter();
    }

    /**
     * Create a new logging filter without using the "new" operator.
     * Will make the DSL look nicer.
     *
     * @param stream The print stream to log to
     * @return a new instance of the filter
     */
    public static Filter logResponseTo(PrintStream stream) {
        return new ResponseLoggingFilter(stream);
    }

    /**
     * Create a new logging filter without using the "new" operator.
     * Will make the DSL look nicer.
     *
     * @param matcher The matcher that must be fulfilled in order for logging to occur
     * @return a new instance of the filter
     */
    public static Filter logResponseIfStatusCodeMatches(Matcher<Integer> matcher) {
        return new ResponseLoggingFilter(matcher);
    }

    /**
     * Create a new logging filter without using the "new" operator.
     * Will make the DSL look nicer.
     *
     * @param statusCode The status code that must be present in order for logging to occur
     * @return a new instance of the filter
     */
    public static Filter logResponseIfStatusCodeIs(int statusCode) {
        return new ResponseLoggingFilter(statusCode);
    }

    /**
     * Create a new logging filter without using the "new" operator.
     * Will make the DSL look nicer.
     *
     * @param stream The print stream to log to
     * @param matcher The matcher that must be fulfilled in order for logging to occur
     * @return a new instance of the filter
     */
    public static Filter logResponseToIfMatches(PrintStream stream, Matcher<Integer> matcher) {
        return new ResponseLoggingFilter(stream, matcher);
    }
}

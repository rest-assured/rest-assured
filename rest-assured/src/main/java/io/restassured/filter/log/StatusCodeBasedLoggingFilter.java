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

import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.internal.print.ResponsePrinter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang3.Validate;
import org.hamcrest.Matcher;

import java.io.PrintStream;

import static io.restassured.RestAssured.config;

class StatusCodeBasedLoggingFilter implements Filter {

    private final PrintStream stream;
    private final Matcher<?> matcher;
    private final LogDetail logDetail;
    private final boolean shouldPrettyPrint;

    /**
     * Log to system out
     *
     * @param matcher The matcher for the logging to take place
     */
    public StatusCodeBasedLoggingFilter(Matcher<? super Integer> matcher) {
        this(System.out, matcher);
    }

    /**
     * Instantiate a error logger using a specific print stream
     *
     * @param stream  The stream to log errors to.
     * @param matcher The matcher for the logging to take place
     */
    public StatusCodeBasedLoggingFilter(PrintStream stream, Matcher<? super Integer> matcher) {
        this(LogDetail.ALL, stream, matcher);
    }

    /**
     * Instantiate a logger using a specific print stream and a specific log detail
     *
     * @param logDetail The log detail
     * @param stream    The stream to log errors to.
     * @param matcher   The matcher for the logging to take place
     */
    public StatusCodeBasedLoggingFilter(LogDetail logDetail, PrintStream stream, Matcher<? super Integer> matcher) {
        this(logDetail, isPrettyPrintingEnabled(), stream, matcher);
    }

    /**
     * Instantiate a logger using a specific print stream and a specific log detail  and the option to pretty printing
     *
     * @param logDetail   The log detail
     * @param prettyPrint Enabled pretty printing if possible
     * @param stream      The stream to log errors to.
     * @param matcher     The matcher for the logging to take place
     */
    public StatusCodeBasedLoggingFilter(LogDetail logDetail, boolean prettyPrint, PrintStream stream, Matcher<? super Integer> matcher) {
        Validate.notNull(logDetail, "Log details cannot be null");
        Validate.notNull(stream, "Print stream cannot be null");
        Validate.notNull(matcher, "Matcher cannot be null");
        if (logDetail == LogDetail.PARAMS || logDetail == LogDetail.URI || logDetail == LogDetail.METHOD) {
            throw new IllegalArgumentException(String.format("%s is not a valid %s for a response.", logDetail, LogDetail.class.getSimpleName()));
        }
        this.shouldPrettyPrint = prettyPrint;
        this.logDetail = logDetail;
        this.stream = stream;
        this.matcher = matcher;
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        Response response = ctx.next(requestSpec, responseSpec);
        final int statusCode = response.statusCode();
        if (matcher.matches(statusCode)) {
            ResponsePrinter.print(response, response, stream, logDetail, shouldPrettyPrint);
            final byte[] responseBody;
            if (logDetail == LogDetail.BODY || logDetail == LogDetail.ALL) {
                responseBody = response.asByteArray();
            } else {
                responseBody = null;
            }
            response = cloneResponseIfNeeded(response, responseBody);
        }

        return response;
    }

    /*
     * If body expectations are defined we need to return a new Response otherwise the stream
     * has been closed due to the logging.
     */
    private Response cloneResponseIfNeeded(Response response, byte[] responseAsString) {
        if (responseAsString != null && response instanceof RestAssuredResponseImpl && !((RestAssuredResponseImpl) response).getHasExpectations()) {
            final Response build = new ResponseBuilder().clone(response).setBody(responseAsString).build();
            ((RestAssuredResponseImpl) build).setHasExpectations(true);
            return build;
        }
        return response;
    }

    private static boolean isPrettyPrintingEnabled() {
        return config == null || config.getLogConfig().isPrettyPrintingEnabled();
    }

    private void throwIAE(LogDetail params) {
        throw new IllegalArgumentException(String.format("%s is not a valid %s for a response.", params, LogDetail.class.getSimpleName()));
    }
}

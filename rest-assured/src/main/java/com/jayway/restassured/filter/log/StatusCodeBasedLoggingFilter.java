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

import com.jayway.restassured.builder.ResponseBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.response.Cookies;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hamcrest.Matcher;

import java.io.PrintStream;

import static com.jayway.restassured.filter.log.LogDetail.*;
import static org.apache.commons.lang.StringUtils.isBlank;

class StatusCodeBasedLoggingFilter implements Filter {

    private final PrintStream stream;
    private final Matcher<?> matcher;
    private final LogDetail logDetail;

    /**
     * Log to system out
     * @param matcher The matcher for the logging to take place
     */
    public StatusCodeBasedLoggingFilter(Matcher<Integer> matcher) {
        this(System.out, matcher);
    }

    /**
     * Instantiate a error logger using a specific print stream
     * @param stream The stream to log errors to.
     * @param matcher The matcher for the logging to take place
     */
    public StatusCodeBasedLoggingFilter(PrintStream stream, Matcher<Integer> matcher) {
        this(ALL, stream, matcher);
    }

    /**
     * Instantiate a logger using a specific print stream and a specific log detail
     *
     * @param logDetail The log detail
     * @param stream The stream to log errors to.
     * @param matcher The matcher for the logging to take place
     */
    public StatusCodeBasedLoggingFilter(LogDetail logDetail, PrintStream stream, Matcher<Integer> matcher) {
        Validate.notNull(logDetail, "Log details cannot be null");
        Validate.notNull(stream, "Print stream cannot be null");
        Validate.notNull(matcher, "Matcher cannot be null");
        this.logDetail = logDetail;
        this.stream = stream;
        this.matcher = matcher;
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        Response response = ctx.next(requestSpec, responseSpec);
        final int statusCode = response.statusCode();
        if(matcher.matches(statusCode)) {
            final String responseAsString = log(response);
            response = cloneResponseIfNeeded(response, responseAsString);
        }

        return response;
    }

    private String log(Response response) {
        final StringBuilder builder = new StringBuilder();
        String responseBody = null;
        if(logDetail == ALL || logDetail == STATUS) {
            builder.append(response.statusLine()).append("\n");
        }
        if(logDetail == ALL || logDetail == HEADERS) {
            final Headers headers = response.headers();
            if(headers.exist()) {
                builder.append(headers.toString()).append("\n");
            }
        } else if(logDetail == COOKIES) {
            final Cookies cookies = response.detailedCookies();
            if(cookies.exist()) {
                builder.append(cookies.toString()).append("\n");
            }
        }
        if(logDetail == ALL || logDetail == BODY) {
            responseBody = response.asString();
            if(logDetail == ALL && !isBlank(responseBody)) {
                builder.append("\n");
            }
            builder.append(responseBody);
        }
        stream.println(builder.toString());
        return responseBody;
    }

    /*
     * If body expectations are defined we need to return a new Response otherwise the stream
     * has been closed due to the logging.
     */
    private Response cloneResponseIfNeeded(Response response, String responseAsString) {
        if(responseAsString != null && response instanceof RestAssuredResponseImpl && !((RestAssuredResponseImpl) response).getHasExpectations()) {
            final Response build = new ResponseBuilder().clone(response).setBody(responseAsString).build();
            ((RestAssuredResponseImpl) build).setHasExpectations(true);
            return build;
        }
        return response;
    }
}

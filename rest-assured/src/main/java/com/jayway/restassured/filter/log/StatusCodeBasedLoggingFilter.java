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
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang.Validate;
import org.hamcrest.Matcher;

import java.io.PrintStream;

class StatusCodeBasedLoggingFilter implements Filter {

    private final PrintStream stream;
    private Matcher<?> matcher;

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
        this.matcher = matcher;
        Validate.notNull(stream, "Print stream cannot be null");
        Validate.notNull(matcher, "Matcher cannot be null");
        this.stream = stream;
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        Response response = ctx.next(requestSpec, responseSpec);
        final int statusCode = response.statusCode();
        if(matcher.matches(statusCode)) {
            final String responseAsString = response.asString();
            stream.println(responseAsString);
            response = cloneResponseIfNeeded(response, responseAsString);
        }

        return response;
    }

    /*
     * If body expectations are defined we need to return a new Response otherwise the stream
     * has been closed due to the logging.
     */
    private Response cloneResponseIfNeeded(Response response, String responseAsString) {
        if(response instanceof RestAssuredResponseImpl && !((RestAssuredResponseImpl) response).getHasExpectations()) {
            RestAssuredResponseImpl restAssuredResponse = new RestAssuredResponseImpl();
            restAssuredResponse.setContent(responseAsString);
            restAssuredResponse.setContentType(response.getContentType());
            restAssuredResponse.setCookies(response.getDetailedCookies());
            restAssuredResponse.setResponseHeaders(response.getHeaders());
            restAssuredResponse.setStatusCode(response.getStatusCode());
            restAssuredResponse.setStatusLine(response.getStatusLine());
            restAssuredResponse.setHasExpectations(true);
            restAssuredResponse.setDefaultContentType(((RestAssuredResponseImpl) response).getDefaultContentType());
            return restAssuredResponse;
        }
        return response;
    }
}

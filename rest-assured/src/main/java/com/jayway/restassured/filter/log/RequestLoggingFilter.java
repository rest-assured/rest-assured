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
import com.jayway.restassured.response.*;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang.Validate;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

import static com.jayway.restassured.filter.log.LogDetail.*;

/**
 * Will log the request before it's passed to HTTP Builder. Note that HTTP Builder and HTTP Client will add additional headers. This filter will <i>only</i>
 * log things specified in the request specification. I.e. you can NOT regard the things logged here to be what's actually sent to the server.
 * Also subsequent filters may alter the request <i>after</i> the logging has took place. You need to log what's <i>actually</i> sent on the wire
 * refer to the <a href="http://hc.apache.org/httpcomponents-client-ga/logging.html">HTTP Client logging docs</a> or use an external tool such as
 * <a href="http://www.wireshark.org/">Wireshark</a>.
 */
public class RequestLoggingFilter implements Filter {

    private static final String TAB = "\t";
    private static final String NEW_LINE = "\n";
    private static final String EQUALS = "=";
    private static final String NONE = "<none>";
    private static final String CONTENT_TYPE = "Content-Type";

    private final LogDetail logDetail;
    private final PrintStream stream;

    /**
     * Logs to System.out
     */
    public RequestLoggingFilter() {
        this(ALL, System.out);
    }

    /**
     * Logs with a specific detail to System.out
     *
     * @param logDetail The log detail
     */
    public RequestLoggingFilter(LogDetail logDetail) {
        this(logDetail, System.out);
    }

    /**
     * Logs everyting to the specified printstream.
     *
     * @param printStream The stream to log to.
     */
    public RequestLoggingFilter(PrintStream printStream) {
        this(ALL, printStream);
    }

    /**
     * Instantiate a  logger using a specific print stream and a specific log detail
     *
     * @param logDetail The log detail
     * @param stream The stream to log to.
     */
    public RequestLoggingFilter(LogDetail logDetail, PrintStream stream) {
        Validate.notNull(stream, "Print stream cannot be null");
        Validate.notNull(logDetail, "Log details cannot be null");
        if(logDetail == STATUS) {
            throw new IllegalArgumentException(String.format("%s is not a valid %s for a request.", STATUS, LogDetail.class.getSimpleName()));
        }
        this.stream = stream;
        this.logDetail = logDetail;
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        final StringBuilder builder = new StringBuilder();
        if(logDetail == ALL) {
            addSingle(builder, "Request method:", ctx.getRequestMethod().toString());
            addSingle(builder, "Request path:", ctx.getRequestPath());
            addMapDetails(builder, "Request params:", requestSpec.getRequestParams());
            addMapDetails(builder, "Query params:", requestSpec.getQueryParams());
            addMapDetails(builder, "Form params:", requestSpec.getFormParams());
            addMapDetails(builder, "Path params:", requestSpec.getPathParams());
        }
        if(logDetail == ALL || logDetail == HEADERS) {
            addHeaders(requestSpec, builder);
        }
        if(logDetail == ALL || logDetail == COOKIES) {
            addCookies(requestSpec, builder);
        }
        if(logDetail == ALL || logDetail == BODY) {
            addBody(requestSpec, builder);
        }
        stream.println(builder.toString());
        return ctx.next(requestSpec, responseSpec);
    }

    private void addBody(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Body:");
        if(requestSpec.getBody() != null) {
            builder.append(NEW_LINE).append(requestSpec.getBody());
        } else {
            appendTab(appendTwoTabs(builder)).append(NONE);
        }
    }

    private void addCookies(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Cookies:");
        final Cookies cookies = requestSpec.getCookies();
        if(!cookies.exist()) {
            appendTwoTabs(builder).append(NONE).append(NEW_LINE);
        }
        int i = 0;
        for (Cookie cookie : cookies) {
            if(i++ == 0) {
                appendTwoTabs(builder);
            } else {
                appendFourTabs(builder);
            }
            builder.append(cookie).append(NEW_LINE);
        }
    }

    private void addHeaders(FilterableRequestSpecification requestSpec, StringBuilder builder) {
        builder.append("Headers:");
        final Headers headers = requestSpec.getHeaders();
        final boolean hasContentTypeHeader = headers.hasHeaderWithName(CONTENT_TYPE);
        if(!hasContentTypeHeader) {
            appendTwoTabs(builder);
            builder.append(CONTENT_TYPE).append(EQUALS).append(requestSpec.getRequestContentType()).append(NEW_LINE);
        }
        int i = 0;
        for (Header header : headers) {
            if(i++ == 0 && hasContentTypeHeader) {
                appendTwoTabs(builder);
            } else {
                appendFourTabs(builder);
            }
            builder.append(header).append(NEW_LINE);
        }
    }

    private void addSingle(StringBuilder builder, String str, String requestPath) {
        appendTab(builder.append(str)).append(requestPath).append(NEW_LINE);
    }

    private void addMapDetails(StringBuilder builder, String title, Map<String, ?> map) {
        appendTab(builder.append(title));
        if(map.isEmpty()) {
            builder.append(NONE).append(NEW_LINE);
        } else {
            int i = 0;
            for (Entry<String, ?> entry : map.entrySet()) {
                if(i++ != 0) {
                    appendFourTabs(builder);
                }
                builder.append(entry.getKey()).append(EQUALS).append(entry.getValue()).append(NEW_LINE);
            }
        }
    }

    private StringBuilder appendFourTabs(StringBuilder builder) {
        appendTwoTabs(appendTwoTabs(builder));
        return builder;
    }

    private StringBuilder appendTwoTabs(StringBuilder builder) {
        appendTab(appendTab(builder));
        return builder;
    }

    private StringBuilder appendTab(StringBuilder builder) {
        return builder.append(TAB);
    }
}

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
package io.restassured.internal.print;

import io.restassured.filter.log.LogDetail;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import org.apache.commons.lang3.SystemUtils;

import java.io.PrintStream;
import java.util.Set;

import static io.restassured.filter.log.LogDetail.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * A response printer can be used to print a response.
 */
public class ResponsePrinter {

    private static final String BLACKLISTED = "[ BLACKLISTED ]";
    private static final String HEADER_NAME_AND_VALUE_SEPARATOR = ": ";
    private static final String STREAMING_BODY_NOT_LOGGED = "<streaming body not logged>";
    private static final String EVENT_STREAM = "text/event-stream";

    /**
     * Prints the response to the print stream
     *
     * @return A string of representing the response
     */
    public static String print(ResponseOptions responseOptions, ResponseBody responseBody, PrintStream stream, LogDetail logDetail, boolean shouldPrettyPrint, Set<String> blacklistedHeaders) {
        return print(responseOptions, responseBody, stream, logDetail, shouldPrettyPrint, blacklistedHeaders, false);
    }

    /**
     * Prints the response to the print stream
     *
     * @param skipStreamingBody <code>true</code> to print a placeholder instead of the body of a streaming response
     *                          (see {@link #isStreaming(ResponseOptions)}), so that printing doesn't wait for a body
     *                          that may never end
     * @return A string of representing the response
     */
    public static String print(ResponseOptions responseOptions, ResponseBody responseBody, PrintStream stream, LogDetail logDetail, boolean shouldPrettyPrint, Set<String> blacklistedHeaders, boolean skipStreamingBody) {
        final StringBuilder builder = new StringBuilder();
        if (logDetail == ALL || logDetail == STATUS) {
            builder.append(responseOptions.statusLine());
        }
        if (logDetail == ALL || logDetail == HEADERS) {
            final Headers headers = responseOptions.headers();
            if (headers.exist()) {
                appendNewLineIfAll(logDetail, builder).append(toString(headers, blacklistedHeaders));
            }
        } else if (logDetail == COOKIES) {
            final Cookies cookies = responseOptions.detailedCookies();
            if (cookies.exist()) {
                appendNewLineIfAll(logDetail, builder).append(cookies.toString());
            }
        }
        if (logDetail == ALL || logDetail == BODY) {
            String responseBodyToAppend;
            if (skipStreamingBody && isStreaming(responseOptions)) {
                responseBodyToAppend = STREAMING_BODY_NOT_LOGGED;
            } else if (shouldPrettyPrint) {
                responseBodyToAppend = new Prettifier().getPrettifiedBodyIfPossible(responseOptions, responseBody);
            } else {
                responseBodyToAppend = responseBody.asString();
            }
            if (logDetail == ALL && !isBlank(responseBodyToAppend)) {
                builder.append(SystemUtils.LINE_SEPARATOR).append(SystemUtils.LINE_SEPARATOR);
            }

            builder.append(responseBodyToAppend);
        }
        String response = builder.toString();
        stream.println(response);
        return response;
    }

    /**
     * @return <code>true</code> if the response is a stream of server-sent events (<code>text/event-stream</code>),
     * whose body may never end
     */
    public static boolean isStreaming(ResponseOptions responseOptions) {
        String contentType = responseOptions.contentType();
        return contentType != null && substringBefore(contentType, ";").trim().equalsIgnoreCase(EVENT_STREAM);
    }

    private static String toString(Headers headers, Set<String> blacklistedHeaders) {
        if (!headers.exist()) {
            return "";
        }

        final StringBuilder builder = new StringBuilder();
        for (Header header : headers) {
            StringBuilder headerStringBuilder = builder.append(header.getName())
                    .append(HEADER_NAME_AND_VALUE_SEPARATOR);
            if (blacklistedHeaders.contains(header.getName())) {
                headerStringBuilder.append(BLACKLISTED);
            } else {
                headerStringBuilder.append(header.getValue());
            }
            headerStringBuilder
                    .append(SystemUtils.LINE_SEPARATOR);
        }
        builder.delete(builder.length() - SystemUtils.LINE_SEPARATOR.length(), builder.length());
        return builder.toString();
    }

    private static StringBuilder appendNewLineIfAll(LogDetail logDetail, StringBuilder builder) {
        if (logDetail == ALL) {
            builder.append(SystemUtils.LINE_SEPARATOR);
        }
        return builder;
    }
}
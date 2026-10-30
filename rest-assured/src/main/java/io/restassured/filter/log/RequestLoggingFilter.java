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

import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.print.RequestPrinter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang3.Validate;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static io.restassured.filter.log.LogDetail.ALL;
import static io.restassured.filter.log.LogDetail.STATUS;

/**
 * Will log the request before it's sent. Note that the HTTP client will add additional headers. This filter will <i>only</i>
 * log things specified in the request specification. I.e. you can NOT regard the things logged here to be what's actually sent to the server.
 * Also subsequent filters may alter the request <i>after</i> the logging has took place. If you need to log what's <i>actually</i> sent on the wire
 * refer to the logging docs of the HTTP client or use an external tool such as <a href="http://www.wireshark.org/">Wireshark</a>.
 * <p>
 * A filter created without a pretty-print argument pretty prints the body if the log config of the request has pretty
 * printing enabled ({@link io.restassured.config.LogConfig#isPrettyPrintingEnabled()}), which it is by default.
 * </p>
 */
public class RequestLoggingFilter implements Filter {

    private static final boolean SHOW_URL_ENCODED_URI = true;
    private final LogDetail logDetail;
    private final PrintStream stream;
    // null to pretty print as the log config of the request says
    private final Boolean shouldPrettyPrint;
    private final boolean showUrlEncodedUri;
    private final Set<String> blacklistedHeaders;
    private final Set<LogDetail> logDetailSet = new HashSet<>();

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
     * Instantiate a  logger using a specific print stream and a specific log detail. Pretty-printing follows the log
     * config of the request.
     *
     * @param logDetail The log detail
     * @param stream    The stream to log to.
     */
    public RequestLoggingFilter(LogDetail logDetail, PrintStream stream) {
        this(null, logDetail, stream, SHOW_URL_ENCODED_URI, new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
    }


    /**
     * Instantiate a logger using a specific print stream and a specific log detail
     *
     * @param logDetail         The log detail
     * @param shouldPrettyPrint <code>true</code> if pretty-printing of the body should occur.
     * @param stream            The stream to log to.
     */
    public RequestLoggingFilter(LogDetail logDetail, boolean shouldPrettyPrint, PrintStream stream) {
        this(logDetail, shouldPrettyPrint, stream, SHOW_URL_ENCODED_URI);
    }

    /**
     * Instantiate a logger using a specific print stream and a specific log detail
     *
     * @param logDetail         The log detail
     * @param shouldPrettyPrint <code>true</code> if pretty-printing of the body should occur.
     * @param stream            The stream to log to.
     * @param showUrlEncodedUri Whether or not to show the request URI as url encoded
     */
    public RequestLoggingFilter(LogDetail logDetail, boolean shouldPrettyPrint, PrintStream stream, boolean showUrlEncodedUri) {
        this(logDetail, shouldPrettyPrint, stream, showUrlEncodedUri, new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Instantiate a logger using a specific print stream and a specific log detail
     *
     * @param logDetail         The log detail
     * @param shouldPrettyPrint <code>true</code> if pretty-printing of the body should occur.
     * @param stream            The stream to log to.
     * @param showUrlEncodedUri Whether or not to show the request URI as url encoded
     */
    public RequestLoggingFilter(LogDetail logDetail, boolean shouldPrettyPrint, PrintStream stream, boolean showUrlEncodedUri, Set<String> blacklistedHeaders) {
        this(Boolean.valueOf(shouldPrettyPrint), logDetail, stream, showUrlEncodedUri, blacklistedHeaders);
    }

    private RequestLoggingFilter(Boolean shouldPrettyPrint, LogDetail logDetail, PrintStream stream, boolean showUrlEncodedUri, Set<String> blacklistedHeaders) {
        Validate.notNull(stream, "Print stream cannot be null");
        Validate.notNull(blacklistedHeaders, "Blacklisted headers cannot be null");
        Validate.notNull(logDetail, "Log details cannot be null");
        if (logDetail == STATUS) {
            throw new IllegalArgumentException(String.format("%s is not a valid %s for a request.", STATUS, LogDetail.class.getSimpleName()));
        }
        this.stream = stream;
        this.logDetail = logDetail;
        TreeSet<String> caseInsensitiveBlacklistedHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveBlacklistedHeaders.addAll(blacklistedHeaders);
        this.blacklistedHeaders = caseInsensitiveBlacklistedHeaders;
        this.shouldPrettyPrint = shouldPrettyPrint;
        this.showUrlEncodedUri = showUrlEncodedUri;
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        String uri = requestSpec.getURI();
        if (!showUrlEncodedUri) {
            uri = UrlDecoder.urlDecode(uri, Charset.forName(requestSpec.getConfig().getEncoderConfig().defaultQueryParameterCharset()), true);
        }

        RestAssuredConfig config = requestSpec.getConfig();
        boolean prettyPrint = shouldPrettyPrint != null ? shouldPrettyPrint : config == null || config.getLogConfig().isPrettyPrintingEnabled();
        if (logDetailSet.isEmpty()) {
            RequestPrinter.print(requestSpec, requestSpec.getMethod(), uri, logDetail, blacklistedHeaders, stream, prettyPrint);
        } else {
            RequestPrinter.print(requestSpec, requestSpec.getMethod(), uri, logDetailSet, blacklistedHeaders, stream, prettyPrint);
        }
        return ctx.next(requestSpec, responseSpec);
    }

    /**
     * Syntactic sugar for doing <code>new RequestLoggingFilter(stream)</code>
     *
     * @param stream The stream to log the request to.
     * @return A new instance of {@link RequestLoggingFilter}.
     */
    public static RequestLoggingFilter logRequestTo(PrintStream stream) {
        return new RequestLoggingFilter(stream);
    }

    /**
     * Logs with specific details to System.out <br>
     * E.g <code>RequestLoggingFilter.with(METHOD, HEADERS, BODY)</code>
     *
     * @param logDetails The varargs of logDetail
     * @return A new instance of {@link RequestLoggingFilter}.
     */
    public static RequestLoggingFilter with(LogDetail... logDetails) {
        return new RequestLoggingFilter().addLog(logDetails);
    }

    private RequestLoggingFilter addLog(LogDetail... logDetails) {
        logDetailSet.addAll(Arrays.asList(logDetails));
        return this;
    }
}

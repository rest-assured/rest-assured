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
import io.restassured.filter.FilterContext;
import io.restassured.internal.print.RequestPrinter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang3.Validate;

import java.io.PrintStream;
import java.nio.charset.Charset;

import static io.restassured.filter.log.LogDetail.ALL;
import static io.restassured.filter.log.LogDetail.STATUS;

/**
 * Will log the request before it's passed to HTTP Builder. Note that HTTP Builder and HTTP Client will add additional headers. This filter will <i>only</i>
 * log things specified in the request specification. I.e. you can NOT regard the things logged here to be what's actually sent to the server.
 * Also subsequent filters may alter the request <i>after</i> the logging has took place. If you need to log what's <i>actually</i> sent on the wire
 * refer to the <a href="http://hc.apache.org/httpcomponents-client-ga/logging.html">HTTP Client logging docs</a> or use an external tool such as
 * <a href="http://www.wireshark.org/">Wireshark</a>.
 */
public class RequestLoggingFilter implements Filter {

    private static final boolean SHOW_URL_ENCODED_URI = true;
    private final LogDetail logDetail;
    private final PrintStream stream;
    private final boolean shouldPrettyPrint;
    private final boolean showUrlEncodedUri;

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
     * Instantiate a  logger using a specific print stream and a specific log detail. Pretty-printing will be enabled if possible.
     *
     * @param logDetail The log detail
     * @param stream    The stream to log to.
     */
    public RequestLoggingFilter(LogDetail logDetail, PrintStream stream) {
        this(logDetail, true, stream);
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
        Validate.notNull(stream, "Print stream cannot be null");
        Validate.notNull(logDetail, "Log details cannot be null");
        if (logDetail == STATUS) {
            throw new IllegalArgumentException(String.format("%s is not a valid %s for a request.", STATUS, LogDetail.class.getSimpleName()));
        }
        this.stream = stream;
        this.logDetail = logDetail;
        this.shouldPrettyPrint = shouldPrettyPrint;
        this.showUrlEncodedUri = showUrlEncodedUri;
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        String uri = requestSpec.getURI();
        if (!showUrlEncodedUri) {
            uri = UrlDecoder.urlDecode(uri, Charset.forName(requestSpec.getConfig().getEncoderConfig().defaultQueryParameterCharset()), true);
        }

        RequestPrinter.print(requestSpec, requestSpec.getMethod(), uri, logDetail, stream, shouldPrettyPrint);
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

}

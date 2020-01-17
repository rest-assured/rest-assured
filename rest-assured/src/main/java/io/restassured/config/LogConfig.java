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

package io.restassured.config;

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.LogSpecification;
import org.apache.commons.lang3.Validate;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Configure the logging for REST Assured. <p>Note that <i>only</i> things known to REST Assured (i.e. the request- and response specifications) will be logged. If you need to log what's <i>actually</i> sent on the wire
 * refer to the <a href="http://hc.apache.org/httpcomponents-client-ga/logging.html">HTTP Client logging docs</a> or use an external tool such as
 * <a href="http://www.wireshark.org/">Wireshark</a>.</p>
 */
public class LogConfig implements Config {

    private final PrintStream defaultPrintStream;
    private final boolean prettyPrintingEnabled;
    private final LogDetail logDetailIfValidationFails;
    private final boolean urlEncodeRequestUri;
    private final boolean isUserDefined;
    private final Set<String> headerBlacklist;

    /**
     * Configure the default stream to use the System.out stream (default).
     */
    public LogConfig() {
        this(System.out, true, null, true, new HashSet<>(), false);
    }

    /**
     * Configure pretty printing and the default stream where logs should be written if <i>not</i> specified explicitly by a filter. I.e. this stream will be used in cases
     * where the log specification DSL is used, e.g.
     * <pre>
     * given().log().all()...
     * </pre>
     * or
     * <pre>
     * expect().log.ifError(). ..
     * </pre>
     * <p/>
     * It will not override explicit streams defined by using the {@link RequestLoggingFilter} or the {@link ResponseLoggingFilter}.
     *
     * @param defaultPrintStream    The default print stream to use for the {@link LogSpecification}'s.
     * @param prettyPrintingEnabled Enable or disable pretty printing when logging. Pretty printing is only possible when content-type is XML, JSON or HTML.
     */
    public LogConfig(PrintStream defaultPrintStream, boolean prettyPrintingEnabled) {
        this(defaultPrintStream, prettyPrintingEnabled, null, true, new HashSet<>(), true);
    }

    /**
     * Configure pretty printing and the default stream where logs should be written if <i>not</i> specified explicitly by a filter. I.e. this stream will be used in cases
     * where the log specification DSL is used, e.g.
     * <pre>
     * given().log().all()...
     * </pre>
     * or
     * <pre>
     * expect().log.ifError(). ..
     * </pre>
     * <p/>
     * It will not override explicit streams defined by using the {@link RequestLoggingFilter} or the {@link ResponseLoggingFilter}.
     *
     * @param defaultPrintStream    The default print stream to use for the {@link LogSpecification}'s.
     * @param prettyPrintingEnabled Enable or disable pretty printing when logging. Pretty printing is only possible when content-type is XML, JSON or HTML.
     */
    private LogConfig(PrintStream defaultPrintStream, boolean prettyPrintingEnabled, LogDetail logDetailIfValidationFails,
                      boolean urlEncodeRequestUri, Set<String> headerBlacklist, boolean isUserDefined) {
        Validate.notNull(defaultPrintStream, "Stream to write logs to cannot be null");
        Validate.notNull(defaultPrintStream, "Stream to write logs to cannot be null");
        this.defaultPrintStream = defaultPrintStream;
        this.prettyPrintingEnabled = prettyPrintingEnabled;
        this.logDetailIfValidationFails = logDetailIfValidationFails;
        this.isUserDefined = isUserDefined;
        this.urlEncodeRequestUri = urlEncodeRequestUri;
        this.headerBlacklist = headerBlacklist;
    }

    /**
     * @return The default stream to use
     */
    public PrintStream defaultStream() {
        return defaultPrintStream;
    }

    /**
     * @return The blacklisted headers
     * @see #blacklistHeader(String, String...)
     */
    public Set<String> blacklistedHeaders() {
        return Collections.unmodifiableSet(headerBlacklist);
    }

    /**
     * Specify a new default stream to the print to.
     *
     * @param printStream The stream
     * @return A new LogConfig instance
     */
    public LogConfig defaultStream(PrintStream printStream) {
        return new LogConfig(printStream, true, logDetailIfValidationFails, urlEncodeRequestUri, headerBlacklist, true);
    }

    /**
     * @return <code>true</code> if pretty printing is enabled, <code>false</code> otherwise.
     */
    public boolean isPrettyPrintingEnabled() {
        return prettyPrintingEnabled;
    }

    /**
     * @return <code>true</code> if request and response logging is enabled if test validation fails, <code>false</code> otherwise.
     */
    public boolean isLoggingOfRequestAndResponseIfValidationFailsEnabled() {
        return logDetailIfValidationFails != null;
    }

    /**
     * @return The log detail to use if request and response logging is enabled if test validation fails.
     */
    public LogDetail logDetailOfRequestAndResponseIfValidationFails() {
        return logDetailIfValidationFails;
    }

    /**
     * Specify a whether or not to enable pretty printing by default.
     *
     * @param shouldEnable <code>true</code> if pretty-printing should be enabled, <code>false</code> otherwise.
     * @return A new LogConfig instance
     */
    public LogConfig enablePrettyPrinting(boolean shouldEnable) {
        return new LogConfig(defaultPrintStream, shouldEnable, logDetailIfValidationFails, urlEncodeRequestUri, headerBlacklist, true);
    }

    /**
     * Enable logging of both the request and the response if REST Assureds test validation fails.
     *
     * @return A new LogConfig instance
     */
    public LogConfig enableLoggingOfRequestAndResponseIfValidationFails() {
        return enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
    }

    /**
     * Enable logging of both the request and the response if REST Assureds test validation fails with the specified log detail
     *
     * @param logDetail The log detail to show in the log
     * @return A new LogConfig instance
     */
    public LogConfig enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
        return new LogConfig(defaultPrintStream, prettyPrintingEnabled, logDetail, urlEncodeRequestUri, headerBlacklist, true);
    }

    /**
     * Instruct REST Assured whether or not to URL encode the request URI when it's presented in the request specification log.
     * By default url encoding of the request uri is enabled to show what the URL targeted by REST Assured actually looks like for real.
     * But there may be cases where you want to make the URI more readable and this is when you might want to consider setting
     * <code>urlEncodeRequestUri</code> to <code>false</code>. Note that this only affects logging.
     *
     * @param urlEncodeRequestUri Whether or not to url encode the request uri when it's presented in the request log
     * @return A new LogConfig instance
     */
    public LogConfig urlEncodeRequestUri(boolean urlEncodeRequestUri) {
        return new LogConfig(defaultPrintStream, prettyPrintingEnabled, logDetailIfValidationFails, urlEncodeRequestUri, headerBlacklist, true);
    }

    /**
     * Blacklist one or more headers. If these headers show up during logging they will be replaced with 'HIDDEN'. The purpose of a blacklist is to prevent sensitive information
     * to be included in the log.
     *
     * @param header The header to include in the blacklist
     * @param otherHeaders Additional headers to include in the blacklist (optional)
     * @return A new LogConfig instance
     */
    public LogConfig blacklistHeader(String header, String... otherHeaders) {
        notNull(header, "header");
        Set<String> newHeaderBlackList = new HashSet<>(headerBlacklist);
        newHeaderBlackList.add(header);
        if (otherHeaders != null && otherHeaders.length > 0) {
            Collections.addAll(newHeaderBlackList, otherHeaders);
        }

        return new LogConfig(defaultPrintStream, prettyPrintingEnabled, logDetailIfValidationFails, urlEncodeRequestUri, newHeaderBlackList, true);
    }

    /**
     * Blacklist one or more headers. If these headers show up during logging they will be hidden. The purpose of a blacklist is to prevent sensitive information
     * to be included in the log. Note that this method replaces the previously defined blacklist.
     *
     * @param headers The headers to include in the blacklist
     * @return A new LogConfig instance
     */
    public LogConfig blacklistHeaders(Collection<String> headers) {
        notNull(headers, "headers");
        Set<String> newHeaderBlackList = headers.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return new LogConfig(defaultPrintStream, prettyPrintingEnabled, logDetailIfValidationFails, urlEncodeRequestUri, newHeaderBlackList, true);
    }

    /**
     * @return <code>true</code> is the request URI should be URL encoded in the request log
     */
    public boolean shouldUrlEncodeRequestUri() {
        return urlEncodeRequestUri;
    }

    /**
     * @return A static way to create a new LogConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static LogConfig logConfig() {
        return new LogConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same log config instance.
     */
    public LogConfig and() {
        return this;
    }

    public boolean isUserConfigured() {
        return isUserDefined;
    }
}
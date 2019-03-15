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

    /**
     * Configure the default stream to use the System.out stream (default).
     */
    public LogConfig() {
        this(System.out, true, null, true, false);
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
        this(defaultPrintStream, prettyPrintingEnabled, null, true, true);
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
                      boolean urlEncodeRequestUri, boolean isUserDefined) {
        Validate.notNull(defaultPrintStream, "Stream to write logs to cannot be null");
        this.defaultPrintStream = defaultPrintStream;
        this.prettyPrintingEnabled = prettyPrintingEnabled;
        this.logDetailIfValidationFails = logDetailIfValidationFails;
        this.isUserDefined = isUserDefined;
        this.urlEncodeRequestUri = urlEncodeRequestUri;
    }

    /**
     * @return The default stream to use
     */
    public PrintStream defaultStream() {
        return defaultPrintStream;
    }

    /**
     * Specify a new default stream to the print to.
     *
     * @param printStream The stream
     * @return A new LogConfig instance
     */
    public LogConfig defaultStream(PrintStream printStream) {
        return new LogConfig(printStream, true, logDetailIfValidationFails, urlEncodeRequestUri, true);
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
        return new LogConfig(defaultPrintStream, shouldEnable, logDetailIfValidationFails, urlEncodeRequestUri, true);
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
        return new LogConfig(defaultPrintStream, prettyPrintingEnabled, logDetail, urlEncodeRequestUri, true);
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
        return new LogConfig(defaultPrintStream, prettyPrintingEnabled, logDetailIfValidationFails, urlEncodeRequestUri, true);
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
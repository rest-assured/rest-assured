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

package com.jayway.restassured.config;

import org.apache.commons.lang.Validate;

import java.io.PrintStream;

/**
 * Configure the logging for REST Assured. <p>Note that <i>only</i>
 * log things known to REST Assured (i.e. the request- and response specifications) will be logged. If you need to log what's <i>actually</i> sent on the wire
 * refer to the <a href="http://hc.apache.org/httpcomponents-client-ga/logging.html">HTTP Client logging docs</a> or use an external tool such as
 * <a href="http://www.wireshark.org/">Wireshark</a>.</p>
 */
public class LogConfig {

    private final PrintStream defaultPrintStream;

    /**
     * Configure the default stream to use the System.out stream (default).
     */
    public LogConfig() {
        this(System.out);
    }

    /**
     * Configure the default stream where logs should be written if <i>not</i> specified explicitly by a filter. I.e. this stream will be used in cases
     * where the log specification DSL is used, e.g.
     * <pre>
     * given().log().all()...
     * </pre>
     * or
     * <pre>
     * expect().log.ifError(). ..
     * </pre>
     *
     * It will not override explicit streams defined by using the {@link com.jayway.restassured.filter.log.RequestLoggingFilter} or the {@link com.jayway.restassured.filter.log.ResponseLoggingFilter}.
     *
     * @param defaultPrintStream The default print stream to use for the {@link com.jayway.restassured.specification.LogSpecification}'s.
     */
    public LogConfig(PrintStream defaultPrintStream) {
        Validate.notNull(defaultPrintStream, "Stream to write logs to cannot be null");
        this.defaultPrintStream = defaultPrintStream;
    }

    /**
     * @return The default stream to use
     */
    public PrintStream defaultStream() {
        return defaultPrintStream;
    }

    public LogConfig defaultStream(PrintStream printStream) {
        return new LogConfig(printStream);
    }

    /**
     * @return A static way to create a new LogConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static LogConfig logConfig() {
        return new LogConfig();
    }
}

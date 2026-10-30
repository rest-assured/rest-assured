/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.log;

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;

import java.io.PrintStream;

/**
 * The filter of <code>log()</code> on a request specification and of <code>log(..)</code> on a spec builder. It takes
 * the blacklisted headers and the URL encoding of the request URI from the log config of the request when it logs, and
 * the print stream and pretty printing too unless they're given. So a log config set after <code>log()</code>, or the
 * one of the specification that a built specification is merged into, is the one that applies.
 */
public class LogConfigRequestLoggingFilter extends RequestLoggingFilter {

    /**
     * @param logDetail         The log detail
     * @param shouldPrettyPrint <code>true</code> if pretty-printing of the body should occur, or <code>null</code> to take it from the log config
     * @param stream            The stream to log to, or <code>null</code> to take it from the log config
     */
    public LogConfigRequestLoggingFilter(LogDetail logDetail, Boolean shouldPrettyPrint, PrintStream stream) {
        super(shouldPrettyPrint, logDetail, stream, null, null);
    }
}

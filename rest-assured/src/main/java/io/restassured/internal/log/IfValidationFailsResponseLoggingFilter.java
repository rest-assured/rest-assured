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
import io.restassured.filter.log.ResponseLoggingFilter;

import java.io.PrintStream;

/**
 * The filter of logging the response if validation fails. It logs every response, since it runs before the validation,
 * but it logs a placeholder instead of the body of a streaming response (<code>text/event-stream</code>). Reading
 * such a body waits until the server ends it, which may be never, even when the validation passes without the body.
 */
public class IfValidationFailsResponseLoggingFilter extends ResponseLoggingFilter {

    /**
     * @param logDetail         The log detail
     * @param shouldPrettyPrint <code>true</code> if pretty-printing of the body should occur
     * @param stream            The stream to log to
     */
    public IfValidationFailsResponseLoggingFilter(LogDetail logDetail, boolean shouldPrettyPrint, PrintStream stream) {
        super(logDetail, shouldPrettyPrint, stream, true);
    }
}

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

package io.restassured.common.exception;

import java.net.SocketTimeoutException;

/**
 * Thrown when waiting for the response takes longer than the read timeout
 * ({@code httpClientConfig().readTimeout(..)}), whichever HTTP client sends the request.
 */
public class ResponseTimeoutException extends SocketTimeoutException {

    /**
     * @param message the message
     * @param cause   the exception of the HTTP client, or {@code null}
     */
    public ResponseTimeoutException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}

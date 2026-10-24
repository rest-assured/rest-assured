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

import java.net.ProtocolException;

/**
 * Thrown when a request is redirected more often than {@code RedirectConfig.maxRedirects(..)} allows, whichever
 * HTTP client sends the request. A circular redirect, or a relative redirect that
 * {@code RedirectConfig.rejectRelativeRedirect(true)} rejects, throws a plain {@link ProtocolException}.
 */
public class TooManyRedirectsException extends ProtocolException {

    /**
     * @param message the message
     */
    public TooManyRedirectsException(String message) {
        super(message);
    }
}

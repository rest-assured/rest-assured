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

package io.restassured.internal.http;

import java.io.IOException;

/**
 * An exception about a response, with its status code and reason phrase. The message is the one of Apache
 * HttpClient 4's {@code HttpResponseException}, which this class extended before REST Assured 7.0.0.
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 * @since 0.5
 */
public class HttpResponseException extends IOException {

    private static final long serialVersionUID = -34809347677236L;

    private final int statusCode;
    private final String reasonPhrase;

    public HttpResponseException(int statusCode, String reasonPhrase) {
        this(statusCode, reasonPhrase, null);
    }

    protected HttpResponseException(int statusCode, String reasonPhrase, Throwable cause) {
        super("status code: " + statusCode + (reasonPhrase == null || reasonPhrase.isBlank() ? "" : ", reason phrase: " + reasonPhrase), cause);
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}

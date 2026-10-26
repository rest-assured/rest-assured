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

package io.restassured.internal.http.client;

import io.restassured.common.exception.ConnectionTimeoutException;
import io.restassured.common.exception.ResponseTimeoutException;
import org.apache.http.conn.ConnectTimeoutException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * Turns the timeout exceptions of the HTTP clients into REST Assured's, so that a timeout throws the same exception
 * whichever client sends the request. Clients are asked to throw REST Assured's exceptions themselves, and this
 * covers the JDK client and HttpClient 4.
 */
public final class TransportExceptions {

    private TransportExceptions() {
    }

    /**
     * @return A {@link ConnectionTimeoutException} or {@link ResponseTimeoutException} with the exception as its cause
     * if it's a timeout of a client, else the exception
     */
    public static IOException translate(IOException e) {
        if (e instanceof ConnectionTimeoutException || e instanceof ResponseTimeoutException) {
            return e;
        } else if (e instanceof HttpConnectTimeoutException || e instanceof ConnectTimeoutException) {
            return new ConnectionTimeoutException(e.getMessage(), e);
        } else if (e instanceof HttpTimeoutException || e instanceof SocketTimeoutException) {
            return new ResponseTimeoutException(e.getMessage(), e);
        }
        return e;
    }
}

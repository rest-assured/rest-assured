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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;

/**
 * Turns the timeout exceptions of the HTTP clients into REST Assured's, so that a timeout throws the same exception
 * whichever client sends the request, and a host that can't be resolved an {@link UnknownHostException}. Clients are
 * asked to throw REST Assured's exceptions themselves, and this covers the JDK client's.
 */
public final class TransportExceptions {

    private TransportExceptions() {
    }

    /**
     * {@link #translate(IOException)}, and an {@link UnknownHostException} for the host, with the exception as its
     * cause, if the exception is the JDK client's {@link ConnectException} for an address that it couldn't resolve
     *
     * @param host The host that the client connects to: the proxy, if the request goes through one, else the host of
     *             the request
     */
    public static IOException translate(IOException e, String host) {
        if (e instanceof ConnectException) {
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof UnresolvedAddressException) {
                    UnknownHostException unknownHost = new UnknownHostException(host);
                    unknownHost.initCause(e);
                    return unknownHost;
                }
            }
        }
        return translate(e);
    }

    /**
     * @return A {@link ConnectionTimeoutException} or {@link ResponseTimeoutException} with the exception as its cause
     * if it's a timeout of a client, else the exception
     */
    public static IOException translate(IOException e) {
        if (e instanceof ConnectionTimeoutException || e instanceof ResponseTimeoutException) {
            return e;
        } else if (e instanceof HttpConnectTimeoutException) {
            return new ConnectionTimeoutException(e.getMessage(), e);
        } else if (e instanceof HttpTimeoutException || e instanceof SocketTimeoutException) {
            return new ResponseTimeoutException(e.getMessage(), e);
        }
        return e;
    }
}

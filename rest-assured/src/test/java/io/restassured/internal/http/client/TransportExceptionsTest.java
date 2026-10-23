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
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class TransportExceptionsTest {

    @Test
    void connect_and_connection_pool_timeouts_become_connection_timeout_exceptions() {
        assertTranslated(new HttpConnectTimeoutException("HTTP connect timed out"), ConnectionTimeoutException.class);
        assertTranslated(new ConnectTimeoutException("Connect to localhost:1 failed: connect timed out"), ConnectionTimeoutException.class);
        assertTranslated(new ConnectionPoolTimeoutException("Timeout waiting for connection from pool"), ConnectionTimeoutException.class);
    }

    @Test
    void read_timeouts_become_response_timeout_exceptions() {
        assertTranslated(new HttpTimeoutException("request timed out"), ResponseTimeoutException.class);
        assertTranslated(new SocketTimeoutException("Read timed out"), ResponseTimeoutException.class);
    }

    @Test
    void rest_assureds_exceptions_and_other_exceptions_stay_as_they_are() {
        IOException[] exceptions = {new ConnectionTimeoutException("connect", null), new ResponseTimeoutException("read", null),
                new ConnectException("Connection refused"), new IOException("other")};
        for (IOException exception : exceptions) {
            assertThat(TransportExceptions.translate(exception)).isSameAs(exception);
        }
    }

    private static void assertTranslated(IOException exception, Class<? extends SocketTimeoutException> type) {
        assertThat(TransportExceptions.translate(exception)).isExactlyInstanceOf(type).hasMessage(exception.getMessage()).hasCause(exception);
    }
}

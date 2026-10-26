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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;

import static org.assertj.core.api.Assertions.assertThat;

class TransportExceptionsTest {

    @Test
    void connect_timeouts_become_connection_timeout_exceptions() {
        assertTranslated(new HttpConnectTimeoutException("HTTP connect timed out"), ConnectionTimeoutException.class);
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

    @Test
    void a_connect_exception_for_an_unresolved_address_becomes_an_unknown_host_exception_for_the_host() {
        // What the JDK client throws on Java 17 and 21 for a host that can't be resolved
        ConnectException exception = new ConnectException();
        ConnectException inner = new ConnectException();
        inner.initCause(new UnresolvedAddressException());
        exception.initCause(inner);

        assertThat(TransportExceptions.translate(exception, "unknown.invalid")).isExactlyInstanceOf(UnknownHostException.class)
                .hasMessage("unknown.invalid").hasCause(exception);
    }

    @Test
    void other_connect_exceptions_and_timeouts_are_translated_as_without_a_host() {
        ConnectException refused = new ConnectException("Connection refused");
        assertThat(TransportExceptions.translate(refused, "localhost")).isSameAs(refused);
        assertThat(TransportExceptions.translate(new HttpConnectTimeoutException("HTTP connect timed out"), "localhost"))
                .isExactlyInstanceOf(ConnectionTimeoutException.class);
    }

    private static void assertTranslated(IOException exception, Class<? extends SocketTimeoutException> type) {
        assertThat(TransportExceptions.translate(exception)).isExactlyInstanceOf(type).hasMessage(exception.getMessage()).hasCause(exception);
    }
}

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

package io.restassured.internal.support;

import io.restassured.config.ConnectionConfig;
import io.restassured.internal.http.client.TransportExceptions;
import org.apache.http.conn.ClientConnectionManager;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * The body of a response that is read after the request, which turns the timeouts of the clients into REST Assured's.
 */
public class CloseHTTPClientConnectionInputStreamWrapper extends InputStream {
    private static final int MINUS_ONE = -1;
    private static final int ZERO = 0;
    private ConnectionConfig connectionConfig;
    private final ClientConnectionManager connectionManager;
    private final Closeable onClose;
    private final InputStream wrapped;

    public CloseHTTPClientConnectionInputStreamWrapper(ConnectionConfig connectionConfig,
                                                       ClientConnectionManager connectionManager, InputStream wrapped) {
        this.connectionConfig = connectionConfig;
        this.connectionManager = connectionManager;
        this.onClose = null;
        this.wrapped = wrapped;
    }

    /**
     * @param onClose closed before the wrapped stream is closed
     */
    public CloseHTTPClientConnectionInputStreamWrapper(Closeable onClose, InputStream wrapped) {
        this.connectionManager = null;
        this.onClose = onClose;
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        try {
            return wrapped == null ? MINUS_ONE : wrapped.read();
        } catch (IOException e) {
            throw TransportExceptions.translate(e);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            return wrapped == null ? MINUS_ONE : wrapped.read(b);
        } catch (IOException e) {
            throw TransportExceptions.translate(e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return wrapped == null ? MINUS_ONE : wrapped.read(b, off, len);
        } catch (IOException e) {
            throw TransportExceptions.translate(e);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
            return wrapped == null ? ZERO : wrapped.skip(n);
        } catch (IOException e) {
            throw TransportExceptions.translate(e);
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return wrapped == null ? ZERO : wrapped.available();
        } catch (IOException e) {
            throw TransportExceptions.translate(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (onClose != null) {
            onClose.close();
        }
        if (connectionManager != null && connectionConfig.shouldCloseIdleConnectionsAfterEachResponse()) {
            connectionManager.closeIdleConnections(connectionConfig.closeIdleConnectionConfig().getIdleTime(),
                    connectionConfig.closeIdleConnectionConfig().getTimeUnit());
        }
        if (wrapped != null) {
            wrapped.close();
        }
    }

    @Override
    public void mark(int readlimit) {
        if (wrapped != null) {
            wrapped.mark(readlimit);
        }
    }

    @Override
    public void reset() throws IOException {
        if (wrapped != null) {
            wrapped.reset();
        }
    }

    @Override
    public boolean markSupported() {
        return wrapped != null && wrapped.markSupported();
    }
}

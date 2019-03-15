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
import org.apache.http.conn.ClientConnectionManager;

import java.io.IOException;
import java.io.InputStream;

public class CloseHTTPClientConnectionInputStreamWrapper extends InputStream {
    private static final int MINUS_ONE = -1;
    private static final int ZERO = 0;
    private ConnectionConfig connectionConfig;
    private final ClientConnectionManager connectionManager;
    private final InputStream wrapped;

    public CloseHTTPClientConnectionInputStreamWrapper(ConnectionConfig connectionConfig,
                                                       ClientConnectionManager connectionManager, InputStream wrapped) {
        this.connectionConfig = connectionConfig;
        this.connectionManager = connectionManager;
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        return wrapped == null ? MINUS_ONE : wrapped.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return wrapped == null ? MINUS_ONE : wrapped.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return wrapped == null ? MINUS_ONE : wrapped.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return wrapped == null ? ZERO : wrapped.skip(n);
    }

    @Override
    public int available() throws IOException {
        return wrapped == null ? ZERO : wrapped.available();
    }

    @Override
    public void close() throws IOException {
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

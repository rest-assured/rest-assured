/*
 * Copyright 2012 the original author or authors.
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

package com.jayway.restassured.internal.support;

import org.apache.http.conn.ClientConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class CloseHTTPClientConnectionInputStreamWrapper extends InputStream {
    private final ClientConnectionManager connectionManager;
    private final InputStream wrapped;

    public CloseHTTPClientConnectionInputStreamWrapper(ClientConnectionManager connectionManager, InputStream wrapped) {
        this.connectionManager = connectionManager;
        this.wrapped = wrapped;
    }

    @Override
    public int read() throws IOException {
        return wrapped.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return wrapped.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return wrapped.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        if(connectionManager != null) {
            connectionManager.closeIdleConnections(0, TimeUnit.NANOSECONDS);
        }
        wrapped.close();
    }

    @Override
    public void mark(int readlimit) {
        wrapped.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        wrapped.reset();
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }
}

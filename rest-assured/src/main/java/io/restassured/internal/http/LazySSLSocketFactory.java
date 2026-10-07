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

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SchemeLayeredSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.function.Supplier;

/**
 * An https scheme socket factory that creates the underlying {@link SSLSocketFactory} the first time an https
 * connection is actually opened. Used when a request starts out as plain http but may be redirected to https, so that
 * the user's SSL configuration is applied to the https hop without loading key or trust stores for requests that never
 * leave http.
 */
public class LazySSLSocketFactory implements SchemeLayeredSocketFactory {

    private final Supplier<SSLSocketFactory> factorySupplier;
    private volatile SSLSocketFactory delegate;

    public LazySSLSocketFactory(Supplier<SSLSocketFactory> factorySupplier) {
        if (factorySupplier == null) {
            throw new IllegalArgumentException("SSLSocketFactory supplier cannot be null");
        }
        this.factorySupplier = factorySupplier;
    }

    private SSLSocketFactory delegate() {
        SSLSocketFactory result = delegate;
        if (result == null) {
            synchronized (this) {
                result = delegate;
                if (result == null) {
                    result = factorySupplier.get();
                    delegate = result;
                }
            }
        }
        return result;
    }

    @Override
    public Socket createLayeredSocket(Socket socket, String target, int port, HttpParams params) throws IOException, UnknownHostException {
        return delegate().createLayeredSocket(socket, target, port, params);
    }

    @Override
    public Socket createSocket(HttpParams params) throws IOException {
        return delegate().createSocket(params);
    }

    @Override
    public Socket connectSocket(Socket sock, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpParams params)
            throws IOException, UnknownHostException, ConnectTimeoutException {
        return delegate().connectSocket(sock, remoteAddress, localAddress, params);
    }

    @Override
    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return delegate().isSecure(sock);
    }
}

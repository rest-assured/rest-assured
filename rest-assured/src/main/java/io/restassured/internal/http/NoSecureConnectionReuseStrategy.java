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

package io.restassured.internal.http;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpConnection;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * A {@link ConnectionReuseStrategy} that never keeps secure (https) connections alive and delegates to another strategy
 * for all other connections.
 * <p>
 * Used for requests that register request specific SSL settings (such as relaxed HTTPS validation) on the http client. Without
 * it a connection established with those settings could be kept in the connection pool and reused, without validating the
 * server certificate again, by a later request that doesn't have the same SSL settings when the http client instance is reused.
 * </p>
 */
public class NoSecureConnectionReuseStrategy implements ConnectionReuseStrategy {

    private final ConnectionReuseStrategy delegate;

    public NoSecureConnectionReuseStrategy(ConnectionReuseStrategy delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate connection reuse strategy cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public boolean keepAlive(HttpResponse response, HttpContext context) {
        if (isSecure(context)) {
            return false;
        }
        return delegate.keepAlive(response, context);
    }

    private static boolean isSecure(HttpContext context) {
        if (context == null) {
            return false;
        }
        Object connection = context.getAttribute(ExecutionContext.HTTP_CONNECTION);
        if (connection instanceof ManagedClientConnection) {
            return ((ManagedClientConnection) connection).isSecure();
        }
        Object targetHost = context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        return connection instanceof HttpConnection && targetHost instanceof HttpHost && "https".equalsIgnoreCase(((HttpHost) targetHost).getSchemeName());
    }
}

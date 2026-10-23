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

import io.restassured.config.ConnectionConfig;
import io.restassured.internal.transport.HttpTransport;
import io.restassured.internal.transport.TransportRequest;
import io.restassured.internal.transport.TransportResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Sends requests with the HttpClient 4 client of an {@link HTTPBuilder}.
 */
public class HttpClient4Transport implements HttpTransport {
    // A response keeps the single connection of HttpClient 4's BasicClientConnectionManager until its body is read, which
    // a body that is read later, or one that couldn't be parsed, may never be. So the next request with the same (reused)
    // client consumes it if it's still unread, since that request fails otherwise. Other connection managers have more
    // connections, which other threads may still be reading from.
    private static final Map<AbstractHttpClient, HttpEntity> UNREAD_BODIES = Collections.synchronizedMap(new WeakHashMap<>());

    private final AbstractHttpClient client;
    private final ConnectionConfig connectionConfig;
    private HttpContext context;

    public HttpClient4Transport(HTTPBuilder http, ConnectionConfig connectionConfig) {
        this.client = http.getClient();
        this.connectionConfig = connectionConfig;
    }

    @Override
    public TransportResponse send(TransportRequest request) throws IOException {
        HttpRequestBase httpRequest = HttpRequestFactory.createHttpRequest(request.uri(), request.method(), request.body() != null);
        for (Map.Entry<String, String> header : request.headers()) {
            httpRequest.addHeader(header.getKey(), header.getValue());
        }
        if (request.body() != null) {
            ((HttpEntityEnclosingRequest) httpRequest).setEntity(RequestBodyToHttpClient4Entity.toHttpEntity(request.body()));
        }

        HttpEntity unreadBody = UNREAD_BODIES.remove(client);
        if (unreadBody != null) EntityUtils.consumeQuietly(unreadBody);
        HttpContextDecorator httpContext = new HttpContextDecorator();
        HttpResponse response = client.execute(httpRequest, httpContext);
        context = httpContext.getDelegate();
        HttpEntity entity = response.getEntity();
        if (entity != null && client.getConnectionManager() instanceof BasicClientConnectionManager) {
            UNREAD_BODIES.put(client, entity);
        }
        return toTransportResponse(response, entity == null ? null : entity.getContent(), this::closeIdleConnectionsIfConfigured);
    }

    /**
     * @return The context of the last request, which knows the URI after redirects
     */
    public HttpContext context() {
        return context;
    }

    // Closes idle connections to the server once REST Assured is done with the response
    private void closeIdleConnectionsIfConfigured() {
        if (connectionConfig.shouldCloseIdleConnectionsAfterEachResponse()) {
            ConnectionConfig.CloseIdleConnectionConfig closeConnectionConfig = connectionConfig.closeIdleConnectionConfig();
            client.getConnectionManager().closeIdleConnections(closeConnectionConfig.getIdleTime(), closeConnectionConfig.getTimeUnit());
        }
    }

    /**
     * @param body the body of the response, or <code>null</code>
     * @return the response as a {@link TransportResponse}
     */
    public static TransportResponse toTransportResponse(HttpResponse response, InputStream body, Closeable onClose) {
        List<Map.Entry<String, String>> headers = new ArrayList<>();
        for (Header header : response.getAllHeaders()) {
            headers.add(new AbstractMap.SimpleImmutableEntry<>(header.getName(), header.getValue()));
        }
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        long contentLength = entity == null ? -1 : entity.getContentLength();
        return new TransportResponse(statusLine.getProtocolVersion().toString(), statusLine.getStatusCode(), statusLine.getReasonPhrase(),
                headers, contentLength, body, onClose);
    }
}

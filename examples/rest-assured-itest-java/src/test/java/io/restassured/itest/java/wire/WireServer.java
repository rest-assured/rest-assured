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

package io.restassured.itest.java.wire;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.eclipse.jetty.http.HttpMethod.CONNECT;

/**
 * A Jetty server that records every request it receives, so that tests can check what REST Assured sends on the wire,
 * whichever HTTP client sends it. Each path answers with the {@link Responder} registered for it, or with
 * {@code 200 ok} as {@code text/plain}. A {@code CONNECT} is recorded and refused with a 405, since the server doesn't
 * open tunnels.
 * <p>
 * The server listens on all interfaces, so {@code localhost} and {@code 127.0.0.1} reach the same server as two
 * different hosts.
 */
public final class WireServer implements AutoCloseable {
    private final Server server;
    private final ServerConnector http;
    private final ServerConnector https;
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private final Map<String, Responder> responders = new ConcurrentHashMap<>();

    private WireServer(String keyStoreResource, String keyStorePassword, String keyStoreType) throws Exception {
        server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSendDateHeader(false);
        httpConfig.setSendServerVersion(false);
        http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(http);

        if (keyStoreResource == null) {
            https = null;
        } else {
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(Objects.requireNonNull(WireServer.class.getClassLoader().getResource(keyStoreResource), keyStoreResource).toExternalForm());
            sslContextFactory.setKeyStorePassword(keyStorePassword);
            sslContextFactory.setKeyStoreType(keyStoreType);
            // Lets tests check the client's host name verification, which Jetty would otherwise also do on its side
            sslContextFactory.setEndpointIdentificationAlgorithm(null);
            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer(false));
            https = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
            server.addConnector(https);
        }

        server.setHandler(new RecordingHandler());
        server.start();
    }

    /**
     * Starts a server that only speaks plain HTTP.
     */
    public static WireServer start() throws Exception {
        return new WireServer(null, null, null);
    }

    /**
     * Starts a server that also speaks HTTPS with the key in the given class path key store.
     */
    public static WireServer startWithTls(String keyStoreResource, String keyStorePassword, String keyStoreType) throws Exception {
        return new WireServer(keyStoreResource, keyStorePassword, keyStoreType);
    }

    public int port() {
        return http.getLocalPort();
    }

    public int httpsPort() {
        if (https == null) {
            throw new IllegalStateException("The server was started without TLS");
        }
        return https.getLocalPort();
    }

    /**
     * @return {@code http://localhost:<port><path>}
     */
    public String url(String path) {
        return url("localhost", path);
    }

    /**
     * @return {@code http://<host>:<port><path>}
     */
    public String url(String host, String path) {
        return "http://" + host + ":" + port() + path;
    }

    /**
     * @return {@code https://<host>:<https port><path>}
     */
    public String httpsUrl(String host, String path) {
        return "https://" + host + ":" + httpsPort() + path;
    }

    /**
     * Answers requests to {@code path} (without the query) with {@code responder} instead of {@code 200 ok}.
     */
    public WireServer respond(String path, Responder responder) {
        responders.put(path, responder);
        return this;
    }

    /**
     * @return the requests received since the last {@link #reset()}, oldest first
     */
    public List<RecordedRequest> requests() {
        return new ArrayList<>(requests);
    }

    /**
     * @return the only request received since the last {@link #reset()}
     * @throws AssertionError if there were none or several
     */
    public RecordedRequest onlyRequest() {
        List<RecordedRequest> received = requests();
        if (received.size() != 1) {
            throw new AssertionError("Expected exactly one request but got " + received.size() + ":\n" + received);
        }
        return received.get(0);
    }

    /**
     * @return the last request received since the last {@link #reset()}
     * @throws AssertionError if there were none
     */
    public RecordedRequest lastRequest() {
        List<RecordedRequest> received = requests();
        if (received.isEmpty()) {
            throw new AssertionError("No request was received");
        }
        return received.get(received.size() - 1);
    }

    /**
     * Forgets the recorded requests and the registered responders.
     */
    public void reset() {
        requests.clear();
        responders.clear();
    }

    @Override
    public void close() throws Exception {
        server.stop();
    }

    // The request target as sent: host:port for a CONNECT, the absolute URI when sent to a proxy, otherwise the still
    // encoded path and query. HttpURI.toString() would add the authority from the Host header to the latter.
    private static String requestTarget(String method, HttpURI uri) {
        if (CONNECT.is(method)) {
            return uri.getHost() + ":" + uri.getPort();
        }
        return uri.getScheme() == null ? uri.getPathQuery() : uri.toString();
    }

    private class RecordingHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
            baseRequest.setHandled(true);
            List<Map.Entry<String, String>> headers = new ArrayList<>();
            for (HttpField field : baseRequest.getHttpFields()) {
                headers.add(new SimpleImmutableEntry<>(field.getName(), field.getValue()));
            }
            byte[] body = request.getInputStream().readAllBytes();
            RecordedRequest recorded = new RecordedRequest(request.getMethod(), requestTarget(request.getMethod(), baseRequest.getHttpURI()),
                    request.getProtocol(), request.getServerName(), request.getLocalPort(), headers, body);
            requests.add(recorded);

            if (CONNECT.is(request.getMethod())) {
                Responder.text(405, "WireServer doesn't open tunnels").respond(recorded, response);
            } else {
                responders.getOrDefault(request.getRequestURI(), Responder.ok()).respond(recorded, response);
            }
        }
    }
}

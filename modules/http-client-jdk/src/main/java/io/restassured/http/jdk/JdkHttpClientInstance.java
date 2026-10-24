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

package io.restassured.http.jdk;

import io.restassured.common.exception.ConnectionTimeoutException;
import io.restassured.common.exception.ResponseTimeoutException;
import io.restassured.common.spi.http.ClientCustomizer;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HostnameVerification;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.common.spi.http.UnsupportedFeatureException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One {@link HttpClient} with the settings of REST Assured: HTTP/1.1, no redirects, no cookies and no authenticator,
 * since REST Assured follows redirects, keeps cookies and answers challenges itself.
 */
final class JdkHttpClientInstance implements HttpClientInstance {
    // HttpClient.close() from Java 21
    private static final MethodHandle CLOSE = findVirtual(HttpClient.class, "close", MethodType.methodType(void.class));
    // HttpRequest.Builder.HEAD() from Java 18, which sends no Content-Length, unlike method("HEAD", noBody())
    private static final MethodHandle HEAD = findVirtual(java.net.http.HttpRequest.Builder.class, "HEAD",
            MethodType.methodType(java.net.http.HttpRequest.Builder.class));
    // A writer body up to this size is written to memory, so that it's sent with a Content-Length and can be resent
    private static final long BUFFERED_WRITER_LIMIT = 1024 * 1024;
    private static final String TUNNELING_DISABLED_SCHEMES = "jdk.http.auth.tunneling.disabledSchemes";
    // Read once, as the JDK client reads it (its default, in net.properties, is Basic)
    private static final boolean TUNNELING_DISABLES_BASIC = disablesBasic(System.getProperty(TUNNELING_DISABLED_SCHEMES, "Basic"));
    // The headers the JDK client refuses unless -Djdk.httpclient.allowRestrictedHeaders names them (Java 17 to 21).
    // Content-Length is restricted too, but REST Assured rejects it before any client sees it.
    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "expect", "host", "upgrade");
    private static final int PROXY_AUTHENTICATION_REQUIRED = 407;
    private static final AtomicBoolean WARNED_ABOUT_CONNECTION_REQUEST_TIMEOUT = new AtomicBoolean();

    private final ClientSettings settings;
    private final boolean tunnelingDisablesBasic;
    // The Proxy-Authorization header to answer a 407 with, or null
    private final String proxyAuthorization;
    // Both are created at their first request, so that the customizers run once for each client that's used, and TLS
    // settings that can't be loaded don't fail plain HTTP. This one is for plain HTTP, and for HTTPS without TLS settings.
    private volatile HttpClient plain;
    // For HTTPS with the TLS settings
    private volatile HttpClient secure;

    JdkHttpClientInstance(ClientSettings settings) {
        this(settings, TUNNELING_DISABLES_BASIC);
    }

    JdkHttpClientInstance(ClientSettings settings, boolean tunnelingDisablesBasic) {
        ProxySettings proxy = settings.proxy();
        if (proxy != null && "https".equalsIgnoreCase(proxy.scheme())) {
            throw new UnsupportedFeatureException("connect to a proxy over TLS (a proxy with the scheme https)", null);
        }
        if (isPositive(settings.connectionRequestTimeout()) && WARNED_ABOUT_CONNECTION_REQUEST_TIMEOUT.compareAndSet(false, true)) {
            System.getLogger("io.restassured").log(System.Logger.Level.WARNING, "httpClientConfig().connectionRequestTimeout(..) has no "
                    + "effect on the JDK HTTP client, which doesn't limit its connections, so a request never waits for one.");
        }
        this.settings = settings;
        this.tunnelingDisablesBasic = tunnelingDisablesBasic;
        proxyAuthorization = proxy == null || proxy.userName() == null ? null : basic(proxy.userName(), proxy.password());
    }

    private static HttpClient build(ClientSettings settings, SSLContext sslContext) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(settings.version() == HttpVersion.HTTP_2 ? Version.HTTP_2 : Version.HTTP_1_1);
        if (isPositive(settings.connectTimeout())) {
            builder.connectTimeout(settings.connectTimeout());
        }
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        // The JDK client resolves an unresolved address at each connect, so a long-lived client follows the proxy's address
        builder.proxy(settings.proxy() == null ? new DefaultProxySelector()
                : ProxySelector.of(InetSocketAddress.createUnresolved(settings.proxy().host(), settings.proxy().port())));
        for (ClientCustomizer customizer : settings.customizers()) {
            customizer.applyTo(builder);
        }
        // REST Assured follows redirects and keeps cookies itself, whatever the customizers did
        builder.followRedirects(Redirect.NEVER);
        if (!settings.customizers().isEmpty()) {
            builder.cookieHandler(NoCookies.INSTANCE);
        }
        HttpClient client = builder.build();
        if (client.authenticator().isPresent()) {
            close(client);
            // The JDK client answers every 401 and 407 itself once it has an authenticator, and fails when it has no credentials
            throw new UnsupportedFeatureException("use an Authenticator set with customizeClient(..), since REST Assured answers "
                    + "authentication challenges itself", "use auth() or a proxy with a user name and password instead");
        }
        return client;
    }

    private HttpClient clientFor(URI uri) throws SSLException {
        boolean withTlsSettings = settings.tls() != null && "https".equalsIgnoreCase(uri.getScheme());
        HttpClient client = withTlsSettings ? secure : plain;
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (withTlsSettings) {
                if (secure == null) {
                    secure = build(settings, sslContext(settings.tls()));
                }
                return secure;
            }
            if (plain == null) {
                plain = build(settings, defaultSslContext());
            }
            return plain;
        }
    }

    // A new context, like HttpClient 4's, so that a new client reads the javax.net.ssl trust store properties as they
    // are then. Unlike SSLContext.getDefault(), it doesn't send the client certificate of javax.net.ssl.keyStore.
    private static SSLContext defaultSslContext() throws SSLException {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            return context;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SSLException("Failed to create the default SSLContext: " + e.getMessage(), e);
        }
    }

    private static SSLContext sslContext(TlsSettings tls) throws SSLException {
        SSLContext context;
        try {
            context = tls.createSslContext();
        } catch (IOException | GeneralSecurityException e) {
            throw new SSLException("Failed to create the SSLContext from the configured SSL settings: " + e.getMessage(), e);
        }
        // Relaxed validation trusts every certificate with a trust manager that also skips the host name check
        if (tls.relaxedProtocol() != null || tls.hostnames() == HostnameVerification.STRICT) {
            return context;
        }
        return new HostnameCheckingSslContext(context, tls.hostnames() == HostnameVerification.CUSTOM ? tls.hostnameVerifier() : null);
    }

    @Override
    public HttpResponse send(HttpRequest request) throws IOException {
        if (request.options().ntlm() != null) {
            throw new UnsupportedFeatureException("authenticate with NTLM", null);
        }
        boolean answersProxyChallenge = proxyAuthorization != null && firstHeader(request, "Proxy-Authorization") == null;
        boolean https = "https".equalsIgnoreCase(request.uri().getScheme());
        if (answersProxyChallenge && https && tunnelingDisablesBasic) {
            throw new UnsupportedFeatureException("log in to a proxy with Basic authentication for an HTTPS request, since "
                    + TUNNELING_DISABLED_SCHEMES + " disables Basic for HTTPS tunnels",
                    "start the JVM with -D" + TUNNELING_DISABLED_SCHEMES + "= (an empty value)");
        }
        String method = request.method().toUpperCase(Locale.ROOT);
        OpenBodies bodies = new OpenBodies(request.body());
        try {
            HttpClient client = clientFor(request.uri());
            java.net.http.HttpResponse<InputStream> response = exchange(client, toJdkRequest(method, request, null, bodies));
            // The JDK client only answers a proxy challenge with an Authenticator, which would also answer every 401.
            // The 407 of an HTTPS request answers the CONNECT of the tunnel, before any body was sent.
            if (response.statusCode() == PROXY_AUTHENTICATION_REQUIRED && answersProxyChallenge && offersBasic(response)
                    && (https || request.body() == null || request.body().isRepeatable())) {
                closeBody(response);
                response = exchange(client, toJdkRequest(method, request, proxyAuthorization, bodies));
            }
            return toResponse(method, response, bodies);
        } catch (IOException | RuntimeException | Error e) {
            bodies.close();
            throw e;
        }
    }

    private static java.net.http.HttpResponse<InputStream> exchange(HttpClient client, java.net.http.HttpRequest request) throws IOException {
        try {
            return client.send(request, BodyHandlers.ofInputStream());
        } catch (HttpConnectTimeoutException e) {
            throw new ConnectionTimeoutException(e.getMessage(), e);
        } catch (HttpTimeoutException e) {
            throw new ResponseTimeoutException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InterruptedIOException interrupted = new InterruptedIOException("Interrupted while waiting for the response");
            interrupted.initCause(e);
            throw interrupted;
        }
    }

    private static java.net.http.HttpRequest toJdkRequest(String method, HttpRequest request, String proxyAuthorization, OpenBodies bodies)
            throws IOException {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder(request.uri());
        boolean expectContinue = request.options().expectContinue();
        boolean hasContentType = false;
        for (HttpHeader header : request.headers()) {
            if (header.name().equalsIgnoreCase("Expect")) {
                if (!header.value().equalsIgnoreCase("100-continue")) {
                    throw new UnsupportedFeatureException("send the header Expect: " + header.value() + ", only Expect: 100-continue", null);
                }
                expectContinue = true;
            } else {
                hasContentType |= header.name().equalsIgnoreCase("Content-Type");
                setHeader(builder, header.name(), header.value());
            }
        }
        RequestBody body = request.body();
        if (body != null && body.getContentType() != null && !hasContentType) {
            builder.header("Content-Type", body.getContentType());
        }
        if (proxyAuthorization != null) {
            builder.header("Proxy-Authorization", proxyAuthorization);
        }
        if (expectContinue) {
            builder.expectContinue(true);
        }
        if (isPositive(request.options().readTimeout())) {
            builder.timeout(request.options().readTimeout());
        }
        if (body != null) {
            builder.method(method, publisher(body, bodies));
        } else if (method.equals("GET")) {
            builder.GET();
        } else if (method.equals("DELETE")) {
            builder.DELETE();
        } else if (method.equals("HEAD") && HEAD != null) {
            invoke(HEAD, builder);
        } else {
            builder.method(method, BodyPublishers.noBody());
        }
        return builder.build();
    }

    private static void setHeader(java.net.http.HttpRequest.Builder builder, String name, String value) {
        try {
            builder.header(name, value);
        } catch (IllegalArgumentException e) {
            if (RESTRICTED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                throw new UnsupportedFeatureException("send a " + name + " header",
                        "start the JVM with -Djdk.httpclient.allowRestrictedHeaders=" + name.toLowerCase(Locale.ROOT));
            }
            throw e;
        }
    }

    private static BodyPublisher publisher(RequestBody body, OpenBodies bodies) throws IOException {
        long length = body.getContentLength();
        switch (body.getType()) {
            case BYTES:
                return BodyPublishers.ofByteArray(body.getBytes());
            case FILE:
                return BodyPublishers.ofFile(body.getFile().toPath());
            case STREAM:
                InputStream stream = body.getStream();
                return withLength(BodyPublishers.ofInputStream(() -> stream), length);
            default:
                RequestBody.ContentWriter writer = body.getWriter();
                if (body.isRepeatable() && length != RequestBody.UNKNOWN_LENGTH && length <= BUFFERED_WRITER_LIMIT) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream((int) length);
                    writer.writeTo(out);
                    return BodyPublishers.ofByteArray(out.toByteArray());
                }
                // A new pipe each time the JDK client sends the body, closed with the others of the request
                return withLength(BodyPublishers.ofInputStream(() -> bodies.add(WriterPipe.start(writer))), length);
        }
    }

    // A body of unknown length is sent chunked
    private static BodyPublisher withLength(BodyPublisher publisher, long length) {
        if (length == RequestBody.UNKNOWN_LENGTH) {
            return publisher;
        }
        return length == 0 ? BodyPublishers.noBody() : BodyPublishers.fromPublisher(publisher, length);
    }

    private static HttpResponse toResponse(String method, java.net.http.HttpResponse<InputStream> response, OpenBodies bodies) throws IOException {
        int status = response.statusCode();
        InputStream body = response.body();
        if (method.equals("HEAD") || status / 100 == 1 || status == 204 || status == 304) {
            if (body != null) {
                closeEmpty(body);
            }
            body = null;
        }
        List<HttpHeader> headers = new ArrayList<>();
        for (Map.Entry<String, List<String>> header : response.headers().map().entrySet()) {
            // Leaves out the pseudo headers of HTTP/2, such as :status
            if (!header.getKey().startsWith(":")) {
                for (String value : header.getValue()) {
                    headers.add(new HttpHeader(header.getKey(), value));
                }
            }
        }
        long contentLength;
        try {
            contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        } catch (NumberFormatException e) {
            contentLength = -1;
        }
        if (body != null && contentLength == 0) {
            body = new FilterInputStream(body) {
                private final AtomicBoolean closed = new AtomicBoolean();

                @Override
                public void close() throws IOException {
                    // Reading a closed body fails, so it's read to its end only the first time
                    if (closed.compareAndSet(false, true)) {
                        closeEmpty(in);
                    }
                }
            };
        }
        String protocolVersion = response.version() == Version.HTTP_2 ? "HTTP/2.0" : "HTTP/1.1";
        InputStream responseBody = body;
        Closeable onClose = () -> {
            try (bodies) {
                if (responseBody != null) {
                    responseBody.close();
                }
            }
        };
        // The JDK client gives no reason phrase
        return new HttpResponse(protocolVersion, status, null, headers, contentLength, body, onClose);
    }

    // Closing a body before its end cancels it, which races with the JDK client putting the connection back in its
    // pool, so that the next request may get a new connection. An empty body is read to its end first, which is at once.
    private static void closeEmpty(InputStream body) throws IOException {
        try (body) {
            byte[] buffer = new byte[64];
            while (body.read(buffer) != -1) {
                // An empty body has nothing to discard
            }
        }
    }

    // The JDK client gives no body for a proxy's answer to the CONNECT of a tunnel
    private static void closeBody(java.net.http.HttpResponse<InputStream> response) throws IOException {
        if (response.body() != null) {
            response.body().close();
        }
    }

    private static boolean offersBasic(java.net.http.HttpResponse<?> response) {
        for (String challenge : response.headers().allValues("Proxy-Authenticate")) {
            String scheme = challenge.trim().split("[\\s,]", 2)[0];
            if (scheme.equalsIgnoreCase("Basic")) {
                return true;
            }
        }
        return false;
    }

    static boolean disablesBasic(String schemes) {
        for (String scheme : schemes.split(",")) {
            if (scheme.trim().equalsIgnoreCase("Basic")) {
                return true;
            }
        }
        return false;
    }

    private static String firstHeader(HttpRequest request, String name) {
        for (HttpHeader header : request.headers()) {
            if (header.name().equalsIgnoreCase(name)) {
                return header.value();
            }
        }
        return null;
    }

    private static String basic(String userName, String password) {
        String userNameAndPassword = userName + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(userNameAndPassword.getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }

    /**
     * Closes the client on Java 21 and later, where it can be closed. On Java 17 it does nothing, and the client's
     * connections are closed once they have been idle for a while (the system property jdk.httpclient.keepalive.timeout).
     */
    @Override
    public void close() {
        close(plain);
        close(secure);
    }

    private static void close(HttpClient client) {
        if (CLOSE != null && client != null) {
            invoke(CLOSE, client);
        }
    }

    @Override
    public String toString() {
        return "JdkHttpClientInstance[" + settings + "]";
    }

    private static MethodHandle findVirtual(Class<?> type, String name, MethodType methodType) {
        try {
            return MethodHandles.publicLookup().findVirtual(type, name, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    private static void invoke(MethodHandle method, Object target) {
        try {
            method.invoke(target);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    // Asks the JVM's default selector at each request, so that a shared client sees proxy system properties set later
    private static final class DefaultProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            ProxySelector selector = ProxySelector.getDefault();
            return selector == null ? List.of(Proxy.NO_PROXY) : selector.select(uri);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress address, IOException e) {
            ProxySelector selector = ProxySelector.getDefault();
            if (selector != null) {
                selector.connectFailed(uri, address, e);
            }
        }
    }

    /**
     * The request body's stream and the pipes of its writer that the JDK client reads. They're closed when the request
     * fails and when its response is closed, since the JDK client stops reading a body when the server answers before
     * it has read it all or the request times out, and a writer thread would wait for it forever.
     */
    private static final class OpenBodies implements Closeable {
        private final List<Closeable> opened = new ArrayList<>();
        private boolean closed;

        OpenBodies(RequestBody body) {
            if (body != null && body.getType() == RequestBody.Type.STREAM) {
                opened.add(body.getStream());
            }
        }

        synchronized <T extends Closeable> T add(T body) {
            if (closed) {
                closeQuietly(body);
            } else {
                opened.add(body);
            }
            return body;
        }

        @Override
        public synchronized void close() {
            closed = true;
            opened.forEach(OpenBodies::closeQuietly);
            opened.clear();
        }

        private static void closeQuietly(Closeable body) {
            try {
                body.close();
            } catch (IOException ignored) {
                // The request is over, whatever the body says
            }
        }
    }

    // Takes the place of a cookie handler that a customizer may have set, since a builder can't remove one
    private static final class NoCookies extends CookieHandler {
        static final NoCookies INSTANCE = new NoCookies();

        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) {
            return Map.of();
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) {
        }
    }
}

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

package io.restassured.http.apache5;

import io.restassured.common.exception.ConnectionTimeoutException;
import io.restassured.common.exception.ResponseTimeoutException;
import io.restassured.common.spi.http.ClientCustomizer;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.NtlmCredentials;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import org.apache.hc.client5.http.AuthenticationStrategy;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.auth.AuthChallenge;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.ChallengeType;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultAuthenticationStrategy;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicSchemeFactory;
import org.apache.hc.client5.http.impl.auth.DigestSchemeFactory;
import org.apache.hc.client5.http.impl.auth.NTLMSchemeFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One {@link CloseableHttpClient} with the settings of REST Assured: no retries, redirects, cookies or decompression,
 * and no answers to the challenges of the server except NTLM, since REST Assured does all of that itself. Its proxy
 * login cache lasts as long as the client, so a proxy login is answered once and then sent with every request.
 */
final class Apache5HttpClientInstance implements HttpClientInstance {
    // Marks the context of a request with NTLM credentials, the only challenge of a server that HttpClient answers
    private static final String NTLM = "io.restassured.http.apache5.ntlm";
    // REST Assured follows the redirects and answers the Basic and Digest challenges of the server itself
    private static final AuthenticationStrategy NTLM_ONLY = Apache5HttpClientInstance::selectNtlm;
    // A connection taken from the pool is checked when it has been idle for this long, since nothing is retried
    private static final TimeValue VALIDATE_AFTER_INACTIVITY = TimeValue.ofSeconds(1);
    // httpcore5 5.2 fails a request for a connection at once when another thread holds the pool's lock and the timeout
    // is disabled, so without a connection request timeout the wait is limited to a year instead
    private static final Timeout NO_CONNECTION_REQUEST_TIMEOUT = Timeout.ofDays(365);
    // What HttpClient 5 throws for a request whose connection manager has been shut down
    private static final String POOL_SHUT_DOWN = "Connection pool shut down";
    // From HttpClient 5.4, which asks to upgrade a plain connection to TLS by default (RFC 2817), which HttpClient 4
    // and the JDK client don't. Called by reflection, since the module works with 5.2 and 5.3 too.
    private static final Method SET_PROTOCOL_UPGRADE_ENABLED = protocolUpgradeSetter();
    // The methods that are sent with an empty body when the request has none
    private static final Set<String> SENT_WITH_AN_EMPTY_BODY = Set.of("POST", "PUT", "PATCH", "QUERY");

    private final ClientSettings settings;
    private final PoolingHttpClientConnectionManager connections;
    private final CloseableHttpClient client;
    // The proxy login, if the proxy has a user name
    private final BasicCredentialsProvider credentials = new BasicCredentialsProvider();
    // Shared by the requests, so that a proxy login that was asked for once is sent with every request after it
    private final BasicAuthCache authCache = new BasicAuthCache();
    private final RequestConfig defaultConfig;
    // Thrown for a request over TLS, to the server or to a proxy with the scheme https, so that TLS settings that can't
    // be loaded don't fail plain HTTP
    private final Exception tlsFailure;
    private final boolean proxyOverTls;

    @SuppressWarnings("deprecation")
    Apache5HttpClientInstance(ClientSettings settings) {
        this.settings = settings;
        if (settings.version() == HttpVersion.HTTP_2) {
            // The classic API sends HTTP/1.1 only
            throw new UnsupportedFeatureException("send HTTP/2 requests", null);
        }
        ProxySettings proxy = settings.proxy();
        LayeredConnectionSocketFactory tls;
        Exception failure = null;
        try {
            tls = Tls.socketFactory(settings.tls());
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            failure = e;
            tls = Tls.defaultSocketFactory();
        }
        tlsFailure = failure;
        proxyOverTls = proxy != null && "https".equalsIgnoreCase(proxy.scheme());
        Timeout connectTimeout = timeout(settings.connectTimeout());
        // No limit on the connections, so that streams left unread can't make a request wait for a connection forever
        connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(tls)
                .setMaxConnTotal(Integer.MAX_VALUE)
                .setMaxConnPerRoute(Integer.MAX_VALUE)
                // HttpClient 5.2.1 to 5.4.4 time the TLS handshake of a new connection with the socket timeout of the
                // SocketConfig, whose default is 3 minutes, and 5.6.5 with the connect timeout. So the connect timeout
                // covers the handshake on every version, and without one, the handshake waits as long as it takes.
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(connectTimeout).build())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(connectTimeout)
                        .setSocketTimeout(Timeout.DISABLED)
                        .setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY)
                        .build())
                .build();
        defaultConfig = withoutProtocolUpgrade(RequestConfig.custom()
                .setResponseTimeout(Timeout.DISABLED)
                .setConnectionRequestTimeout(connectionRequestTimeout(settings.connectionRequestTimeout()))
                .setRedirectsEnabled(false)
                .setExpectContinueEnabled(false))
                .build();
        HttpRoutePlanner routes;
        if (proxy == null) {
            // Asks ProxySelector.getDefault() at each request, so it sees the proxy system properties as they are then
            routes = new SystemDefaultRoutePlanner(null);
        } else {
            // A proxy with the scheme https is connected to over TLS, with the same TLS settings as the server
            routes = new DefaultProxyRoutePlanner(new HttpHost(proxy.scheme(), proxy.host(), proxy.port()));
        }
        setProxyLogin(credentials);
        Lookup<AuthSchemeFactory> schemes = RegistryBuilder.<AuthSchemeFactory>create()
                .register(StandardAuthScheme.BASIC, BasicSchemeFactory.INSTANCE)
                .register(StandardAuthScheme.DIGEST, DigestSchemeFactory.INSTANCE)
                .register(StandardAuthScheme.NTLM, NTLMSchemeFactory.INSTANCE)
                .build();
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setConnectionManager(connections)
                .setDefaultAuthSchemeRegistry(schemes)
                .setDefaultCredentialsProvider(credentials)
                .setDefaultRequestConfig(defaultConfig);
        try {
            for (ClientCustomizer customizer : settings.customizers()) {
                customizer.applyTo(builder);
            }
            // What REST Assured does itself, or decides, whatever the customizers did
            client = builder
                    .disableAutomaticRetries()
                    .disableRedirectHandling()
                    .disableContentCompression()
                    .disableCookieManagement()
                    .setTargetAuthenticationStrategy(NTLM_ONLY)
                    .setRoutePlanner(routes)
                    .build();
        } catch (RuntimeException | Error e) {
            connections.close();
            throw e;
        }
    }

    @Override
    public HttpResponse send(HttpRequest request) throws IOException {
        String method = request.method().toUpperCase(Locale.ROOT);
        RequestBody body = request.body();
        try {
            if (body != null && method.equals("TRACE")) {
                // HttpClient 5 refuses it
                throw new UnsupportedFeatureException("send a body with a TRACE request", null);
            }
            URI uri = request.uri();
            if (tlsFailure != null && (proxyOverTls || "https".equalsIgnoreCase(uri.getScheme()))) {
                throwTlsFailure();
            }
            // HttpClient 5 would send the characters that aren't ASCII as '?', the JDK client and HttpClient 4 encode them
            HttpUriRequestBase message = new HttpUriRequestBase(method, URI.create(uri.toASCIIString()));
            if (uri.getRawUserInfo() != null) {
                // HttpClient 5 refuses user info in the request URI, which the JDK client and HttpClient 4 leave out
                message.setAuthority(new URIAuthority(uri.getHost(), uri.getPort()));
            }
            boolean hasContentType = false;
            for (HttpHeader header : request.headers()) {
                message.addHeader(header.name(), header.value());
                hasContentType |= header.name().equalsIgnoreCase("Content-Type");
            }
            if (body != null) {
                if (body.getContentType() != null && !hasContentType) {
                    message.addHeader("Content-Type", body.getContentType());
                }
                message.setEntity(Entities.of(body));
            } else if (SENT_WITH_AN_EMPTY_BODY.contains(method)) {
                // So that every HttpClient 5 sends Content-Length: 0, as the JDK client and HttpClient 4 do. Without an
                // entity, HttpClient 5.2 and 5.3 send none, and neither do 5.4 and 5.5 for a QUERY (HttpCore 5.3).
                message.setEntity(Entities.empty());
            }
            HttpClientContext context = context(message, request.options());
            return toResponse(message, execute(message, context), body);
        } catch (IOException | RuntimeException | Error e) {
            closeStream(body);
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    private HttpClientContext context(HttpUriRequestBase message, RequestOptions options) {
        RequestConfig.Builder config = RequestConfig.copy(defaultConfig)
                .setResponseTimeout(timeout(options.readTimeout()))
                .setExpectContinueEnabled(options.expectContinue());
        HttpClientContext context = HttpClientContext.create();
        context.setAuthCache(authCache);
        NtlmCredentials ntlm = options.ntlm();
        if (ntlm == null) {
            context.setCredentialsProvider(credentials);
        } else {
            // From HttpClient 5.3, NTLM is only answered when it's a preferred scheme
            config.setTargetPreferredAuthSchemes(List.of(StandardAuthScheme.NTLM));
            context.setCredentialsProvider(withNtlm(ntlm));
            context.setAttribute(NTLM, Boolean.TRUE);
        }
        // The request's config replaces the client's default, also one that a customizer set
        message.setConfig(config.build());
        return context;
    }

    @SuppressWarnings("deprecation")
    private BasicCredentialsProvider withNtlm(NtlmCredentials ntlm) {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        setProxyLogin(provider);
        provider.setCredentials(new AuthScope(ntlm.host(), ntlm.port()),
                new NTCredentials(ntlm.userName(), chars(ntlm.password()), ntlm.workstation(), ntlm.domain()));
        return provider;
    }

    private void setProxyLogin(BasicCredentialsProvider provider) {
        ProxySettings proxy = settings.proxy();
        if (proxy != null && proxy.userName() != null) {
            provider.setCredentials(new AuthScope(proxy.host(), proxy.port()), new UsernamePasswordCredentials(proxy.userName(), chars(proxy.password())));
        }
    }

    private ClassicHttpResponse execute(HttpUriRequestBase message, HttpClientContext context) throws IOException {
        try {
            return client.executeOpen(null, message, context);
        } catch (ConnectTimeoutException | ConnectionRequestTimeoutException e) {
            throw new ConnectionTimeoutException(e.getMessage(), e);
        } catch (SocketTimeoutException e) {
            throw new ResponseTimeoutException(e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw connectionManagerShutDown(e);
        }
    }

    // Our own connection manager is only shut down with this client, so it's one that a customizer set and that the
    // client of an earlier request shut down
    private IllegalStateException connectionManagerShutDown(IllegalStateException e) {
        if (!POOL_SHUT_DOWN.equals(e.getMessage()) || settings.customizers().isEmpty()) {
            return e;
        }
        return new IllegalStateException("The connection manager that a customizer of HttpClientBuilder set has been shut down. A client "
                + "shuts its connection manager down when it's closed, and REST Assured closes the client of a request after its response, "
                + "unless httpClientConfig().reuseHttpClientInstance() keeps it. Create the connection manager inside the customizer, or "
                + "create it once, call setConnectionManagerShared(true) on the builder and close it yourself. A limit on the connections "
                + "that holds across requests needs reuseHttpClientInstance() or such a shared connection manager.", e);
    }

    private static HttpResponse toResponse(HttpUriRequestBase message, ClassicHttpResponse response, RequestBody requestBody) throws IOException {
        List<HttpHeader> headers = new ArrayList<>();
        for (Header header : response.getHeaders()) {
            headers.add(new HttpHeader(header.getName(), header.getValue()));
        }
        String protocolVersion = response.getVersion() == null ? "HTTP/1.1" : response.getVersion().format();
        Release release = new Release(message, response, requestBody);
        // None for HEAD, 204 and 304
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return new HttpResponse(protocolVersion, response.getCode(), response.getReasonPhrase(), headers, 0, null, release);
        }
        try {
            release.body = new ResponseBody(entity.getContent(), entity.getContentLength(), release);
        } catch (IOException | RuntimeException e) {
            release.close();
            throw e;
        }
        return new HttpResponse(protocolVersion, response.getCode(), response.getReasonPhrase(), headers, entity.getContentLength(), release.body, release);
    }

    private static Method protocolUpgradeSetter() {
        try {
            return RequestConfig.Builder.class.getMethod("setProtocolUpgradeEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            // HttpClient 5.2 and 5.3 don't ask
            return null;
        }
    }

    private static RequestConfig.Builder withoutProtocolUpgrade(RequestConfig.Builder config) {
        if (SET_PROTOCOL_UPGRADE_ENABLED != null) {
            try {
                SET_PROTOCOL_UPGRADE_ENABLED.invoke(config, false);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Can't turn off the protocol upgrade of Apache HttpClient 5", e);
            }
        }
        return config;
    }

    // As the JDK client does
    private void throwTlsFailure() throws SSLException {
        if (tlsFailure instanceof IllegalArgumentException) {
            throw (IllegalArgumentException) tlsFailure;
        }
        throw new SSLException("Failed to create the SSLContext from the configured SSL settings: " + tlsFailure.getMessage(), tlsFailure);
    }

    private static List<AuthScheme> selectNtlm(ChallengeType type, Map<String, AuthChallenge> challenges, HttpContext context) {
        AuthChallenge ntlm = challenges.get("ntlm");
        if (ntlm == null || context.getAttribute(NTLM) == null) {
            // The 401 goes to REST Assured
            return new ArrayList<>();
        }
        return DefaultAuthenticationStrategy.INSTANCE.select(type, Map.of("ntlm", ntlm), context);
    }

    /**
     * Closes the connections that have been idle for longer than this, as REST Assured 6.x did after each response
     *
     * @return {@code true}
     */
    @Override
    public boolean closeIdleConnections(Duration idleTime) {
        connections.closeIdle(TimeValue.ofMilliseconds(idleTime.toMillis()));
        return true;
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            // The connections are closed, whatever closing them says
        }
        // The client doesn't own it when a customizer set a connection manager of its own
        connections.close();
    }

    @Override
    public String toString() {
        return "Apache5HttpClientInstance[" + settings + "]";
    }

    private static Timeout timeout(Duration duration) {
        return duration == null || duration.isZero() || duration.isNegative() ? Timeout.DISABLED : Timeout.ofMilliseconds(duration.toMillis());
    }

    private static Timeout connectionRequestTimeout(Duration duration) {
        Timeout timeout = timeout(duration);
        return timeout == Timeout.DISABLED ? NO_CONNECTION_REQUEST_TIMEOUT : timeout;
    }

    private static char[] chars(String password) {
        return password == null ? new char[0] : password.toCharArray();
    }

    // The stream of a body is closed when the request fails or its response is closed, also when HttpClient didn't send it
    private static void closeStream(RequestBody body) {
        if (body != null && body.getType() == RequestBody.Type.STREAM) {
            try {
                body.getStream().close();
            } catch (IOException | RuntimeException ignored) {
                // The request is over, whatever the body says, which mustn't hide why it failed
            }
        }
    }

    /**
     * Tells HttpClient that REST Assured is done with a response. A body that was read to its end gives its connection
     * back to the pool. Otherwise the connection is dropped, which takes a moment, where HttpClient 5.4 and later would
     * read the rest of the body first.
     */
    private static final class Release implements Closeable {
        private final HttpUriRequestBase message;
        private final ClassicHttpResponse response;
        private final RequestBody requestBody;
        private final AtomicBoolean released = new AtomicBoolean();
        private ResponseBody body;

        Release(HttpUriRequestBase message, ClassicHttpResponse response, RequestBody requestBody) {
            this.message = message;
            this.response = response;
            this.requestBody = requestBody;
        }

        @Override
        public void close() {
            if (!released.compareAndSet(false, true)) {
                return;
            }
            if (body != null && !body.isAtEnd()) {
                message.cancel();
            }
            try {
                response.close();
            } catch (IOException e) {
                // Such as "Socket closed" after the cancel
            }
            closeStream(requestBody);
        }
    }
}

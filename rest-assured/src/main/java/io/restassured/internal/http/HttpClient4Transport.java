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

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.NtlmCredentials;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.internal.http.auth.CoreAuthenticationContext;
import io.restassured.internal.http.client.ClientSettingsFactory;
import io.restassured.internal.proxy.RestAssuredProxySelector;
import io.restassured.internal.proxy.RestAssuredProxySelectorRoutePlanner;
import io.restassured.specification.ProxySpecification;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import static io.restassured.internal.util.GroovyStringConversion.castToString;

/**
 * Sends requests with the HttpClient 4 client of an {@link HTTPBuilder}, which it configures for one request. Call
 * {@link #close()} once the request has been sent.
 * <p>
 * It isn't an ordinary {@link HttpClientInstance}: core creates it for each request, and it isn't discovered or shared.
 * {@link #close()} doesn't close the client, it only puts back the https scheme and the connection reuse strategy of
 * the client, which the user may reuse. {@link #send(HttpRequest)} only reads the NTLM credentials of
 * {@link HttpRequest#options()}, since the read timeout and expect-continue are set on the client through its
 * parameters.
 * </p><p>
 * The client only sends the requests: REST Assured follows the redirects, answers the challenges, keeps the cookies
 * and signs the requests itself, so the client is configured not to. A legacy authentication scheme may still add a
 * request interceptor to the client, which runs for every hop, so a {@link CrossHostSensitiveHeaderStripper} runs
 * after it.
 * </p>
 */
public class HttpClient4Transport implements HttpClientInstance {
    // A response keeps the single connection of HttpClient 4's BasicClientConnectionManager until its body is read, which
    // a body that is read later, or one that couldn't be parsed, may never be. So the next request with the same (reused)
    // client consumes it if it's still unread, since that request fails otherwise. Other connection managers have more
    // connections, which other threads may still be reading from.
    private static final Map<AbstractHttpClient, HttpEntity> UNREAD_BODIES = Collections.synchronizedMap(new WeakHashMap<>());

    private final AbstractHttpClient client;
    private final ConnectionConfig connectionConfig;
    private final SchemeRegistry schemeRegistry;
    private final Scheme originalHttpsScheme;
    private final ConnectionReuseStrategy originalReuseStrategy;
    // One context for every hop of the request, so that the proxy authentication state of the client is kept between them
    private final HttpContextDecorator context = new HttpContextDecorator();

    /**
     * Configures the client of <code>http</code> for the request: its proxy, its parameters and the TLS settings. It
     * also authenticates the request with <code>legacyAuthenticationScheme</code>, which may change the headers and
     * the URI of <code>http</code>, so build the request afterwards.
     *
     * @param config                     the config of the request
     * @param httpClientParams           the parameters to set on the client
     * @param legacyAuthenticationScheme a scheme that only implements the deprecated
     *                                   {@link AuthenticationScheme#authenticate(HTTPBuilder)}, or <code>null</code>
     * @param authentication             what the authentication scheme of the request asked for
     */
    @SuppressWarnings("deprecation")
    public HttpClient4Transport(HTTPBuilder http, RestAssuredConfig config, ProxySpecification proxySpecification, Map<?, ?> httpClientParams,
                                AuthenticationScheme legacyAuthenticationScheme, CoreAuthenticationContext authentication) {
        this.client = http.getClient();
        this.connectionConfig = config.getConnectionConfig();
        // Applying the SSL config or certificate authentication replaces the https scheme of the http client's scheme registry.
        // Restore the original scheme once the request has been sent, and don't keep https connections established with the
        // request specific scheme alive, so that the SSL settings of this request don't leak into later requests if the http
        // client instance (or its connection manager) is reused.
        this.schemeRegistry = client.getConnectionManager().getSchemeRegistry();
        this.originalHttpsScheme = schemeRegistry.get("https");
        this.originalReuseStrategy = client.getConnectionReuseStrategy();

        applyProxySettings(proxySpecification);
        HttpParams params = client.getParams();
        // Groovy callers may use GString keys, which Groovy converted to String
        for (Map.Entry<?, ?> entry : httpClientParams.entrySet()) {
            params.setParameter(castToString(entry.getKey()), entry.getValue());
        }
        // REST Assured follows the redirects and keeps the cookies of the responses itself
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        boolean configured = false;
        try {
            boolean certificateAuthentication = authentication.certificate() != null || legacyAuthenticationScheme instanceof CertAuthScheme;
            if (!certificateAuthentication && shouldApplySSLConfig(http, config)) {
                SSLConfig sslConfig = config.getSSLConfig();
                http.getAuth().certificate(ClientSettingsFactory.tlsSettings(sslConfig), sslConfig.getPort(), ClientSettingsFactory.userSocketFactory(sslConfig));
            }
            if (legacyAuthenticationScheme != null) {
                legacyAuthenticationScheme.authenticate(http);
            } else if (authentication.certificate() != null) {
                http.getAuth().certificate(authentication.certificate(), authentication.certificatePort(), null);
            }

            if (schemeRegistry.get("https") != originalHttpsScheme) {
                client.setReuseStrategy(new NoSecureConnectionReuseStrategy(originalReuseStrategy));
            }
            // Registered after the legacy authentication scheme has added its interceptors, so that it runs after them
            // and none of them adds Authorization again on a hop to another host. Removed first, so that a reused
            // client doesn't get it twice.
            client.removeRequestInterceptorByClass(CrossHostSensitiveHeaderStripper.class);
            RedirectConfig redirectConfig = config.getRedirectConfig() == null ? new RedirectConfig() : config.getRedirectConfig();
            if (redirectConfig.stripsSensitiveHeadersOnCrossHostRedirect()) {
                client.addRequestInterceptor(new CrossHostSensitiveHeaderStripper());
            }
            configured = true;
        } finally {
            if (!configured) {
                close();
            }
        }
    }

    private void applyProxySettings(ProxySpecification proxySpecification) {
        // make client aware of JRE proxy settings http://freeside.co/betamax/
        RestAssuredProxySelector proxySelector = new RestAssuredProxySelector();
        proxySelector.setDelegatingProxySelector(ProxySelector.getDefault());
        proxySelector.setProxySpecification(proxySpecification);
        client.setRoutePlanner(new RestAssuredProxySelectorRoutePlanner(client.getConnectionManager().getSchemeRegistry(), proxySelector, proxySpecification));
        if (proxySpecification != null && (proxySpecification.getUsername() != null || proxySpecification.getPassword() != null)) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            InetSocketAddress address = new InetSocketAddress(proxySpecification.getHost(), proxySpecification.getPort());
            // We need to convert the host to an IP since that's what our proxy selector (RestAssuredProxySelector) expects
            AuthScope authScope = new AuthScope(address.getAddress().getHostAddress(), proxySpecification.getPort());
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxySpecification.getUsername(), proxySpecification.getPassword());
            credsProvider.setCredentials(authScope, credentials);
            client.setCredentialsProvider(credsProvider);
        }
    }

    private static boolean shouldApplySSLConfig(HTTPBuilder http, RestAssuredConfig config) {
        URI uri = ((URIBuilder) http.getUri()).toURI();
        if (uri == null) throw new IllegalStateException("a default URI must be set");
        if (!config.getSSLConfig().isUserConfigured()) {
            return false;
        }
        if (uri.getScheme() != null && "https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
            return true;
        }
        // A non-https request may be redirected to https (see issue #790) so the SSL config is applied to it as well (the SSL
        // socket factory is then created lazily, see AuthConfig#certificate). This is only done for http clients created by
        // REST Assured's default http client factory since a custom factory may have registered its own https scheme that
        // must not be replaced for requests that don't start out as https.
        return config.getHttpClientConfig().usesDefaultHttpClientFactory();
    }

    /**
     * Puts back the https scheme and the connection reuse strategy that the client had before this transport configured it.
     */
    @Override
    public void close() {
        if (originalHttpsScheme == null) {
            schemeRegistry.unregister("https");
        } else {
            schemeRegistry.register(originalHttpsScheme);
        }
        client.setReuseStrategy(originalReuseStrategy);
    }

    @Override
    public io.restassured.common.spi.http.HttpResponse send(HttpRequest request) throws IOException {
        applyNtlmCredentials(request.options().ntlm());
        HttpRequestBase httpRequest = HttpRequestFactory.createHttpRequest(request.uri(), request.method(), request.body() != null);
        for (HttpHeader header : request.headers()) {
            httpRequest.addHeader(header.name(), header.value());
        }
        if (request.body() != null) {
            ((HttpEntityEnclosingRequest) httpRequest).setEntity(RequestBodyToHttpClient4Entity.toHttpEntity(request.body()));
        }

        HttpEntity unreadBody = UNREAD_BODIES.remove(client);
        if (unreadBody != null) EntityUtils.consumeQuietly(unreadBody);
        org.apache.http.HttpResponse response = client.execute(httpRequest, context);
        HttpEntity entity = response.getEntity();
        if (entity != null && client.getConnectionManager() instanceof BasicClientConnectionManager) {
            UNREAD_BODIES.put(client, entity);
        }
        return toHttpResponse(response, entity == null ? null : entity.getContent(), this::closeIdleConnectionsIfConfigured);
    }

    // NTLM is bound to the connection, so the client answers its challenges itself
    private void applyNtlmCredentials(NtlmCredentials ntlm) {
        if (ntlm == null) {
            return;
        }
        CredentialsProvider credentials = client.getCredentialsProvider();
        credentials.setCredentials(new AuthScope(ntlm.host(), ntlm.port()),
                new NTCredentials(ntlm.userName(), ntlm.password(), ntlm.workstation(), ntlm.domain()));
    }

    /**
     * @return The context of the last request, which knows the URI after redirects
     */
    public HttpContext context() {
        return context.getDelegate();
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
     * @return the response as an SPI {@link io.restassured.common.spi.http.HttpResponse}
     */
    public static io.restassured.common.spi.http.HttpResponse toHttpResponse(org.apache.http.HttpResponse response, InputStream body, Closeable onClose) {
        List<HttpHeader> headers = new ArrayList<>();
        for (Header header : response.getAllHeaders()) {
            headers.add(new HttpHeader(header.getName(), header.getValue()));
        }
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        long contentLength = entity == null ? -1 : entity.getContentLength();
        return new io.restassured.common.spi.http.HttpResponse(statusLine.getProtocolVersion().toString(), statusLine.getStatusCode(), statusLine.getReasonPhrase(),
                headers, contentLength, body, onClose);
    }
}

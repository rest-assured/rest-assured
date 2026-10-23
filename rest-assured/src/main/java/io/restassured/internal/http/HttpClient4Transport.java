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
import io.restassured.config.ConnectionConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
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
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpRequestBase;
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
 * the client, which the user may reuse. {@link #send(HttpRequest)} ignores {@link HttpRequest#options()}, since the
 * read timeout, expect-continue and NTLM are set on the client through its parameters and authentication.
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
    private HttpContext context;

    /**
     * Configures the client of <code>http</code> for the request: its proxy, its parameters, the SSL config and the
     * redirect handling. It also authenticates the request with <code>authenticationScheme</code>, which may change the
     * headers and the URI of <code>http</code>, so build the request afterwards.
     *
     * @param config           the config of the request
     * @param httpClientParams the parameters to set on the client
     */
    public HttpClient4Transport(HTTPBuilder http, RestAssuredConfig config, ProxySpecification proxySpecification, Map<?, ?> httpClientParams,
                                AuthenticationScheme authenticationScheme) {
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
        boolean configured = false;
        try {
            if (shouldApplySSLConfig(http, config, authenticationScheme)) {
                SSLConfig sslConfig = config.getSSLConfig();
                http.getAuth().certificate(ClientSettingsFactory.tlsSettings(sslConfig), sslConfig.getPort(), ClientSettingsFactory.userSocketFactory(sslConfig));
            }

            authenticationScheme.authenticate(http);

            if (schemeRegistry.get("https") != originalHttpsScheme) {
                client.setReuseStrategy(new NoSecureConnectionReuseStrategy(originalReuseStrategy));
            }

            // Register the cross-host header stripper after authentication so it runs last in the interceptor chain.
            RedirectConfig redirectConfig = config.getRedirectConfig() == null ? new RedirectConfig() : config.getRedirectConfig();
            applyCrossHostRedirectHeaderStripping(redirectConfig);
            applyRedirectStrategy(redirectConfig.followsRedirectsForAllMethods());
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

    private static boolean shouldApplySSLConfig(HTTPBuilder http, RestAssuredConfig config, AuthenticationScheme authenticationScheme) {
        URI uri = ((URIBuilder) http.getUri()).toURI();
        if (uri == null) throw new IllegalStateException("a default URI must be set");
        if (!config.getSSLConfig().isUserConfigured() || authenticationScheme instanceof CertAuthScheme) {
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

    private void applyCrossHostRedirectHeaderStripping(RedirectConfig redirectConfig) {
        // Remove first so repeated configuration (e.g. a reused HttpClient instance) never stacks duplicates.
        // Register last (after authentication schemes have added their interceptors) so that an auth interceptor
        // such as the OAuth/OAuth2 signer cannot re-add Authorization after we have stripped it on a redirect.
        client.removeRequestInterceptorByClass(CrossHostSensitiveHeaderStripper.class);
        if (redirectConfig.stripsSensitiveHeadersOnCrossHostRedirect()) {
            client.addRequestInterceptor(new CrossHostSensitiveHeaderStripper());
        }
    }

    private void applyRedirectStrategy(boolean followRedirectsForAllMethods) {
        RedirectStrategy redirectStrategy = client.getRedirectStrategy();
        if (followRedirectsForAllMethods && !(redirectStrategy instanceof AllMethodsRedirectStrategy)) {
            client.setRedirectStrategy(new AllMethodsRedirectStrategy(redirectStrategy));
        } else if (!followRedirectsForAllMethods && redirectStrategy instanceof AllMethodsRedirectStrategy) {
            // A reused client gets back the strategy it had, for example one installed by an HttpClientFactory
            client.setRedirectStrategy(((AllMethodsRedirectStrategy) redirectStrategy).getReplacedStrategy());
        }
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
        HttpRequestBase httpRequest = HttpRequestFactory.createHttpRequest(request.uri(), request.method(), request.body() != null);
        for (HttpHeader header : request.headers()) {
            httpRequest.addHeader(header.name(), header.value());
        }
        if (request.body() != null) {
            ((HttpEntityEnclosingRequest) httpRequest).setEntity(RequestBodyToHttpClient4Entity.toHttpEntity(request.body()));
        }

        HttpEntity unreadBody = UNREAD_BODIES.remove(client);
        if (unreadBody != null) EntityUtils.consumeQuietly(unreadBody);
        HttpContextDecorator httpContext = new HttpContextDecorator();
        org.apache.http.HttpResponse response = client.execute(httpRequest, httpContext);
        context = httpContext.getDelegate();
        HttpEntity entity = response.getEntity();
        if (entity != null && client.getConnectionManager() instanceof BasicClientConnectionManager) {
            UNREAD_BODIES.put(client, entity);
        }
        return toHttpResponse(response, entity == null ? null : entity.getContent(), this::closeIdleConnectionsIfConfigured);
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

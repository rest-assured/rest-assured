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

package io.restassured.internal.http.client;

import io.restassured.authentication.AuthenticationContext;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.authentication.ContextAuthenticationScheme;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.internal.http.HTTPBuilder;
import io.restassured.internal.http.auth.CoreAuthenticationContext;
import io.restassured.internal.http.client.HttpClientInstances.Lease;
import io.restassured.internal.http.client.HttpClientProviders.Selection;
import io.restassured.specification.ProxySpecification;

import java.io.IOException;

/**
 * Sends a request with the client of an {@link io.restassured.common.spi.http.HttpClientProvider}, through an instance
 * from {@link HttpClientInstances}, which it releases once REST Assured is done with the response. {@link #close()}
 * does nothing, since the response releases the instance.
 */
public final class ProviderTransport implements HttpClientInstance {
    private final Selection selection;
    private final HttpClientInstances instances;
    private final ClientSettings settings;
    private final HttpClientConfig httpClientConfig;
    private final ConnectionConfig connectionConfig;

    ProviderTransport(Selection selection, HttpClientInstances instances, ClientSettings settings, HttpClientConfig httpClientConfig,
                      ConnectionConfig connectionConfig) {
        this.selection = selection;
        this.instances = instances;
        this.settings = settings;
        this.httpClientConfig = httpClientConfig;
        this.connectionConfig = connectionConfig;
    }

    /**
     * @return Whether the authentication scheme can only be applied through an {@link HTTPBuilder}, which only
     * Apache HttpClient 4 has: a scheme that doesn't implement
     * {@link AuthenticationScheme#authenticate(io.restassured.authentication.AuthenticationContext)}, and
     * certificate authentication with the deprecated HttpClient 4 SSLSocketFactory
     */
    @SuppressWarnings("deprecation")
    public static boolean needsHttpBuilder(AuthenticationScheme authenticationScheme) {
        return onlyImplementsHttpBuilder(authenticationScheme)
                || authenticationScheme instanceof CertAuthScheme && ((CertAuthScheme) authenticationScheme).getSslSocketFactory() != null;
    }

    /**
     * @return The exception for an authentication scheme that {@link #needsHttpBuilder} while another client than
     * HttpClient 4 sends the request
     */
    public static UnsupportedOperationException cantAuthenticate(Selection selection, AuthenticationScheme authenticationScheme) {
        if (onlyImplementsHttpBuilder(authenticationScheme)) {
            return UnsupportedFeatures.httpClient4Only(selection, "authenticate with " + authenticationScheme.getClass().getName()
                            + ", which only implements the deprecated authenticate(HTTPBuilder)",
                    "implement " + ContextAuthenticationScheme.class.getName() + " instead");
        }
        return UnsupportedFeatures.httpClient4Only(selection, "use the HttpClient 4 SSLSocketFactory of certificate authentication",
                "give certificate authentication an SSLContext");
    }

    // A scheme written for REST Assured 6.x, which only knows the HTTPBuilder of HttpClient 4
    private static boolean onlyImplementsHttpBuilder(AuthenticationScheme authenticationScheme) {
        try {
            return authenticationScheme.getClass().getMethod("authenticate", AuthenticationContext.class)
                    .getDeclaringClass() == AuthenticationScheme.class;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }

    /**
     * Translates the config of the request into the settings of the client.
     *
     * @param authentication What the authentication scheme of the request asked for
     */
    public static ProviderTransport create(Selection selection, RestAssuredConfig config, ProxySpecification proxySpecification,
                                           AuthenticationScheme authenticationScheme, CoreAuthenticationContext authentication) {
        HttpClientConfig httpClientConfig = config.getHttpClientConfig();
        if (!httpClientConfig.usesDefaultHttpClientFactory()) {
            throw UnsupportedFeatures.httpClient4Only(selection, "use the HttpClient 4 client of httpClientConfig().httpClientFactory(..)",
                    "customize the client with httpClientConfig().customizeClient(..)");
        }
        SSLConfig sslConfig = config.getSSLConfig();
        // The SSL config isn't used with certificate authentication, as with HttpClient 4
        TlsSettings certificate = authentication.certificate();
        if (certificate == null && sslConfig.isUserConfigured() && ClientSettingsFactory.userSocketFactory(sslConfig) != null) {
            throw UnsupportedFeatures.httpClient4Only(selection, "use the HttpClient 4 SSLSocketFactory of sslConfig().sslSocketFactory(..)",
                    "use sslConfig().sslContext(..)");
        }
        ClientSettings settings = ClientSettingsFactory.clientSettings(sslConfig, certificate, proxySpecification, httpClientConfig);
        return new ProviderTransport(selection, HttpClientInstances.global(), settings, httpClientConfig, config.getConnectionConfig());
    }

    @Override
    public HttpResponse send(HttpRequest request) throws IOException {
        Lease lease = acquire();
        HttpResponse response = null;
        try {
            response = lease.instance().send(request);
        } catch (UnsupportedFeatureException e) {
            throw UnsupportedFeatures.translate(selection, e);
        } finally {
            if (response == null) {
                lease.release();
            }
        }
        HttpResponse sent = response;
        return new HttpResponse(sent.protocolVersion(), sent.statusCode(), sent.reasonPhrase(), sent.headers(), sent.contentLength(), sent.body(), () -> {
            try {
                sent.close();
            } finally {
                lease.release();
            }
        });
    }

    private Lease acquire() {
        try {
            return instances.acquire(selection.provider(), settings, httpClientConfig, connectionConfig);
        } catch (UnsupportedFeatureException e) {
            throw UnsupportedFeatures.translate(selection, e);
        }
    }

    /**
     * Does nothing, since the response releases the client instance.
     */
    @Override
    public void close() {
    }
}

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

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.authentication.ExplicitNoAuthScheme;
import io.restassured.authentication.NoAuthScheme;
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
import io.restassured.internal.http.client.HttpClientInstances.Lease;
import io.restassured.internal.http.client.HttpClientProviders.Selection;
import io.restassured.specification.ProxySpecification;
import org.apache.http.auth.AuthScope;
import org.apache.http.impl.client.AbstractHttpClient;

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
     * @return Whether REST Assured needs an HttpClient 4 client in the {@link HTTPBuilder} to see what the
     * authentication scheme asks for
     */
    public static boolean needsHttpClient4ToAuthenticate(AuthenticationScheme authenticationScheme) {
        return !(authenticationScheme == null || authenticationScheme instanceof NoAuthScheme || authenticationScheme instanceof ExplicitNoAuthScheme
                || authenticationScheme instanceof CertAuthScheme);
    }

    /**
     * Translates the config of the request into the settings of the client. It also authenticates the request with
     * <code>authenticationScheme</code>, which may change the headers and the URI of <code>http</code>, so build the
     * request afterwards.
     *
     * @param http The builder of the request, with an unused HttpClient 4 client if {@link #needsHttpClient4ToAuthenticate}
     */
    public static ProviderTransport create(Selection selection, HTTPBuilder http, RestAssuredConfig config, ProxySpecification proxySpecification,
                                           AuthenticationScheme authenticationScheme) {
        HttpClientConfig httpClientConfig = config.getHttpClientConfig();
        if (!httpClientConfig.usesDefaultHttpClientFactory()) {
            throw UnsupportedFeatures.httpClient4Only(selection, "use the HttpClient 4 client of httpClientConfig().httpClientFactory(..)",
                    "customize the client with httpClientConfig().customizeClient(..)");
        }
        SSLConfig sslConfig = config.getSSLConfig();
        TlsSettings certificate = null;
        if (authenticationScheme instanceof CertAuthScheme) {
            CertAuthScheme certAuthScheme = (CertAuthScheme) authenticationScheme;
            if (certAuthScheme.getSslSocketFactory() != null) {
                throw UnsupportedFeatures.httpClient4Only(selection, "use the HttpClient 4 SSLSocketFactory of certificate authentication",
                        "give certificate authentication an SSLContext");
            }
            // The SSL config isn't used with certificate authentication, as with HttpClient 4
            certificate = ClientSettingsFactory.certificateTlsSettings(certAuthScheme);
        } else {
            if (sslConfig.isUserConfigured() && ClientSettingsFactory.userSocketFactory(sslConfig) != null) {
                throw UnsupportedFeatures.httpClient4Only(selection, "use the HttpClient 4 SSLSocketFactory of sslConfig().sslSocketFactory(..)",
                        "use sslConfig().sslContext(..)");
            }
            if (needsHttpClient4ToAuthenticate(authenticationScheme)) {
                authenticate(selection, http, authenticationScheme);
            }
        }
        ClientSettings settings = ClientSettingsFactory.clientSettings(sslConfig, certificate, proxySpecification, httpClientConfig);
        return new ProviderTransport(selection, HttpClientInstances.global(), settings, httpClientConfig, config.getConnectionConfig());
    }

    // Until REST Assured answers challenges and signs OAuth requests itself, only schemes that set headers work
    private static void authenticate(Selection selection, HTTPBuilder http, AuthenticationScheme authenticationScheme) {
        AbstractHttpClient client = http.getClient();
        int interceptors = client.getRequestInterceptorCount();
        authenticationScheme.authenticate(http);
        if (client.getRequestInterceptorCount() != interceptors || client.getCredentialsProvider().getCredentials(AuthScope.ANY) != null) {
            throw UnsupportedFeatures.httpClient4Only(selection, "authenticate with " + authenticationScheme.getClass().getSimpleName() + " yet",
                    "use preemptive basic authentication");
        }
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

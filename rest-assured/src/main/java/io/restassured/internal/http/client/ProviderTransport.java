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

import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
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
     * Translates the config of the request into the settings of the client.
     *
     * @param authentication What the authentication scheme of the request asked for
     */
    public static ProviderTransport create(Selection selection, RestAssuredConfig config, ProxySpecification proxySpecification,
                                           CoreAuthenticationContext authentication) {
        HttpClientConfig httpClientConfig = config.getHttpClientConfig();
        // The SSL config isn't used with certificate authentication, as in REST Assured 6.x
        ClientSettings settings = ClientSettingsFactory.clientSettings(config.getSSLConfig(), authentication.certificate(), proxySpecification, httpClientConfig);
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
        } catch (IOException e) {
            // A proxy set in the JVM's system properties isn't known here, so the host of the request is named then
            throw TransportExceptions.translate(e, settings.proxy() == null ? request.uri().getHost() : settings.proxy().host());
        } finally {
            if (response == null) {
                lease.release();
            }
        }
        HttpResponse sent = response;
        String reasonPhrase = ReasonPhrases.orStandard(sent.reasonPhrase(), sent.statusCode());
        return new HttpResponse(sent.protocolVersion(), sent.statusCode(), reasonPhrase, sent.headers(), sent.contentLength(), sent.body(), () -> {
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

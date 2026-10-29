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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends the hops of one request, its redirects and the answers to its challenges, with the client of an
 * {@link io.restassured.common.spi.http.HttpClientProvider}. It takes an instance from {@link HttpClientInstances} at
 * the first hop and keeps it for the others, so that a client that's created for each request is created once for all
 * of them, as in REST Assured 6.x. It releases the instance once it's closed and REST Assured is done with every
 * response.
 */
public final class ProviderTransport implements HttpClientInstance {
    private final Selection selection;
    private final HttpClientInstances instances;
    private final ClientSettings settings;
    private final HttpClientConfig httpClientConfig;
    private final ConnectionConfig connectionConfig;
    // Guarded by this
    private Lease lease;
    private int openResponses;
    private boolean closed;

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
     * @param instances      The instances to take the client instance from
     */
    public static ProviderTransport create(Selection selection, RestAssuredConfig config, ProxySpecification proxySpecification,
                                           CoreAuthenticationContext authentication, HttpClientInstances instances) {
        HttpClientConfig httpClientConfig = config.getHttpClientConfig();
        // The SSL config isn't used with certificate authentication, as in REST Assured 6.x
        ClientSettings settings = ClientSettingsFactory.clientSettings(config.getSSLConfig(), authentication.certificate(), proxySpecification, httpClientConfig);
        return new ProviderTransport(selection, instances, settings, httpClientConfig, config.getConnectionConfig());
    }

    @Override
    public HttpResponse send(HttpRequest request) throws IOException {
        HttpClientInstance instance = acquire();
        HttpResponse response = null;
        try {
            response = instance.send(request);
        } catch (UnsupportedFeatureException e) {
            throw UnsupportedFeatures.translate(selection, e);
        } catch (IOException e) {
            // A proxy set in the JVM's system properties isn't known here, so the host of the request is named then
            throw TransportExceptions.translate(e, settings.proxy() == null ? request.uri().getHost() : settings.proxy().host());
        } finally {
            if (response == null) {
                responseDone();
            }
        }
        HttpResponse sent = response;
        String reasonPhrase = ReasonPhrases.orStandard(sent.reasonPhrase(), sent.statusCode());
        AtomicBoolean done = new AtomicBoolean();
        return new HttpResponse(sent.protocolVersion(), sent.statusCode(), reasonPhrase, sent.headers(), sent.contentLength(), sent.body(), () -> {
            try {
                sent.close();
            } finally {
                if (done.compareAndSet(false, true)) {
                    responseDone();
                }
            }
        });
    }

    // The instance of the request, taken at its first hop, with one more response open
    private synchronized HttpClientInstance acquire() {
        if (closed) {
            throw new IllegalStateException("The request has been sent");
        }
        if (lease == null) {
            try {
                lease = instances.acquire(selection.provider(), settings, httpClientConfig, connectionConfig);
            } catch (UnsupportedFeatureException e) {
                throw UnsupportedFeatures.translate(selection, e);
            }
        }
        openResponses++;
        return lease.instance();
    }

    private void responseDone() {
        Lease released;
        synchronized (this) {
            openResponses--;
            released = closed && openResponses == 0 ? lease : null;
        }
        if (released != null) {
            released.release();
        }
    }

    /**
     * Tells that the request sends no more hops. The client instance is released now, or once the last response is closed.
     */
    @Override
    public void close() {
        Lease released;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            released = openResponses == 0 ? lease : null;
        }
        if (released != null) {
            released.release();
        }
    }
}

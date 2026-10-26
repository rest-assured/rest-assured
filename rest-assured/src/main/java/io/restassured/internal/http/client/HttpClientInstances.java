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
import io.restassured.common.spi.http.ClientSharing;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpClientProvider;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.internal.http.cookie.CookieStore;
import io.restassured.internal.support.VersionFiles;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.Logger.Level.WARNING;

/**
 * Decides which client instance sends a request, and when REST Assured closes it:
 * <ul>
 * <li>With {@link ConnectionConfig#closeIdleConnectionsAfterEachResponse()}, a new instance that is closed once
 * REST Assured is done with the response.</li>
 * <li>A client that is {@link ClientSharing#SHARED} is shared by all requests with the same settings, up to
 * 16 instances. When another one is needed, the least recently used one is removed, and closed once the responses
 * that use it are done.</li>
 * <li>A client that is {@link ClientSharing#PER_REQUEST} gets a new instance for each request, which is closed, or with
 * {@link HttpClientConfig#reuseHttpClientInstance()} one instance for each config and its copies
 * ({@link HttpClientConfig#reuseIdentity()}), provider and settings, which isn't.</li>
 * </ul>
 */
public final class HttpClientInstances {
    static final int MAX_SHARED = 16;

    private static final HttpClientInstances INSTANCE = new HttpClientInstances();

    private final Map<Key, Shared> shared = new LinkedHashMap<>(MAX_SHARED, 0.75f, true);
    // Kept for each reuse identity of the configs for as long as a config with it is used
    private final Map<Object, Map<Key, HttpClientInstance>> reused = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object, CookieStore> cookieStores = Collections.synchronizedMap(new WeakHashMap<>());

    HttpClientInstances() {
    }

    /**
     * @return The instances that REST Assured uses
     */
    public static HttpClientInstances global() {
        return INSTANCE;
    }

    /**
     * @return The cookie store of a config with {@link HttpClientConfig#reuseHttpClientInstance()} and its copies, which
     * keeps the cookies of the responses of their requests as long as they're used, as the client instances are kept
     */
    public CookieStore cookieStore(HttpClientConfig httpClientConfig) {
        return cookieStores.computeIfAbsent(httpClientConfig.reuseIdentity(), identity -> new CookieStore());
    }

    /**
     * @return The instance that sends the request, which the caller releases once it's done with the response
     * @throws io.restassured.common.spi.http.UnsupportedFeatureException If the client can't do what the settings ask for
     */
    public Lease acquire(HttpClientProvider provider, ClientSettings settings, HttpClientConfig httpClientConfig, ConnectionConfig connectionConfig) {
        if (connectionConfig.shouldCloseIdleConnectionsAfterEachResponse()) {
            if (connectionConfig.closeIdleConnectionConfig().getIdleTime() <= 0) {
                HttpClientInstance instance = provider.newClient(settings);
                return new Lease(instance, () -> closeQuietly(instance));
            } else if (provider.sharing() == ClientSharing.SHARED) {
                VersionFiles.warnOnce("connectionConfig().closeIdleConnectionsAfterEachResponseAfter(..) with an idle time above 0 has no equivalent in "
                        + provider.displayName() + ", which closes idle connections after a time that its library decides. For the JDK HTTP client, "
                        + "set it with the system property jdk.httpclient.keepalive.timeout (in seconds).");
            }
        }
        Key key = new Key(provider.getClass(), settings);
        if (provider.sharing() == ClientSharing.SHARED) {
            return shared(provider, key);
        } else if (httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance()) {
            Map<Key, HttpClientInstance> instances = reused.computeIfAbsent(httpClientConfig.reuseIdentity(), identity -> new ConcurrentHashMap<>());
            return new Lease(instances.computeIfAbsent(key, it -> provider.newClient(settings)), () -> { });
        }
        HttpClientInstance instance = provider.newClient(settings);
        return new Lease(instance, () -> closeQuietly(instance));
    }

    private Lease shared(HttpClientProvider provider, Key key) {
        Shared entry = leaseShared(key, null);
        if (entry == null) {
            // Created outside the lock, since creating a client may take a while. If another thread created one
            // meanwhile, that one is used and this one closed.
            Shared created = new Shared(provider.newClient(key.settings()));
            entry = leaseShared(key, created);
            if (entry != created) {
                closeQuietly(created.instance);
            }
        }
        Shared leased = entry;
        return new Lease(leased.instance, () -> release(leased));
    }

    // Leases the shared instance for the key, or else the created one if it isn't null
    private Shared leaseShared(Key key, Shared created) {
        Shared evicted = null;
        Shared entry;
        synchronized (shared) {
            entry = shared.get(key);
            if (entry == null && created != null) {
                entry = created;
                shared.put(key, created);
                if (shared.size() > MAX_SHARED) {
                    Iterator<Shared> eldest = shared.values().iterator();
                    evicted = eldest.next();
                    eldest.remove();
                    evicted.evicted = true;
                    if (evicted.leases > 0) {
                        evicted = null;
                    }
                }
            }
            if (entry != null) {
                entry.leases++;
            }
        }
        if (evicted != null) {
            closeQuietly(evicted.instance);
        }
        return entry;
    }

    private void release(Shared entry) {
        boolean close;
        synchronized (shared) {
            entry.leases--;
            close = entry.evicted && entry.leases == 0;
        }
        if (close) {
            closeQuietly(entry.instance);
        }
    }

    private static void closeQuietly(HttpClientInstance instance) {
        try {
            instance.close();
        } catch (RuntimeException | LinkageError e) {
            System.getLogger("io.restassured").log(WARNING, "Closing the HTTP client " + instance + " failed", e);
        }
    }

    /**
     * The instance that sends a request, which is released once REST Assured is done with the response.
     */
    public static final class Lease {
        private final HttpClientInstance instance;
        private final Runnable onRelease;
        private final AtomicBoolean released = new AtomicBoolean();

        Lease(HttpClientInstance instance, Runnable onRelease) {
            this.instance = instance;
            this.onRelease = onRelease;
        }

        /**
         * @return The instance
         */
        public HttpClientInstance instance() {
            return instance;
        }

        /**
         * Tells that REST Assured is done with the response, which may close the instance. Only the first call counts.
         */
        public void release() {
            if (released.compareAndSet(false, true)) {
                onRelease.run();
            }
        }
    }

    // Guarded by the lock on the shared map
    private static final class Shared {
        final HttpClientInstance instance;
        int leases;
        boolean evicted;

        Shared(HttpClientInstance instance) {
            this.instance = instance;
        }
    }

    private record Key(Class<?> providerClass, ClientSettings settings) {
    }
}

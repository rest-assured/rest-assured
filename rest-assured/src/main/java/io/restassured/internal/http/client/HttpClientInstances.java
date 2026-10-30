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
import io.restassured.config.ConnectionConfig.CloseIdleConnectionConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.internal.http.cookie.CookieStore;
import io.restassured.internal.support.VersionFiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.Logger.Level.WARNING;

/**
 * Decides which client instance sends a request, and when REST Assured closes it:
 * <ul>
 * <li>With {@link ConnectionConfig#closeIdleConnectionsAfterEachResponse()}, a new instance that is closed once
 * REST Assured is done with the response. With an idle time above 0, an instance that is kept closes its connections
 * that have been idle for longer than that ({@link HttpClientInstance#closeIdleConnections(Duration)}).</li>
 * <li>A client that is {@link ClientSharing#SHARED} is shared by all requests with the same settings, up to
 * 16 instances. When another one is needed, the least recently used one is removed, and closed once the responses
 * that use it are done.</li>
 * <li>A client that is {@link ClientSharing#PER_REQUEST} gets a new instance for each request, which is closed, or with
 * {@link HttpClientConfig#reuseHttpClientInstance()} one instance for each config and its copies
 * ({@link HttpClientConfig#reuseIdentity()}), provider and settings, which isn't. The instances of a
 * {@link io.restassured.RestAssuredClient} ({@link #owned()}) keep one instance for each provider and settings instead,
 * with or without {@link HttpClientConfig#reuseHttpClientInstance()}, up to 16, and close the least recently used one
 * as shared ones are.</li>
 * </ul>
 * The instances of a {@link io.restassured.RestAssuredClient} are closed with {@link #close()}.
 */
public final class HttpClientInstances {
    static final int MAX_SHARED = 16;
    private static final String CLOSED = "This RestAssuredClient is closed";

    private static final HttpClientInstances GLOBAL = new HttpClientInstances(false);

    private final boolean owned;
    // Guarded by the lock on shared, like kept and closed: the instances of SHARED clients, least recently used first
    private final Map<Key, Entry> shared = new LinkedHashMap<>(MAX_SHARED, 0.75f, true);
    // The instances of PER_REQUEST clients that owned instances keep, least recently used first
    private final Map<Key, Entry> kept = new LinkedHashMap<>(MAX_SHARED, 0.75f, true);
    // Kept for each reuse identity of the configs for as long as a config with it is used
    private final Map<Object, Map<Key, HttpClientInstance>> reused = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object, CookieStore> cookieStores = Collections.synchronizedMap(new WeakHashMap<>());
    private volatile boolean closed;

    HttpClientInstances() {
        this(false);
    }

    private HttpClientInstances(boolean owned) {
        this.owned = owned;
    }

    /**
     * @return The instances that the static API, and specifications that no {@link io.restassured.RestAssuredClient}
     * created, use. They're never closed.
     */
    public static HttpClientInstances global() {
        return GLOBAL;
    }

    /**
     * @return New instances for a {@link io.restassured.RestAssuredClient}, which closes them with {@link #close()}
     */
    public static HttpClientInstances owned() {
        return new HttpClientInstances(true);
    }

    /**
     * @return The cookie store of a config with {@link HttpClientConfig#reuseHttpClientInstance()} and its copies, which
     * keeps the cookies of the responses of their requests as long as they're used, as the client instances are kept
     */
    public CookieStore cookieStore(HttpClientConfig httpClientConfig) {
        throwIfClosed();
        return cookieStores.computeIfAbsent(httpClientConfig.reuseIdentity(), identity -> new CookieStore());
    }

    /**
     * @return The instance that sends the request, which the caller releases once it's done with the response
     * @throws io.restassured.common.spi.http.UnsupportedFeatureException If the client can't do what the settings ask for
     * @throws IllegalStateException If these instances are closed
     */
    public Lease acquire(HttpClientProvider provider, ClientSettings settings, HttpClientConfig httpClientConfig, ConnectionConfig connectionConfig) {
        // Checked again under the lock for the instances that are kept. A new instance for one request is closed after
        // the response anyway.
        throwIfClosed();
        Duration idleTime = null;
        if (connectionConfig.shouldCloseIdleConnectionsAfterEachResponse()) {
            CloseIdleConnectionConfig closeIdle = connectionConfig.closeIdleConnectionConfig();
            if (closeIdle.getIdleTime() <= 0) {
                HttpClientInstance instance = provider.newClient(settings);
                return new Lease(instance, () -> closeQuietly(instance));
            }
            idleTime = Duration.of(closeIdle.getIdleTime(), closeIdle.getTimeUnit().toChronoUnit());
        }
        Key key = new Key(provider.getClass(), settings);
        Lease lease;
        if (provider.sharing() == ClientSharing.SHARED) {
            lease = leaseCached(shared, provider, key);
        } else if (owned) {
            lease = leaseCached(kept, provider, key);
        } else if (httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance()) {
            Map<Key, HttpClientInstance> instances = reused.computeIfAbsent(httpClientConfig.reuseIdentity(), identity -> new ConcurrentHashMap<>());
            lease = new Lease(instances.computeIfAbsent(key, it -> provider.newClient(settings)), () -> { });
        } else {
            // Closed after the response anyway
            HttpClientInstance instance = provider.newClient(settings);
            return new Lease(instance, () -> closeQuietly(instance));
        }
        return idleTime == null ? lease : closingIdleConnections(lease, idleTime, provider);
    }

    // An instance that is kept closes the connections that have been idle for longer than the idle time once it's released
    private static Lease closingIdleConnections(Lease lease, Duration idleTime, HttpClientProvider provider) {
        return new Lease(lease.instance(), () -> {
            try {
                if (!lease.instance().closeIdleConnections(idleTime)) {
                    VersionFiles.warnOnce("connectionConfig().closeIdleConnectionsAfterEachResponseAfter(..) with an idle time above 0 has no equivalent in "
                            + provider.displayName() + ", which closes idle connections after a time that its library decides. For the JDK HTTP client, "
                            + "set it with the system property jdk.httpclient.keepalive.timeout (in seconds).");
                }
            } finally {
                lease.release();
            }
        });
    }

    // Leases the instance for the key from entries, the shared or the kept ones
    private Lease leaseCached(Map<Key, Entry> entries, HttpClientProvider provider, Key key) {
        Entry entry = leaseEntry(entries, key, null);
        if (entry == null) {
            // Created outside the lock, since creating a client may take a while. If another thread created one
            // meanwhile, that one is used and this one closed, as it is when these instances were closed meanwhile.
            Entry created = new Entry(provider.newClient(key.settings()));
            try {
                entry = leaseEntry(entries, key, created);
            } finally {
                if (entry != created) {
                    closeQuietly(created.instance);
                }
            }
        }
        Entry leased = entry;
        return new Lease(leased.instance, () -> release(leased));
    }

    // Leases the instance of entries for the key, or else the created one if it isn't null
    private Entry leaseEntry(Map<Key, Entry> entries, Key key, Entry created) {
        Entry evicted = null;
        Entry entry;
        synchronized (shared) {
            throwIfClosed();
            entry = entries.get(key);
            if (entry == null && created != null) {
                entry = created;
                entries.put(key, created);
                if (entries.size() > MAX_SHARED) {
                    Iterator<Entry> eldest = entries.values().iterator();
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

    private void release(Entry entry) {
        boolean close;
        synchronized (shared) {
            entry.leases--;
            close = entry.evicted && entry.leases == 0;
        }
        if (close) {
            closeQuietly(entry.instance);
        }
    }

    /**
     * @throws IllegalStateException If {@link #close()} was called
     */
    public void throwIfClosed() {
        if (closed) {
            throw new IllegalStateException(CLOSED);
        }
    }

    /**
     * @return Whether {@link #close()} was called
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes owned instances: {@link #acquire} fails from now on, the instances that no response uses are closed now and
     * the others once the last response that uses them is done, and the kept cookies are dropped. Only the first call
     * counts. The {@link #global()} instances aren't closed.
     */
    public void close() {
        if (!owned) {
            return;
        }
        List<Entry> unused = new ArrayList<>();
        synchronized (shared) {
            if (closed) {
                return;
            }
            closed = true;
            for (Map<Key, Entry> entries : List.of(shared, kept)) {
                for (Entry entry : entries.values()) {
                    entry.evicted = true;
                    if (entry.leases == 0) {
                        unused.add(entry);
                    }
                }
                entries.clear();
            }
        }
        cookieStores.clear();
        unused.forEach(entry -> closeQuietly(entry.instance));
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

    // Guarded by the lock on the shared map. Evicted means removed from its map, so it's closed after its last lease.
    private static final class Entry {
        final HttpClientInstance instance;
        int leases;
        boolean evicted;

        Entry(HttpClientInstance instance) {
            this.instance = instance;
        }
    }

    private record Key(Class<?> providerClass, ClientSettings settings) {
    }
}

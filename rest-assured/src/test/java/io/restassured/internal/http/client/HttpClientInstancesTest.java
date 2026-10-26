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
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.MultipartMode;
import io.restassured.internal.http.client.FakeHttpClients.FakeClient;
import io.restassured.internal.http.client.HttpClientInstances.Lease;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.config.ConnectionConfig.connectionConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static org.assertj.core.api.Assertions.assertThat;

class HttpClientInstancesTest {
    private static final ClientSettings SETTINGS = settings(null);
    private static final ConnectionConfig DEFAULT_CONNECTIONS = connectionConfig();

    private final HttpClientInstances instances = new HttpClientInstances();

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
    }

    @Test
    void a_shared_client_is_shared_by_requests_with_equal_settings() {
        Lease first = instances.acquire(new FakeHttpClients.Jdk(), SETTINGS, httpClientConfig(), DEFAULT_CONNECTIONS);
        Lease second = instances.acquire(new FakeHttpClients.Jdk(), settings(null), httpClientConfig(), DEFAULT_CONNECTIONS);
        Lease other = instances.acquire(new FakeHttpClients.Jdk(), settings(Duration.ofSeconds(1)), httpClientConfig(), DEFAULT_CONNECTIONS);

        assertThat(second.instance()).isSameAs(first.instance());
        assertThat(other.instance()).isNotSameAs(first.instance());
        first.release();
        second.release();
        assertThat(closed(first)).isFalse();
    }

    @Test
    void the_least_recently_used_shared_client_is_closed_when_there_are_too_many() {
        FakeHttpClients.Jdk provider = new FakeHttpClients.Jdk();
        Lease first = acquireAndRelease(provider, 1);
        Lease second = acquireAndRelease(provider, 2);
        for (int i = 3; i <= HttpClientInstances.MAX_SHARED; i++) {
            acquireAndRelease(provider, i);
        }
        // Used again, so the second is now the least recently used
        acquireAndRelease(provider, 1);

        acquireAndRelease(provider, 100);

        assertThat(closed(second)).isTrue();
        assertThat(closed(first)).isFalse();
        assertThat(FakeHttpClients.created).hasSize(HttpClientInstances.MAX_SHARED + 1);
        assertThat(FakeHttpClients.created.stream().filter(client -> client.closed)).hasSize(1);
    }

    @Test
    void an_evicted_shared_client_is_closed_once_the_last_response_that_uses_it_is_done() {
        FakeHttpClients.Jdk provider = new FakeHttpClients.Jdk();
        Lease first = instances.acquire(provider, settings(Duration.ofSeconds(1)), httpClientConfig(), DEFAULT_CONNECTIONS);
        Lease second = instances.acquire(provider, settings(Duration.ofSeconds(1)), httpClientConfig(), DEFAULT_CONNECTIONS);
        for (int i = 2; i <= HttpClientInstances.MAX_SHARED + 1; i++) {
            acquireAndRelease(provider, i);
        }

        assertThat(closed(first)).isFalse();
        first.release();
        // A lease counts once, however often it's released
        first.release();
        assertThat(closed(first)).isFalse();
        second.release();
        assertThat(((FakeClient) first.instance()).closes).hasValue(1);
        // A new instance for the settings of the evicted one
        assertThat(acquireAndRelease(provider, 1).instance()).isNotSameAs(first.instance());
    }

    @Test
    void a_per_request_client_is_new_for_each_request_and_closed_once_after_the_response() {
        Lease first = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, httpClientConfig(), DEFAULT_CONNECTIONS);
        Lease second = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, httpClientConfig(), DEFAULT_CONNECTIONS);

        assertThat(second.instance()).isNotSameAs(first.instance());
        first.release();
        first.release();
        assertThat(((FakeClient) first.instance()).closes).hasValue(1);
        assertThat(closed(second)).isFalse();
    }

    @Test
    void a_client_that_fails_to_close_doesnt_fail_the_release() {
        Lease lease = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, httpClientConfig(), DEFAULT_CONNECTIONS);
        FakeHttpClients.failOnClose = true;

        lease.release();

        assertThat(closed(lease)).isTrue();
    }

    @Test
    void copies_of_a_reused_config_that_keep_how_the_client_is_created_share_its_instances_and_cookie_store() {
        HttpClientConfig reused = httpClientConfig().reuseHttpClientInstance();
        HttpClientConfig copy = reused.client("apache5").multipartMode(MultipartMode.BROWSER_COMPATIBLE);
        HttpClientConfig withOtherParams = reused.setParam("a", "b");
        Lease first = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, reused, DEFAULT_CONNECTIONS);
        Lease fromCopy = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, copy, DEFAULT_CONNECTIONS);
        Lease fromCopyWithOtherParams = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, withOtherParams, DEFAULT_CONNECTIONS);

        assertThat(fromCopy.instance()).isSameAs(first.instance());
        assertThat(fromCopyWithOtherParams.instance()).isNotSameAs(first.instance());
        assertThat(instances.cookieStore(copy)).isSameAs(instances.cookieStore(reused));
        assertThat(instances.cookieStore(withOtherParams)).isNotSameAs(instances.cookieStore(reused));
    }

    @Test
    void a_reused_config_keeps_one_per_request_client_for_each_provider_and_settings() {
        HttpClientConfig reused = httpClientConfig().reuseHttpClientInstance();
        Lease first = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, reused, DEFAULT_CONNECTIONS);
        Lease second = instances.acquire(new FakeHttpClients.Apache5(), settings(null), reused, DEFAULT_CONNECTIONS);
        Lease otherSettings = instances.acquire(new FakeHttpClients.Apache5(), settings(Duration.ofSeconds(1)), reused, DEFAULT_CONNECTIONS);
        Lease otherProvider = instances.acquire(new FakeHttpClients.OkHttp(), SETTINGS, reused, DEFAULT_CONNECTIONS);
        Lease otherConfig = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, httpClientConfig().reuseHttpClientInstance(), DEFAULT_CONNECTIONS);

        assertThat(second.instance()).isSameAs(first.instance());
        assertThat(otherSettings.instance()).isNotSameAs(first.instance());
        assertThat(otherProvider.instance()).isNotSameAs(first.instance());
        assertThat(((FakeClient) otherProvider.instance()).provider).isInstanceOf(FakeHttpClients.OkHttp.class);
        assertThat(otherConfig.instance()).isNotSameAs(first.instance());
        first.release();
        assertThat(closed(first)).isFalse();
    }

    @Test
    void closing_idle_connections_after_each_response_gives_a_new_client_that_is_closed() {
        ConnectionConfig closeIdle = connectionConfig().closeIdleConnectionsAfterEachResponse();

        Lease shared = instances.acquire(new FakeHttpClients.Jdk(), SETTINGS, httpClientConfig(), closeIdle);
        Lease reused = instances.acquire(new FakeHttpClients.Apache5(), SETTINGS, httpClientConfig().reuseHttpClientInstance(), closeIdle);
        Lease next = instances.acquire(new FakeHttpClients.Jdk(), SETTINGS, httpClientConfig(), closeIdle);
        shared.release();
        reused.release();

        assertThat(next.instance()).isNotSameAs(shared.instance());
        assertThat(closed(shared)).isTrue();
        assertThat(closed(reused)).isTrue();
    }

    @Test
    void an_idle_time_above_0_uses_the_shared_client() {
        ConnectionConfig closeIdleAfter = connectionConfig().closeIdleConnectionsAfterEachResponseAfter(1, TimeUnit.HOURS);

        Lease first = instances.acquire(new FakeHttpClients.Jdk(), SETTINGS, httpClientConfig(), closeIdleAfter);
        Lease second = instances.acquire(new FakeHttpClients.Jdk(), SETTINGS, httpClientConfig(), closeIdleAfter);
        first.release();

        assertThat(second.instance()).isSameAs(first.instance());
        assertThat(closed(first)).isFalse();
    }

    private Lease acquireAndRelease(FakeHttpClients.Jdk provider, int connectTimeoutSeconds) {
        Lease lease = instances.acquire(provider, settings(Duration.ofSeconds(connectTimeoutSeconds)), httpClientConfig(), DEFAULT_CONNECTIONS);
        lease.release();
        return lease;
    }

    private static boolean closed(Lease lease) {
        return ((FakeClient) lease.instance()).closed;
    }

    private static ClientSettings settings(Duration connectTimeout) {
        return new ClientSettings(null, null, HttpVersion.HTTP_1_1, connectTimeout, null, List.of());
    }
}

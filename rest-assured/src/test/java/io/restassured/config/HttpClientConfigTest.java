/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@SuppressWarnings("unchecked")
public class HttpClientConfigTest {

    private static final String CUSTOM_MAX_REDIRECTS = "100";

    @Test
    public void cookiePolicyIsSetToIgnoreCookiesByDefault() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig();

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params).containsEntry(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
    }

    @Test
    public void setParamsRespectsOtherConfigurationSettings() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .setParam(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS)
                .reuseHttpClientInstance()
                .setParam(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(
                        entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS),
                        entry(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)
                );
        assertThat(httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance()).isTrue();
    }

    @Test
    public void setParamsCorrectlyUpdatesPreviousSetting() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .setParam(ClientPNames.MAX_REDIRECTS, "50")
                .setParam(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS));
    }

    @Test
    public void addParamsRespectsOtherConfigurationSettings() {
        final Map<String, String> redirectParam = new HashMap<String, String>();
        final Map<String, String> cookieParam = new HashMap<String, String>();

        redirectParam.put(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS);
        cookieParam.put(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .addParams(redirectParam)
                .reuseHttpClientInstance()
                .addParams(cookieParam);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(
                        entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS),
                        entry(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)
                );
        assertThat(httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance()).isTrue();
    }

    @Test
    public void addParamCorrectlyUpdatesPreviousSetting() {
        final Map<String, String> redirectParam = new HashMap<String, String>();
        final Map<String, String> cookieParam = new HashMap<String, String>();

        redirectParam.put(ClientPNames.MAX_REDIRECTS, "50");
        cookieParam.put(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS);

        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .addParams(redirectParam)
                .reuseHttpClientInstance()
                .addParams(cookieParam);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS));

    }

    @Test
    @Timeout(10)
    public void reusedHttpClientInstanceIsCreatedOnceWhenManyThreadsAskForItAtTheSameTime() throws Exception {
        final int numberOfThreads = 16;
        final AtomicInteger numberOfCreatedClients = new AtomicInteger();
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .httpClientFactory(() -> {
                    numberOfCreatedClients.incrementAndGet();
                    try {
                        // Widen the window between the null check and the assignment
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return new DefaultHttpClient();
                })
                .reuseHttpClientInstance();

        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        try {
            final CountDownLatch ready = new CountDownLatch(numberOfThreads);
            final CountDownLatch start = new CountDownLatch(1);
            final List<Future<HttpClient>> futures = new ArrayList<>();
            for (int i = 0; i < numberOfThreads; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return httpClientConfig.httpClientInstance();
                }));
            }
            ready.await();
            start.countDown();

            final HttpClient first = futures.get(0).get();
            for (Future<HttpClient> future : futures) {
                assertThat(future.get()).isSameAs(first);
            }
            assertThat(numberOfCreatedClients).hasValue(1);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void reusedHttpClientInstanceIsKeptByCopiesThatDontChangeHowTheClientIsCreated() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig().reuseHttpClientInstance();
        final HttpClient httpClient = httpClientConfig.httpClientInstance();

        assertThat(httpClientConfig.httpClientInstance()).isSameAs(httpClient);
        assertThat(httpClientConfig.httpMultipartMode(HttpMultipartMode.BROWSER_COMPATIBLE).httpClientInstance()).isSameAs(httpClient);
        assertThat(httpClientConfig.reuseHttpClientInstance().httpClientInstance()).isSameAs(httpClient);
        assertThat(httpClientConfig.setParam(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS).httpClientInstance()).isNotSameAs(httpClient);
    }
}
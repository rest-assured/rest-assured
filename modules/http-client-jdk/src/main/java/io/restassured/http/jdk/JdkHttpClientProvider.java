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

package io.restassured.http.jdk;

import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.ClientSharing;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpClientProvider;

import java.net.http.HttpClient;

/**
 * Sends REST Assured's requests with the HTTP client of the JDK, {@link java.net.http.HttpClient}. One client is shared
 * by the requests with the same settings. Customize it with
 * {@code httpClientConfig().customizeClient(HttpClient.Builder.class, builder -> ..)}.
 * <p>
 * REST Assured creates the provider itself. It's public so that it can be found with {@link java.util.ServiceLoader}.
 */
public final class JdkHttpClientProvider implements HttpClientProvider {

    /**
     * Created by REST Assured.
     */
    public JdkHttpClientProvider() {
    }

    @Override
    public String name() {
        return "jdk";
    }

    @Override
    public String displayName() {
        return "the JDK HTTP client";
    }

    @Override
    public String artifact() {
        return "io.rest-assured:http-client-jdk";
    }

    @Override
    public String libraryDescription() {
        return "Java " + Runtime.version();
    }

    @Override
    public ClientSharing sharing() {
        return ClientSharing.SHARED;
    }

    @Override
    public Class<?> builderType() {
        return HttpClient.Builder.class;
    }

    @Override
    public HttpClientInstance newClient(ClientSettings settings) {
        return new JdkHttpClientInstance(settings);
    }
}

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

package io.restassured.http.apache5;

import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.ClientSharing;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpClientProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

/**
 * Sends REST Assured's requests with Apache HttpClient 5, through its classic (blocking) API. Each request gets a new
 * client, which it keeps across its redirects and authentication challenges, and which is closed once REST Assured is
 * done with the response. With {@code httpClientConfig().reuseHttpClientInstance()}, the requests of a config share one
 * client. Customize it with {@code httpClientConfig().customizeClient(HttpClientBuilder.class, builder -> ..)}, see the
 * {@linkplain io.restassured.http.apache5 package} for what a customizer can change.
 * <p>
 * REST Assured creates the provider itself. It's public so that it can be found with {@link java.util.ServiceLoader}.
 * Creating it fails with an {@link IllegalStateException} when HttpClient 5 is missing or older than 5.2.1.
 */
public final class Apache5HttpClientProvider implements HttpClientProvider {

    /**
     * Created by REST Assured.
     *
     * @throws IllegalStateException if Apache HttpClient 5.2.1 or later isn't on the classpath
     */
    public Apache5HttpClientProvider() {
        // Before any HttpClient 5 class is used, so that a missing or old library fails with a message
        Apache5Library.check();
    }

    @Override
    public String name() {
        return "apache5";
    }

    @Override
    public String displayName() {
        return "Apache HttpClient 5";
    }

    @Override
    public String artifact() {
        return "io.rest-assured:http-client-apache5";
    }

    @Override
    public String libraryDescription() {
        return Apache5Library.description();
    }

    @Override
    public ClientSharing sharing() {
        return ClientSharing.PER_REQUEST;
    }

    @Override
    public Class<?> builderType() {
        return HttpClientBuilder.class;
    }

    @Override
    public HttpClientInstance newClient(ClientSettings settings) {
        return new Apache5HttpClientInstance(settings);
    }
}

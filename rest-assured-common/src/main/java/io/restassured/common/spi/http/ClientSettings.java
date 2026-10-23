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

package io.restassured.common.spi.http;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * The settings a client is created with. Equal settings give an equal client, so REST Assured shares one client for
 * each distinct settings of a {@link ClientSharing#SHARED} provider.
 *
 * @param tls                      the TLS settings, or {@code null} for the library's defaults
 * @param proxy                    the proxy, or {@code null} to use {@link java.net.ProxySelector#getDefault()}
 * @param version                  the HTTP version to use
 * @param connectTimeout           how long to wait for a connection, or {@code null} for the library's default
 * @param connectionRequestTimeout how long to wait for a connection from the pool, or {@code null} for the library's default
 * @param customizers              what the user customizes on the library's builder, in order
 */
public record ClientSettings(TlsSettings tls, ProxySettings proxy, HttpVersion version, Duration connectTimeout,
                             Duration connectionRequestTimeout, List<ClientCustomizer> customizers) {

    public ClientSettings {
        Objects.requireNonNull(version, "version");
        customizers = List.copyOf(customizers);
    }
}

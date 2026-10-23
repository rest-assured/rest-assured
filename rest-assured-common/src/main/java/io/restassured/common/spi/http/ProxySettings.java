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

import java.util.Objects;

/**
 * The proxy that requests are sent through.
 *
 * @param scheme   how the client talks to the proxy, {@code http} or {@code https}
 * @param host     the host of the proxy
 * @param port     the port of the proxy
 * @param userName the user name to log in to the proxy with, or {@code null}
 * @param password the password to log in to the proxy with, or {@code null}
 */
public record ProxySettings(String scheme, String host, int port, String userName, String password) {

    public ProxySettings {
        Objects.requireNonNull(scheme, "scheme");
        Objects.requireNonNull(host, "host");
    }

    @Override
    public String toString() {
        return "ProxySettings[scheme=" + scheme + ", host=" + host + ", port=" + port + ", userName=" + userName + "]";
    }
}

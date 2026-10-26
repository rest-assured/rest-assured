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

package io.restassured.config;

/**
 * The HTTP clients of REST Assured's own modules, to pick one with {@link HttpClientConfig#client(HttpClientType)}.
 */
public enum HttpClientType {
    /**
     * The JDK HTTP client ({@code java.net.http.HttpClient}), from {@code io.rest-assured:http-client-jdk}.
     */
    JDK("jdk"),
    /**
     * Apache HttpClient 5, from {@code io.rest-assured:http-client-apache5}.
     */
    APACHE5("apache5");

    private final String providerName;

    HttpClientType(String providerName) {
        this.providerName = providerName;
    }

    /**
     * @return The name of the client, which {@link HttpClientConfig#client(String)} and the system property
     * {@code restassured.http.client} also take, such as {@code jdk}
     */
    public String providerName() {
        return providerName;
    }
}

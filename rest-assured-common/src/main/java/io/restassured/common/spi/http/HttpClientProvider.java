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

/**
 * Creates the clients of one HTTP client library. REST Assured finds its own providers by class name and those of
 * other libraries with {@link java.util.ServiceLoader}. A provider is stateless and has a public no-argument constructor.
 */
public interface HttpClientProvider {

    /**
     * @return the short name users choose the client by, for example {@code jdk}
     */
    String name();

    /**
     * @return the name shown in messages, for example {@code the JDK HTTP client}
     */
    String displayName();

    /**
     * @return the Maven coordinates of the artifact that holds the provider, as {@code groupId:artifactId}
     */
    String artifact();

    /**
     * @return the library and its version, for example {@code Java 17.0.20}, or {@code null} if it's unknown
     */
    String libraryDescription();

    /**
     * @return whether one client is shared by requests or each request gets its own
     */
    ClientSharing sharing();

    /**
     * @return the type of the builder that {@link ClientCustomizer}s customize, or {@code null} if the client can't be
     * customized
     */
    Class<?> builderType();

    /**
     * @param settings the settings of the client
     * @return a new client with these settings
     * @throws UnsupportedFeatureException if the library can't do what the settings ask for
     */
    HttpClientInstance newClient(ClientSettings settings);
}

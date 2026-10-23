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
import java.util.function.Consumer;

/**
 * Something a user customizes on the builder of an HTTP client library, applied after the client has applied its
 * {@link ClientSettings}. Two customizers are equal when they have the same builder type and the same consumer instance.
 */
public final class ClientCustomizer {

    private final Class<?> builderType;
    private final Consumer<Object> customizer;

    /**
     * @param builderType the type of the builder, see {@link HttpClientProvider#builderType()}
     * @param customizer  what to do with the builder
     */
    @SuppressWarnings("unchecked")
    public <B> ClientCustomizer(Class<B> builderType, Consumer<? super B> customizer) {
        this.builderType = Objects.requireNonNull(builderType, "builderType");
        this.customizer = (Consumer<Object>) Objects.requireNonNull(customizer, "customizer");
    }

    /**
     * @return the type of the builder this customizer customizes
     */
    public Class<?> builderType() {
        return builderType;
    }

    /**
     * @param builder the builder of the client
     * @throws ClassCastException if the builder isn't a {@link #builderType()}
     */
    public <B> void applyTo(B builder) {
        customizer.accept(builderType.cast(builder));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientCustomizer)) return false;
        ClientCustomizer that = (ClientCustomizer) o;
        return builderType.equals(that.builderType) && customizer == that.customizer;
    }

    @Override
    public int hashCode() {
        return 31 * builderType.hashCode() + System.identityHashCode(customizer);
    }

    @Override
    public String toString() {
        return "ClientCustomizer[builderType=" + builderType.getName() + ", customizer=" + customizer + "]";
    }
}

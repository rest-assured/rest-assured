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

package io.restassured.authentication;

import io.restassured.internal.http.HTTPBuilder;
import io.restassured.internal.http.HTTPBuilderAuthenticationContext;

/**
 * An {@link AuthenticationScheme} that only implements {@link #authenticate(AuthenticationContext)}, so that it doesn't
 * depend on the HTTP client. For example:
 * <pre>
 * RestAssured.authentication = (ContextAuthenticationScheme) context -> context.header("X-Api-Key", "secret");
 * </pre>
 * In REST Assured 7.0.0, {@link #authenticate(AuthenticationContext)} is the method every {@link AuthenticationScheme}
 * implements, and this interface is deprecated.
 */
@FunctionalInterface
public interface ContextAuthenticationScheme extends AuthenticationScheme {

    @Override
    void authenticate(AuthenticationContext context);

    /**
     * Calls {@link #authenticate(AuthenticationContext)}.
     */
    @Override
    @SuppressWarnings("deprecation")
    default void authenticate(HTTPBuilder httpBuilder) {
        authenticate(new HTTPBuilderAuthenticationContext(httpBuilder));
    }
}

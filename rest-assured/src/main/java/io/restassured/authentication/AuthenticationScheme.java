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

package io.restassured.authentication;

import io.restassured.internal.http.HTTPBuilder;

/**
 * Authenticates a request. Implement {@link ContextAuthenticationScheme} to write your own scheme without depending on
 * the HTTP client.
 */
public interface AuthenticationScheme {

    /**
     * @param httpBuilder The HTTP builder of the request
     * @deprecated HTTPBuilder exposes HttpClient 4 and is removed in REST Assured 7.0.0. Implement
     * {@link ContextAuthenticationScheme} instead, which only needs {@link #authenticate(AuthenticationContext)}.
     */
    @Deprecated
    void authenticate(HTTPBuilder httpBuilder);

    /**
     * Authenticate the request through the given context. In REST Assured 6.x this throws unless the scheme implements
     * it, and REST Assured calls {@link #authenticate(HTTPBuilder)}. In 7.0.0 this is the method every scheme implements.
     *
     * @param context What the scheme can do to the request
     */
    default void authenticate(AuthenticationContext context) {
        throw new UnsupportedOperationException(getClass().getName() + " doesn't implement authenticate(AuthenticationContext). Implement "
                + ContextAuthenticationScheme.class.getName() + " instead of " + AuthenticationScheme.class.getName() + ".");
    }
}

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

/**
 * An {@link AuthenticationScheme} that only implements {@link #authenticate(AuthenticationContext)}. Since REST Assured
 * 7.0.0 that's the only method of {@link AuthenticationScheme}, so this interface adds nothing.
 *
 * @deprecated Implement {@link AuthenticationScheme} instead.
 */
@Deprecated
@FunctionalInterface
public interface ContextAuthenticationScheme extends AuthenticationScheme {

    @Override
    void authenticate(AuthenticationContext context);
}

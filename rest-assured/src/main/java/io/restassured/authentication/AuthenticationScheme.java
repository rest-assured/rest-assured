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

/**
 * Authenticates a request, without depending on the HTTP client that sends it. For example:
 * <pre>
 * RestAssured.authentication = context -> context.header("X-Api-Key", "secret");
 * </pre>
 */
@FunctionalInterface
public interface AuthenticationScheme {

    /**
     * Authenticate the request through the given context.
     *
     * @param context What the scheme can do to the request
     */
    void authenticate(AuthenticationContext context);
}

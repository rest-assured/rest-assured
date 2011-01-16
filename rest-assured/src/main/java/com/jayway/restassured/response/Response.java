/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.response;

/**
 * The response of a request made by REST Assured.
 * <p>
 * The response can only be returned if you don't use any expectations on the body. E.g.
 * <pre>
 *   expect().body(equalTo("my body")).when().get("/something").asString()
 * </pre>
 * will throw an {@link IllegalStateException} because of the <code>body</code> expectation matcher.
 *
 * </p>
 */
public interface Response {

    /**
     * Syntactic sugar, simply returns the same response instance.
     *
     * @return The same response instance.
     */
    Response andReturn();

    /**
     * Syntactic sugar, simply returns the same response instance.
     *
     * @return The same response instance.
     */
    Response thenReturn();

    /**
     * Syntactic sugar, simply returns the same response instance.
     *
     * @return The same response instance.
     */
    Response body();

    /**
     * Get the body as a string. You can only do this if you've used REST Assured response expectations.
     *
     * @return The body as a string.
     */
    String asString();

    /**
     * Get the body as a byte array. You can only do this if you've used REST Assured response expectations.
     *
     * @return The body as a array.
     */
    byte[] asByteArray();
}

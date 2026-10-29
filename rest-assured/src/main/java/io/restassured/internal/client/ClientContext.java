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

package io.restassured.internal.client;

import io.restassured.internal.http.client.HttpClientInstances;

import java.util.function.Supplier;

/**
 * What a request specification takes from the {@link io.restassured.RestAssuredClient} whose <code>given()</code>
 * created it, besides the defaults it starts from: the HTTP client instances that send it, and the defaults that
 * <code>then()</code> validates its response with. Merging a specification never copies it.
 *
 * @param instances        The HTTP client instances and kept cookies
 * @param responseDefaults Gives the defaults of <code>then()</code> when it's called
 */
public record ClientContext(HttpClientInstances instances, Supplier<ResponseDefaults> responseDefaults) {

    /**
     * The context of the static API, and of specifications that no client created, such as those of
     * {@link io.restassured.builder.RequestSpecBuilder}: the global instances, and <code>then()</code> reads
     * {@link io.restassured.RestAssured#rootPath} and {@link io.restassured.RestAssured#responseSpecification}.
     */
    public static final ClientContext STATIC = new ClientContext(HttpClientInstances.global(), ResponseDefaults::ofStaticFields);
}

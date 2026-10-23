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
 * The engines that can evaluate JSON paths. Use it with {@link JsonConfig#pathEngine(JsonPathEngine)}.
 */
public enum JsonPathEngine {
    /**
     * GPath, the Groovy path syntax that REST Assured has always used, such as {@code store.book.findAll { it.price < 10 }.title},
     * from {@code io.rest-assured:json-path-gpath}.
     */
    GPATH,
    /**
     * JSONPath, as implemented by Jayway JsonPath, such as {@code $.store.book[?(@.price < 10)].title}, from
     * {@code io.rest-assured:json-path-jayway}.
     */
    JAYWAY
}

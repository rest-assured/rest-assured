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

package io.restassured.internal.mapping;

final class MapperSupport {

    private MapperSupport() {
    }

    /**
     * The type that a mapper passes to its object mapper factory. The mappers used to be written in Groovy, where
     * {@code null.getClass()} doesn't throw, so a {@code null} object is still serialized (to {@code "null"} by the JSON
     * mappers). Groovy passed its internal {@code NullObject} class to the factory; {@code Object.class} is used instead.
     */
    static Class<?> typeOf(Object object) {
        return object == null ? Object.class : object.getClass();
    }
}

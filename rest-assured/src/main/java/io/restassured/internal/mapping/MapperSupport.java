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

import io.restassured.mapper.ObjectMapperDeserializationContext;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

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

    /**
     * A reader of the body stream with the platform default charset, as Groovy's {@code withReader(InputStream, ..)} that
     * the Johnzon and JSON-B mappers used to read the body with.
     */
    static Reader platformCharsetReader(ObjectMapperDeserializationContext context) {
        return new InputStreamReader(context.getDataToDeserialize().asInputStream(), Charset.defaultCharset());
    }

    /**
     * The class that a JAXB mapper deserializes to. JAXB unmarshals to classes only, not to generic types.
     */
    static Class<?> classToUnmarshal(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        throw new IllegalArgumentException("JAXB can only deserialize to a class, not to " + type + ".");
    }
}

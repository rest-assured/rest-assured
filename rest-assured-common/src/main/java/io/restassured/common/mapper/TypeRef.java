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
package io.restassured.common.mapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


/**
 * Used to specify generic type information when de-serializing a Response.
 *
 * <pre>
 * TypeRef ref = new TypeRef<List<Map<String, Integer>>() { };
 * </pre>
 *
 * Code is heavily inspired by (some might even say copied), from the <a href="https://github.com/json-path/JsonPath">JsonPath</a> project.
 * @param <T> The type
 */
public abstract class TypeRef<T> implements Comparable<TypeRef<T>> {
    protected final Type type;

    protected TypeRef() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) {
            throw new IllegalArgumentException("No type info in TypeRef");
        }
        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }

    public Class<T> getTypeAsClass() {
        //noinspection unchecked
        return (Class<T>) ((ParameterizedType) type).getRawType();
    }

    /**
     * The only reason we define this method (and require implementation
     * of <code>Comparable</code>) is to prevent constructing a
     * reference without type information.
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(TypeRef<T> ignored) {
        return 0;
    }
}


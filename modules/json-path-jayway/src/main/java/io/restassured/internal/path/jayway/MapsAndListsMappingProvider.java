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

package io.restassured.internal.path.jayway;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Maps the values that {@link JsonParser} creates to the type asked for with {@code DocumentContext.read(path, type)}:
 * a value that already is of the type, a number to another number type, an object or array to a JSON String, and a
 * number or boolean to its String. Other mappings, such
 * as to a bean, need an object mapper.
 */
class MapsAndListsMappingProvider implements MappingProvider {

    @Override
    public <T> T map(Object source, Class<T> targetType, Configuration configuration) {
        if (source == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) MethodType.methodType(targetType).wrap().returnType();
        if (type.isInstance(source)) {
            return type.cast(source);
        } else if (type == String.class) {
            // An object or array as JSON, and a number or boolean as its text
            return type.cast(source instanceof Map || source instanceof List ? configuration.jsonProvider().toJson(source) : source.toString());
        } else if (source instanceof Number number) {
            Object converted = convert(number, type);
            if (converted != null) {
                return type.cast(converted);
            }
        }
        throw new MappingException("json-path-jayway can't map a " + source.getClass().getName() + " to " + targetType.getName()
                + ". Read the value without a type, or map it with an object mapper.");
    }

    @Override
    public <T> T map(Object source, TypeRef<T> targetType, Configuration configuration) {
        throw new MappingException("json-path-jayway can't map to a TypeRef. Read the value without a type, or map it with an object mapper.");
    }

    private static Object convert(Number number, Class<?> type) {
        if (type == Integer.class) {
            return number.intValue();
        } else if (type == Long.class) {
            return number.longValue();
        } else if (type == Double.class) {
            return number.doubleValue();
        } else if (type == Float.class) {
            return number.floatValue();
        } else if (type == Short.class) {
            return number.shortValue();
        } else if (type == Byte.class) {
            return number.byteValue();
        } else if (type == BigDecimal.class) {
            return new BigDecimal(number.toString());
        } else if (type == BigInteger.class) {
            return new BigDecimal(number.toString()).toBigInteger();
        }
        return null;
    }
}

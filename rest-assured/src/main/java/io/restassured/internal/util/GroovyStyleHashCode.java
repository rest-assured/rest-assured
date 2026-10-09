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

package io.restassured.internal.util;

import java.util.Arrays;

/**
 * The hash code that Groovy's {@code @Canonical} (through {@code @EqualsAndHashCode} and {@code HashCodeHelper})
 * generated for classes that used to be written in Groovy.
 */
public final class GroovyStyleHashCode {

    private GroovyStyleHashCode() {
    }

    /**
     * @param self       The object whose hash code is computed. A property that refers to it is left out.
     * @param properties The values of the properties, in declaration order.
     * @return The hash code
     */
    public static int hashCode(Object self, Object... properties) {
        int hash = 127;
        for (Object value : properties) {
            if (value != self) {
                hash = 59 * hash + hashOf(value);
            }
        }
        return hash;
    }

    private static int hashOf(Object value) {
        if (value == null) {
            return 0;
        } else if (value instanceof Boolean) {
            return (Boolean) value ? 79 : 97;
        } else if (value instanceof Object[]) {
            return Arrays.hashCode((Object[]) value);
        } else if (value.getClass().isArray()) {
            return primitiveArrayHashCode(value);
        }
        return value.hashCode();
    }

    private static int primitiveArrayHashCode(Object array) {
        if (array instanceof byte[]) {
            return Arrays.hashCode((byte[]) array);
        } else if (array instanceof int[]) {
            return Arrays.hashCode((int[]) array);
        } else if (array instanceof char[]) {
            return Arrays.hashCode((char[]) array);
        } else if (array instanceof long[]) {
            return Arrays.hashCode((long[]) array);
        } else if (array instanceof short[]) {
            return Arrays.hashCode((short[]) array);
        } else if (array instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) array);
        } else if (array instanceof float[]) {
            return Arrays.hashCode((float[]) array);
        }
        return Arrays.hashCode((double[]) array);
    }
}

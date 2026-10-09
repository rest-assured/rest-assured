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

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Renders values the way a dynamically dispatched {@code value.toString()} does in Groovy, which is what users saw
 * when this code was written in Groovy. For example a {@code byte[]} is rendered as {@code [65, 66]} (not {@code [B@1b6d3586}),
 * a map as {@code [a:1]} (not {@code {a=1}}) and a {@code char[]} as the string it holds. Other values use their own
 * {@code toString()}.
 */
public class GroovyStyleToString {

    private GroovyStyleToString() {
    }

    /**
     * @param value The value
     * @return {@code value} rendered like Groovy's {@code value.toString()}.
     */
    public static String toString(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof char[]) {
            return new String((char[]) value);
        } else if (value.getClass().isArray()) {
            return format(value);
        } else if ((value instanceof Collection || value instanceof Map) && !overridesToString(value.getClass())) {
            // Groovy's toString() for maps and collections only applies when no public class below
            // AbstractMap/AbstractCollection (or Object) declares its own toString(), e.g. ConcurrentHashMap keeps "{a=1}".
            return format(value);
        }
        return value.toString();
    }

    private static boolean overridesToString(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (Modifier.isPublic(current.getModifiers()) && declaresToString(current)) {
                return current != Object.class && current != AbstractMap.class && current != AbstractCollection.class;
            }
        }
        return false;
    }

    private static boolean declaresToString(Class<?> type) {
        try {
            type.getDeclaredMethod("toString");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // Port of Groovy's InvokerHelper.format(Object, verbose = false)
    private static String format(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof char[]) {
            return new String((char[]) value);
        } else if (value instanceof Collection) {
            return formatCollection((Collection<?>) value);
        } else if (value instanceof Map) {
            return formatMap((Map<?, ?>) value);
        } else if (value.getClass().isArray()) {
            return formatArray(value);
        }
        return String.valueOf(value.toString());
    }

    private static String formatArray(Object array) {
        StringBuilder builder = new StringBuilder("[");
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(format(Array.get(array, i)));
        }
        return builder.append(']').toString();
    }

    private static String formatCollection(Collection<?> collection) {
        StringBuilder builder = new StringBuilder("[");
        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Object item = iterator.next();
            builder.append(item == collection ? "(this Collection)" : format(item));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }

    private static String formatMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "[:]";
        }
        StringBuilder builder = new StringBuilder("[");
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            builder.append(entry.getKey() == map ? "(this Map)" : format(entry.getKey()))
                    .append(':')
                    .append(entry.getValue() == map ? "(this Map)" : format(entry.getValue()));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.append(']').toString();
    }
}

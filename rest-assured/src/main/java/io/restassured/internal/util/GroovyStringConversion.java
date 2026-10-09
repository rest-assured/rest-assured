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

import io.restassured.internal.common.util.GroovyStyleToString;
import org.w3c.dom.Element;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Converts an object to a String the way Groovy does when it coerces a value to String, for example when a method
 * declared to return String returns something else. Code ported from Groovy uses it where that String is visible to users.
 * <p>
 * Groovy renders arrays and collections as {@code [a, b]}, maps as {@code [a:1]} (and {@code [:]} when empty),
 * {@code char[]} as its characters, Groovy ranges and everything else by {@code toString()}, and DOM elements as
 * pretty-printed XML. Nested values are rendered the same way, with {@code null} as {@code "null"}.
 */
public final class GroovyStringConversion {

    private static final String GROOVY_RANGE = "groovy.lang.Range";

    private GroovyStringConversion() {
    }

    /**
     * @return {@code null} for {@code null}, the String itself for a String, otherwise what Groovy renders for the object.
     */
    public static String castToString(Object object) {
        if (object == null || object instanceof String) {
            return (String) object;
        }
        return format(object);
    }

    private static String format(Object object) {
        if (object == null) {
            return "null";
        }
        if (object.getClass().isArray()) {
            if (object instanceof Object[]) {
                return formatArray((Object[]) object);
            }
            if (object instanceof char[]) {
                return new String((char[]) object);
            }
            return formatPrimitiveArray(object);
        }
        if (isGroovyRange(object.getClass())) {
            return object.toString();
        }
        if (object instanceof Collection) {
            return formatCollection((Collection<?>) object);
        }
        if (object instanceof Map) {
            return formatMap((Map<?, ?>) object);
        }
        if (object instanceof Element) {
            return GroovyStyleToString.serialize((Element) object);
        }
        return object.toString();
    }

    private static String formatArray(Object[] array) {
        StringBuilder buffer = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(array[i] == array ? "(this array)" : format(array[i]));
        }
        return buffer.append(']').toString();
    }

    private static String formatPrimitiveArray(Object array) {
        StringBuilder buffer = new StringBuilder("[");
        for (int i = 0; i < Array.getLength(array); i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(Array.get(array, i));
        }
        return buffer.append(']').toString();
    }

    private static String formatCollection(Collection<?> collection) {
        StringBuilder buffer = new StringBuilder("[");
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                buffer.append(", ");
            }
            first = false;
            buffer.append(item == collection ? "(this Collection)" : format(item));
        }
        return buffer.append(']').toString();
    }

    private static String formatMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "[:]";
        }
        StringBuilder buffer = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                buffer.append(", ");
            }
            first = false;
            buffer.append(entry.getKey() == map ? "(this Map)" : format(entry.getKey()))
                    .append(':')
                    .append(entry.getValue() == map ? "(this Map)" : format(entry.getValue()));
        }
        return buffer.append(']').toString();
    }

    private static boolean isGroovyRange(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> anInterface : current.getInterfaces()) {
                if (GROOVY_RANGE.equals(anInterface.getName()) || isGroovyRange(anInterface)) {
                    return true;
                }
            }
        }
        return false;
    }
}

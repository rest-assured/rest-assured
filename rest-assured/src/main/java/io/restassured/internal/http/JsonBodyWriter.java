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

package io.restassured.internal.http;

import java.lang.reflect.Array;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Writes a Map or Collection request body as JSON, the way Groovy's {@code JsonBuilder} did before REST Assured 7.0.0:
 * <ul>
 * <li>Strings and other {@link CharSequence}s, {@link Character}s and {@link URL}s are written as strings, with
 * {@code "}, {@code \}, control characters and everything above {@code ~} escaped. {@link UUID}s and enums (by name)
 * are written in quotes as they are.</li>
 * <li>Numbers are written with their {@code toString()}. A {@link Double} or {@link Float} that is NaN or infinite
 * fails.</li>
 * <li>A {@link Date} or {@link Calendar} is written as {@code yyyy-MM-dd'T'HH:mm:ssZ} in GMT.</li>
 * <li>Arrays, {@link Iterable}s, {@link Iterator}s and {@link Enumeration}s are written as JSON arrays, and maps as
 * JSON objects with the {@code toString()} of their keys.</li>
 * </ul>
 * Any other value, such as a bean or a record, is written by the given object mapper.
 */
class JsonBodyWriter {
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private final Function<Object, String> objectMapper;

    /**
     * @param objectMapper writes a value that isn't one of the JDK types above as JSON
     */
    JsonBodyWriter(Function<Object, String> objectMapper) {
        this.objectMapper = objectMapper;
    }

    String write(Object value) {
        StringBuilder json = new StringBuilder();
        writeValue(value, json);
        return json.toString();
    }

    private void writeValue(Object value, StringBuilder json) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof CharSequence) {
            writeString(value.toString(), json);
        } else if (value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Number) {
            writeNumber((Number) value, json);
        } else if (value instanceof Date) {
            writeDate((Date) value, json);
        } else if (value instanceof Calendar) {
            writeDate(((Calendar) value).getTime(), json);
        } else if (value instanceof Map) {
            writeMap((Map<?, ?>) value, json);
        } else if (value instanceof Iterable) {
            writeIterator(((Iterable<?>) value).iterator(), json);
        } else if (value instanceof Iterator) {
            writeIterator((Iterator<?>) value, json);
        } else if (value instanceof Character || value instanceof URL) {
            writeString(value.toString(), json);
        } else if (value instanceof UUID) {
            writeQuoted(value.toString(), json);
        } else if (value instanceof Enumeration) {
            writeIterator(Collections.list((Enumeration<?>) value).iterator(), json);
        } else if (value.getClass().isArray()) {
            writeArray(value, json);
        } else if (value instanceof Enum) {
            writeQuoted(((Enum<?>) value).name(), json);
        } else {
            json.append(objectMapper.apply(value));
        }
    }

    private static void writeNumber(Number number, StringBuilder json) {
        if (number instanceof Double || number instanceof Float) {
            double value = number.doubleValue();
            if (Double.isInfinite(value)) {
                throw new IllegalArgumentException("Number " + number + " can't be serialized as JSON: infinite are not allowed in JSON.");
            } else if (Double.isNaN(value)) {
                throw new IllegalArgumentException("Number " + number + " can't be serialized as JSON: NaN are not allowed in JSON.");
            }
        }
        json.append(number);
    }

    private static void writeDate(Date date, StringBuilder json) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        format.setTimeZone(GMT);
        writeQuoted(format.format(date), json);
    }

    private void writeMap(Map<?, ?> map, StringBuilder json) {
        json.append('{');
        String separator = "";
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Maps with null keys can't be converted to JSON");
            }
            json.append(separator);
            writeString(entry.getKey().toString(), json);
            json.append(':');
            writeValue(entry.getValue(), json);
            separator = ",";
        }
        json.append('}');
    }

    private void writeIterator(Iterator<?> iterator, StringBuilder json) {
        json.append('[');
        String separator = "";
        while (iterator.hasNext()) {
            json.append(separator);
            writeValue(iterator.next(), json);
            separator = ",";
        }
        json.append(']');
    }

    /**
     * Elements of a primitive array are written with their {@code toString()}, without the NaN and infinity check of
     * {@link #writeNumber(Number, StringBuilder)}, as {@code JsonBuilder} did. A {@code char[]} is written as an array of strings.
     */
    private void writeArray(Object array, StringBuilder json) {
        if (array instanceof Object[]) {
            writeIterator(Arrays.asList((Object[]) array).iterator(), json);
            return;
        }
        json.append('[');
        for (int i = 0, length = Array.getLength(array); i < length; i++) {
            if (i > 0) {
                json.append(',');
            }
            Object element = Array.get(array, i);
            if (element instanceof Character) {
                writeString(element.toString(), json);
            } else {
                json.append(element);
            }
        }
        json.append(']');
    }

    private static void writeQuoted(String string, StringBuilder json) {
        json.append('"').append(string).append('"');
    }

    private static void writeString(String string, StringBuilder json) {
        json.append('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"':
                    json.append("\\\"");
                    break;
                case '\\':
                    json.append("\\\\");
                    break;
                case '\b':
                    json.append("\\b");
                    break;
                case '\f':
                    json.append("\\f");
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\r':
                    json.append("\\r");
                    break;
                case '\t':
                    json.append("\\t");
                    break;
                default:
                    if (c < ' ' || c > '~') {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
            }
        }
        json.append('"');
    }
}

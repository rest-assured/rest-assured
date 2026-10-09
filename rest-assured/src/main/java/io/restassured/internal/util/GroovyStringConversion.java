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

/**
 * Converts an object to a String the way Groovy does when it coerces a value to String, for example when a method
 * declared to return String returns something else. Code ported from Groovy uses it where that String is visible to users.
 * <p>
 * Groovy renders arrays and collections as {@code [a, b]}, maps as {@code [a:1]} (and {@code [:]} when empty),
 * {@code char[]} as its characters, Groovy ranges and everything else by {@code toString()}, and DOM elements as
 * pretty-printed XML. Nested values are rendered the same way, with {@code null} as {@code "null"}.
 */
public final class GroovyStringConversion {

    private GroovyStringConversion() {
    }

    /**
     * @return {@code null} for {@code null}, the String itself for a String, otherwise what Groovy renders for the object.
     */
    public static String castToString(Object object) {
        if (object == null || object instanceof String) {
            return (String) object;
        }
        return GroovyStyleToString.format(object);
    }
}

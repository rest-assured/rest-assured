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

package io.restassured.internal.serialization;

import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Locale;
import java.util.UUID;

import static io.restassured.internal.util.GroovyTypes.isGString;

public class SerializationSupport {

    public static boolean isSerializableCandidate(Object object) {
        if (object == null) {
            return false;
        }
        Class clazz = object.getClass();
        return !(Number.class.isAssignableFrom(clazz) || String.class.isAssignableFrom(clazz)
                || isGString(object) || Boolean.class.isAssignableFrom(clazz)
                || Character.class.isAssignableFrom(clazz) || object instanceof Enum ||
                Locale.class.isAssignableFrom(clazz) || Class.class.isAssignableFrom(clazz) || UUID.class.isAssignableFrom(clazz));
    }

    /**
     * Like {@link #isSerializableCandidate(Object)} but for parameter, header and cookie values. These are always sent
     * as text, so java.time values (LocalDate, Instant, Duration, ZoneId, ...) use their ISO-8601 {@code toString()},
     * which is what a server expects in a URL. An object mapper would quote them or, without the JSR-310 module, fail
     * or serialize their internal fields. A request body or multipart content still goes through the object mapper.
     */
    public static boolean isParameterSerializableCandidate(Object object) {
        return isSerializableCandidate(object) && !isJavaTimeValue(object);
    }

    /**
     * When a parameter, header or cookie value is serialized by an object mapper, a value object whose JSON form is a
     * plain string (for example a record with a {@code @JsonValue} accessor) comes back as a quoted JSON string literal.
     * Quotes make no sense in a URL, header or cookie, so unwrap such a literal into the string it represents. Any other
     * serialized form (objects, arrays, numbers, XML) is returned unchanged.
     */
    public static String unwrapJsonStringLiteral(String serialized) {
        if (serialized == null || serialized.length() < 2 || serialized.charAt(0) != '"' || serialized.charAt(serialized.length() - 1) != '"') {
            return serialized;
        }
        StringBuilder unescaped = new StringBuilder(serialized.length() - 2);
        int end = serialized.length() - 1;
        for (int i = 1; i < end; i++) {
            char c = serialized.charAt(i);
            if (c == '"') {
                // An unescaped quote inside means this isn't a single JSON string literal
                return serialized;
            } else if (c != '\\') {
                unescaped.append(c);
            } else if (i + 1 >= end) {
                return serialized;
            } else {
                char escaped = serialized.charAt(++i);
                switch (escaped) {
                    case '"', '\\', '/' -> unescaped.append(escaped);
                    case 'b' -> unescaped.append('\b');
                    case 'f' -> unescaped.append('\f');
                    case 'n' -> unescaped.append('\n');
                    case 'r' -> unescaped.append('\r');
                    case 't' -> unescaped.append('\t');
                    case 'u' -> {
                        if (i + 4 >= end) {
                            return serialized;
                        }
                        int codeUnit = 0;
                        for (int j = i + 1; j <= i + 4; j++) {
                            int digit = Character.digit(serialized.charAt(j), 16);
                            if (digit < 0) {
                                return serialized;
                            }
                            codeUnit = codeUnit * 16 + digit;
                        }
                        unescaped.append((char) codeUnit);
                        i += 4;
                    }
                    default -> {
                        return serialized;
                    }
                }
            }
        }
        return unescaped.toString();
    }

    private static boolean isJavaTimeValue(Object object) {
        return object instanceof TemporalAccessor || object instanceof TemporalAmount || object instanceof ZoneId;
    }
}

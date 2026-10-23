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

import com.jayway.jsonpath.InvalidJsonException;
import io.restassured.common.config.NumberReturnType;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A strict JSON parser (RFC 8259) that reads a document into {@link LinkedHashMap}s, {@link ArrayList}s, strings,
 * booleans, {@code null} and numbers. Numbers are converted as REST Assured's GPath parser converts them, following the
 * {@link NumberReturnType}: an integer is an {@link Integer}, a {@link Long} or a {@link BigInteger}, whichever fits, and
 * a number with a fraction or an exponent is a {@link BigDecimal}, which keeps its scale, unless the type asks for
 * floats and doubles. Jayway's own json-smart parser can't do that, since it turns short decimals into doubles.
 */
public final class JsonParser {
    /**
     * The default maximum number of characters of a number, as in REST Assured's JsonConfig.
     */
    public static final int DEFAULT_NUMBER_LENGTH_LIMIT = 1000;
    private static final BigInteger MIN_INTEGER = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger MAX_INTEGER = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigDecimal MAX_FLOAT = BigDecimal.valueOf(Float.MAX_VALUE);

    private final String json;
    private final NumberReturnType numberReturnType;
    private final int numberLengthLimit;
    private int position;

    private JsonParser(String json, NumberReturnType numberReturnType, int numberLengthLimit) {
        this.json = json;
        this.numberReturnType = numberReturnType;
        this.numberLengthLimit = numberLengthLimit;
    }

    /**
     * Parse a JSON document. Like REST Assured's GPath parser, empty text fails, while text with only whitespace gives {@code null}.
     *
     * @param numberLengthLimit The maximum number of characters of a number, or a negative number for no limit
     * @throws InvalidJsonException If the text isn't a JSON document
     */
    public static Object parse(String json, NumberReturnType numberReturnType, int numberLengthLimit) {
        if (json == null || json.isEmpty()) {
            throw new InvalidJsonException("The JSON input text should neither be null nor empty.");
        }
        return new JsonParser(json, numberReturnType, numberLengthLimit).document();
    }

    /**
     * Parse a JSON document from a reader. Like REST Assured's GPath parser, an empty document gives {@code null}.
     *
     * @see #parse(String, NumberReturnType, int)
     */
    public static Object parse(Reader reader, NumberReturnType numberReturnType, int numberLengthLimit) {
        StringBuilder json = new StringBuilder();
        char[] buffer = new char[8192];
        try (reader) {
            for (int read = reader.read(buffer); read != -1; read = reader.read(buffer)) {
                json.append(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new JsonParser(json.toString(), numberReturnType, numberLengthLimit).document();
    }

    private Object document() {
        skipWhitespace();
        if (position == json.length()) {
            return null;
        }
        Object value = value();
        skipWhitespace();
        if (position < json.length()) {
            throw error("Expected the end of the document");
        }
        return value;
    }

    private Object value() {
        if (position == json.length()) {
            throw error("Expected a value");
        }
        char c = json.charAt(position);
        switch (c) {
            case '{':
                return object();
            case '[':
                return array();
            case '"':
                return string();
            case 't':
                return literal("true", Boolean.TRUE);
            case 'f':
                return literal("false", Boolean.FALSE);
            case 'n':
                return literal("null", null);
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    return number();
                }
                throw error("Expected a value");
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> object = new LinkedHashMap<>();
        position++;
        skipWhitespace();
        if (consume('}')) {
            return object;
        }
        do {
            skipWhitespace();
            if (position == json.length() || json.charAt(position) != '"') {
                throw error("Expected a string key");
            }
            String key = string();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            object.put(key, value());
            skipWhitespace();
        } while (consume(','));
        expect('}');
        return object;
    }

    private List<Object> array() {
        List<Object> array = new ArrayList<>();
        position++;
        skipWhitespace();
        if (consume(']')) {
            return array;
        }
        do {
            skipWhitespace();
            array.add(value());
            skipWhitespace();
        } while (consume(','));
        expect(']');
        return array;
    }

    private String string() {
        position++;
        StringBuilder string = new StringBuilder();
        while (true) {
            if (position == json.length()) {
                throw error("Expected the end of the string");
            }
            char c = json.charAt(position++);
            if (c == '"') {
                return string.toString();
            } else if (c == '\\') {
                string.append(escaped());
            } else if (c < 0x20) {
                position--;
                throw error("Expected an escaped control character");
            } else {
                string.append(c);
            }
        }
    }

    private char escaped() {
        if (position == json.length()) {
            throw error("Expected an escaped character");
        }
        char c = json.charAt(position++);
        switch (c) {
            case '"':
            case '\\':
            case '/':
                return c;
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'u':
                if (position + 4 <= json.length()) {
                    try {
                        char unicode = (char) Integer.parseInt(json.substring(position, position + 4), 16);
                        position += 4;
                        return unicode;
                    } catch (NumberFormatException e) {
                        // Reported below
                    }
                }
                throw error("Expected four hexadecimal digits");
            default:
                position--;
                throw error("Expected an escaped character");
        }
    }

    private Object literal(String text, Object value) {
        if (!json.startsWith(text, position)) {
            throw error("Expected a value");
        }
        position += text.length();
        return value;
    }

    private Object number() {
        int start = position;
        consume('-');
        if (!consume('0')) {
            digits();
        }
        boolean decimal = false;
        if (consume('.')) {
            decimal = true;
            digits();
        }
        if (consume('e') || consume('E')) {
            decimal = true;
            if (!consume('+')) {
                consume('-');
            }
            digits();
        }
        String text = json.substring(start, position);
        if (numberLengthLimit >= 0 && text.length() > numberLengthLimit) {
            throw new InvalidJsonException("JSON number length (" + text.length() + " chars) exceeds the maximum allowed length of "
                    + numberLengthLimit + " chars. Raise or disable it via JsonConfig.numberLengthLimit(int).");
        }
        return decimal ? decimal(new BigDecimal(text)) : integer(new BigInteger(text));
    }

    private Object decimal(BigDecimal value) {
        if (numberReturnType == NumberReturnType.DOUBLE || (numberReturnType == NumberReturnType.FLOAT_AND_DOUBLE && value.abs().compareTo(MAX_FLOAT) > 0)) {
            return value.doubleValue();
        } else if (numberReturnType == NumberReturnType.FLOAT_AND_DOUBLE) {
            return value.floatValue();
        }
        return value;
    }

    private Object integer(BigInteger value) {
        if (numberReturnType == NumberReturnType.BIG_INTEGER) {
            return value;
        } else if (value.compareTo(MIN_INTEGER) >= 0 && value.compareTo(MAX_INTEGER) <= 0) {
            return value.intValue();
        } else if (value.compareTo(MIN_LONG) >= 0 && value.compareTo(MAX_LONG) <= 0) {
            return value.longValue();
        }
        return value;
    }

    private void digits() {
        int start = position;
        while (position < json.length() && json.charAt(position) >= '0' && json.charAt(position) <= '9') {
            position++;
        }
        if (position == start) {
            throw error("Expected a digit");
        }
    }

    private void skipWhitespace() {
        while (position < json.length()) {
            char c = json.charAt(position);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return;
            }
            position++;
        }
    }

    private boolean consume(char c) {
        if (position < json.length() && json.charAt(position) == c) {
            position++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!consume(c)) {
            throw error("Expected '" + c + "'");
        }
    }

    private InvalidJsonException error(String expected) {
        int line = 1;
        int lineStart = 0;
        for (int i = 0; i < position && i < json.length(); i++) {
            if (json.charAt(i) == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        String found = position < json.length() ? "'" + json.charAt(position) + "'" : "the end of the document";
        return new InvalidJsonException(expected + " on line " + line + ", column " + (position - lineStart + 1) + ", but found " + found + ".");
    }
}

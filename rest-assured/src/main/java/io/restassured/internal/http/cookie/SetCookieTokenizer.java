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

package io.restassured.internal.http.cookie;

/**
 * Reads the name, the value and the attributes of a {@code Set-Cookie} header, as REST Assured has always read them:
 * surrounding white space is dropped, a run of white space inside a token becomes one space, and a value may be a
 * quoted string, whose quotes are either part of the value or not, depending on the cookie format.
 */
final class SetCookieTokenizer {
    private static final char DQUOTE = '"';
    private static final char ESCAPE = '\\';

    private final String header;
    private int position;

    SetCookieTokenizer(String header) {
        this.header = header;
    }

    boolean atEnd() {
        return position >= header.length();
    }

    private char current() {
        return header.charAt(position);
    }

    private void next() {
        position++;
    }

    /**
     * @param name  the name, which may be empty
     * @param value the value after the equals sign, or {@code null} if there is no equals sign
     */
    record Pair(String name, String value) {
    }

    /**
     * Reads the next {@code name=value} pair and the semicolon that ends it.
     *
     * @param unquote whether the quotes of a quoted value are removed
     */
    Pair pair(boolean unquote) {
        String name = read("=;", false);
        String value = null;
        if (!atEnd()) {
            boolean hasValue = current() == '=';
            next();
            if (hasValue) {
                value = read(";", unquote);
                if (!atEnd()) {
                    next();
                }
            }
        }
        return new Pair(name, value);
    }

    private String read(String delimiters, boolean unquote) {
        StringBuilder token = new StringBuilder();
        boolean whitespace = false;
        while (!atEnd()) {
            char current = current();
            if (delimiters.indexOf(current) >= 0) {
                break;
            } else if (isWhitespace(current)) {
                skipWhitespace();
                whitespace = true;
            } else {
                if (whitespace && token.length() > 0) {
                    token.append(' ');
                }
                if (unquote && current == DQUOTE) {
                    copyQuoted(token);
                } else {
                    copyPlain(token, delimiters);
                }
                whitespace = false;
            }
        }
        return token.toString();
    }

    private void copyPlain(StringBuilder token, String delimiters) {
        while (!atEnd()) {
            char current = current();
            if (delimiters.indexOf(current) >= 0 || isWhitespace(current)) {
                return;
            }
            token.append(current);
            next();
        }
    }

    private void copyQuoted(StringBuilder token) {
        next();
        boolean escaped = false;
        while (!atEnd()) {
            char current = current();
            next();
            if (escaped) {
                if (current != DQUOTE && current != ESCAPE) {
                    token.append(ESCAPE);
                }
                token.append(current);
                escaped = false;
            } else if (current == DQUOTE) {
                return;
            } else if (current == ESCAPE) {
                escaped = true;
            } else if (current != '\r' && current != '\n') {
                token.append(current);
            }
        }
    }

    private void skipWhitespace() {
        while (!atEnd() && isWhitespace(current())) {
            next();
        }
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }
}

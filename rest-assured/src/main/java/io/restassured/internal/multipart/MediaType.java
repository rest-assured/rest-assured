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

package io.restassured.internal.multipart;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * A content-type as a multipart writes it in its headers. It parses, merges and formats the way HttpClient 4's
 * {@code org.apache.http.entity.ContentType} does, so the headers keep the bytes they had when httpmime wrote them:
 * parameters are written as {@code name=value} separated by {@code "; "}, and a value is quoted only when it contains a
 * separator character.
 */
final class MediaType {
    private static final String SEPARATORS = " ;,:@()<>\\\"/[]?={}\t";
    private static final String CHARSET = "charset";

    private final String mimeType;
    private final Charset charset;
    // null when the content-type was created without parameters
    private final List<Parameter> parameters;

    private MediaType(String mimeType, Charset charset, List<Parameter> parameters) {
        this.mimeType = mimeType;
        this.charset = charset;
        this.parameters = parameters;
    }

    /**
     * @param mimeType the mime-type without parameters, such as {@code multipart/form-data}
     * @return the content-type with the mime-type in lower case
     * @throws IllegalArgumentException if the mime-type contains {@code "}, {@code ,} or {@code ;}
     */
    static MediaType create(String mimeType) {
        String lowerCase = mimeType.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("\"") || lowerCase.contains(",") || lowerCase.contains(";")) {
            throw new IllegalArgumentException("MIME type may not contain reserved characters");
        }
        return new MediaType(lowerCase, null, null);
    }

    /**
     * Parses the first element of a header value, such as {@code text/plain; charset=UTF-8}. The mime-type keeps its
     * case and the parameters keep their order.
     *
     * @throws IllegalArgumentException            if the value has no element
     * @throws java.nio.charset.UnsupportedCharsetException if the charset parameter names an unknown charset
     */
    static MediaType parse(String value) {
        Parser parser = new Parser(value);
        while (!parser.atEnd()) {
            Parameter element = parser.nameValuePair();
            List<Parameter> parameters = new ArrayList<>();
            if (!parser.atEnd() && parser.previous() != ',') {
                parameters = parser.parameters();
            }
            if (!(element.name.isEmpty() && element.value == null)) {
                return withCharsetFromParameters(element.name, parameters);
            }
        }
        throw new IllegalArgumentException("Invalid content type: " + value);
    }

    /**
     * @return a copy with the parameter added, or replaced if one with exactly this name exists
     */
    MediaType withParameter(String name, String value) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                merged.put(parameter.name, parameter.value);
            }
        }
        merged.put(name, value);
        List<Parameter> newParameters = new ArrayList<>();
        if (charset != null && !merged.containsKey(CHARSET)) {
            newParameters.add(new Parameter(CHARSET, charset.name()));
        }
        merged.forEach((n, v) -> newParameters.add(new Parameter(n, v)));
        return withCharsetFromParameters(mimeType, newParameters);
    }

    /**
     * @return the charset of the first {@code charset} parameter, or {@code null}
     */
    Charset getCharset() {
        return charset;
    }

    @Override
    public String toString() {
        StringBuilder value = new StringBuilder(mimeType);
        if (parameters != null) {
            value.append("; ");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    value.append("; ");
                }
                appendParameter(value, parameters.get(i));
            }
        } else if (charset != null) {
            value.append("; charset=").append(charset.name());
        }
        return value.toString();
    }

    private static MediaType withCharsetFromParameters(String mimeType, List<Parameter> parameters) {
        Charset charset = null;
        for (Parameter parameter : parameters) {
            if (parameter.name.equalsIgnoreCase(CHARSET)) {
                if (!isBlank(parameter.value)) {
                    charset = Charset.forName(parameter.value);
                }
                break;
            }
        }
        return new MediaType(mimeType, charset, parameters.isEmpty() ? null : parameters);
    }

    private static void appendParameter(StringBuilder buffer, Parameter parameter) {
        buffer.append(parameter.name);
        if (parameter.value == null) {
            return;
        }
        buffer.append('=');
        boolean quote = parameter.value.chars().anyMatch(ch -> SEPARATORS.indexOf(ch) >= 0);
        if (quote) {
            buffer.append('"');
        }
        for (char ch : parameter.value.toCharArray()) {
            if (ch == '"' || ch == '\\') {
                buffer.append('\\');
            }
            buffer.append(ch);
        }
        if (quote) {
            buffer.append('"');
        }
    }

    private static final class Parameter {
        private final String name;
        private final String value;

        private Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * Reads header elements like HttpClient 4's {@code BasicHeaderValueParser}: a name is read up to {@code =},
     * {@code ;} or {@code ,}, a value up to {@code ;} or {@code ,} unless quoted, and whitespace inside them collapses
     * to one space.
     */
    private static final class Parser {
        private final String text;
        private int position;

        private Parser(String text) {
            this.text = text;
        }

        private boolean atEnd() {
            return position >= text.length();
        }

        private char previous() {
            return text.charAt(position - 1);
        }

        private List<Parameter> parameters() {
            skipWhitespace();
            List<Parameter> parameters = new ArrayList<>();
            while (!atEnd()) {
                parameters.add(nameValuePair());
                if (previous() == ',') {
                    break;
                }
            }
            return parameters;
        }

        private Parameter nameValuePair() {
            String name = token("=;,", false);
            if (atEnd()) {
                return new Parameter(name, null);
            }
            char delimiter = text.charAt(position++);
            if (delimiter != '=') {
                return new Parameter(name, null);
            }
            String value = token(";,", true);
            if (!atEnd()) {
                position++;
            }
            return new Parameter(name, value);
        }

        private String token(String delimiters, boolean allowQuotes) {
            StringBuilder token = new StringBuilder();
            boolean whitespace = false;
            while (!atEnd()) {
                char current = text.charAt(position);
                if (delimiters.indexOf(current) >= 0) {
                    break;
                } else if (isWhitespace(current)) {
                    skipWhitespace();
                    whitespace = true;
                } else {
                    if (whitespace && token.length() > 0) {
                        token.append(' ');
                    }
                    if (allowQuotes && current == '"') {
                        copyQuoted(token);
                    } else {
                        copyUnquoted(token, delimiters, allowQuotes);
                    }
                    whitespace = false;
                }
            }
            return token.toString();
        }

        private void copyUnquoted(StringBuilder token, String delimiters, boolean stopAtQuote) {
            while (!atEnd()) {
                char current = text.charAt(position);
                if (delimiters.indexOf(current) >= 0 || isWhitespace(current) || (stopAtQuote && current == '"')) {
                    break;
                }
                token.append(current);
                position++;
            }
        }

        private void copyQuoted(StringBuilder token) {
            position++; // the opening quote
            boolean escaped = false;
            while (!atEnd()) {
                char current = text.charAt(position++);
                if (escaped) {
                    if (current != '"' && current != '\\') {
                        token.append('\\');
                    }
                    token.append(current);
                    escaped = false;
                } else if (current == '"') {
                    return;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current != '\r' && current != '\n') {
                    token.append(current);
                }
            }
        }

        private void skipWhitespace() {
            while (!atEnd() && isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        private static boolean isWhitespace(char ch) {
            return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
        }
    }
}

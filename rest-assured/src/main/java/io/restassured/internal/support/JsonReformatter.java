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

package io.restassured.internal.support;

import java.util.regex.Pattern;

/**
 * Pretty prints JSON for logging, token by token, the way Groovy's {@code JsonOutput.prettyPrint} followed by an
 * unescaping of unicode escapes did before REST Assured 7.0.0. The JSON isn't validated, so a truncated body is
 * formatted as far as it goes:
 * <ul>
 * <li>Objects and arrays are indented by four spaces, with each member on a line of its own, also when they're empty.</li>
 * <li>Numbers, {@code true}, {@code false} and {@code null} are written as they are.</li>
 * <li>Strings are unescaped and then escaped again, with {@code \"}, {@code \\}, {@code \b}, {@code \f}, {@code \n},
 * {@code \r} and {@code \t} only. Other characters are written as they are.</li>
 * <li>Formatting stops at a string that has no end or an invalid escape.</li>
 * </ul>
 * A token that isn't JSON, such as {@code undefined}, makes it fail with an {@link IllegalArgumentException}.
 */
class JsonReformatter {
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
    private static final Pattern HEX = Pattern.compile("[0-9a-fA-F]{4}");
    private static final int INDENT = 4;

    private final String json;
    private final StringBuilder output = new StringBuilder();
    private int position;
    private int indent;

    private JsonReformatter(String json) {
        this.json = json;
    }

    static String prettify(String json) {
        return new JsonReformatter(json).reformat();
    }

    private String reformat() {
        while (skipWhitespace()) {
            char c = json.charAt(position);
            switch (c) {
                case '{':
                case '[':
                    position++;
                    indent += INDENT;
                    output.append(c).append('\n').append(indentation());
                    break;
                case '}':
                case ']':
                    position++;
                    indent -= INDENT;
                    output.append('\n');
                    if (indent > 0) {
                        output.append(indentation());
                    }
                    output.append(c);
                    break;
                case ',':
                    position++;
                    output.append(",\n").append(indentation());
                    break;
                case ':':
                    position++;
                    output.append(": ");
                    break;
                case 't':
                    constant("true");
                    break;
                case 'f':
                    constant("false");
                    break;
                case 'n':
                    constant("null");
                    break;
                case '"':
                    if (!string()) {
                        return output.toString();
                    }
                    break;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        number();
                    } else {
                        throw new IllegalArgumentException("Can't format '" + c + "' at position " + position + " as JSON.");
                    }
            }
        }
        return output.toString();
    }

    private boolean skipWhitespace() {
        while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
            position++;
        }
        return position < json.length();
    }

    private String indentation() {
        if (indent < 0) {
            throw new IllegalArgumentException("Can't format JSON with more closing than opening braces or brackets.");
        }
        return " ".repeat(indent);
    }

    private void constant(String constant) {
        if (!json.startsWith(constant, position)) {
            throw new IllegalArgumentException("Can't format JSON at position " + position + ", which was expected to be " + constant + ".");
        }
        position += constant.length();
        output.append(constant);
    }

    private void number() {
        int start = position;
        while (position < json.length() && "0123456789.+-eE".indexOf(json.charAt(position)) >= 0) {
            position++;
        }
        String number = json.substring(start, position);
        if (!NUMBER.matcher(number).matches()) {
            throw new IllegalArgumentException("Can't format " + number + " at position " + start + " as a JSON number.");
        }
        output.append(number);
    }

    /**
     * Writes the string that starts at the current position.
     *
     * @return {@code false} if the string has no end or an invalid escape, which ends the formatting
     */
    private boolean string() {
        StringBuilder text = new StringBuilder();
        int i = position + 1;
        while (i < json.length()) {
            char c = json.charAt(i++);
            if (c == '"') {
                position = i;
                writeString(text);
                return true;
            } else if (c != '\\') {
                text.append(c);
            } else if (i == json.length()) {
                return false;
            } else {
                char escaped = json.charAt(i++);
                int simple = "\"\\/bfnrt".indexOf(escaped);
                if (simple >= 0) {
                    text.append("\"\\/\b\f\n\r\t".charAt(simple));
                } else if (escaped == 'u' && i + 4 <= json.length() && isHex(json.substring(i, i + 4))) {
                    text.append((char) Integer.parseInt(json.substring(i, i + 4), 16));
                    i += 4;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean isHex(String digits) {
        return HEX.matcher(digits).matches();
    }

    private void writeString(CharSequence text) {
        output.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    output.append("\\\"");
                    break;
                case '\\':
                    output.append("\\\\");
                    break;
                case '\b':
                    output.append("\\b");
                    break;
                case '\f':
                    output.append("\\f");
                    break;
                case '\n':
                    output.append("\\n");
                    break;
                case '\r':
                    output.append("\\r");
                    break;
                case '\t':
                    output.append("\\t");
                    break;
                default:
                    output.append(c);
            }
        }
        output.append('"');
    }
}

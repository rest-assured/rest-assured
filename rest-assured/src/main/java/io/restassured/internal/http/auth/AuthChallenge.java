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

package io.restassured.internal.http.auth;

import io.restassured.common.spi.http.HttpHeader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A challenge of a {@code WWW-Authenticate} header: its authentication scheme and its parameters.
 *
 * @param scheme     the name of the authentication scheme, in lower case
 * @param parameters the parameters of the challenge, by their names in lower case
 */
public record AuthChallenge(String scheme, Map<String, String> parameters) {

    /**
     * @param headers the headers of a response
     * @return the challenges of its {@code WWW-Authenticate} headers, by their scheme names in lower case. A scheme
     * that is offered more than once is taken from the last header that offers it.
     */
    public static Map<String, AuthChallenge> of(List<HttpHeader> headers) {
        Map<String, AuthChallenge> challenges = new LinkedHashMap<>();
        for (HttpHeader header : headers) {
            if (!"WWW-Authenticate".equalsIgnoreCase(header.name()) || header.value() == null) {
                continue;
            }
            String value = header.value().trim();
            int end = 0;
            while (end < value.length() && !Character.isWhitespace(value.charAt(end))) {
                end++;
            }
            if (end == 0) {
                continue;
            }
            String scheme = value.substring(0, end).toLowerCase(Locale.ROOT);
            challenges.put(scheme, new AuthChallenge(scheme, parameters(value.substring(end))));
        }
        return challenges;
    }

    /**
     * @return the parameter, or {@code null} if the challenge doesn't have it
     */
    public String parameter(String name) {
        return parameters.get(name);
    }

    // The comma separated name=value pairs of a challenge, whose values may be quoted strings
    private static Map<String, String> parameters(String value) {
        Map<String, String> parameters = new LinkedHashMap<>();
        int at = 0;
        while (at < value.length()) {
            while (at < value.length() && (Character.isWhitespace(value.charAt(at)) || value.charAt(at) == ',')) {
                at++;
            }
            int nameStart = at;
            while (at < value.length() && value.charAt(at) != '=' && value.charAt(at) != ',') {
                at++;
            }
            String name = value.substring(nameStart, at).trim().toLowerCase(Locale.ROOT);
            String parameterValue = null;
            if (at < value.length() && value.charAt(at) == '=') {
                at++;
                while (at < value.length() && Character.isWhitespace(value.charAt(at))) {
                    at++;
                }
                if (at < value.length() && value.charAt(at) == '"') {
                    at++;
                    StringBuilder quoted = new StringBuilder();
                    while (at < value.length() && value.charAt(at) != '"') {
                        if (value.charAt(at) == '\\' && at + 1 < value.length()) {
                            at++;
                        }
                        quoted.append(value.charAt(at++));
                    }
                    at++;
                    parameterValue = quoted.toString();
                } else {
                    int valueStart = at;
                    while (at < value.length() && value.charAt(at) != ',') {
                        at++;
                    }
                    parameterValue = value.substring(valueStart, at).trim();
                }
            }
            if (!name.isEmpty()) {
                parameters.put(name, parameterValue);
            }
        }
        return parameters;
    }
}

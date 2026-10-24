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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads {@code Set-Cookie} headers as RFC 6265 does, which is what
 * {@link io.restassured.filter.cookie.CookieFilter} has always used: the quotes of a quoted value are removed,
 * {@code Max-Age} wins over {@code Expires}, {@code Expires} is read in the three standard formats, a cookie without
 * a {@code Path} attribute belongs to the directory of the request's path, and the domain isn't checked against the
 * host of the request.
 */
public final class Rfc6265CookieSpec implements CookieSpec {
    @Override
    public StoredCookie parse(String header, CookieOrigin origin) {
        SetCookieTokenizer tokenizer = new SetCookieTokenizer(header);
        SetCookieTokenizer.Pair nameAndValue = tokenizer.pair(true);
        // A cookie needs an equals sign
        StoredCookie cookie = nameAndValue.value() == null ? null : CookieSpec.cookieOf(nameAndValue.name(), nameAndValue.value(), origin);
        if (cookie == null) {
            return null;
        }
        // The last of two attributes with the same name wins
        Map<String, String> attributes = new LinkedHashMap<>();
        while (!tokenizer.atEnd()) {
            SetCookieTokenizer.Pair attribute = tokenizer.pair(false);
            attributes.put(attribute.name().toLowerCase(Locale.ROOT), attribute.value());
        }
        // Max-Age wins over Expires
        if (attributes.containsKey("max-age")) {
            attributes.remove("expires");
        }
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            if (!apply(cookie, attribute.getKey(), attribute.getValue())) {
                return null;
            }
        }
        return cookie;
    }

    // Returns false if the attribute makes the cookie invalid
    private static boolean apply(StoredCookie cookie, String attribute, String value) {
        switch (attribute) {
            case "httponly":
                cookie.setHttpOnly(true);
                return true;
            case "max-age":
                int maxAge;
                try {
                    maxAge = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (maxAge < 0) {
                    return false;
                }
                cookie.setExpiryDate(new Date(System.currentTimeMillis() + maxAge * 1000L));
                return true;
            default:
                return CookieSpec.apply(cookie, attribute, value, CookieDates.STANDARD_PATTERNS);
        }
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the {@code Set-Cookie} headers that {@code httpClientConfig().keepResponseCookies(true)} keeps, as the
 * original Netscape cookie draft describes them, which is what REST Assured has always used for them: the quotes of a
 * value are part of the value, {@code Expires} is read with the patterns of
 * {@code httpClientConfig().cookieDatePatterns(..)}, a {@code Max-Age} attribute is ignored, and a cookie whose
 * {@code Domain} isn't the host of the response, or a domain of it, is dropped.
 */
public final class KeptCookieSpec implements CookieSpec {
    private final List<String> datePatterns;

    /**
     * @param datePatterns the patterns to read an {@code Expires} attribute with
     */
    public KeptCookieSpec(List<String> datePatterns) {
        this.datePatterns = datePatterns.isEmpty() ? CookieDates.STANDARD_PATTERNS : List.copyOf(datePatterns);
    }

    @Override
    public StoredCookie parse(String header, CookieOrigin origin) {
        SetCookieTokenizer tokenizer = new SetCookieTokenizer(header);
        SetCookieTokenizer.Pair nameAndValue = tokenizer.pair(false);
        StoredCookie cookie = CookieSpec.cookieOf(nameAndValue.name(), nameAndValue.value(), origin);
        if (cookie == null) {
            return null;
        }
        List<SetCookieTokenizer.Pair> attributes = new ArrayList<>();
        while (!tokenizer.atEnd()) {
            attributes.add(tokenizer.pair(false));
        }
        // The attributes are applied in reverse order, so that the first of two with the same name wins
        for (int i = attributes.size() - 1; i >= 0; i--) {
            String value = attributes.get(i).value();
            if (!apply(cookie, attributes.get(i).name().toLowerCase(Locale.ROOT), value == null ? "" : value)) {
                return null;
            }
        }
        // The cookie is only kept for the host it came from, or a host of its domain
        String domain = cookie.getDomain();
        if (!origin.host().equals(domain) && !CookieSpec.domainMatches(domain, origin.host())) {
            return null;
        }
        return cookie;
    }

    // Returns false if the attribute makes the cookie invalid. Max-Age, HttpOnly and the other attributes of later
    // cookie standards are ignored.
    private boolean apply(StoredCookie cookie, String attribute, String value) {
        if (attribute.equals("comment")) {
            cookie.setComment(value);
            return true;
        }
        return CookieSpec.apply(cookie, attribute, value, datePatterns);
    }
}

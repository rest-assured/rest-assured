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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

/**
 * The cookies that REST Assured has stored, ordered by name, then domain, then path, as REST Assured has always
 * ordered them. A cookie replaces a stored one with the same name, domain and path, and a cookie that has expired
 * isn't stored but removes the one it replaces.
 */
public final class CookieStore {
    private static final Comparator<StoredCookie> IDENTITY = Comparator
            .comparing(StoredCookie::getName)
            .thenComparing(cookie -> cookie.getDomain() == null ? "" : cookie.getDomain(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(cookie -> cookie.getPath() == null ? "/" : cookie.getPath());

    private final TreeSet<StoredCookie> cookies = new TreeSet<>(IDENTITY);

    /**
     * Stores the cookie, unless it has already expired, in which case it only removes the one it replaces.
     */
    public synchronized void add(StoredCookie cookie) {
        cookies.remove(cookie);
        if (!cookie.isExpired(new Date())) {
            cookies.add(cookie);
        }
    }

    /**
     * @return the stored cookies, in order
     */
    public synchronized List<StoredCookie> cookies() {
        return new ArrayList<>(cookies);
    }

    /**
     * Removes the cookies that have expired at the given time.
     *
     * @return whether a cookie was removed
     */
    public synchronized boolean clearExpired(Date now) {
        return cookies.removeIf(cookie -> cookie.isExpired(now));
    }

    /**
     * Removes every cookie.
     */
    public synchronized void clear() {
        cookies.clear();
    }

    /**
     * @return the stored cookies that the origin gets, in order, with the expired ones removed from the store
     */
    public List<StoredCookie> match(CookieSpec spec, CookieOrigin origin) {
        Date now = new Date();
        List<StoredCookie> matched = new ArrayList<>();
        boolean expired = false;
        for (StoredCookie cookie : cookies()) {
            if (cookie.isExpired(now)) {
                expired = true;
            } else if (spec.matches(cookie, origin)) {
                matched.add(cookie);
            }
        }
        if (expired) {
            clearExpired(now);
        }
        return matched;
    }
}

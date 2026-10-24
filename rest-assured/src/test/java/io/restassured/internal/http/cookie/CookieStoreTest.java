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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CookieStoreTest {
    private final CookieStore store = new CookieStore();

    private static StoredCookie cookie(String name, String value, String domain, String path) {
        StoredCookie cookie = new StoredCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        return cookie;
    }

    @Test
    void the_cookies_are_ordered_by_name_then_domain_then_path() {
        store.add(cookie("b", "1", "example.com", "/"));
        store.add(cookie("a", "1", "b.example.com", "/"));
        store.add(cookie("a", "2", "a.example.com", "/z"));
        store.add(cookie("a", "3", "a.example.com", "/a"));

        assertThat(store.cookies()).extracting(StoredCookie::getValue).containsExactly("3", "2", "1", "1");
    }

    @Test
    void a_cookie_replaces_the_one_with_the_same_name_domain_and_path() {
        store.add(cookie("a", "old", "example.com", "/"));
        store.add(cookie("a", "new", "example.com", "/"));
        store.add(cookie("a", "other path", "example.com", "/sub"));

        assertThat(store.cookies()).extracting(StoredCookie::getValue).containsExactly("new", "other path");
    }

    @Test
    void a_cookie_that_has_expired_only_removes_the_one_it_replaces() {
        store.add(cookie("a", "1", "example.com", "/"));
        StoredCookie expired = cookie("a", "2", "example.com", "/");
        expired.setExpiryDate(new Date(System.currentTimeMillis() - 1000));

        store.add(expired);

        assertThat(store.cookies()).isEmpty();
    }

    @Test
    void clear_expired_removes_the_cookies_that_have_expired() {
        StoredCookie soon = cookie("soon", "1", "example.com", "/");
        StoredCookie later = cookie("later", "1", "example.com", "/");
        store.add(soon);
        store.add(later);
        soon.setExpiryDate(new Date(1000));
        later.setExpiryDate(new Date(10000));

        assertThat(store.clearExpired(new Date(5000))).isTrue();
        assertThat(store.clearExpired(new Date(5000))).isFalse();

        assertThat(store.cookies()).extracting(StoredCookie::getName).containsExactly("later");
    }

    @Test
    void clear_removes_every_cookie() {
        store.add(cookie("a", "1", "example.com", "/"));

        store.clear();

        assertThat(store.cookies()).isEmpty();
    }

    @Test
    void match_gives_the_cookies_of_the_origin_in_order_and_evicts_the_expired_ones() {
        StoredCookie expired = cookie("expired", "1", "www.example.com", "/");
        store.add(cookie("b", "1", "www.example.com", "/"));
        store.add(cookie("a", "1", "www.example.com", "/dir"));
        store.add(cookie("c", "1", "www.example.com", "/other"));
        store.add(expired);
        // The store doesn't take a cookie that has already expired, so it expires once it's stored
        expired.setExpiryDate(new Date(System.currentTimeMillis() - 1000));

        List<StoredCookie> matched = store.match(new Rfc6265CookieSpec(), CookieOrigin.of(URI.create("http://www.example.com/dir/page")));

        assertThat(matched).extracting(StoredCookie::getName).containsExactly("a", "b");
        assertThat(store.cookies()).extracting(StoredCookie::getName).containsExactly("a", "b", "c");
    }
}

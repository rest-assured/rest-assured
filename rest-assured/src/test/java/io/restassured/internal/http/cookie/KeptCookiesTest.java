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

import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeptCookiesTest {
    private final CookieStore store = new CookieStore();
    private final KeptCookies cookies = new KeptCookies(store, new KeptCookieSpec(List.of()));

    private static HttpResponse response(String... setCookies) {
        List<HttpHeader> headers = new java.util.ArrayList<>();
        headers.add(new HttpHeader("Content-Type", "text/plain"));
        for (String setCookie : setCookies) {
            headers.add(new HttpHeader("Set-Cookie", setCookie));
        }
        return new HttpResponse("HTTP/1.1", 200, "OK", headers, 0, null, null);
    }

    @Test
    void no_cookies_means_no_header() {
        assertThat(cookies.cookieHeader(URI.create("http://www.example.com/"))).isNull();
    }

    @Test
    void the_cookies_of_a_response_are_sent_in_the_next_request() {
        cookies.store(response("a=1", "b=2"), URI.create("http://www.example.com/dir/page"));

        assertThat(cookies.cookieHeader(URI.create("http://www.example.com/dir/other"))).isEqualTo("a=1; b=2");
    }

    @Test
    void a_cookie_is_only_sent_where_it_belongs() {
        cookies.store(response("a=1; Path=/dir", "b=2; Path=/other"), URI.create("http://www.example.com/"));

        assertThat(cookies.cookieHeader(URI.create("http://www.example.com/dir"))).isEqualTo("a=1");
        assertThat(cookies.cookieHeader(URI.create("http://other.example.com/dir"))).isNull();
    }

    @Test
    void a_cookie_without_a_value_is_sent_as_its_name() {
        cookies.store(response("a"), URI.create("http://www.example.com/"));

        assertThat(cookies.cookieHeader(URI.create("http://www.example.com/"))).isEqualTo("a");
    }

    @Test
    void a_cookie_the_spec_rejects_isnt_kept() {
        cookies.store(response("a=1; Domain=.other.com", "b=2"), URI.create("http://www.example.com/"));

        assertThat(store.cookies()).extracting(StoredCookie::getName).containsExactly("b");
    }

    @Test
    void a_later_response_replaces_the_cookie_of_an_earlier_one() {
        cookies.store(response("a=1"), URI.create("http://www.example.com/"));
        cookies.store(response("a=2"), URI.create("http://www.example.com/"));

        assertThat(cookies.cookieHeader(URI.create("http://www.example.com/"))).isEqualTo("a=2");
    }
}

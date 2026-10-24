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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The RFC 6265 rules that {@link io.restassured.filter.cookie.CookieFilter} has always read its cookies with.
 */
class Rfc6265CookieSpecTest {
    private static final CookieOrigin ORIGIN = CookieOrigin.of(URI.create("http://www.example.com/dir/page"));

    private final Rfc6265CookieSpec spec = new Rfc6265CookieSpec();

    @Test
    void a_name_and_value_get_the_host_and_the_directory_of_the_request() {
        StoredCookie cookie = spec.parse("name=value", ORIGIN);

        assertThat(cookie.getName()).isEqualTo("name");
        assertThat(cookie.getValue()).isEqualTo("value");
        assertThat(cookie.getDomain()).isEqualTo("www.example.com");
        assertThat(cookie.hasDomainAttribute()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/dir");
        assertThat(cookie.getExpiryDate()).isNull();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.isHttpOnly()).isFalse();
    }

    @Test
    void a_request_whose_host_is_unknown_keeps_and_sends_no_cookies() {
        CookieOrigin unknown = CookieOrigin.of(URI.create("http:/dir/page"));
        StoredCookie cookie = spec.parse("name=value", ORIGIN);

        assertThat(unknown.host()).isNull();
        assertThat(spec.parse("name=value", unknown)).isNull();
        assertThat(new KeptCookieSpec(java.util.List.of()).parse("name=value", unknown)).isNull();
        assertThat(spec.matches(cookie, unknown)).isFalse();
    }

    @Test
    void a_host_whose_name_isnt_a_domain_name_is_the_domain_of_its_cookies() {
        StoredCookie cookie = spec.parse("name=value", CookieOrigin.of(URI.create("http://My_Svc:8080/dir/page")));

        assertThat(cookie.getDomain()).isEqualTo("my_svc");
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://my_svc:8080/dir/other")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://other_svc:8080/dir/other")))).isFalse();
    }

    @Test
    void a_quoted_value_is_unquoted() {
        assertThat(spec.parse("name=\"value\"", ORIGIN).getValue()).isEqualTo("value");
        assertThat(spec.parse("name=\"a;b\"", ORIGIN).getValue()).isEqualTo("a;b");
    }

    @Test
    void a_header_without_a_name_or_an_equals_sign_is_no_cookie() {
        assertThat(spec.parse("", ORIGIN)).isNull();
        assertThat(spec.parse("justaname", ORIGIN)).isNull();
        assertThat(spec.parse("=value", ORIGIN)).isNull();
    }

    @Test
    void the_attributes_are_read() {
        StoredCookie cookie = spec.parse("name=value; Path=/other; Domain=.example.com; Secure; HttpOnly", ORIGIN);

        assertThat(cookie.getPath()).isEqualTo("/other");
        assertThat(cookie.getDomain()).isEqualTo("example.com");
        assertThat(cookie.hasDomainAttribute()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void an_empty_path_is_the_root() {
        assertThat(spec.parse("name=value; Path=", ORIGIN).getPath()).isEqualTo("/");
    }

    @Test
    void an_empty_domain_makes_the_cookie_invalid() {
        assertThat(spec.parse("name=value; Domain=", ORIGIN)).isNull();
    }

    @Test
    void a_domain_that_ends_with_a_dot_is_ignored() {
        StoredCookie cookie = spec.parse("name=value; Domain=example.com.", ORIGIN);

        assertThat(cookie.getDomain()).isEqualTo("www.example.com");
        assertThat(cookie.hasDomainAttribute()).isFalse();
    }

    @Test
    void max_age_wins_over_expires() {
        StoredCookie cookie = spec.parse("name=value; Expires=Wed, 21 Oct 2015 07:28:00 GMT; Max-Age=3600", ORIGIN);

        assertThat(cookie.getExpiryDate()).isAfter(new Date());
    }

    @Test
    void a_negative_or_unreadable_max_age_makes_the_cookie_invalid() {
        assertThat(spec.parse("name=value; Max-Age=-1", ORIGIN)).isNull();
        assertThat(spec.parse("name=value; Max-Age=soon", ORIGIN)).isNull();
        assertThat(spec.parse("name=value; Max-Age=0", ORIGIN)).isNotNull();
    }

    @Test
    void an_unreadable_expires_makes_the_cookie_invalid() {
        assertThat(spec.parse("name=value; Expires=NO DATE!", ORIGIN)).isNull();
        assertThat(spec.parse("name=value; Expires=Wed, 21 Oct 2015 07:28:00 GMT", ORIGIN).getExpiryDate()).isBefore(new Date());
    }

    @Test
    void a_comment_attribute_is_ignored() {
        assertThat(spec.parse("name=value; Comment=hello", ORIGIN).getComment()).isNull();
    }

    @Test
    void a_cookie_is_sent_to_its_own_path_and_below_it() {
        StoredCookie cookie = spec.parse("name=value; Path=/dir", ORIGIN);

        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/dir")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/dir/deeper")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/other")))).isFalse();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/directory")))).isFalse();
    }

    @Test
    void a_cookie_without_a_domain_attribute_is_sent_only_to_its_own_host() {
        StoredCookie cookie = spec.parse("name=value; Path=/", ORIGIN);

        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://other.example.com/")))).isFalse();
    }

    @Test
    void a_cookie_with_a_domain_attribute_is_sent_to_its_subdomains() {
        StoredCookie cookie = spec.parse("name=value; Path=/; Domain=example.com", ORIGIN);

        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://example.com/")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://other.example.com/")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://notexample.com/")))).isFalse();
    }

    @Test
    void a_secure_cookie_is_only_sent_over_https() {
        StoredCookie cookie = spec.parse("name=value; Path=/; Secure", ORIGIN);

        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("https://www.example.com/")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/")))).isFalse();
    }
}

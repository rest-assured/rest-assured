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

/**
 * The Netscape draft rules that {@code httpClientConfig().keepResponseCookies(true)} has always kept its cookies with.
 */
class KeptCookieSpecTest {
    private static final CookieOrigin ORIGIN = CookieOrigin.of(URI.create("http://www.example.com/dir/page"));

    private final KeptCookieSpec spec = new KeptCookieSpec(List.of());

    @Test
    void a_name_and_value_get_the_host_and_the_directory_of_the_request() {
        StoredCookie cookie = spec.parse("name=value", ORIGIN);

        assertThat(cookie.getName()).isEqualTo("name");
        assertThat(cookie.getValue()).isEqualTo("value");
        assertThat(cookie.getDomain()).isEqualTo("www.example.com");
        assertThat(cookie.getPath()).isEqualTo("/dir");
    }

    @Test
    void the_quotes_of_a_value_are_part_of_the_value() {
        assertThat(spec.parse("name=\"value\"", ORIGIN).getValue()).isEqualTo("\"value\"");
    }

    @Test
    void a_cookie_without_a_value_is_kept() {
        assertThat(spec.parse("name", ORIGIN).getValue()).isNull();
    }

    @Test
    void a_header_without_a_name_is_no_cookie() {
        assertThat(spec.parse("", ORIGIN)).isNull();
        assertThat(spec.parse("=value", ORIGIN)).isNull();
    }

    @Test
    void the_attributes_are_read() {
        StoredCookie cookie = spec.parse("name=value; Path=/other; Domain=.example.com; Secure; Comment=why", ORIGIN);

        assertThat(cookie.getPath()).isEqualTo("/other");
        assertThat(cookie.getDomain()).isEqualTo("example.com");
        assertThat(cookie.hasDomainAttribute()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getComment()).isEqualTo("why");
    }

    @Test
    void max_age_is_ignored() {
        assertThat(spec.parse("name=value; Max-Age=3600", ORIGIN).getExpiryDate()).isNull();
        assertThat(spec.parse("name=value; Max-Age=-1", ORIGIN)).isNotNull();
    }

    @Test
    void expires_is_read_with_the_standard_patterns() {
        assertThat(spec.parse("name=value; Expires=Wed, 21 Oct 2099 07:28:00 GMT", ORIGIN).getExpiryDate()).isAfter(new Date());
        assertThat(spec.parse("name=value; Expires=Wed Oct 21 07:28:00 2015", ORIGIN).getExpiryDate()).isBefore(new Date());
        assertThat(spec.parse("name=value; Expires=NO DATE!", ORIGIN)).isNull();
    }

    @Test
    void expires_is_read_with_the_configured_patterns_too() {
        KeptCookieSpec withPattern = new KeptCookieSpec(List.of("dd MMM yyyy HH:mm:ss"));

        assertThat(withPattern.parse("name=value; Expires=21 Oct 2099 07:28:00", ORIGIN).getExpiryDate()).isAfter(new Date());
        // The configured patterns replace the standard ones
        assertThat(withPattern.parse("name=value; Expires=Wed Oct 21 07:28:00 2099", ORIGIN)).isNull();
    }

    @Test
    void the_first_of_two_attributes_with_the_same_name_wins() {
        assertThat(spec.parse("name=value; Path=/first; Path=/second", ORIGIN).getPath()).isEqualTo("/first");
    }

    @Test
    void a_cookie_of_a_foreign_domain_is_dropped() {
        assertThat(spec.parse("name=value; Domain=.example.com", ORIGIN)).isNotNull();
        assertThat(spec.parse("name=value; Domain=www.example.com", ORIGIN)).isNotNull();
        assertThat(spec.parse("name=value; Domain=.other.com", ORIGIN)).isNull();
    }

    @Test
    void a_cookie_set_by_an_ip_address_is_only_kept_for_that_address() {
        CookieOrigin ip = CookieOrigin.of(URI.create("http://127.0.0.1/dir/page"));

        assertThat(spec.parse("name=value", ip)).isNotNull();
        assertThat(spec.parse("name=value; Domain=127.0.0.1", ip)).isNotNull();
        assertThat(spec.parse("name=value; Domain=.0.0.1", ip)).isNull();
    }

    @Test
    void a_cookie_is_sent_to_its_own_path_and_below_it() {
        StoredCookie cookie = spec.parse("name=value", ORIGIN);

        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/dir/deeper")))).isTrue();
        assertThat(spec.matches(cookie, CookieOrigin.of(URI.create("http://www.example.com/other")))).isFalse();
    }
}

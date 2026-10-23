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

package io.restassured.itest.java.wire;

import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

public class WireCookieITest extends WireTest {

    // Cookies set on the request

    @Test public void
    cookies_are_sent_in_one_cookie_header_separated_by_semicolon_and_space_in_the_order_they_were_added() {
        given().cookie("b", "2").cookie("a", "1").cookie("c", "3").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("b=2; a=1; c=3");
    }

    @Test public void
    a_cookie_with_several_values_is_sent_once_per_value() {
        given().cookie("a", "1", "2").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("a=1; a=2");
    }

    @Test public void
    a_cookie_added_twice_is_sent_twice() {
        given().cookie("a", "1").cookie("a", "2").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("a=1; a=2");
    }

    @Test public void
    a_cookie_without_value_is_sent_with_an_equals_sign() {
        given().cookie("a").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("a=");
    }

    @Test public void
    a_cookie_with_an_empty_value_is_sent_with_an_equals_sign() {
        given().cookie("a", "").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("a=");
    }

    @Test public void
    cookie_values_are_sent_as_is_without_quoting_or_escaping() {
        given().cookie("a", "x y").cookie("b", "\"q\"").cookie("c", "1;d=2").cookie("e", "f=g").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("a=x y; b=\"q\"; c=1;d=2; e=f=g");
    }

    @Test public void
    cookies_from_a_map_are_sent_in_map_order_with_a_null_value_sent_as_an_empty_value() {
        Map<String, Object> cookies = new LinkedHashMap<>();
        cookies.put("z", "1");
        cookies.put("y", 2);
        cookies.put("x", null);

        given().cookies(cookies).get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("z=1; y=2; x=");
    }

    @Test public void
    cookies_from_name_value_pairs_are_sent_in_order() {
        given().cookies("b", "2", "a", "1").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("b=2; a=1");
    }

    @Test public void
    the_attributes_of_a_cookie_object_are_not_sent() {
        Cookie cookie = new Cookie.Builder("a", "1").setDomain("example.com").setPath("/other").setSecured(true).setHttpOnly(true)
                .setMaxAge(10).setComment("comment").setVersion(1).setSameSite("Lax").build();

        given().cookie(cookie).get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("a=1");
    }

    @Test public void
    cookies_objects_are_sent_after_the_cookies_added_before() {
        given().cookie("first", "0").cookies(new Cookies(new Cookie.Builder("a", "1").build(), new Cookie.Builder("b", "2").build()))
                .get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("first=0; a=1; b=2");
    }

    @Test public void
    a_cookie_header_is_sent_as_is() {
        given().header("Cookie", "h=1; i=2").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("h=1; i=2");
    }

    @Test public void
    a_cookie_header_and_the_cookies_are_sent_in_one_cookie_header_with_the_header_first() {
        given().header("Cookie", "h=1").cookie("a", "1").get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("h=1; a=1");
    }

    @Test public void
    no_cookie_header_is_sent_without_cookies() {
        given().get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().hasHeader("Cookie")).isFalse();
    }

    // Set-Cookie on a response, without CookieFilter

    @Test public void
    response_cookies_are_not_sent_on_the_next_request_by_default() {
        server.respond("/set", setCookies("a=1", "b=2; Path=/"));

        given().get(server.url("/set")).then().cookie("a", "1").cookie("b", "2");
        given().get(server.url("/p")).then().statusCode(200);

        assertThat(server.lastRequest().hasHeader("Cookie")).isFalse();
    }

    // The tests below read each response body (asString()), which a reused HTTP client needs before the next request
    // (see WireConnectionITest)

    @Test public void
    response_cookies_are_not_sent_on_the_next_request_with_keep_response_cookies_without_reusing_the_http_client() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().keepResponseCookies(true));
        server.respond("/set", setCookies("a=1"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();

        assertThat(server.lastRequest().hasHeader("Cookie")).isFalse();
    }

    @Test public void
    response_cookies_are_not_sent_on_the_next_request_when_reusing_the_http_client_without_keep_response_cookies() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance());
        server.respond("/set", setCookies("a=1"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();

        assertThat(server.lastRequest().hasHeader("Cookie")).isFalse();
    }

    @Test public void
    response_cookies_are_sent_on_the_next_request_when_reusing_the_http_client_with_keep_response_cookies() {
        RestAssuredConfig config = reuseAndKeepResponseCookies();
        server.respond("/set", setCookies("a=1", "b=2; Path=/"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("a=1; b=2");
    }

    @Test public void
    kept_response_cookies_are_sent_in_a_second_cookie_header_after_the_request_cookies() {
        RestAssuredConfig config = reuseAndKeepResponseCookies();
        server.respond("/set", setCookies("a=1"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).cookie("x", "9").cookie("a", "explicit").get(server.url("/p")).asString();

        // Today's behavior: two Cookie headers, and a kept cookie is sent even when the request sets one with the same name
        assertThat(server.lastRequest().headers("Cookie")).containsExactly("x=9; a=explicit", "a=1");
    }

    @Test public void
    kept_response_cookies_are_not_sent_to_another_host() {
        RestAssuredConfig config = reuseAndKeepResponseCookies();
        server.respond("/set", setCookies("a=1"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("127.0.0.1", "/p")).asString();

        assertThat(server.lastRequest().hasHeader("Cookie")).isFalse();
    }

    @Test public void
    kept_response_cookies_follow_path_secure_and_domain_and_keep_quotes() {
        RestAssuredConfig config = reuseAndKeepResponseCookies();
        server.respond("/set", setCookies("plain=1", "secure=1; Secure", "other=1; Path=/other", "foreign=1; Domain=127.0.0.1", "quoted=\"x y\""));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();
        given().config(config).get(server.url("/other/x")).asString();

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(1).headers("Cookie")).containsExactly("plain=1; quoted=\"x y\"");
        assertThat(requests.get(2).headers("Cookie")).containsExactly("other=1; plain=1; quoted=\"x y\"");
    }

    @Test public void
    kept_response_cookies_parse_expires_with_the_default_cookie_date_patterns() {
        RestAssuredConfig config = reuseAndKeepResponseCookies();
        server.respond("/set", setCookies(
                "dashes=1; Expires=Wed, 21-Oct-2099 07:28:00 GMT",
                "spaces=1; Expires=Wed, 21 Oct 2099 07:28:00 GMT",
                "rfc850=1; Expires=Wednesday, 21-Oct-99 07:28:00 GMT",
                "asctime=1; Expires=Wed Oct 21 07:28:00 2099",
                "pastDashes=1; Expires=Wed, 21-Oct-2015 07:28:00 GMT",
                "pastSpaces=1; Expires=Wed, 21 Oct 2015 07:28:00 GMT"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();

        // An asctime date matches no pattern, which makes the cookie invalid. Today's behavior: the two-digit year 99 is
        // read as the year 99, so that cookie has already expired
        assertThat(server.lastRequest().headers("Cookie")).containsExactly("dashes=1; spaces=1");
    }

    @Test public void
    cookie_date_patterns_replace_the_default_patterns() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance().keepResponseCookies(true).cookieDatePatterns("dd MMM yyyy HH:mm:ss"));
        server.respond("/set", setCookies(
                "custom=1; Expires=21 Oct 2099 07:28:00",
                "pastCustom=1; Expires=21 Oct 2015 07:28:00",
                "spaces=1; Expires=Wed, 21 Oct 2099 07:28:00 GMT"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("custom=1");
    }

    @Test public void
    a_kept_response_cookie_with_max_age_zero_is_still_sent() {
        RestAssuredConfig config = reuseAndKeepResponseCookies();
        server.respond("/set", setCookies("gone=1; Max-Age=0"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();

        // Today's behavior: the default cookie spec ignores Max-Age on cookies without a Version attribute
        assertThat(server.lastRequest().headers("Cookie")).containsExactly("gone=1");
    }

    @Test public void
    the_browser_compatibility_cookie_policy_keeps_response_cookies_when_reusing_the_http_client() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().reuseHttpClientInstance().setParam(COOKIE_POLICY, BROWSER_COMPATIBILITY));
        server.respond("/set", setCookies("a=1"));

        given().config(config).get(server.url("/set")).asString();
        given().config(config).get(server.url("/p")).asString();

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("a=1");
    }

    // CookieFilter

    @Test public void
    cookie_filter_sends_the_cookies_of_a_response_on_the_next_request_to_the_same_host() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("a=1", "b=2"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).hasHeader("Cookie")).isFalse();
        assertThat(requests.get(1).headers("Cookie")).containsExactly("a=1; b=2");
    }

    @Test public void
    cookie_filter_doesnt_send_cookies_to_another_host() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("a=1", "b=2; Domain=localhost"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("127.0.0.1", "/p")).then().statusCode(200);

        assertThat(server.lastRequest().hasHeader("Cookie")).isFalse();
    }

    @Test public void
    cookie_filter_sends_a_cookie_without_path_only_below_the_directory_of_the_request_path() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/dir/set", setCookies("a=1"));

        given().filter(cookieFilter).get(server.url("/dir/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/dir/x")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/dir")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/directory")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(1).headers("Cookie")).containsExactly("a=1");
        assertThat(requests.get(2).headers("Cookie")).containsExactly("a=1");
        assertThat(requests.get(3).hasHeader("Cookie")).isFalse();
        assertThat(requests.get(4).hasHeader("Cookie")).isFalse();
    }

    @Test public void
    cookie_filter_sends_a_cookie_with_a_path_only_on_that_path() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/dir/set", setCookies("root=1; Path=/", "other=1; Path=/other"));

        given().filter(cookieFilter).get(server.url("/dir/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/dir/x")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/other/x")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(1).headers("Cookie")).containsExactly("root=1");
        assertThat(requests.get(2).headers("Cookie")).containsExactly("other=1; root=1");
    }

    @Test public void
    cookie_filter_doesnt_send_a_secure_cookie_over_http() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("secure=1; Secure", "plain=1"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("plain=1");
    }

    @Test public void
    cookie_filter_sends_a_cookie_with_the_domain_of_the_request_host() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("a=1; Domain=localhost"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("a=1");
    }

    @Test public void
    cookie_filter_stores_a_cookie_for_another_domain_but_doesnt_send_it() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("foreign=1; Domain=example.com"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        assertThat(server.lastRequest().hasHeader("Cookie")).isFalse();
        // Today's behavior: the domain isn't validated against the request host when the cookie is stored
        assertThat(cookieFilter.getCookies().get("foreign").getDomain()).isEqualTo("example.com");
    }

    @Test public void
    cookie_filter_doesnt_send_cookies_that_expired_with_max_age_zero_or_an_expires_in_the_past() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("maxAgeZero=1; Max-Age=0", "past=1; Expires=Wed, 21 Oct 2015 07:28:00 GMT", "alive=1"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("alive=1");
        assertThat(cookieFilter.getCookies().asList()).extracting(Cookie::getName).containsExactly("alive");
    }

    @Test public void
    cookie_filter_replaces_a_cookie_with_a_new_value_and_removes_it_on_max_age_zero() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("a=1", "b=1"));
        server.respond("/update", setCookies("a=2", "b=1; Max-Age=0"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/update")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(1).headers("Cookie")).containsExactly("a=1; b=1");
        assertThat(requests.get(2).headers("Cookie")).containsExactly("a=2");
    }

    @Test public void
    cookie_filter_parses_expires_in_several_formats_regardless_of_the_cookie_date_patterns() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies(
                "rfc1123=1; Expires=Wed, 21 Oct 2099 07:28:00 GMT",
                "dashes=1; Expires=Wed, 21-Oct-2099 07:28:00 GMT",
                "rfc850=1; Expires=Wednesday, 21-Oct-99 07:28:00 GMT",
                "asctime=1; Expires=Wed Oct 21 07:28:00 2099"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("asctime=1; dashes=1; rfc1123=1; rfc850=1");
        Date expected = Date.from(Instant.parse("2099-10-21T07:28:00Z"));
        assertThat(cookieFilter.getCookies().asList()).extracting(Cookie::getExpiryDate).containsOnly(expected);
    }

    @Test public void
    cookie_filter_sends_its_cookies_sorted_by_name() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("c=1", "a=1; Path=/", "b=1"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).cookie("z", "0").get(server.url("/p")).then().statusCode(200);

        // Today's behavior: the order of the store (by name), not the response order or longest path first; request cookies come first
        assertThat(server.lastRequest().headers("Cookie")).containsExactly("z=0; a=1; b=1; c=1");
    }

    @Test public void
    cookie_filter_sends_a_quoted_value_without_the_quotes() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("q=\"x y\""));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        // Today's behavior: the quotes are removed when the cookie is stored, so the value goes out unquoted with its space
        assertThat(server.lastRequest().headers("Cookie")).containsExactly("q=x y");
    }

    @Test public void
    cookie_filter_doesnt_send_a_stored_cookie_when_the_request_sets_one_with_the_same_name() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("a=1", "b=1"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).cookie("a", "explicit").get(server.url("/p")).then().statusCode(200);

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("a=explicit; b=1");
    }

    @Test public void
    cookie_filter_sends_only_the_first_stored_cookie_with_a_name_by_default() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/set", setCookies("a=root; Path=/", "a=x; Path=/x"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/x/y")).then().statusCode(200);

        // Today's behavior: the first in store order (by path), not the one with the longest path
        assertThat(server.lastRequest().headers("Cookie")).containsExactly("a=root");
    }

    @Test public void
    cookie_filter_allowing_multiple_cookies_with_the_same_name_sends_all_of_them() {
        CookieFilter cookieFilter = new CookieFilter(true);
        server.respond("/set", setCookies("a=root; Path=/", "a=x; Path=/x"));

        given().filter(cookieFilter).get(server.url("/set")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/x/y")).then().statusCode(200);

        assertThat(server.lastRequest().headers("Cookie")).containsExactly("a=root; a=x");
    }

    @Test public void
    cookie_filter_get_cookies_describes_the_stored_cookies() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/dir/set", setCookies("plain=1", "detailed=2; Path=/; Domain=localhost; Secure; HttpOnly; Max-Age=100"));

        long before = System.currentTimeMillis();
        given().filter(cookieFilter).get(server.url("/dir/set")).then().statusCode(200);

        Cookies cookies = cookieFilter.getCookies();
        assertThat(cookies.asList()).extracting(Cookie::getName).containsExactly("detailed", "plain");
        Cookie plain = cookies.get("plain");
        assertThat(plain.getValue()).isEqualTo("1");
        assertThat(plain.getDomain()).isEqualTo("localhost");
        assertThat(plain.getPath()).isEqualTo("/dir");
        assertThat(plain.isSecured()).isFalse();
        assertThat(plain.isHttpOnly()).isFalse();
        assertThat(plain.hasExpiryDate()).isFalse();
        Cookie detailed = cookies.get("detailed");
        assertThat(detailed.getDomain()).isEqualTo("localhost");
        assertThat(detailed.getPath()).isEqualTo("/");
        assertThat(detailed.isSecured()).isTrue();
        assertThat(detailed.isHttpOnly()).isTrue();
        assertThat(detailed.hasMaxAge()).isFalse();
        assertThat(detailed.getExpiryDate().getTime()).isCloseTo(before + 100_000, within(10_000L));
    }

    @Test public void
    cookie_filter_add_cookie_is_sent_to_its_domain_and_path() {
        CookieFilter cookieFilter = new CookieFilter()
                .addCookie(new Cookie.Builder("everywhere", "1").setDomain("localhost").build())
                .addCookie(new Cookie.Builder("onX", "1").setDomain("localhost").setPath("/x").build())
                .addCookie(new Cookie.Builder("otherHost", "1").setDomain("127.0.0.1").build());

        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/x/y")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("127.0.0.1", "/p")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).headers("Cookie")).containsExactly("everywhere=1");
        assertThat(requests.get(1).headers("Cookie")).containsExactly("everywhere=1; onX=1");
        assertThat(requests.get(2).headers("Cookie")).containsExactly("otherHost=1");
    }

    @Test public void
    cookie_filter_add_cookie_doesnt_send_secure_or_expired_cookies_over_http() {
        CookieFilter cookieFilter = new CookieFilter()
                .addCookie(new Cookie.Builder("secure", "1").setDomain("localhost").setSecured(true).build())
                .addCookie(new Cookie.Builder("expired", "1").setDomain("localhost").setExpiryDate(new Date(0)).build())
                .addCookie(new Cookie.Builder("maxAgeZero", "1").setDomain("localhost").setMaxAge(0).build())
                .addCookie(new Cookie.Builder("alive", "1").setDomain("localhost").setMaxAge(100).build());

        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        assertThat(server.onlyRequest().headers("Cookie")).containsExactly("alive=1");
        assertThat(cookieFilter.getCookies().asList()).extracting(Cookie::getName).containsExactly("alive", "secure");
    }

    @Test public void
    cookie_filter_add_cookie_requires_a_domain() {
        CookieFilter cookieFilter = new CookieFilter();

        assertThatThrownBy(() -> cookieFilter.addCookie(new Cookie.Builder("a", "1").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cookie 'a' needs a domain to be added to a CookieFilter");
    }

    // Cookies and redirects

    @Test public void
    request_cookies_are_resent_on_a_redirect_to_the_same_host_but_response_cookies_of_the_redirect_are_not() {
        server.respond("/from", Responder.redirect(302, "/to").withHeader("Set-Cookie", "fromRedirect=1"));

        Response response = given().cookie("c", "v").get(server.url("/from"));

        response.then().statusCode(200);
        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).headers("Cookie")).containsExactly("c=v");
        assertThat(requests.get(1).headers("Cookie")).containsExactly("c=v");
        assertThat(response.getCookies()).isEmpty();
    }

    @Test public void
    keep_response_cookies_sends_the_cookie_of_the_redirect_response_on_the_follow_up_in_a_second_cookie_header() {
        server.respond("/from", Responder.redirect(302, "/to").withHeader("Set-Cookie", "fromRedirect=1"));

        Response response = given().config(config().httpClient(httpClientConfig().keepResponseCookies(true))).cookie("c", "v").get(server.url("/from"));

        response.then().statusCode(200);
        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).headers("Cookie")).containsExactly("c=v");
        // Today's behavior: two Cookie headers on the follow-up
        assertThat(requests.get(1).headers("Cookie")).containsExactly("c=v", "fromRedirect=1");
        assertThat(response.getCookies()).isEmpty();
    }

    @Test public void
    the_browser_compatibility_cookie_policy_sends_the_cookie_of_the_redirect_response_on_the_follow_up() {
        server.respond("/from", Responder.redirect(302, "/to").withHeader("Set-Cookie", "fromRedirect=1"));

        given().config(config().httpClient(httpClientConfig().setParam(COOKIE_POLICY, BROWSER_COMPATIBILITY))).get(server.url("/from")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).hasHeader("Cookie")).isFalse();
        assertThat(requests.get(1).headers("Cookie")).containsExactly("fromRedirect=1");
    }

    @Test public void
    keep_response_cookies_doesnt_send_the_cookie_of_the_redirect_response_to_another_host() {
        server.respond("/from", Responder.redirect(302, server.url("127.0.0.1", "/to")).withHeader("Set-Cookie", "fromRedirect=1"));

        given().config(config().httpClient(httpClientConfig().keepResponseCookies(true))).cookie("c", "v").get(server.url("/from")).then().statusCode(200);

        RecordedRequest followUp = server.lastRequest();
        assertThat(followUp.host()).isEqualTo("127.0.0.1");
        assertThat(followUp.hasHeader("Cookie")).isFalse();
    }

    @Test public void
    the_cookie_of_a_redirect_response_is_returned_when_redirects_are_not_followed() {
        server.respond("/from", Responder.redirect(302, "/to").withHeader("Set-Cookie", "fromRedirect=1"));

        given().redirects().follow(false).get(server.url("/from")).then().statusCode(302).cookie("fromRedirect", "1");

        assertThat(server.onlyRequest().target()).isEqualTo("/from");
    }

    @Test public void
    cookie_filter_doesnt_store_the_cookie_of_a_followed_redirect_response() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/from", Responder.redirect(302, "/to").withHeader("Set-Cookie", "fromRedirect=1"));

        given().filter(cookieFilter).get(server.url("/from")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(1).hasHeader("Cookie")).isFalse();
        assertThat(requests.get(2).hasHeader("Cookie")).isFalse();
        assertThat(cookieFilter.getCookies().exist()).isFalse();
    }

    @Test public void
    cookie_filter_stores_the_cookie_of_the_final_response_with_the_path_of_the_redirect_target() {
        CookieFilter cookieFilter = new CookieFilter();
        server.respond("/from", Responder.redirect(302, "/deep/to"));
        server.respond("/deep/to", setCookies("final=1"));

        given().filter(cookieFilter).get(server.url("/from")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/deep/x")).then().statusCode(200);
        given().filter(cookieFilter).get(server.url("/p")).then().statusCode(200);

        assertThat(cookieFilter.getCookies().get("final").getPath()).isEqualTo("/deep");
        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(2).headers("Cookie")).containsExactly("final=1");
        assertThat(requests.get(3).hasHeader("Cookie")).isFalse();
    }

    @Test public void
    cookie_filter_cookies_are_not_resent_on_a_redirect_to_another_host() {
        CookieFilter cookieFilter = new CookieFilter().addCookie(new Cookie.Builder("stored", "1").setDomain("localhost").build());
        server.respond("/from", Responder.redirect(302, server.url("127.0.0.1", "/to")));

        given().filter(cookieFilter).get(server.url("/from")).then().statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).headers("Cookie")).containsExactly("stored=1");
        assertThat(requests.get(1).host()).isEqualTo("127.0.0.1");
        assertThat(requests.get(1).hasHeader("Cookie")).isFalse();
    }

    private static RestAssuredConfig reuseAndKeepResponseCookies() {
        return config().httpClient(httpClientConfig().reuseHttpClientInstance().keepResponseCookies(true));
    }

    private static Responder setCookies(String... setCookieHeaders) {
        Responder responder = Responder.ok();
        for (int i = setCookieHeaders.length - 1; i >= 0; i--) {
            responder = responder.withHeader("Set-Cookie", setCookieHeaders[i]);
        }
        return responder;
    }
}

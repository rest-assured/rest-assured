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

package io.restassured.internal.http.exchange;

import io.restassured.common.exception.TooManyRedirectsException;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.ProtocolException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedirectsTest {
    private static final RedirectSettings DEFAULTS = new RedirectSettings(true, 100, false, false, false, true);

    private static HttpResponse response(int statusCode, String location) {
        List<HttpHeader> headers = location == null ? List.of() : List.of(new HttpHeader("Location", location));
        return new HttpResponse("HTTP/1.1", statusCode, "Found", headers, 0, null, null);
    }

    private static HttpRequest request(String method, String uri, RequestBody body) {
        return new HttpRequest(method, URI.create(uri), List.of(new HttpHeader("Accept", "*/*")), body, RequestOptions.DEFAULT);
    }

    private static RedirectSettings settings(boolean allowCircular, boolean rejectRelative, boolean followAllMethods, int max) {
        return new RedirectSettings(true, max, allowCircular, rejectRelative, followAllMethods, true);
    }

    @ParameterizedTest(name = "{0} {1} with followAllMethods {2} is followed: {3}")
    @CsvSource({
            "GET, 301, false, true",
            "GET, 302, false, true",
            "GET, 303, false, true",
            "GET, 307, false, true",
            "GET, 308, false, true",
            "GET, 300, false, false",
            "GET, 304, false, false",
            "GET, 200, false, false",
            "HEAD, 301, false, true",
            "POST, 301, false, false",
            "POST, 302, false, false",
            "POST, 307, false, false",
            "POST, 303, false, true",
            "POST, 301, true, true",
            "POST, 302, true, true",
            "POST, 307, true, true",
            "POST, 308, true, true",
            "PUT, 303, false, true"})
    void which_responses_are_redirects(String method, int statusCode, boolean followAllMethods, boolean redirected) {
        RedirectSettings settings = settings(false, false, followAllMethods, 100);

        assertThat(Redirects.isRedirected(method, response(statusCode, "/to"), settings)).isEqualTo(redirected);
    }

    @Test
    void a_302_without_a_location_is_not_a_redirect_but_a_301_is() {
        assertThat(Redirects.isRedirected("GET", response(302, null), DEFAULTS)).isFalse();
        assertThat(Redirects.isRedirected("GET", response(301, null), DEFAULTS)).isTrue();
    }

    @ParameterizedTest(name = "{0} after a {1} is a {2}")
    @CsvSource({
            "GET, 301, GET",
            "GET, 307, GET",
            "HEAD, 301, HEAD",
            "HEAD, 307, HEAD",
            "POST, 301, GET",
            "POST, 302, GET",
            "POST, 303, GET",
            "POST, 307, POST",
            "POST, 308, POST",
            "PUT, 303, GET",
            "DELETE, 308, DELETE"})
    void the_method_of_the_follow_up(String method, int statusCode, String followUpMethod) {
        HttpRequest request = request(method, "http://example.com/from", RequestBody.bytes("body".getBytes(UTF_8), "text/plain"));

        HttpRequest followUp = Redirects.followUp(request, statusCode, URI.create("http://example.com/to"), request.headers());

        assertThat(followUp.method()).isEqualTo(followUpMethod);
        assertThat(followUp.uri()).isEqualTo(URI.create("http://example.com/to"));
        assertThat(followUp.headers()).isEqualTo(request.headers());
        assertThat(followUp.body() != null).isEqualTo(Redirects.resendsBody(request, statusCode));
    }

    @ParameterizedTest(name = "{0} without a body after a {1} is a {2}")
    @CsvSource({
            "POST, 307, POST",
            "DELETE, 307, DELETE",
            "DELETE, 308, DELETE",
            "DELETE, 302, GET",
            "GET, 307, GET"})
    void a_request_without_a_body_keeps_its_method_after_a_307_or_308_and_resends_no_body(String method, int statusCode, String followUpMethod) {
        HttpRequest request = request(method, "http://example.com/from", null);

        HttpRequest followUp = Redirects.followUp(request, statusCode, URI.create("http://example.com/to"), request.headers());

        assertThat(Redirects.resendsBody(request, statusCode)).isFalse();
        assertThat(followUp.method()).isEqualTo(followUpMethod);
        assertThat(followUp.body()).isNull();
    }

    @Test
    void the_follow_up_sends_the_headers_it_is_given_not_the_hops_own() {
        HttpRequest hop = request("GET", "http://example.com/from", null);
        List<HttpHeader> original = List.of(new HttpHeader("X-Original", "1"));

        assertThat(Redirects.followUp(hop, 302, URI.create("http://example.com/to"), original).headers()).isEqualTo(original);
    }

    @Test
    void an_absolute_location_is_where_the_response_leads() throws Exception {
        assertThat(location("http://example.com/from", "http://other.com/to")).isEqualTo(URI.create("http://other.com/to"));
    }

    @Test
    void a_relative_location_is_resolved_against_the_request() throws Exception {
        assertThat(location("http://example.com/dir/from", "/to")).isEqualTo(URI.create("http://example.com/to"));
        assertThat(location("http://example.com/dir/from", "to")).isEqualTo(URI.create("http://example.com/dir/to"));
        assertThat(location("http://example.com/dir/from", "../to")).isEqualTo(URI.create("http://example.com/to"));
        assertThat(location("http://example.com/dir/from", "//other.com/to")).isEqualTo(URI.create("http://other.com/to"));
    }

    @Test
    void a_location_that_is_only_a_query_replaces_the_query_and_keeps_the_path() throws Exception {
        assertThat(location("http://example.com/dir/from?old=1", "?new=2")).isEqualTo(URI.create("http://example.com/dir/from?new=2"));
    }

    @Test
    void an_empty_location_is_the_request_itself() throws Exception {
        assertThat(location("http://example.com/dir/from?old=1", "")).isEqualTo(URI.create("http://example.com/dir/from?old=1"));
        assertThat(location("http://example.com/dir/from#here", "")).isEqualTo(URI.create("http://example.com/dir/from"));
    }

    @Test
    void the_scheme_and_the_host_of_a_location_are_lower_cased_and_its_dot_segments_removed() throws Exception {
        assertThat(location("http://example.com/from", "HTTP://EXAMPLE.COM/a/./b/../c"))
                .isEqualTo(URI.create("http://example.com/a/c"));
    }

    @Test
    void a_raw_non_ascii_character_of_a_location_is_replaced() throws Exception {
        // Jetty writes a Location header as ISO-8859-1, so a raw character arrives as a single byte that isn't UTF-8
        assertThat(location("http://example.com/from", "/t\u00f6")).isEqualTo(URI.create("http://example.com/t%EF%BF%BD"));
        // Two bytes that are UTF-8 are read as the character they encode
        assertThat(location("http://example.com/from", "/t\u00c3\u00b6")).isEqualTo(URI.create("http://example.com/t%C3%B6"));
    }

    @Test
    void the_escaped_characters_of_a_location_are_kept_as_the_server_wrote_them() throws Exception {
        assertThat(location("http://example.com/from", "http://example.com/a%2Fb?q=a%2Bb"))
                .isEqualTo(URI.create("http://example.com/a%2Fb?q=a%2Bb"));
    }

    @Test
    void a_location_without_a_location_header_fails() {
        assertThatThrownBy(() -> Redirects.location(URI.create("http://example.com/from"), response(302, null), DEFAULTS, new ArrayList<>()))
                .isExactlyInstanceOf(ProtocolException.class)
                .hasMessage("Received redirect response HTTP/1.1 302 Found but no location header");
    }

    @Test
    void a_location_that_isnt_a_uri_fails() {
        assertThatThrownBy(() -> location("http://example.com/from", "/t o"))
                .isExactlyInstanceOf(ProtocolException.class)
                .hasMessage("Invalid redirect URI: /t o");
    }

    @Test
    void a_relative_location_fails_when_relative_redirects_are_rejected() {
        assertThatThrownBy(() -> Redirects.location(URI.create("http://example.com/from"), response(302, "/to"),
                settings(false, true, false, 100), new ArrayList<>()))
                .isExactlyInstanceOf(ProtocolException.class)
                .hasMessage("Relative redirect location '/to' not allowed");
    }

    @Test
    void a_location_the_request_has_already_been_to_fails_unless_circular_redirects_are_allowed() throws Exception {
        List<URI> visited = new ArrayList<>();
        Redirects.location(URI.create("http://example.com/a"), response(302, "/b"), DEFAULTS, visited);

        assertThat(visited).containsExactly(URI.create("http://example.com/b"));
        assertThatThrownBy(() -> Redirects.location(URI.create("http://example.com/b"), response(302, "/b"), DEFAULTS, visited))
                .isExactlyInstanceOf(ProtocolException.class)
                .hasMessage("Circular redirect to 'http://example.com/b'");

        Redirects.location(URI.create("http://example.com/b"), response(302, "/b"), settings(true, false, false, 100), visited);
    }

    @Test
    void the_max_is_checked_before_the_location_is_read() {
        assertThatThrownBy(() -> Redirects.checkCount(0, settings(false, false, false, 0)))
                .isExactlyInstanceOf(TooManyRedirectsException.class)
                .hasMessage("Maximum redirects (0) exceeded");
        assertThatThrownBy(() -> Redirects.checkCount(2, settings(false, false, false, 2)))
                .isExactlyInstanceOf(TooManyRedirectsException.class)
                .hasMessage("Maximum redirects (2) exceeded");
    }

    @Test
    void a_redirect_below_the_max_is_allowed() throws Exception {
        Redirects.checkCount(1, settings(false, false, false, 2));
    }

    @Test
    void too_many_redirects_is_a_protocol_exception() {
        assertThat(new TooManyRedirectsException("x")).isInstanceOf(ProtocolException.class);
    }

    @Test
    void the_fragment_of_a_location_isnt_sent() {
        assertThat(Redirects.withoutFragment(URI.create("http://example.com/to#here"))).isEqualTo(URI.create("http://example.com/to"));
        assertThat(Redirects.withoutFragment(URI.create("http://example.com/to?q=1#here"))).isEqualTo(URI.create("http://example.com/to?q=1"));
        assertThat(Redirects.withoutFragment(URI.create("http://example.com/to"))).isEqualTo(URI.create("http://example.com/to"));
    }

    private static URI location(String requestUri, String location) throws ProtocolException {
        return Redirects.location(URI.create(requestUri), response(302, location), DEFAULTS, new ArrayList<>());
    }
}

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

import io.restassured.authentication.OAuthSignature;
import io.restassured.common.exception.TooManyRedirectsException;
import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.internal.http.auth.ChallengeHandler;
import io.restassured.internal.http.auth.CoreOAuthSigner;
import io.restassured.internal.http.auth.Credentials;
import io.restassured.internal.http.cookie.CookieStore;
import io.restassured.internal.http.cookie.KeptCookieSpec;
import io.restassured.internal.http.cookie.KeptCookies;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeTest {
    private static final RedirectSettings DEFAULTS = new RedirectSettings(true, 100, false, false, false, true);
    private static final URI START = URI.create("http://example.com/start");

    private final FakeTransport transport = new FakeTransport();

    /**
     * A transport that records the requests it is asked to send and answers with what the test tells it to.
     */
    private static final class FakeTransport implements HttpClientInstance {
        private final List<HttpRequest> sent = new ArrayList<>();
        private final List<InputStream> bodies = new ArrayList<>();
        private Function<HttpRequest, HttpResponse> answer = request -> ok();

        @Override
        public HttpResponse send(HttpRequest request) {
            sent.add(request);
            HttpResponse response = answer.apply(request);
            bodies.add(response.body());
            return response;
        }

        @Override
        public void close() {
        }

        private List<String> targets() {
            List<String> targets = new ArrayList<>();
            for (HttpRequest request : sent) {
                targets.add(request.uri().toString());
            }
            return targets;
        }

        private List<String> headers(int hop, String name) {
            List<String> values = new ArrayList<>();
            for (HttpHeader header : sent.get(hop).headers()) {
                if (header.name().equalsIgnoreCase(name)) {
                    values.add(header.value());
                }
            }
            return values;
        }
    }

    private static HttpResponse ok() {
        return new HttpResponse("HTTP/1.1", 200, "OK", List.of(), 2, new ByteArrayInputStream("ok".getBytes(UTF_8)), null);
    }

    private static HttpResponse response(int statusCode, HttpHeader... headers) {
        return new HttpResponse("HTTP/1.1", statusCode, "Found", List.of(headers), 0, new ByteArrayInputStream(new byte[0]), null);
    }

    private static HttpRequest request(String method, URI uri, RequestBody body, HttpHeader... headers) {
        return new HttpRequest(method, uri, List.of(headers), body, RequestOptions.DEFAULT);
    }

    private Exchange exchange(RedirectSettings redirects, ChallengeHandler challenges, KeptCookies cookies, String userAgent) {
        return new Exchange(transport, redirects, challenges, null, cookies, userAgent);
    }

    @Test
    void a_request_without_a_redirect_is_sent_once() throws IOException {
        Exchange.Exchanged exchanged = exchange(DEFAULTS, null, null, null).send(request("GET", START, null));

        assertThat(transport.targets()).containsExactly(START.toString());
        assertThat(exchanged.response().statusCode()).isEqualTo(200);
        assertThat(exchanged.effectiveUri()).isEqualTo(START);
    }

    @Test
    void a_redirect_is_followed_and_the_last_uri_is_the_effective_one() throws IOException {
        transport.answer = request -> request.uri().getPath().equals("/start")
                ? response(302, new HttpHeader("Location", "/moved")) : ok();

        Exchange.Exchanged exchanged = exchange(DEFAULTS, null, null, null).send(request("GET", START, null));

        assertThat(transport.targets()).containsExactly("http://example.com/start", "http://example.com/moved");
        assertThat(exchanged.effectiveUri()).isEqualTo(URI.create("http://example.com/moved"));
    }

    @Test
    void the_follow_up_sends_the_headers_of_the_original_request() throws IOException {
        transport.answer = request -> request.uri().getPath().equals("/start")
                ? response(302, new HttpHeader("Location", "/moved")) : ok();

        exchange(DEFAULTS, null, null, null).send(request("GET", START, null, new HttpHeader("X-Original", "1")));

        assertThat(transport.headers(1, "X-Original")).containsExactly("1");
    }

    @Test
    void a_redirect_isnt_followed_when_redirects_are_off() throws IOException {
        transport.answer = request -> response(302, new HttpHeader("Location", "/moved"));
        RedirectSettings off = new RedirectSettings(false, 100, false, false, false, true);

        Exchange.Exchanged exchanged = exchange(off, null, null, null).send(request("GET", START, null));

        assertThat(transport.sent).hasSize(1);
        assertThat(exchanged.response().statusCode()).isEqualTo(302);
    }

    @Test
    void too_many_redirects_fails() {
        transport.answer = request -> response(302, new HttpHeader("Location", "/loop"));
        RedirectSettings two = new RedirectSettings(true, 2, true, false, false, true);

        assertThatThrownBy(() -> exchange(two, null, null, null).send(request("GET", START, null)))
                .isExactlyInstanceOf(TooManyRedirectsException.class).hasMessage("Maximum redirects (2) exceeded");
        assertThat(transport.sent).hasSize(3);
    }

    @Test
    void a_body_that_cant_be_sent_again_isnt_resent_so_the_redirect_isnt_followed() throws IOException {
        transport.answer = request -> response(307, new HttpHeader("Location", "/moved"));
        RequestBody stream = RequestBody.stream(new ByteArrayInputStream("body".getBytes(UTF_8)), 4, "text/plain");
        RedirectSettings allMethods = new RedirectSettings(true, 100, false, false, true, true);

        Exchange.Exchanged exchanged = exchange(allMethods, null, null, null).send(request("POST", START, stream));

        assertThat(transport.sent).hasSize(1);
        assertThat(exchanged.response().statusCode()).isEqualTo(307);
    }

    @Test
    void a_repeatable_body_is_resent_on_a_307() throws IOException {
        transport.answer = request -> request.uri().getPath().equals("/start")
                ? response(307, new HttpHeader("Location", "/moved")) : ok();
        RequestBody bytes = RequestBody.bytes("body".getBytes(UTF_8), "text/plain");
        RedirectSettings allMethods = new RedirectSettings(true, 100, false, false, true, true);

        exchange(allMethods, null, null, null).send(request("POST", START, bytes));

        assertThat(transport.sent).hasSize(2);
        assertThat(transport.sent.get(1).method()).isEqualTo("POST");
        assertThat(transport.sent.get(1).body()).isEqualTo(bytes);
    }

    @Test
    void the_default_user_agent_is_only_added_when_the_request_has_none() throws IOException {
        exchange(DEFAULTS, null, null, "core/1.0").send(request("GET", START, null));
        exchange(DEFAULTS, null, null, "core/1.0").send(request("GET", START, null, new HttpHeader("User-Agent", "mine")));

        assertThat(transport.headers(0, "User-Agent")).containsExactly("core/1.0");
        assertThat(transport.headers(1, "User-Agent")).containsExactly("mine");
    }

    @Test
    void a_challenge_is_answered_by_sending_the_hop_again() throws IOException {
        transport.answer = request -> request.headers().stream().anyMatch(header -> header.name().equals("Authorization"))
                ? ok() : response(401, new HttpHeader("WWW-Authenticate", "Basic realm=\"test\""));
        ChallengeHandler challenges = new ChallengeHandler(Credentials.basic("user", "password", START));

        Exchange.Exchanged exchanged = exchange(DEFAULTS, challenges, null, null).send(request("GET", START, null));

        assertThat(transport.sent).hasSize(2);
        assertThat(transport.headers(0, "Authorization")).isEmpty();
        assertThat(transport.headers(1, "Authorization")).containsExactly("Basic dXNlcjpwYXNzd29yZA==");
        assertThat(exchanged.response().statusCode()).isEqualTo(200);
    }

    @Test
    void a_challenge_that_isnt_accepted_gives_the_second_401() throws IOException {
        transport.answer = request -> response(401, new HttpHeader("WWW-Authenticate", "Basic realm=\"test\""));
        ChallengeHandler challenges = new ChallengeHandler(Credentials.basic("user", "password", START));

        Exchange.Exchanged exchanged = exchange(DEFAULTS, challenges, null, null).send(request("GET", START, null));

        assertThat(transport.sent).hasSize(2);
        assertThat(exchanged.response().statusCode()).isEqualTo(401);
    }

    @Test
    void the_credentials_are_sent_again_on_a_redirect_to_the_same_host() throws IOException {
        transport.answer = request -> {
            if (!request.headers().stream().anyMatch(header -> header.name().equals("Authorization"))) {
                return response(401, new HttpHeader("WWW-Authenticate", "Basic realm=\"test\""));
            }
            return request.uri().getPath().equals("/start") ? response(302, new HttpHeader("Location", "/moved")) : ok();
        };
        ChallengeHandler challenges = new ChallengeHandler(Credentials.basic("user", "password", START));

        exchange(DEFAULTS, challenges, null, null).send(request("GET", START, null));

        assertThat(transport.targets()).containsExactly("http://example.com/start", "http://example.com/start", "http://example.com/moved");
        assertThat(transport.headers(2, "Authorization")).containsExactly("Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    void sensitive_headers_are_dropped_on_a_redirect_to_another_host() throws IOException {
        transport.answer = request -> request.uri().getHost().equals("example.com")
                ? response(302, new HttpHeader("Location", "http://other.com/moved")) : ok();

        exchange(DEFAULTS, null, null, null).send(request("GET", START, null,
                new HttpHeader("Authorization", "Bearer token"), new HttpHeader("Cookie", "session=abc"), new HttpHeader("Accept", "*/*")));

        assertThat(transport.headers(1, "Authorization")).isEmpty();
        assertThat(transport.headers(1, "Cookie")).isEmpty();
        assertThat(transport.headers(1, "Accept")).containsExactly("*/*");
    }

    @Test
    void sensitive_headers_are_kept_on_a_cross_host_redirect_when_stripping_is_off() throws IOException {
        transport.answer = request -> request.uri().getHost().equals("example.com")
                ? response(302, new HttpHeader("Location", "http://other.com/moved")) : ok();
        RedirectSettings keep = new RedirectSettings(true, 100, false, false, false, false);

        exchange(keep, null, null, null).send(request("GET", START, null, new HttpHeader("Authorization", "Bearer token")));

        assertThat(transport.headers(1, "Authorization")).containsExactly("Bearer token");
    }

    @Test
    void a_redirect_to_the_same_host_on_another_port_is_cross_host() throws IOException {
        transport.answer = request -> request.uri().getPort() < 0
                ? response(302, new HttpHeader("Location", "http://example.com:8080/moved")) : ok();

        exchange(DEFAULTS, null, null, null).send(request("GET", START, null, new HttpHeader("Authorization", "Bearer token")));

        assertThat(transport.headers(1, "Authorization")).isEmpty();
    }

    @Test
    void a_challenge_isnt_answered_on_a_cross_host_hop() throws IOException {
        transport.answer = request -> request.uri().getHost().equals("example.com")
                ? response(302, new HttpHeader("Location", "http://other.com/moved"))
                : response(401, new HttpHeader("WWW-Authenticate", "Basic realm=\"test\""));
        ChallengeHandler challenges = new ChallengeHandler(Credentials.basic("user", "password", START));

        Exchange.Exchanged exchanged = exchange(DEFAULTS, challenges, null, null).send(request("GET", START, null));

        assertThat(transport.sent).hasSize(2);
        assertThat(exchanged.response().statusCode()).isEqualTo(401);
    }

    @Test
    void kept_cookies_are_sent_in_a_second_cookie_header_on_the_next_hop() throws IOException {
        transport.answer = request -> request.uri().getPath().equals("/start")
                ? response(302, new HttpHeader("Location", "/moved"), new HttpHeader("Set-Cookie", "kept=1")) : ok();
        KeptCookies cookies = new KeptCookies(new CookieStore(), new KeptCookieSpec(List.of()));

        exchange(DEFAULTS, null, cookies, null).send(request("GET", START, null, new HttpHeader("Cookie", "mine=2")));

        assertThat(transport.headers(0, "Cookie")).containsExactly("mine=2");
        assertThat(transport.headers(1, "Cookie")).containsExactly("mine=2", "kept=1");
    }

    @Test
    void the_response_of_a_hop_that_is_left_behind_is_closed_and_the_last_one_isnt() throws IOException {
        List<Boolean> closed = new ArrayList<>();
        transport.answer = request -> {
            int hop = closed.size();
            closed.add(false);
            InputStream body = new ByteArrayInputStream(new byte[0]) {
                @Override
                public void close() {
                    closed.set(hop, true);
                }
            };
            return request.uri().getPath().equals("/start")
                    ? new HttpResponse("HTTP/1.1", 302, "Found", List.of(new HttpHeader("Location", "/moved")), 0, body, null)
                    : new HttpResponse("HTTP/1.1", 200, "OK", List.of(), 0, body, null);
        };

        exchange(DEFAULTS, null, null, null).send(request("GET", START, null));

        assertThat(closed).containsExactly(true, false);
    }

    @Test
    void nothing_meant_for_a_host_whose_name_isnt_a_domain_name_is_sent_to_another_host() throws IOException {
        URI start = URI.create("http://a_b:8080/start");
        transport.answer = request -> request.uri().getRawAuthority().equals("a_b:8080")
                ? response(302, new HttpHeader("Location", "http://evil_c.attacker.example:8080/moved"), new HttpHeader("Set-Cookie", "kept=1"))
                : ok();
        KeptCookies cookies = new KeptCookies(new CookieStore(), new KeptCookieSpec(List.of()));
        Exchange exchange = new Exchange(transport, DEFAULTS, null, CoreOAuthSigner.oauth2("token", OAuthSignature.QUERY_STRING), cookies, null);

        exchange.send(request("GET", start, null, new HttpHeader("Authorization", "Bearer secret"), new HttpHeader("Cookie", "mine=1")));

        assertThat(transport.targets()).containsExactly("http://a_b:8080/start?access_token=token", "http://evil_c.attacker.example:8080/moved");
        assertThat(transport.headers(0, "Authorization")).containsExactly("Bearer secret");
        assertThat(transport.headers(1, "Authorization")).isEmpty();
        assertThat(transport.headers(1, "Cookie")).isEmpty();
    }

    @Test
    void a_challenge_of_a_host_whose_name_isnt_a_domain_name_is_answered() throws IOException {
        URI start = URI.create("http://my_svc:8080/start");
        transport.answer = request -> request.headers().stream().anyMatch(header -> header.name().equals("Authorization"))
                ? ok() : response(401, new HttpHeader("WWW-Authenticate", "Basic realm=\"test\""));
        ChallengeHandler challenges = new ChallengeHandler(Credentials.basic("user", "password", start));

        Exchange.Exchanged exchanged = exchange(DEFAULTS, challenges, null, null).send(request("GET", start, null));

        assertThat(transport.headers(1, "Authorization")).containsExactly("Basic dXNlcjpwYXNzd29yZA==");
        assertThat(exchanged.response().statusCode()).isEqualTo(200);
    }

    @Test
    void a_body_that_cant_be_sent_again_isnt_resent_to_answer_a_challenge() throws IOException {
        transport.answer = request -> response(401, new HttpHeader("WWW-Authenticate", "Basic realm=\"test\""));
        RequestBody stream = RequestBody.stream(new ByteArrayInputStream("body".getBytes(UTF_8)), 4, "text/plain");
        ChallengeHandler challenges = new ChallengeHandler(Credentials.basic("user", "password", START));

        Exchange.Exchanged exchanged = exchange(DEFAULTS, challenges, null, null).send(request("POST", START, stream));

        assertThat(transport.sent).hasSize(1);
        assertThat(exchanged.response().statusCode()).isEqualTo(401);
    }

    @Test
    void a_307_keeps_the_method_of_a_request_without_a_body() throws IOException {
        transport.answer = request -> request.uri().getPath().equals("/start")
                ? response(307, new HttpHeader("Location", "/moved")) : ok();
        RedirectSettings allMethods = new RedirectSettings(true, 100, false, false, true, true);

        exchange(allMethods, null, null, null).send(request("DELETE", START, null));

        assertThat(transport.sent.get(1).method()).isEqualTo("DELETE");
        assertThat(transport.sent.get(1).body()).isNull();
    }

    @Test
    void the_answer_is_sent_again_on_a_redirect_back_from_another_host_and_never_to_that_host() throws IOException {
        transport.answer = request -> {
            if (request.uri().getHost().equals("other.com")) {
                return response(302, new HttpHeader("Location", "http://example.com/back"));
            }
            if (request.headers().stream().noneMatch(header -> header.name().equals("Authorization"))) {
                return response(401, new HttpHeader("WWW-Authenticate", "Basic realm=\"test\""));
            }
            return request.uri().getPath().equals("/start") ? response(302, new HttpHeader("Location", "http://other.com/away")) : ok();
        };
        ChallengeHandler challenges = new ChallengeHandler(Credentials.basic("user", "password", START));

        Exchange.Exchanged exchanged = exchange(DEFAULTS, challenges, null, null).send(request("GET", START, null));

        assertThat(transport.targets()).containsExactly("http://example.com/start", "http://example.com/start",
                "http://other.com/away", "http://example.com/back");
        assertThat(transport.headers(2, "Authorization")).isEmpty();
        assertThat(transport.headers(3, "Authorization")).containsExactly("Basic dXNlcjpwYXNzd29yZA==");
        assertThat(exchanged.response().statusCode()).isEqualTo(200);
    }

    @Test
    void a_request_to_another_host_isnt_signed_when_sensitive_headers_are_stripped() throws IOException {
        transport.answer = request -> request.uri().getHost().equals("example.com")
                ? response(302, new HttpHeader("Location", "http://other.com/moved")) : ok();
        CoreOAuthSigner signer = CoreOAuthSigner.oauth("key", "secret", "token", "tokenSecret", OAuthSignature.QUERY_STRING, false);

        new Exchange(transport, DEFAULTS, null, signer, null, null).send(request("GET", START, null));

        assertThat(transport.sent.get(0).uri().getRawQuery()).contains("oauth_signature=");
        assertThat(transport.sent.get(1).uri()).isEqualTo(URI.create("http://other.com/moved"));
    }

    @Test
    void a_response_is_closed_even_if_closing_its_body_fails() throws IOException {
        boolean[] closed = {false};
        InputStream broken = new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() throws IOException {
                throw new IOException("broken");
            }
        };
        transport.answer = request -> request.uri().getPath().equals("/start")
                ? new HttpResponse("HTTP/1.1", 302, "Found", List.of(new HttpHeader("Location", "/moved")), 0, broken, () -> closed[0] = true)
                : ok();

        exchange(DEFAULTS, null, null, null).send(request("GET", START, null));

        assertThat(closed[0]).isTrue();
    }

    @Test
    void a_response_is_closed_when_keeping_its_cookies_fails() {
        boolean[] closed = {false};
        transport.answer = request -> new HttpResponse("HTTP/1.1", 200, "OK", List.of(new HttpHeader("Set-Cookie", "a=1; expires=x")), 0,
                new ByteArrayInputStream(new byte[0]), () -> closed[0] = true);
        // A date pattern that SimpleDateFormat refuses makes reading the cookie fail
        KeptCookies cookies = new KeptCookies(new CookieStore(), new KeptCookieSpec(List.of("'unterminated")));

        assertThatThrownBy(() -> exchange(DEFAULTS, null, cookies, null).send(request("GET", START, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(closed[0]).isTrue();
    }

    @Test
    void removes_the_empty_path_segments_but_the_last_like_http_client_4() {
        assertThat(Exchange.withoutEmptyPathSegments(URI.create("http://h:1/x//y?q=a%20b#f"))).hasToString("http://h:1/x/y?q=a%20b#f");
        assertThat(Exchange.withoutEmptyPathSegments(URI.create("http://h/x//"))).hasToString("http://h/x/");
        assertThat(Exchange.withoutEmptyPathSegments(URI.create("http://h//a%2Fb///c"))).hasToString("http://h/a%2Fb/c");
        assertThat(Exchange.withoutEmptyPathSegments(URI.create("http://h//"))).hasToString("http://h/");
        URI unchanged = URI.create("http://h/x/y/?q=a//b");
        assertThat(Exchange.withoutEmptyPathSegments(unchanged)).isSameAs(unchanged);
    }

    @Test
    void sends_each_hop_without_empty_path_segments() throws IOException {
        transport.answer = request -> request.uri().getPath().equals("/a/b") ? response(302, new HttpHeader("Location", "/c//d")) : ok();

        Exchange.Exchanged exchanged = exchange(DEFAULTS, null, null, null).send(request("GET", URI.create("http://example.com/a//b"), null));

        assertThat(transport.targets()).containsExactly("http://example.com/a/b", "http://example.com/c/d");
        assertThat(exchanged.effectiveUri()).hasToString("http://example.com/c/d");
    }
}

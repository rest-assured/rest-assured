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

package io.restassured.internal.http.auth;

import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChallengeHandlerTest {
    private static final URI URL = URI.create("http://example.com/path");

    private static HttpResponse unauthorized(String... challenges) {
        List<HttpHeader> headers = new ArrayList<>();
        for (String challenge : challenges) {
            headers.add(new HttpHeader("WWW-Authenticate", challenge));
        }
        return new HttpResponse("HTTP/1.1", 401, "Unauthorized", headers, 0, null, null);
    }

    private static HttpResponse ok() {
        return new HttpResponse("HTTP/1.1", 200, "OK", List.of(), 0, null, null);
    }

    private static ChallengeHandler basic() {
        return new ChallengeHandler(Credentials.basic("user", "password", URL));
    }

    @Test
    void nothing_is_sent_before_a_challenge_arrives() {
        assertThat(basic().authorization("GET", URL, null)).isNull();
    }

    @Test
    void a_basic_challenge_is_answered_with_the_credentials() {
        ChallengeHandler handler = basic();

        assertThat(handler.handle(unauthorized("Basic realm=\"test\""))).isTrue();
        assertThat(handler.authorization("GET", URL, null)).isEqualTo("Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    void the_credentials_are_read_as_us_ascii_so_other_characters_become_a_question_mark() {
        ChallengeHandler handler = new ChallengeHandler(Credentials.basic("üser", "pass", URL));
        handler.handle(unauthorized("Basic realm=\"test\""));

        assertThat(handler.authorization("GET", URL, null)).isEqualTo("Basic " + java.util.Base64.getEncoder()
                .encodeToString("?ser:pass".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }

    @Test
    void digest_is_preferred_when_the_server_offers_both() {
        ChallengeHandler handler = basic();

        assertThat(handler.handle(unauthorized("Basic realm=\"test\"", "Digest realm=\"test\", nonce=\"n\""))).isTrue();
        assertThat(handler.authorization("GET", URL, null)).startsWith("Digest ");
    }

    @Test
    void digest_credentials_dont_answer_a_basic_challenge() {
        ChallengeHandler handler = new ChallengeHandler(Credentials.digest("user", "password", URL));

        assertThat(handler.handle(unauthorized("Basic realm=\"test\""))).isFalse();
        assertThat(handler.authorization("GET", URL, null)).isNull();
    }

    @Test
    void a_401_without_a_challenge_isnt_answered() {
        assertThat(basic().handle(unauthorized())).isFalse();
    }

    @Test
    void a_second_401_of_the_same_scheme_isnt_answered_again() {
        ChallengeHandler handler = basic();
        handler.handle(unauthorized("Basic realm=\"test\""));

        assertThat(handler.handle(unauthorized("Basic realm=\"test\""))).isFalse();
        // The answer that wasn't accepted isn't sent again either
        assertThat(handler.handle(unauthorized("Basic realm=\"test\""))).isFalse();
    }

    @Test
    void a_401_of_another_scheme_is_answered_with_that_scheme() {
        ChallengeHandler handler = basic();
        handler.handle(unauthorized("Basic realm=\"test\""));

        assertThat(handler.handle(unauthorized("Digest realm=\"test\", nonce=\"n\""))).isTrue();
        assertThat(handler.authorization("GET", URL, null)).startsWith("Digest ");
    }

    @Test
    void the_credentials_are_sent_again_once_the_server_has_accepted_them() {
        ChallengeHandler handler = basic();
        handler.handle(unauthorized("Basic realm=\"test\""));

        assertThat(handler.handle(ok())).isFalse();
        assertThat(handler.authorization("GET", URI.create("http://example.com/other"), null)).isEqualTo("Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    void a_401_after_a_successful_answer_is_answered_again() {
        ChallengeHandler handler = basic();
        handler.handle(unauthorized("Basic realm=\"test\""));
        handler.handle(ok());

        assertThat(handler.handle(unauthorized("Basic realm=\"other\""))).isTrue();
        assertThat(handler.authorization("GET", URL, null)).isEqualTo("Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    void the_credentials_are_only_sent_to_the_host_they_are_for() {
        ChallengeHandler handler = basic();

        assertThat(handler.appliesTo(URI.create("http://example.com/other"))).isTrue();
        assertThat(handler.appliesTo(URI.create("http://EXAMPLE.com/other"))).isTrue();
        assertThat(handler.appliesTo(URI.create("http://example.com:8080/other"))).isTrue();
        assertThat(handler.appliesTo(URI.create("http://other.com/other"))).isFalse();
    }

    @Test
    void credentials_for_one_port_are_only_sent_to_that_port() {
        ChallengeHandler handler = new ChallengeHandler(Credentials.basic("user", "password", URI.create("http://example.com:8080/path")));

        assertThat(handler.appliesTo(URI.create("http://example.com:8080/other"))).isTrue();
        assertThat(handler.appliesTo(URI.create("http://example.com/other"))).isFalse();
    }

    @Test
    void credentials_for_the_default_port_are_sent_to_a_uri_without_one() {
        ChallengeHandler http = new ChallengeHandler(Credentials.basic("user", "password", URI.create("http://example.com:80/path")));
        ChallengeHandler https = new ChallengeHandler(Credentials.basic("user", "password", URI.create("https://example.com:443/path")));

        assertThat(http.appliesTo(URI.create("http://example.com/other"))).isTrue();
        assertThat(https.appliesTo(URI.create("https://example.com/other"))).isTrue();
    }

    @Test
    void a_hop_answers_at_most_two_challenges() {
        assertThat(ChallengeHandler.MAX_ANSWERS_PER_HOP).isEqualTo(2);
    }

    @Test
    void a_digest_challenge_that_cant_be_answered_gives_the_401() {
        assertThat(basic().handle(unauthorized("Digest qop=auth, nonce=\"n\""))).isFalse();
        assertThat(basic().handle(unauthorized("Digest realm=\"r\", nonce=\"n\", algorithm=SHA-256-sess"))).isFalse();
    }

    @Test
    void basic_is_answered_when_the_digest_challenge_cant_be() {
        ChallengeHandler handler = basic();

        assertThat(handler.handle(unauthorized("Digest realm=\"r\", qop=future, nonce=\"n\"", "Basic realm=\"test\""))).isTrue();
        assertThat(handler.authorization("GET", URL, null)).isEqualTo("Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    void a_stale_digest_nonce_is_answered_again_with_the_new_nonce() {
        ChallengeHandler handler = basic();
        handler.handle(unauthorized("Digest realm=\"r\", nonce=\"first\""));

        assertThat(handler.handle(unauthorized("Digest realm=\"r\", nonce=\"second\", stale=true"))).isTrue();
        assertThat(handler.authorization("GET", URL, null)).contains("nonce=\"second\"");
        assertThat(handler.handle(unauthorized("Digest realm=\"r\", nonce=\"third\""))).isFalse();
    }

    @Test
    void a_host_whose_name_isnt_a_domain_name_gets_its_credentials_and_no_other_host_does() {
        ChallengeHandler handler = new ChallengeHandler(Credentials.basic("user", "password", URI.create("http://my_svc:8080/path")));

        assertThat(handler.appliesTo(URI.create("http://MY_SVC:8080/other"))).isTrue();
        assertThat(handler.appliesTo(URI.create("http://my_svc:9090/other"))).isFalse();
        assertThat(handler.appliesTo(URI.create("http://other_svc:8080/other"))).isFalse();
        assertThat(handler.appliesTo(URI.create("http://my_svc.attacker_example:8080/other"))).isFalse();
    }
}

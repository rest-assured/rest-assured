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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthChallengeTest {

    private static Map<String, AuthChallenge> challenges(String... headers) {
        List<HttpHeader> httpHeaders = new java.util.ArrayList<>();
        httpHeaders.add(new HttpHeader("Content-Type", "text/plain"));
        for (String header : headers) {
            httpHeaders.add(new HttpHeader("WWW-Authenticate", header));
        }
        return AuthChallenge.of(httpHeaders);
    }

    @Test
    void a_response_without_a_www_authenticate_header_offers_nothing() {
        assertThat(challenges()).isEmpty();
    }

    @Test
    void the_scheme_is_read_in_lower_case() {
        assertThat(challenges("BASIC realm=\"test\"")).containsOnlyKeys("basic");
        assertThat(challenges("basic").get("basic").scheme()).isEqualTo("basic");
    }

    @Test
    void the_parameters_are_read_by_their_names_in_lower_case() {
        AuthChallenge challenge = challenges("Digest Realm=\"testrealm@host.com\", qop=\"auth,auth-int\", nonce=\"n\", stale=TRUE").get("digest");

        assertThat(challenge.parameter("realm")).isEqualTo("testrealm@host.com");
        assertThat(challenge.parameter("qop")).isEqualTo("auth,auth-int");
        assertThat(challenge.parameter("nonce")).isEqualTo("n");
        assertThat(challenge.parameter("stale")).isEqualTo("TRUE");
        assertThat(challenge.parameter("opaque")).isNull();
    }

    @Test
    void an_escaped_quote_in_a_quoted_parameter_is_part_of_its_value() {
        assertThat(challenges("Basic realm=\"a\\\"b\"").get("basic").parameter("realm")).isEqualTo("a\"b");
    }

    @Test
    void a_parameter_without_a_value_is_read_as_null() {
        assertThat(challenges("Negotiate").get("negotiate").parameters()).isEmpty();
        assertThat(challenges("Basic realm").get("basic").parameters()).containsEntry("realm", null);
    }

    @Test
    void several_headers_offer_several_schemes() {
        assertThat(challenges("Basic realm=\"a\"", "Digest realm=\"b\", nonce=\"n\"")).containsOnlyKeys("basic", "digest");
    }

    @Test
    void a_scheme_that_is_offered_twice_is_taken_from_the_last_header() {
        assertThat(challenges("Basic realm=\"first\"", "Basic realm=\"second\"").get("basic").parameter("realm")).isEqualTo("second");
    }

    @Test
    void a_header_without_a_scheme_is_ignored() {
        assertThat(challenges("", "   ")).isEmpty();
    }
}

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
import io.restassured.common.spi.http.RequestBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class DigestAuthTest {
    private static final Credentials CREDENTIALS = Credentials.digest("Mufasa", "Circle Of Life", URI.create("http://www.nowhere.org/dir/index.html"));

    private static AuthChallenge challenge(String header) {
        Map<String, AuthChallenge> challenges = AuthChallenge.of(List.of(new HttpHeader("WWW-Authenticate", header)));
        return challenges.values().iterator().next();
    }

    private static DigestAuth digest(String header, String... clientNonces) {
        List<String> nonces = List.of(clientNonces);
        int[] at = {0};
        return DigestAuth.of(CREDENTIALS, challenge(header), () -> nonces.get(at[0]++));
    }

    /**
     * The example of RFC 2617, section 3.5, with its own client nonce and nonce count.
     */
    @Test
    void the_example_of_rfc_2617() {
        DigestAuth digest = digest("Digest realm=\"testrealm@host.com\", qop=\"auth,auth-int\", "
                + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"", "0a4f113b");

        String authorization = digest.authorization("GET", "/dir/index.html", null);

        assertThat(authorization).isEqualTo("Digest username=\"Mufasa\", realm=\"testrealm@host.com\", "
                + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", uri=\"/dir/index.html\", "
                + "response=\"6629fae49393a05397450978507c4ef1\", qop=auth, nc=00000001, cnonce=\"0a4f113b\", "
                + "algorithm=MD5, opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"");
    }

    @Test
    void a_challenge_without_qop_is_answered_without_a_client_nonce() {
        DigestAuth digest = digest("Digest realm=\"testrealm@host.com\", nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\"", "0a4f113b");

        String authorization = digest.authorization("GET", "/dir/index.html", null);

        assertThat(authorization).doesNotContain("qop", "nc=", "cnonce");
        // The RFC 2069 response: MD5(A1):nonce:MD5(A2)
        assertThat(authorization).contains("response=\"670fd8c2df070c60b045671b8b24ff02\"");
    }

    @Test
    void the_nonce_count_counts_the_answers_to_one_server_nonce() {
        DigestAuth digest = digest("Digest realm=\"r\", qop=auth, nonce=\"n\"", "cn");

        assertThat(digest.authorization("GET", "/", null)).contains("nc=00000001", "cnonce=\"cn\"");
        assertThat(digest.authorization("GET", "/", null)).contains("nc=00000002", "cnonce=\"cn\"");
        assertThat(digest.authorization("GET", "/", null)).contains("nc=00000003");
    }

    @Test
    void the_uri_includes_the_query() {
        String authorization = digest("Digest realm=\"r\", qop=auth, nonce=\"n\"", "cn").authorization("GET", "/path?q=1", null);

        assertThat(authorization).contains("uri=\"/path?q=1\"");
    }

    @Test
    void md5_sess_hashes_a1_with_the_nonces() {
        String md5 = digest("Digest realm=\"r\", qop=auth, nonce=\"n\", algorithm=MD5", "cn").authorization("GET", "/", null);
        String md5sess = digest("Digest realm=\"r\", qop=auth, nonce=\"n\", algorithm=MD5-sess", "cn").authorization("GET", "/", null);

        assertThat(md5).contains("algorithm=MD5");
        assertThat(md5sess).contains("algorithm=MD5-sess");
        assertThat(md5sess).isNotEqualTo(md5);
    }

    @Test
    void auth_int_covers_the_body_when_the_server_offers_it_and_the_body_can_be_read_again() {
        RequestBody body = RequestBody.bytes("body".getBytes(UTF_8), "text/plain");

        String withBody = digest("Digest realm=\"r\", qop=\"auth,auth-int\", nonce=\"n\"", "cn").authorization("POST", "/", body);
        String withoutBody = digest("Digest realm=\"r\", qop=\"auth,auth-int\", nonce=\"n\"", "cn").authorization("POST", "/", null);

        assertThat(withBody).contains("qop=auth-int");
        assertThat(withoutBody).contains("qop=auth, ");
    }

    @Test
    void auth_int_isnt_used_for_a_body_that_cant_be_read_again() {
        RequestBody body = RequestBody.stream(new java.io.ByteArrayInputStream("body".getBytes(UTF_8)), 4, "text/plain");

        assertThat(digest("Digest realm=\"r\", qop=\"auth,auth-int\", nonce=\"n\"", "cn").authorization("POST", "/", body))
                .contains("qop=auth, ");
    }

    @Test
    void auth_int_without_a_body_covers_an_empty_body() {
        String authorization = digest("Digest realm=\"r\", qop=auth-int, nonce=\"n\"", "cn").authorization("POST", "/", null);

        assertThat(authorization).contains("qop=auth-int", "response=\"b4fd26aaaad716e02d9e308ce686fd47\"");
    }

    @Test
    void only_auth_int_with_a_body_that_cant_be_read_again_isnt_answered() {
        RequestBody body = RequestBody.stream(new java.io.ByteArrayInputStream("body".getBytes(UTF_8)), 4, "text/plain");

        assertThat(digest("Digest realm=\"r\", qop=auth-int, nonce=\"n\"", "cn").authorization("POST", "/", body)).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "Digest qop=auth, nonce=\"n\"",
            "Digest realm=\"r\", qop=auth",
            "Digest realm=\"r\", qop=future, nonce=\"n\"",
            "Digest realm=\"r\", nonce=\"n\", algorithm=SHA-999",
            "Digest realm=\"r\", nonce=\"n\", algorithm=SHA-256-sess"})
    void a_challenge_without_a_realm_or_a_nonce_or_with_a_qop_or_algorithm_rest_assured_doesnt_know_isnt_answered(String header) {
        assertThat(DigestAuth.of(CREDENTIALS, challenge(header))).isNull();
    }
}

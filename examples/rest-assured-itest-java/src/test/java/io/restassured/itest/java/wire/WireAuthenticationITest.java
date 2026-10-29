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

import io.restassured.RestAssured;
import io.restassured.authentication.OAuthSignature;
import io.restassured.config.RedirectConfig;
import io.restassured.itest.java.support.TestClient;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

public class WireAuthenticationITest extends WireTest {
    private static final String USER = "user";
    private static final String PASSWORD = "pass";
    private static final String BASIC_USER_PASS = basicAuthorization(USER, PASSWORD);
    private static final String REALM = "wire";
    private static final String NONCE = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
    private static final String OPAQUE = "5ccc069c403ebaf9f0171e9517f40e41";
    private static final String BASIC_CHALLENGE = "Basic realm=\"" + REALM + "\"";
    private static final String DIGEST_CHALLENGE = "Digest realm=\"" + REALM + "\", qop=\"auth\", nonce=\"" + NONCE + "\", opaque=\"" + OPAQUE + "\", algorithm=MD5";
    private static final String CONSUMER_KEY = "consumer-key";
    private static final String CONSUMER_SECRET = "consumer-secret";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String TOKEN_SECRET = "token-secret";
    // The NTLMv1 NT response of PASSWORD to the challenge of ntlmChallenge(): the MD4 hash of the UTF-16LE password,
    // padded to 21 bytes, as three DES keys that each encrypt the challenge (MS-NLMP 3.3.1). Computed without HttpClient,
    // and that computation gives the NTLMv1 response of the example in MS-NLMP 4.2.2.
    private static final String NT_RESPONSE = "e9f1a5ed64533073f2fd0b2fa096a44a882e9ce4dcdb1500";

    // Basic and digest challenges

    @Test public void
    basic_against_a_basic_server_sends_the_credentials_only_after_the_challenge() {
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/basic")).
        then().
                statusCode(200).
                body(equalTo("authenticated"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).hasHeader("Authorization")).isFalse();
        assertThat(requests.get(1).headers("Authorization")).containsExactly(BASIC_USER_PASS);
    }

    @Test public void
    basic_against_a_digest_server_answers_the_digest_challenge() {
        server.respond("/digest", digestServer(USER, PASSWORD));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/digest")).
        then().
                statusCode(200).
                body(equalTo("authenticated"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).hasHeader("Authorization")).isFalse();
        assertThat(requests.get(1).headers("Authorization")).singleElement().asString().startsWith("Digest ");
        assertThat(digestResponseIsValid(requests.get(1), PASSWORD)).isTrue();
    }

    @Test public void
    digest_against_a_digest_server_sends_a_valid_digest_response_after_the_challenge() {
        server.respond("/digest", digestServer(USER, PASSWORD));

        given().
                auth().digest(USER, PASSWORD).
        when().
                get(server.url("/digest")).
        then().
                statusCode(200).
                body(equalTo("authenticated"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).hasHeader("Authorization")).isFalse();
        RecordedRequest answer = requests.get(1);
        Map<String, String> params = authParams(answer.header("Authorization"), "Digest");
        assertThat(params).containsOnlyKeys("username", "realm", "nonce", "uri", "response", "qop", "nc", "cnonce", "algorithm", "opaque");
        assertThat(params).containsEntry("username", USER).containsEntry("realm", REALM).containsEntry("nonce", NONCE).
                containsEntry("uri", "/digest").containsEntry("qop", "auth").containsEntry("nc", "00000001").
                containsEntry("algorithm", "MD5").containsEntry("opaque", OPAQUE);
        assertThat(digestResponseIsValid(answer, PASSWORD)).isTrue();
    }

    @Test public void
    digest_response_covers_the_query_string_in_the_uri() {
        server.respond("/digest", digestServer(USER, PASSWORD));

        given().
                auth().digest(USER, PASSWORD).
                queryParam("a", "b c").
        when().
                get(server.url("/digest")).
        then().
                statusCode(200);

        RecordedRequest answer = server.lastRequest();
        assertThat(authParams(answer.header("Authorization"), "Digest")).containsEntry("uri", "/digest?a=b%20c");
        assertThat(digestResponseIsValid(answer, PASSWORD)).isTrue();
    }

    @Test public void
    digest_against_a_basic_server_sends_no_credentials_and_returns_the_401() {
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().digest(USER, PASSWORD).
        when().
                get(server.url("/basic")).
        then().
                statusCode(401).
                body(equalTo("unauthorized")).
                header("WWW-Authenticate", BASIC_CHALLENGE);

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    @Test public void
    a_server_offering_basic_and_digest_gets_a_digest_answer() {
        server.respond("/both", (request, response) -> {
            String authorization = request.header("Authorization");
            if (authorization != null && authorization.startsWith("Digest ") && digestResponseIsValid(request, PASSWORD)) {
                Responder.text(200, "authenticated").respond(request, response);
            } else {
                Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", BASIC_CHALLENGE).withHeader("WWW-Authenticate", DIGEST_CHALLENGE).respond(request, response);
            }
        });

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/both")).
        then().
                statusCode(200);

        assertThat(server.requests()).hasSize(2);
        assertThat(server.lastRequest().header("Authorization")).startsWith("Digest ");
    }

    @Test public void
    basic_with_a_wrong_password_answers_the_challenge_once_and_returns_the_401() {
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().basic(USER, "wrong").
        when().
                get(server.url("/basic")).
        then().
                statusCode(401).
                body(equalTo("unauthorized")).
                header("WWW-Authenticate", BASIC_CHALLENGE);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(1).header("Authorization")).isEqualTo(basicAuthorization(USER, "wrong"));
    }

    @Test public void
    digest_with_a_wrong_password_answers_the_challenge_once_and_returns_the_401() {
        server.respond("/digest", digestServer(USER, PASSWORD));

        given().
                auth().digest(USER, "wrong").
        when().
                get(server.url("/digest")).
        then().
                statusCode(401).
                body(equalTo("unauthorized"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(digestResponseIsValid(requests.get(1), "wrong")).isTrue();
    }

    @Test public void
    basic_sends_no_credentials_when_the_server_does_not_challenge() {
        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/open")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    @Test public void
    basic_encodes_non_ascii_credentials_as_question_marks_after_a_challenge() {
        server.respond("/basic", basicServer("jöhn", "pässword"));

        given().
                auth().basic("jöhn", "pässword").
        when().
                get(server.url("/basic")).
        then().
                statusCode(401);

        // Today's behavior: the credentials are encoded as US-ASCII, as HttpClient 4 did, so non-ASCII characters become '?'
        assertThat(server.lastRequest().header("Authorization")).isEqualTo(basicAuthorization("j?hn", "p?ssword"));
    }

    // Preemptive basic

    @Test public void
    preemptive_basic_sends_the_credentials_on_the_first_request() {
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().preemptive().basic(USER, PASSWORD).
        when().
                get(server.url("/basic")).
        then().
                statusCode(200).
                body(equalTo("authenticated"));

        assertThat(server.onlyRequest().headers("Authorization")).containsExactly(BASIC_USER_PASS);
    }

    @Test public void
    preemptive_basic_encodes_the_credentials_as_iso_8859_1() {
        given().
                auth().preemptive().basic("jöhn", "pässword").
        when().
                get(server.url("/open")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().header("Authorization")).isEqualTo("Basic " + Base64.getEncoder().encodeToString("jöhn:pässword".getBytes(ISO_8859_1)));
    }

    @Test public void
    preemptive_basic_with_a_wrong_password_sends_one_request_and_returns_the_401() {
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().preemptive().basic(USER, "wrong").
        when().
                get(server.url("/basic")).
        then().
                statusCode(401);

        assertThat(server.onlyRequest().header("Authorization")).isEqualTo(basicAuthorization(USER, "wrong"));
    }

    @Test public void
    preemptive_basic_replaces_an_authorization_header_set_before() {
        given().
                header("Authorization", "Bearer before").
                auth().preemptive().basic(USER, PASSWORD).
        when().
                get(server.url("/open")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().headers("Authorization")).containsExactly(BASIC_USER_PASS);
    }

    // Credentials after a redirect

    @Test public void
    basic_answers_a_challenge_after_a_redirect_to_the_same_host() {
        server.respond("/redirect", Responder.redirect(302, server.url("/basic")));
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/redirect")).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::target).containsExactly("/redirect", "/basic", "/basic");
        assertThat(requests).extracting(r -> r.header("Authorization")).containsExactly(null, null, BASIC_USER_PASS);
    }

    @Test public void
    basic_does_not_answer_a_challenge_after_a_redirect_to_another_host() {
        server.respond("/redirect", Responder.redirect(302, server.url("127.0.0.1", "/basic")));
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/redirect")).
        then().
                statusCode(401);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::target).containsExactly("/redirect", "/basic");
        assertThat(requests).allSatisfy(r -> assertThat(r.hasHeader("Authorization")).isFalse());
    }

    @Test public void
    basic_keeps_sending_the_credentials_after_authenticating_and_being_redirected_to_the_same_host() {
        server.respond("/basic", basicServer(USER, PASSWORD, Responder.redirect(302, server.url("/target"))));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/basic")).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::target).containsExactly("/basic", "/basic", "/target");
        assertThat(requests).extracting(r -> r.header("Authorization")).containsExactly(null, BASIC_USER_PASS, BASIC_USER_PASS);
    }

    @Test public void
    basic_does_not_send_the_credentials_after_authenticating_and_being_redirected_to_another_host() {
        server.respond("/basic", basicServer(USER, PASSWORD, Responder.redirect(302, server.url("127.0.0.1", "/target"))));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/basic")).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::target).containsExactly("/basic", "/basic", "/target");
        assertThat(requests).extracting(r -> r.header("Authorization")).containsExactly(null, BASIC_USER_PASS, null);
    }

    @Test public void
    preemptive_basic_is_sent_again_after_a_redirect_to_the_same_host() {
        server.respond("/redirect", Responder.redirect(302, server.url("/target")));

        given().
                auth().preemptive().basic(USER, PASSWORD).
        when().
                get(server.url("/redirect")).
        then().
                statusCode(200);

        assertThat(server.requests()).extracting(r -> r.header("Authorization")).containsExactly(BASIC_USER_PASS, BASIC_USER_PASS);
    }

    @Test public void
    preemptive_basic_is_not_sent_after_a_redirect_to_another_host() {
        server.respond("/redirect", Responder.redirect(302, server.url("127.0.0.1", "/target")));

        given().
                auth().preemptive().basic(USER, PASSWORD).
        when().
                get(server.url("/redirect")).
        then().
                statusCode(200);

        assertThat(server.requests()).extracting(r -> r.header("Authorization")).containsExactly(BASIC_USER_PASS, null);
    }

    @Test public void
    authorization_header_is_sent_again_after_a_redirect_to_the_same_host() {
        server.respond("/redirect", Responder.redirect(302, server.url("/target")));

        given().
                header("Authorization", "Custom secret").
        when().
                get(server.url("/redirect")).
        then().
                statusCode(200);

        assertThat(server.requests()).extracting(r -> r.header("Authorization")).containsExactly("Custom secret", "Custom secret");
    }

    @Test public void
    authorization_header_is_not_sent_after_a_redirect_to_another_host() {
        server.respond("/redirect", Responder.redirect(302, server.url("127.0.0.1", "/target")));

        given().
                header("Authorization", "Custom secret").
        when().
                get(server.url("/redirect")).
        then().
                statusCode(200);

        assertThat(server.requests()).extracting(r -> r.header("Authorization")).containsExactly("Custom secret", null);
    }

    @Test public void
    authorization_header_is_sent_to_another_host_when_cross_host_stripping_is_disabled() {
        server.respond("/redirect", Responder.redirect(302, server.url("127.0.0.1", "/target")));

        given().
                config(config().redirect(redirectConfig().stripSensitiveHeadersOnCrossHostRedirect(false))).
                header("Authorization", "Custom secret").
        when().
                get(server.url("/redirect")).
        then().
                statusCode(200);

        assertThat(server.requests()).extracting(r -> r.header("Authorization")).containsExactly("Custom secret", "Custom secret");
    }

    // OAuth 1

    @Test public void
    oauth_signs_the_request_with_hmac_sha1_in_the_authorization_header() {
        given().
                auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET).
                queryParam("q", "a b").
        when().
                get(server.url("/oauth")).
        then().
                statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("/oauth?q=a%20b");
        assertThat(request.headers("Authorization")).singleElement().asString().startsWith("OAuth ");
        Map<String, String> params = authParams(request.header("Authorization"), "OAuth");
        assertThat(params).containsOnlyKeys("oauth_consumer_key", "oauth_nonce", "oauth_signature", "oauth_signature_method", "oauth_timestamp", "oauth_token", "oauth_version");
        assertThat(params).containsEntry("oauth_consumer_key", CONSUMER_KEY).containsEntry("oauth_token", ACCESS_TOKEN).
                containsEntry("oauth_signature_method", "HMAC-SHA1").containsEntry("oauth_version", "1.0");
        assertThat(params.get("oauth_nonce")).isNotEmpty();
        assertThat(params.get("oauth_timestamp")).matches("\\d+");
        assertThat(oauthHeaderSignatureIsValid(request, server.url("/oauth"))).isTrue();
    }

    @Test public void
    oauth_signature_covers_the_form_parameters_of_the_body() {
        given().
                auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET).
                formParam("name", "a b").
        when().
                post(server.url("/oauth")).
        then().
                statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.bodyAsString()).isEqualTo("name=a%20b");
        assertThat(oauthHeaderSignatureIsValid(request, server.url("/oauth"))).isTrue();
    }

    @Test public void
    oauth_with_query_string_signature_puts_the_oauth_parameters_in_the_query() {
        given().
                auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET, OAuthSignature.QUERY_STRING).
                queryParam("q", "a").
        when().
                get(server.url("/oauth")).
        then().
                statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.hasHeader("Authorization")).isFalse();
        assertThat(request.target()).startsWith("/oauth?");
        Map<String, String> params = queryParams(request.target());
        assertThat(params).containsOnlyKeys("q", "oauth_consumer_key", "oauth_nonce", "oauth_signature", "oauth_signature_method", "oauth_timestamp", "oauth_token", "oauth_version");
        assertThat(params).containsEntry("q", "a").containsEntry("oauth_consumer_key", CONSUMER_KEY).containsEntry("oauth_token", ACCESS_TOKEN).
                containsEntry("oauth_signature_method", "HMAC-SHA1").containsEntry("oauth_version", "1.0");
        assertThat(oauthSignatureIsValid(request.method(), server.url("/oauth"), List.of(params))).isTrue();
    }

    @Test public void
    oauth_signs_every_redirect_hop_to_the_same_host_for_its_own_url() {
        server.respond("/oauth", Responder.redirect(302, server.url("/target")));

        given().
                auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET).
        when().
                get(server.url("/oauth")).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(RecordedRequest::target).containsExactly("/oauth", "/target");
        Map<String, String> first = authParams(requests.get(0).header("Authorization"), "OAuth");
        Map<String, String> second = authParams(requests.get(1).header("Authorization"), "OAuth");
        assertThat(oauthHeaderSignatureIsValid(requests.get(0), server.url("/oauth"))).isTrue();
        assertThat(oauthHeaderSignatureIsValid(requests.get(1), server.url("/target"))).isTrue();
        assertThat(second.get("oauth_nonce")).isNotEqualTo(first.get("oauth_nonce"));
    }

    @Test public void
    oauth_does_not_sign_a_redirect_hop_to_another_host() {
        server.respond("/oauth", Responder.redirect(302, server.url("127.0.0.1", "/target")));

        given().
                auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET).
        when().
                get(server.url("/oauth")).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests.get(0).header("Authorization")).startsWith("OAuth ");
        // The cross-host header stripper runs after the OAuth signer and removes its header
        assertThat(requests.get(1).hasHeader("Authorization")).isFalse();
    }

    @Test public void
    oauth_signs_a_redirect_hop_to_another_host_when_cross_host_stripping_is_disabled() {
        server.respond("/oauth", Responder.redirect(302, server.url("127.0.0.1", "/target")));

        given().
                config(config().redirect(new RedirectConfig().stripSensitiveHeadersOnCrossHostRedirect(false))).
                auth().oauth(CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, TOKEN_SECRET).
        when().
                get(server.url("/oauth")).
        then().
                statusCode(200);

        RecordedRequest hop = server.lastRequest();
        assertThat(oauthHeaderSignatureIsValid(hop, server.url("127.0.0.1", "/target"))).isTrue();
    }

    // OAuth 2

    @Test public void
    oauth2_sends_a_bearer_token_on_the_first_request() {
        given().
                auth().oauth2("token-123").
        when().
                get(server.url("/oauth2")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().headers("Authorization")).containsExactly("Bearer token-123");
    }

    @Test public void
    preemptive_oauth2_sends_a_bearer_token_on_the_first_request() {
        given().
                auth().preemptive().oauth2("token-123").
        when().
                get(server.url("/oauth2")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().headers("Authorization")).containsExactly("Bearer token-123");
    }

    @Test public void
    oauth2_with_header_signature_sends_a_bearer_token() {
        given().
                auth().oauth2("token-123", OAuthSignature.HEADER).
        when().
                get(server.url("/oauth2")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().headers("Authorization")).containsExactly("Bearer token-123");
    }

    @Test public void
    oauth2_with_query_string_signature_sends_the_token_as_access_token_query_parameter() {
        given().
                auth().oauth2("token-123", OAuthSignature.QUERY_STRING).
                queryParam("q", "a").
        when().
                get(server.url("/oauth2")).
        then().
                statusCode(200);

        RecordedRequest request = server.onlyRequest();
        assertThat(request.target()).isEqualTo("/oauth2?q=a&access_token=token-123");
        assertThat(request.hasHeader("Authorization")).isFalse();
    }

    // NTLM

    @Test @Tag(TestClient.APACHE5_ONLY) public void
    ntlm_answers_the_challenge_with_a_negotiate_and_then_an_authenticate_message() {
        server.respond("/ntlm", ntlmServer(NT_RESPONSE));
        RequestSpecification request = given().auth().ntlm(USER, PASSWORD, "WORKSTATION", "DOMAIN");

        if (TestClient.isJdk()) {
            assertThatThrownBy(() -> request.get(server.url("/ntlm")))
                    .isExactlyInstanceOf(UnsupportedOperationException.class)
                    .hasMessage(TestClient.cantMessage("authenticate with NTLM") + ".");
            assertThat(server.requests()).isEmpty();
            return;
        }
        request.get(server.url("/ntlm")).then().statusCode(200).body(equalTo("authenticated"));

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).extracting(WireAuthenticationITest::ntlmMessageType).containsExactly(0, 1, 3);
        assertThat(ntResponse(requests.get(2))).isEqualTo(NT_RESPONSE);
        String authenticate = new String(ntlmMessage(requests.get(2)), UTF_16LE);
        assertThat(authenticate).contains(USER, "DOMAIN");
    }

    // No authentication

    @Test public void
    none_removes_the_statically_configured_authentication() {
        RestAssured.authentication = RestAssured.preemptive().basic(USER, PASSWORD);

        given().
                auth().none().
        when().
                get(server.url("/open")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    @Test public void
    statically_configured_preemptive_basic_is_sent_without_none() {
        RestAssured.authentication = RestAssured.preemptive().basic(USER, PASSWORD);

        given().
        when().
                get(server.url("/open")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().headers("Authorization")).containsExactly(BASIC_USER_PASS);
    }

    @Test public void
    none_removes_an_authorization_header_set_before() {
        given().
                header("Authorization", "Custom secret").
                auth().none().
        when().
                get(server.url("/open")).
        then().
                statusCode(200);

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    @Test public void
    none_does_not_answer_a_challenge() {
        RestAssured.authentication = RestAssured.basic(USER, PASSWORD);
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().none().
        when().
                get(server.url("/basic")).
        then().
                statusCode(401);

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    // Unusable challenges

    @Test public void
    a_digest_challenge_without_a_nonce_is_returned_after_one_request() {
        server.respond("/digest", Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", "Digest realm=\"" + REALM + "\", qop=\"auth\""));

        given().
                auth().digest(USER, PASSWORD).
        when().
                get(server.url("/digest")).
        then().
                statusCode(401).
                body(equalTo("unauthorized"));

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    @Test public void
    a_binary_byte_array_body_is_sent_again_to_answer_a_challenge() {
        server.respond("/basic", basicServer(USER, PASSWORD));
        byte[] body = {0, 1, 2, (byte) 0xff};

        given().
                auth().basic(USER, PASSWORD).
                contentType("application/octet-stream").
                body(body).
        when().
                post(server.url("/basic")).
        then().
                statusCode(200);

        List<RecordedRequest> requests = server.requests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(1).header("Authorization")).isEqualTo(BASIC_USER_PASS);
        assertThat(requests.get(0).body()).isEqualTo(body);
        assertThat(requests.get(1).body()).isEqualTo(body);
    }

    @Test public void
    a_challenge_to_a_request_whose_body_cant_be_sent_again_is_returned_after_one_request() {
        server.respond("/basic", basicServer(USER, PASSWORD));

        given().
                auth().basic(USER, PASSWORD).
                contentType("application/octet-stream").
                body(new ByteArrayInputStream("body".getBytes(UTF_8))).
        when().
                post(server.url("/basic")).
        then().
                statusCode(401);

        assertThat(server.onlyRequest().bodyAsString()).isEqualTo("body");
    }

    @Test public void
    a_401_without_a_challenge_is_returned_after_one_request() {
        server.respond("/no-challenge", Responder.text(401, "unauthorized"));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/no-challenge")).
        then().
                statusCode(401).
                body(equalTo("unauthorized"));

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    @Test public void
    a_challenge_with_an_unknown_scheme_is_returned_after_one_request() {
        server.respond("/unknown", Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", "Custom realm=\"" + REALM + "\""));

        given().
                auth().basic(USER, PASSWORD).
        when().
                get(server.url("/unknown")).
        then().
                statusCode(401).
                body(equalTo("unauthorized")).
                header("WWW-Authenticate", "Custom realm=\"" + REALM + "\"");

        assertThat(server.onlyRequest().hasHeader("Authorization")).isFalse();
    }

    // Servers

    private static Responder basicServer(String user, String password) {
        return basicServer(user, password, Responder.text(200, "authenticated"));
    }

    private static Responder basicServer(String user, String password, Responder whenAuthenticated) {
        String expected = basicAuthorization(user, password);
        return (request, response) -> {
            if (expected.equals(request.header("Authorization"))) {
                whenAuthenticated.respond(request, response);
            } else {
                Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", BASIC_CHALLENGE).respond(request, response);
            }
        };
    }

    // Asks for NTLM, answers a negotiate message with a challenge, and an authenticate message with 200 when it has the
    // expected NT response
    private static Responder ntlmServer(String expectedNtResponse) {
        return (request, response) -> {
            int type = ntlmMessageType(request);
            if (type == 1) {
                Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", "NTLM " + Base64.getEncoder().encodeToString(ntlmChallenge()))
                        .respond(request, response);
            } else if (type == 3 && expectedNtResponse.equals(ntResponse(request))) {
                Responder.text(200, "authenticated").respond(request, response);
            } else {
                Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", "NTLM").respond(request, response);
            }
        };
    }

    // The NTLM message in the Authorization header, or null
    private static byte[] ntlmMessage(RecordedRequest request) {
        String authorization = request.header("Authorization");
        if (authorization == null || !authorization.startsWith("NTLM ")) {
            return null;
        }
        return Base64.getDecoder().decode(authorization.substring("NTLM ".length()));
    }

    // The type of the NTLM message in the Authorization header, or 0
    private static int ntlmMessageType(RecordedRequest request) {
        byte[] message = ntlmMessage(request);
        return message == null ? 0 : ByteBuffer.wrap(message, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    // The NT response of the authenticate message in the Authorization header, in hex
    private static String ntResponse(RecordedRequest request) {
        ByteBuffer message = ByteBuffer.wrap(ntlmMessage(request)).order(ByteOrder.LITTLE_ENDIAN);
        // The NtChallengeResponseFields: length, maximum length, offset
        int length = message.getShort(20);
        int offset = message.getInt(24);
        return HexFormat.of().formatHex(message.array(), offset, offset + length);
    }

    // A challenge message with no target name or information, for NTLM with Unicode strings
    private static byte[] ntlmChallenge() {
        ByteBuffer message = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);
        message.put("NTLMSSP\0".getBytes(US_ASCII));
        message.putInt(2);
        // Target name: length, maximum length, offset
        message.putShort((short) 0).putShort((short) 0).putInt(48);
        // Negotiate Unicode and NTLM
        message.putInt(0x00000201);
        // The server's challenge, and 8 reserved bytes
        message.put(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        message.put(new byte[8]);
        // Target information: length, maximum length, offset
        message.putShort((short) 0).putShort((short) 0).putInt(48);
        return message.array();
    }

    private static Responder digestServer(String user, String password) {
        return (request, response) -> {
            String authorization = request.header("Authorization");
            if (authorization != null && authorization.startsWith("Digest ") && user.equals(authParams(authorization, "Digest").get("username"))
                    && digestResponseIsValid(request, password)) {
                Responder.text(200, "authenticated").respond(request, response);
            } else {
                Responder.text(401, "unauthorized").withHeader("WWW-Authenticate", DIGEST_CHALLENGE).respond(request, response);
            }
        };
    }

    // RFC 7616 with qop=auth and MD5
    private static boolean digestResponseIsValid(RecordedRequest request, String password) {
        Map<String, String> params = authParams(request.header("Authorization"), "Digest");
        if (!NONCE.equals(params.get("nonce")) || !OPAQUE.equals(params.get("opaque")) || !REALM.equals(params.get("realm"))
                || !request.target().equals(params.get("uri"))) {
            return false;
        }
        String ha1 = md5(params.get("username") + ":" + REALM + ":" + password);
        String ha2 = md5(request.method() + ":" + params.get("uri"));
        String expected = md5(ha1 + ":" + NONCE + ":" + params.get("nc") + ":" + params.get("cnonce") + ":" + params.get("qop") + ":" + ha2);
        return expected.equals(params.get("response"));
    }

    // The OAuth parameters of the Authorization header, the query and the form parameters of the body are signed
    private static boolean oauthHeaderSignatureIsValid(RecordedRequest request, String baseUrl) {
        List<Map<String, String>> params = new ArrayList<>();
        params.add(authParams(request.header("Authorization"), "OAuth"));
        if (request.target().indexOf('?') >= 0) {
            params.add(queryParams(request.target()));
        }
        String contentType = request.header("Content-Type");
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
            params.add(formParams(request.bodyAsString()));
        }
        return oauthSignatureIsValid(request.method(), baseUrl, params);
    }

    // RFC 5849 section 3.4 with HMAC-SHA1: oauth_signature is among the params, and signs all the others
    private static boolean oauthSignatureIsValid(String method, String baseUrl, List<Map<String, String>> params) {
        String signature = null;
        List<String> pairs = new ArrayList<>();
        for (Map<String, String> source : params) {
            for (Map.Entry<String, String> param : source.entrySet()) {
                if (param.getKey().equals("oauth_signature")) {
                    signature = param.getValue();
                } else {
                    pairs.add(percentEncode(param.getKey()) + "=" + percentEncode(param.getValue()));
                }
            }
        }
        pairs.sort(null);
        String baseString = method + "&" + percentEncode(baseUrl) + "&" + percentEncode(String.join("&", pairs));
        String key = percentEncode(CONSUMER_SECRET) + "&" + percentEncode(TOKEN_SECRET);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(UTF_8), "HmacSHA1"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(baseString.getBytes(UTF_8)));
            return expected.equals(signature);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Map<String, String> authParams(String header, String scheme) {
        assertThat(header).startsWith(scheme + " ");
        Map<String, String> params = new HashMap<>();
        Matcher matcher = Pattern.compile("([A-Za-z0-9_-]+)\\s*=\\s*(?:\"([^\"]*)\"|([^,\\s]*))").matcher(header.substring(scheme.length() + 1));
        while (matcher.find()) {
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            params.put(matcher.group(1), scheme.equals("OAuth") ? URLDecoder.decode(value, UTF_8) : value);
        }
        return params;
    }

    private static Map<String, String> queryParams(String target) {
        return formParams(target.substring(target.indexOf('?') + 1));
    }

    private static Map<String, String> formParams(String encoded) {
        Map<String, String> params = new HashMap<>();
        for (String pair : encoded.split("&")) {
            int equals = pair.indexOf('=');
            params.put(URLDecoder.decode(pair.substring(0, equals), UTF_8), URLDecoder.decode(pair.substring(equals + 1), UTF_8));
        }
        return params;
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value, UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private static String md5(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(ISO_8859_1)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}

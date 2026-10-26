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

import io.restassured.authentication.OAuthSignature;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class CoreOAuthSignerTest {
    private static final URI URL = URI.create("http://example.com/path?q=a%20b");

    private static HttpRequest request(String method, URI uri, RequestBody body, HttpHeader... headers) {
        return new HttpRequest(method, uri, List.of(headers), body, RequestOptions.DEFAULT);
    }

    private static String header(HttpRequest request, String name) {
        for (HttpHeader header : request.headers()) {
            if (header.name().equalsIgnoreCase(name)) {
                return header.value();
            }
        }
        return null;
    }

    private static CoreOAuthSigner oauth1(OAuthSignature signature) {
        return CoreOAuthSigner.oauth("key", "secret", "access", "token", signature, false);
    }

    @Test
    void oauth1_signs_in_the_authorization_header() {
        HttpRequest signed = oauth1(OAuthSignature.HEADER).sign(request("GET", URL, null));

        assertThat(signed.uri()).isEqualTo(URL);
        assertThat(header(signed, "Authorization")).startsWith("OAuth ")
                .contains("oauth_consumer_key=\"key\"", "oauth_token=\"access\"", "oauth_signature_method=\"HMAC-SHA1\"",
                        "oauth_version=\"1.0\"", "oauth_signature=");
    }

    @Test
    void oauth1_replaces_an_authorization_header_the_request_already_has() {
        HttpRequest signed = oauth1(OAuthSignature.HEADER).sign(request("GET", URL, null, new HttpHeader("Authorization", "Basic x")));

        assertThat(signed.headers()).filteredOn(header -> header.name().equals("Authorization")).hasSize(1);
        assertThat(header(signed, "Authorization")).startsWith("OAuth ");
    }

    @Test
    void oauth1_with_a_query_string_signature_puts_the_parameters_in_the_uri() {
        HttpRequest signed = oauth1(OAuthSignature.QUERY_STRING).sign(request("GET", URL, null));

        assertThat(header(signed, "Authorization")).isNull();
        assertThat(signed.uri().toString()).startsWith("http://example.com/path?")
                .contains("oauth_consumer_key=key", "oauth_token=access", "oauth_signature=");
        assertThat(signed.uri().getQuery()).contains("q=a b");
    }

    @Test
    void the_signature_of_a_hop_covers_its_own_uri() {
        CoreOAuthSigner signer = oauth1(OAuthSignature.HEADER);

        String first = header(signer.sign(request("GET", URL, null)), "Authorization");
        String second = header(signer.sign(request("GET", URI.create("http://example.com/other"), null)), "Authorization");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void the_form_parameters_of_the_body_are_signed() {
        RequestBody form = RequestBody.bytes("a=1&b=a%20b".getBytes(UTF_8), "application/x-www-form-urlencoded");
        RequestBody other = RequestBody.bytes("a=1&b=a%20b".getBytes(UTF_8), "text/plain");

        String withForm = header(oauth1(OAuthSignature.HEADER).sign(request("POST", URL, form)), "Authorization");
        String withoutForm = header(oauth1(OAuthSignature.HEADER).sign(request("POST", URL, other)), "Authorization");

        assertThat(withForm).isNotEqualTo(withoutForm);
        assertThat(signatureOf(withForm)).isNotEqualTo(signatureOf(withoutForm));
    }

    @Test
    void a_method_that_oauth_doesnt_know_isnt_signed() {
        HttpRequest request = request("REPORT", URL, null);

        assertThat(oauth1(OAuthSignature.HEADER).sign(request)).isSameAs(request);
    }

    @Test
    void oauth2_sends_a_bearer_token_in_the_authorization_header() {
        HttpRequest signed = CoreOAuthSigner.oauth2("token", OAuthSignature.HEADER).sign(request("GET", URL, null));

        assertThat(header(signed, "Authorization")).isEqualTo("Bearer token");
        assertThat(signed.uri()).isEqualTo(URL);
    }

    @Test
    void oauth2_with_a_query_string_signature_adds_the_access_token_to_the_uri() {
        HttpRequest withQuery = CoreOAuthSigner.oauth2("to ken", OAuthSignature.QUERY_STRING).sign(request("GET", URL, null));
        HttpRequest withoutQuery = CoreOAuthSigner.oauth2("token", OAuthSignature.QUERY_STRING)
                .sign(request("GET", URI.create("http://example.com/path"), null));

        assertThat(header(withQuery, "Authorization")).isNull();
        assertThat(withQuery.uri().toString()).isEqualTo("http://example.com/path?q=a%20b&access_token=to+ken");
        assertThat(withoutQuery.uri().toString()).isEqualTo("http://example.com/path?access_token=token");
    }

    private static String signatureOf(String authorization) {
        int at = authorization.indexOf("oauth_signature=\"");
        return authorization.substring(at, authorization.indexOf('"', at + "oauth_signature=\"".length()));
    }
}

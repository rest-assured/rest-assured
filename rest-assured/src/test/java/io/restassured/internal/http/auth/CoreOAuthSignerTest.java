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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // RFC 5849

    @Test
    void the_signature_of_the_example_of_rfc_5849_section_1_2() {
        // That example has no oauth_version, so the base string and signature are checked rather than a signed request
        List<Map.Entry<String, String>> parameters = List.of(Map.entry("file", "vacation.jpg"), Map.entry("size", "original"),
                Map.entry("oauth_consumer_key", "dpf43f3p2l4k3l03"), Map.entry("oauth_token", "nnch734d00sl2jdk"),
                Map.entry("oauth_signature_method", "HMAC-SHA1"), Map.entry("oauth_timestamp", "137131202"), Map.entry("oauth_nonce", "chapoH"));

        String baseString = CoreOAuthSigner.baseString("GET", URI.create("http://photos.example.net/photos?file=vacation.jpg&size=original"), parameters);

        assertThat(CoreOAuthSigner.hmacSha1(baseString, "kd94hf93k423kf44", "pfkkdhi9sl3r4s00")).isEqualTo("MdpQcU8iPSUjWoN/UDMsK2sui9I=");
    }

    @Test
    void the_base_string_of_the_example_of_rfc_5849_section_3_4_1() {
        // The query and body parameters as the signer decodes them from "b5=%3D%253D&a3=a&c%40=&a2=r%20b" and "c2&a3=2+q"
        List<Map.Entry<String, String>> parameters = List.of(Map.entry("b5", "=%3D"), Map.entry("a3", "a"), Map.entry("c@", ""),
                Map.entry("a2", "r b"), Map.entry("c2", ""), Map.entry("a3", "2 q"),
                Map.entry("oauth_consumer_key", "9djdj82h48djs9d2"), Map.entry("oauth_token", "kkk9d7dh3k39sjv7"),
                Map.entry("oauth_signature_method", "HMAC-SHA1"), Map.entry("oauth_timestamp", "137131201"), Map.entry("oauth_nonce", "7d8f3e4a"));

        assertThat(CoreOAuthSigner.baseString("POST", URI.create("http://example.com/request?b5=%3D%253D&a3=a&c%40=&a2=r%20b"), parameters))
                .isEqualTo("POST&http%3A%2F%2Fexample.com%2Frequest&a2%3Dr%2520b%26a3%3D2%2520q%26a3%3Da%26b5%3D%253D%25253D%26c%2540%3D"
                        + "%26c2%3D%26oauth_consumer_key%3D9djdj82h48djs9d2%26oauth_nonce%3D7d8f3e4a%26oauth_signature_method%3DHMAC-SHA1"
                        + "%26oauth_timestamp%3D137131201%26oauth_token%3Dkkk9d7dh3k39sjv7");
    }

    @Test
    void the_base_string_uri_follows_rfc_5849_section_3_4_1_2() {
        assertThat(baseStringUri("HTTP://EXAMPLE.COM:80/r%20v/X?id=123")).isEqualTo("http%3A%2F%2Fexample.com%2Fr%2520v%2FX");
        assertThat(baseStringUri("https://www.example.net:8080/?q=1")).isEqualTo("https%3A%2F%2Fwww.example.net%3A8080%2F");
        assertThat(baseStringUri("https://example.net:443")).isEqualTo("https%3A%2F%2Fexample.net%2F");
        assertThat(baseStringUri("http://user:password@example.net:8080/a:80/b#fragment")).isEqualTo("http%3A%2F%2Fexample.net%3A8080%2Fa%3A80%2Fb");
    }

    private static String baseStringUri(String uri) {
        return CoreOAuthSigner.baseString("GET", URI.create(uri), List.of()).split("&")[1];
    }

    @Test
    void percent_encoding_leaves_only_the_unreserved_characters_of_rfc_3986() {
        assertThat(CoreOAuthSigner.percentEncode("AZaz09-._~")).isEqualTo("AZaz09-._~");
        assertThat(CoreOAuthSigner.percentEncode("Ladies + Gentlemen*!")).isEqualTo("Ladies%20%2B%20Gentlemen%2A%21");
        assertThat(CoreOAuthSigner.percentEncode("Dogs, Cats & Mice=%/")).isEqualTo("Dogs%2C%20Cats%20%26%20Mice%3D%25%2F");
        assertThat(CoreOAuthSigner.percentEncode("é☃😀")).isEqualTo("%C3%A9%E2%98%83%F0%9F%98%80");
    }

    @Test
    void the_header_is_written_with_the_parameters_in_the_order_of_earlier_versions() {
        CoreOAuthSigner signer = CoreOAuthSigner.oauth("key", "secret", "access", "token", OAuthSignature.HEADER, false,
                Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC), () -> "nonce");

        String header = header(signer.sign(request("GET", URL, null)), "Authorization");

        assertThat(header).matches("OAuth oauth_nonce=\"nonce\", oauth_signature=\"[A-Za-z0-9%]{28,}\", oauth_token=\"access\", "
                + "oauth_consumer_key=\"key\", oauth_timestamp=\"1700000000\", oauth_signature_method=\"HMAC-SHA1\", oauth_version=\"1.0\"");
    }

    @Test
    void the_nonce_differs_between_requests() {
        CoreOAuthSigner signer = oauth1(OAuthSignature.QUERY_STRING);

        String first = signer.sign(request("GET", URL, null)).uri().getQuery();
        String second = signer.sign(request("GET", URL, null)).uri().getQuery();

        assertThat(nonceOf(first)).matches("\\d+").isNotEqualTo(nonceOf(second));
    }

    private static String nonceOf(String query) {
        return query.replaceAll(".*oauth_nonce=([^&]*).*", "$1");
    }

    @Test
    void the_oauth_parameters_of_a_query_string_signature_go_before_the_fragment() {
        HttpRequest signed = oauth1(OAuthSignature.QUERY_STRING).sign(request("GET", URI.create("http://example.com/path#part"), null));

        assertThat(signed.uri().getRawFragment()).isEqualTo("part");
        assertThat(signed.uri().getRawQuery()).contains("oauth_signature=");
    }

    @Test
    void the_credentials_must_not_be_null() {
        assertThatThrownBy(() -> CoreOAuthSigner.oauth("key", "secret", null, "token", OAuthSignature.HEADER, false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("accessToken");
    }

    @Test
    void an_empty_consumer_secret_is_allowed() {
        HttpRequest signed = CoreOAuthSigner.oauth("key", "", "access", "token", OAuthSignature.HEADER, false).sign(request("GET", URL, null));

        assertThat(header(signed, "Authorization")).contains("oauth_signature=");
    }

    // OAuth 2

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

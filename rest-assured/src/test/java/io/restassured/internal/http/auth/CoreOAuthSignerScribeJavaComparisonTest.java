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

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.SignatureType;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.services.TimestampService;
import io.restassured.authentication.OAuthSignature;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.RequestBody;
import io.restassured.common.spi.http.RequestOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.net.URLEncoder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Signs the same requests with {@link CoreOAuthSigner} and with scribejava 2.5.3, which REST Assured signed OAuth 1
 * requests with before 7.0, with the same nonce and timestamp, and checks that the Authorization header or the URI is the
 * same, so that servers that verified the signatures of earlier versions keep verifying them.
 */
class CoreOAuthSignerScribeJavaComparisonTest {
    private static final String NONCE = "1234567890";
    private static final long TIMESTAMP = 1_700_000_000L;

    record Case(String name, String method, String uri, List<Map.Entry<String, String>> form, String consumerKey,
                String consumerSecret, String accessToken, String tokenSecret, boolean addEmptyTokenToBaseString) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static Case get(String name, String uri) {
        return new Case(name, "GET", uri, List.of(), "key", "secret", "token", "tokenSecret", false);
    }

    private static Case post(String name, String uri, List<Map.Entry<String, String>> form) {
        return new Case(name, "POST", uri, form, "key", "secret", "token", "tokenSecret", false);
    }

    private static Case credentials(String name, String consumerKey, String consumerSecret, String accessToken, String tokenSecret,
                                    boolean addEmptyTokenToBaseString) {
        return new Case(name, "GET", "http://example.com/path?q=1", List.of(), consumerKey, consumerSecret, accessToken, tokenSecret,
                addEmptyTokenToBaseString);
    }

    static Stream<Case> cases() {
        return Stream.of(
                get("GET without a query", "http://example.com/path"),
                get("GET with a query", "http://example.com/path?q=a%20b&plus=c+d&reserved=%2B%26%3D%2A%7E&tilde=~"),
                get("GET with repeated query parameters", "http://example.com/path?a=2&b=x&a=1&a=10&a=1"),
                get("GET with unicode values", "http://example.com/path?name=caf%C3%A9&emoji=%F0%9F%98%80&yen=%C2%A5"),
                get("GET with a parameter without a value", "http://example.com/path?flag&empty="),
                get("GET with an encoded path", "http://example.com/a%20b/c%2Fd/%C3%A9"),
                get("GET to the root", "http://example.com/"),
                get("GET to a non-default port", "http://example.com:8080/path?q=1"),
                get("GET to a non-default HTTPS port", "https://example.com:8443/path"),
                get("GET to the default HTTP port", "http://example.com:80/path?q=1"),
                get("GET to the default HTTPS port", "https://example.com:443/path"),
                get("GET to an IP address", "http://127.0.0.1:8080/path"),
                post("POST with form parameters", "http://example.com/form",
                        List.of(Map.entry("name", "a b"), Map.entry("plus", "1+1=2"), Map.entry("empty", ""))),
                post("POST with repeated and unicode form parameters and a query", "http://example.com/form?q=x",
                        List.of(Map.entry("a", "2"), Map.entry("a", "1"), Map.entry("city", "Zürich"), Map.entry("snow", "☃ 😀"))),
                new Case("PUT", "PUT", "http://example.com/path", List.of(Map.entry("a", "1")), "key", "secret", "token", "tokenSecret", false),
                new Case("lower-case method", "delete", "http://example.com/path?q=1", List.of(), "key", "secret", "token", "tokenSecret", false),
                new Case("PATCH", "PATCH", "http://example.com/path", List.of(), "key", "secret", "token", "tokenSecret", false),
                new Case("HEAD", "HEAD", "http://example.com/path", List.of(), "key", "secret", "token", "tokenSecret", false),
                new Case("OPTIONS", "OPTIONS", "http://example.com/path", List.of(), "key", "secret", "token", "tokenSecret", false),
                credentials("reserved characters in the credentials", "k e+y*", "s&e=c%", "t~o.k-e_n", "ts/+=", false),
                credentials("unicode credentials", "clé", "sécret", "tøken", "☃", false),
                credentials("empty access token and secret", "key", "secret", "", "", false),
                credentials("empty access token and secret with addEmptyTokenToBaseString", "key", "secret", "", "", true),
                credentials("empty access token with a secret", "key", "secret", "", "tokenSecret", false),
                credentials("access token with addEmptyTokenToBaseString", "key", "secret", "token", "tokenSecret", true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void the_authorization_header_is_the_one_scribejava_writes(Case request) {
        HttpRequest signed = restAssured(request, OAuthSignature.HEADER);

        assertThat(signed.uri()).isEqualTo(URI.create(request.uri()));
        assertThat(signed.headers()).containsExactly(new HttpHeader("Authorization", scribeJava(request, SignatureType.Header).getHeaders().get("Authorization")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void the_query_string_is_the_one_scribejava_writes(Case request) {
        HttpRequest signed = restAssured(request, OAuthSignature.QUERY_STRING);

        assertThat(signed.headers()).isEmpty();
        assertThat(signed.uri().toString()).isEqualTo(scribeJava(request, SignatureType.QueryString).getCompleteUrl());
    }

    // Where scribejava 2.5.3 doesn't follow RFC 5849, REST Assured follows the RFC (see CoreOAuthSignerTest)

    @Test
    void a_parameter_name_that_isnt_ascii_is_sorted_by_its_encoding_unlike_scribejava() {
        // scribejava sorts the decoded names, so "é" comes after "oauth_..." in its base string, not before
        assertDiffersFromScribeJava(get("unicode name", "http://example.com/path?%C3%A9=1"));
    }

    @Test
    void a_query_value_with_an_equals_sign_is_signed_whole_unlike_scribejava() {
        // scribejava signs "abc" for "abc==", which the server can't verify
        assertDiffersFromScribeJava(get("base64 value", "http://example.com/path?token=abc=="));
    }

    @Test
    void the_scheme_and_host_are_lower_case_in_the_base_string_unlike_scribejava() {
        assertDiffersFromScribeJava(get("upper-case host", "HTTP://Example.COM/path"));
    }

    @Test
    void an_empty_path_is_signed_as_slash_unlike_scribejava() {
        // An HTTP client sends "/" for an empty path, so that's what the server signs
        assertDiffersFromScribeJava(get("empty path", "http://example.com"));
    }

    @Test
    void an_empty_consumer_secret_is_allowed_unlike_scribejava() {
        Case request = credentials("empty consumer secret", "key", "", "token", "tokenSecret", false);

        assertThatThrownBy(() -> scribeJava(request, SignatureType.Header)).hasRootCauseMessage("Api secret cant be null or empty string");
        assertThat(restAssured(request, OAuthSignature.HEADER).headers()).singleElement().extracting(HttpHeader::value).asString().startsWith("OAuth ");
    }

    private static void assertDiffersFromScribeJava(Case request) {
        assertThat(restAssured(request, OAuthSignature.HEADER).headers().get(0).value())
                .isNotEqualTo(scribeJava(request, SignatureType.Header).getHeaders().get("Authorization"));
    }

    private static HttpRequest restAssured(Case request, OAuthSignature signature) {
        RequestBody body = request.form().isEmpty() ? null
                : RequestBody.bytes(formBody(request.form()).getBytes(UTF_8), "application/x-www-form-urlencoded; charset=UTF-8");
        CoreOAuthSigner signer = CoreOAuthSigner.oauth(request.consumerKey(), request.consumerSecret(), request.accessToken(),
                request.tokenSecret(), signature, request.addEmptyTokenToBaseString(),
                Clock.fixed(Instant.ofEpochSecond(TIMESTAMP), ZoneOffset.UTC), () -> NONCE);
        return signer.sign(new HttpRequest(request.method(), URI.create(request.uri()), List.of(), body, RequestOptions.DEFAULT));
    }

    private static String formBody(List<Map.Entry<String, String>> form) {
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> parameter : form) {
            pairs.add(URLEncoder.encode(parameter.getKey(), UTF_8) + "=" + URLEncoder.encode(parameter.getValue(), UTF_8));
        }
        return String.join("&", pairs);
    }

    // Signs as CoreOAuthSigner did with scribejava before 7.0, with the fixed nonce and timestamp
    private static OAuthRequest scribeJava(Case request, SignatureType type) {
        DefaultApi10a api = new DefaultApi10a() {
            @Override
            public String getRequestTokenEndpoint() {
                return null;
            }

            @Override
            public String getAuthorizationUrl(OAuth1RequestToken requestToken) {
                return null;
            }

            @Override
            public String getAccessTokenEndpoint() {
                return null;
            }

            @Override
            public boolean isEmptyOAuthTokenParamIsRequired() {
                return request.addEmptyTokenToBaseString();
            }

            @Override
            public TimestampService getTimestampService() {
                return new TimestampService() {
                    @Override
                    public String getTimestampInSeconds() {
                        return Long.toString(TIMESTAMP);
                    }

                    @Override
                    public String getNonce() {
                        return NONCE;
                    }
                };
            }
        };
        OAuthConfig config = new OAuthConfig(request.consumerKey(), request.consumerSecret(), null, type, null, null, null, null, null, null, null);
        OAuthRequest oauthRequest = new OAuthRequest(Verb.valueOf(request.method().toUpperCase(Locale.ROOT)), request.uri(), null);
        for (Map.Entry<String, String> parameter : request.form()) {
            oauthRequest.addBodyParameter(parameter.getKey(), parameter.getValue());
        }
        new OAuth10aService(api, config).signRequest(new OAuth1AccessToken(request.accessToken(), request.tokenSecret()), oauthRequest);
        return oauthRequest;
    }
}

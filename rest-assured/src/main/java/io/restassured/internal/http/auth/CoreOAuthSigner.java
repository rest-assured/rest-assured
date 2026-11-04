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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.util.SafeExceptionRethrower.safeRethrow;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Signs a request with OAuth 1, or sends an OAuth 2 access token with it, as {@code auth().oauth(..)} and
 * {@code auth().oauth2(..)} ask for. Each hop of a redirect is signed for its own URI.
 * <p>
 * OAuth 1 requests are signed with HMAC-SHA1 (RFC 5849). The <code>Authorization</code> header and the query string are
 * written as scribejava 2.5.3, which REST Assured signed with before 7.0, wrote them.
 * </p>
 */
public final class CoreOAuthSigner {
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    // The methods that scribejava 2.5.3 signed. Requests with other methods are sent unsigned, as before 7.0.
    private static final Set<String> SIGNED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "PATCH");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HEX = "0123456789ABCDEF";

    private final String consumerKey;
    private final String consumerSecret;
    private final String accessToken;
    private final String tokenSecret;
    private final String oauth2AccessToken;
    private final OAuthSignature signature;
    private final boolean addEmptyTokenToBaseString;
    private final Clock clock;
    private final Supplier<String> nonces;

    private CoreOAuthSigner(String consumerKey, String consumerSecret, String accessToken, String tokenSecret,
                            String oauth2AccessToken, OAuthSignature signature, boolean addEmptyTokenToBaseString,
                            Clock clock, Supplier<String> nonces) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.tokenSecret = tokenSecret;
        this.oauth2AccessToken = oauth2AccessToken;
        this.signature = signature;
        this.addEmptyTokenToBaseString = addEmptyTokenToBaseString;
        this.clock = clock;
        this.nonces = nonces;
    }

    /**
     * @param signature                 where the signature goes
     * @param addEmptyTokenToBaseString whether an empty {@code oauth_token} parameter is part of the base string
     * @return a signer that signs with OAuth 1
     */
    public static CoreOAuthSigner oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken,
                                        OAuthSignature signature, boolean addEmptyTokenToBaseString) {
        return oauth(consumerKey, consumerSecret, accessToken, secretToken, signature, addEmptyTokenToBaseString,
                Clock.systemUTC(), () -> Long.toString(RANDOM.nextLong() & Long.MAX_VALUE));
    }

    // With the clock that gives the timestamp and the supplier of nonces, for tests
    static CoreOAuthSigner oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken,
                                 OAuthSignature signature, boolean addEmptyTokenToBaseString, Clock clock, Supplier<String> nonces) {
        notNull(consumerKey, "consumerKey");
        notNull(consumerSecret, "consumerSecret");
        notNull(accessToken, "accessToken");
        notNull(secretToken, "secretToken");
        return new CoreOAuthSigner(consumerKey, consumerSecret, accessToken, secretToken, null, signature,
                addEmptyTokenToBaseString, clock, nonces);
    }

    /**
     * @param signature where the access token goes
     * @return a signer that sends an OAuth 2 access token
     */
    public static CoreOAuthSigner oauth2(String accessToken, OAuthSignature signature) {
        return new CoreOAuthSigner(null, null, null, null, accessToken, signature, false, null, null);
    }

    /**
     * @param request the request to sign
     * @return the signed request
     */
    public HttpRequest sign(HttpRequest request) {
        if (oauth2AccessToken != null) {
            return signOAuth2(request);
        }
        String method = request.method().toUpperCase(Locale.ROOT);
        if (!SIGNED_METHODS.contains(method)) {
            return request;
        }
        // The protocol parameters in the order scribejava wrote them in, which the signature is then added to
        List<Map.Entry<String, String>> oauth = new ArrayList<>();
        oauth.add(Map.entry("oauth_nonce", nonces.get()));
        // Like scribejava, the token is left out only when it and its secret are both empty
        if (addEmptyTokenToBaseString || !accessToken.isEmpty() || !tokenSecret.isEmpty()) {
            oauth.add(Map.entry("oauth_token", accessToken));
        }
        oauth.add(Map.entry("oauth_consumer_key", consumerKey));
        oauth.add(Map.entry("oauth_timestamp", Long.toString(clock.instant().getEpochSecond())));
        oauth.add(Map.entry("oauth_signature_method", "HMAC-SHA1"));
        oauth.add(Map.entry("oauth_version", "1.0"));

        URI uri = request.uri();
        List<Map.Entry<String, String>> parameters = new ArrayList<>(decode(uri.getRawQuery(), UTF_8));
        parameters.addAll(formParameters(request.body()));
        parameters.addAll(oauth);
        oauth.add(1, Map.entry("oauth_signature", hmacSha1(baseString(method, uri, parameters), consumerSecret, tokenSecret)));

        List<String> pairs = new ArrayList<>();
        if (signature == OAuthSignature.HEADER) {
            for (Map.Entry<String, String> parameter : oauth) {
                pairs.add(parameter.getKey() + "=\"" + percentEncode(parameter.getValue()) + "\"");
            }
            List<HttpHeader> headers = new ArrayList<>(request.headers());
            setHeader(headers, "Authorization", "OAuth " + String.join(", ", pairs));
            return new HttpRequest(request.method(), uri, headers, request.body(), request.options());
        }
        for (Map.Entry<String, String> parameter : oauth) {
            pairs.add(percentEncode(parameter.getKey()) + "=" + percentEncode(parameter.getValue()));
        }
        String target = uri.toString();
        int hash = target.indexOf('#');
        String fragment = hash < 0 ? "" : target.substring(hash);
        target = hash < 0 ? target : target.substring(0, hash);
        return withUri(request, target + (target.indexOf('?') < 0 ? "?" : "&") + String.join("&", pairs) + fragment);
    }

    // The signature base string of RFC 5849, section 3.4.1
    static String baseString(String method, URI uri, List<Map.Entry<String, String>> parameters) {
        List<Map.Entry<String, String>> encoded = new ArrayList<>();
        for (Map.Entry<String, String> parameter : parameters) {
            encoded.add(Map.entry(percentEncode(parameter.getKey()), percentEncode(parameter.getValue())));
        }
        encoded.sort(Map.Entry.<String, String>comparingByKey().thenComparing(Map.Entry.comparingByValue()));
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> parameter : encoded) {
            pairs.add(parameter.getKey() + "=" + parameter.getValue());
        }
        return method + "&" + percentEncode(baseStringUri(uri)) + "&" + percentEncode(String.join("&", pairs));
    }

    // RFC 5849, section 3.4.1.2: lower-case scheme and host, no user info or default port, and no query or fragment
    private static String baseStringUri(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String authority = uri.getRawAuthority();
        authority = authority.substring(authority.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        String defaultPort = scheme.equals("http") ? ":80" : scheme.equals("https") ? ":443" : null;
        if (defaultPort != null && authority.endsWith(defaultPort)) {
            authority = authority.substring(0, authority.length() - defaultPort.length());
        }
        String path = uri.getRawPath();
        return scheme + "://" + authority + (path == null || path.isEmpty() ? "/" : path);
    }

    // RFC 5849, section 3.4.2
    static String hmacSha1(String baseString, String consumerSecret, String tokenSecret) {
        String key = percentEncode(consumerSecret) + "&" + percentEncode(tokenSecret);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(baseString.getBytes(UTF_8)));
        } catch (GeneralSecurityException e) {
            return safeRethrow(e);
        }
    }

    // RFC 5849, section 3.6: every byte of the UTF-8 encoding but the unreserved characters of RFC 3986 is encoded
    static String percentEncode(String value) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : value.getBytes(UTF_8)) {
            char c = (char) (b & 0xFF);
            if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-' || c == '.' || c == '_' || c == '~') {
                encoded.append(c);
            } else {
                encoded.append('%').append(HEX.charAt(c >> 4)).append(HEX.charAt(c & 0xF));
            }
        }
        return encoded.toString();
    }

    private HttpRequest signOAuth2(HttpRequest request) {
        if (signature == OAuthSignature.HEADER) {
            List<HttpHeader> headers = new ArrayList<>(request.headers());
            setHeader(headers, "Authorization", "Bearer " + oauth2AccessToken);
            return new HttpRequest(request.method(), request.uri(), headers, request.body(), request.options());
        }
        // The access_token query parameter of RFC 6750, section 2.3
        String uri = request.uri().toString();
        String separator = uri.indexOf('?') < 0 ? "?" : uri.endsWith("?") || uri.endsWith("&") ? "" : "&";
        return withUri(request, uri + separator + "access_token=" + URLEncoder.encode(oauth2AccessToken, UTF_8));
    }

    private static HttpRequest withUri(HttpRequest request, String uri) {
        try {
            return new HttpRequest(request.method(), new URI(uri), request.headers(), request.body(), request.options());
        } catch (URISyntaxException e) {
            return safeRethrow(e);
        }
    }

    // The form parameters of the body, which the signature covers
    private static List<Map.Entry<String, String>> formParameters(RequestBody body) {
        if (body == null || body.getType() != RequestBody.Type.BYTES || body.getContentType() == null
                || !body.getContentType().toLowerCase(Locale.ROOT).startsWith(FORM_CONTENT_TYPE)) {
            return List.of();
        }
        Charset charset = charsetOf(body.getContentType());
        return decode(new String(body.getBytes(), charset), charset);
    }

    // The parameters of a query or form body (RFC 5849, section 3.4.1.3.1)
    private static List<Map.Entry<String, String>> decode(String encoded, Charset charset) {
        if (encoded == null) {
            return List.of();
        }
        List<Map.Entry<String, String>> parameters = new ArrayList<>();
        for (String pair : encoded.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            parameters.add(Map.entry(URLDecoder.decode(name, charset), URLDecoder.decode(value, charset)));
        }
        return parameters;
    }

    private static Charset charsetOf(String contentType) {
        for (String parameter : contentType.split(";")) {
            String trimmed = parameter.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                try {
                    return Charset.forName(trimmed.substring("charset=".length()).replace("\"", "").trim());
                } catch (RuntimeException e) {
                    return UTF_8;
                }
            }
        }
        return UTF_8;
    }

    // Replaces the first header with this name, or adds one
    private static void setHeader(List<HttpHeader> headers, String name, String value) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).name().equalsIgnoreCase(name)) {
                headers.set(i, new HttpHeader(name, value));
                for (int next = headers.size() - 1; next > i; next--) {
                    if (headers.get(next).name().equalsIgnoreCase(name)) {
                        headers.remove(next);
                    }
                }
                return;
            }
        }
        headers.add(new HttpHeader(name, value));
    }
}

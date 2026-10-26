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
import io.restassured.authentication.OAuthSignature;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.RequestBody;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.internal.util.SafeExceptionRethrower.safeRethrow;

/**
 * Signs a request with OAuth 1, or sends an OAuth 2 access token with it, as {@code auth().oauth(..)} and
 * {@code auth().oauth2(..)} ask for. Each hop of a redirect is signed for its own URI.
 */
public final class CoreOAuthSigner {
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final OAuthConfig oauthConfig;
    private final OAuth1AccessToken accessToken;
    private final String oauth2AccessToken;
    private final OAuthSignature signature;
    private final boolean addEmptyTokenToBaseString;

    private CoreOAuthSigner(OAuthConfig oauthConfig, OAuth1AccessToken accessToken, String oauth2AccessToken,
                            OAuthSignature signature, boolean addEmptyTokenToBaseString) {
        this.oauthConfig = oauthConfig;
        this.accessToken = accessToken;
        this.oauth2AccessToken = oauth2AccessToken;
        this.signature = signature;
        this.addEmptyTokenToBaseString = addEmptyTokenToBaseString;
    }

    /**
     * @param signature                 where the signature goes
     * @param addEmptyTokenToBaseString whether an empty {@code oauth_token} parameter is part of the base string
     * @return a signer that signs with OAuth 1
     */
    public static CoreOAuthSigner oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken,
                                        OAuthSignature signature, boolean addEmptyTokenToBaseString) {
        SignatureType type = signature == OAuthSignature.HEADER ? SignatureType.Header : SignatureType.QueryString;
        OAuthConfig oauthConfig = new OAuthConfig(consumerKey, consumerSecret, null, type, null, null, null, null, null, null, null);
        return new CoreOAuthSigner(oauthConfig, new OAuth1AccessToken(accessToken, secretToken), null, signature, addEmptyTokenToBaseString);
    }

    /**
     * @param signature where the access token goes
     * @return a signer that sends an OAuth 2 access token
     */
    public static CoreOAuthSigner oauth2(String accessToken, OAuthSignature signature) {
        return new CoreOAuthSigner(null, null, accessToken, signature, false);
    }

    /**
     * @param request the request to sign
     * @return the signed request
     */
    public HttpRequest sign(HttpRequest request) {
        if (oauth2AccessToken != null) {
            return signOAuth2(request);
        }
        Verb verb = verbOf(request.method());
        if (verb == null) {
            return request;
        }
        OAuthRequest oauthRequest = new OAuthRequest(verb, request.uri().toString(), null);
        for (Map.Entry<String, String> parameter : formParameters(request.body())) {
            oauthRequest.addBodyParameter(parameter.getKey(), parameter.getValue());
        }
        service().signRequest(accessToken, oauthRequest);
        if (signature == OAuthSignature.HEADER) {
            List<HttpHeader> headers = new ArrayList<>(request.headers());
            for (Map.Entry<String, String> header : oauthRequest.getHeaders().entrySet()) {
                setHeader(headers, header.getKey(), header.getValue());
            }
            return new HttpRequest(request.method(), request.uri(), headers, request.body(), request.options());
        }
        return withUri(request, oauthRequest.getCompleteUrl());
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
        return withUri(request, uri + separator + "access_token=" + URLEncoder.encode(oauth2AccessToken, StandardCharsets.UTF_8));
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
        List<Map.Entry<String, String>> parameters = new ArrayList<>();
        for (String pair : new String(body.getBytes(), charset).split("&")) {
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
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static Verb verbOf(String method) {
        try {
            return Verb.valueOf(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private OAuth10aService service() {
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
                return addEmptyTokenToBaseString;
            }
        };
        return new OAuth10aService(api, oauthConfig);
    }
}

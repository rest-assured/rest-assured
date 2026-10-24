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

import io.restassured.common.spi.http.HttpClientInstance;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.internal.http.Origin;
import io.restassured.internal.http.auth.ChallengeHandler;
import io.restassured.internal.http.auth.CoreOAuthSigner;
import io.restassured.internal.http.cookie.KeptCookies;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends one request of REST Assured: it follows the redirects, answers the authentication challenges, keeps the
 * cookies of the responses and signs every hop, so that a filter sees one request and one response however many were
 * sent. Each hop:
 * <ol>
 * <li>sends the headers of the original request, with the cookies that earlier responses set added in a second
 * {@code Cookie} header, the credentials of an answered challenge, and the OAuth signature of the hop's own URI;</li>
 * <li>drops {@code Authorization} and {@code Cookie}, and isn't signed, if it's a redirect to another scheme, host or
 * port than the original request, or to a host that can't be read, and
 * {@code RedirectConfig.stripSensitiveHeadersOnCrossHostRedirect} is set, which it is by default;</li>
 * <li>keeps the cookies the response set, answers a {@code 401} challenge by sending the hop again, unless its body
 * can't be sent again, and then follows a redirect.</li>
 * </ol>
 */
public final class Exchange {
    private static final String AUTHORIZATION = "Authorization";
    private static final String COOKIE = "Cookie";
    private static final String USER_AGENT = "User-Agent";

    private final HttpClientInstance transport;
    private final RedirectSettings redirects;
    private final ChallengeHandler challenges;
    private final CoreOAuthSigner signer;
    private final KeptCookies cookies;
    private final String defaultUserAgent;

    /**
     * @param transport        the client that sends the requests
     * @param redirects        how redirects are followed
     * @param challenges       what answers a {@code 401}, or {@code null} if nothing does
     * @param signer           what signs the requests, or {@code null} if they aren't signed
     * @param cookies          the cookies of the responses that are kept, or {@code null} if they aren't kept
     * @param defaultUserAgent the {@code User-Agent} of a request that doesn't have one, or {@code null}
     */
    public Exchange(HttpClientInstance transport, RedirectSettings redirects, ChallengeHandler challenges,
                    CoreOAuthSigner signer, KeptCookies cookies, String defaultUserAgent) {
        this.transport = transport;
        this.redirects = redirects;
        this.challenges = challenges;
        this.signer = signer;
        this.cookies = cookies;
        this.defaultUserAgent = defaultUserAgent;
    }

    /**
     * The response of a request, and the URI it came from.
     *
     * @param response     the response of the last hop
     * @param effectiveUri the URI of the last hop, which is where the request was redirected to
     */
    public record Exchanged(HttpResponse response, URI effectiveUri) {
    }

    /**
     * @param request the request, with the headers that every hop sends
     * @return the response of the last hop
     * @throws io.restassured.common.exception.TooManyRedirectsException if the request is redirected too often
     * @throws java.net.ProtocolException                                if a redirect is circular, or relative and
     *                                                                  relative redirects are rejected
     */
    public Exchanged send(HttpRequest request) throws IOException {
        Origin origin = Origin.of(request.uri());
        List<HttpHeader> headers = request.headers();
        List<URI> visited = new ArrayList<>();
        HttpRequest hop = request;
        int redirectCount = 0;
        while (true) {
            boolean crossHost = redirectCount > 0 && !origin.isSameAs(Origin.of(hop.uri()));
            HttpResponse response = send(hop, crossHost);
            // A body that has been sent and can't be sent again isn't resent, so the 401 is the response
            if (challenges != null && !crossHost && challenges.appliesTo(hop.uri()) && (hop.body() == null || hop.body().isRepeatable())) {
                for (int answers = 0; answers < ChallengeHandler.MAX_ANSWERS_PER_HOP && challenges.handle(response); answers++) {
                    close(response);
                    response = send(hop, crossHost);
                }
            }
            if (!redirects.follow() || !Redirects.isRedirected(hop.method(), response, redirects)
                    || Redirects.resendsBody(hop, response.statusCode()) && !hop.body().isRepeatable()) {
                // A body that has been sent and can't be sent again isn't resent, so that redirect isn't followed
                return new Exchanged(response, hop.uri());
            }
            try {
                Redirects.checkCount(redirectCount, redirects);
                redirectCount++;
                URI location = Redirects.withoutFragment(Redirects.location(hop.uri(), response, redirects, visited));
                hop = Redirects.followUp(hop, response.statusCode(), location, headers);
            } finally {
                close(response);
            }
        }
    }

    private HttpResponse send(HttpRequest hop, boolean crossHost) throws IOException {
        boolean strip = crossHost && redirects.stripSensitiveHeaders();
        List<HttpHeader> headers = new ArrayList<>(hop.headers());
        if (defaultUserAgent != null && !hasHeader(headers, USER_AGENT)) {
            headers.add(new HttpHeader(USER_AGENT, defaultUserAgent));
        }
        if (cookies != null) {
            String cookieHeader = cookies.cookieHeader(hop.uri());
            if (cookieHeader != null) {
                headers.add(new HttpHeader(COOKIE, cookieHeader));
            }
        }
        if (challenges != null && !crossHost && challenges.appliesTo(hop.uri()) && !hasHeader(headers, AUTHORIZATION)) {
            String authorization = challenges.authorization(hop.method(), hop.uri(), hop.body());
            if (authorization != null) {
                headers.add(new HttpHeader(AUTHORIZATION, authorization));
            }
        }
        HttpRequest request = new HttpRequest(hop.method(), hop.uri(), headers, hop.body(), hop.options());
        if (signer != null && !strip) {
            request = signer.sign(request);
        }
        if (strip) {
            List<HttpHeader> stripped = new ArrayList<>(request.headers());
            stripped.removeIf(header -> header.name().equalsIgnoreCase(AUTHORIZATION) || header.name().equalsIgnoreCase(COOKIE));
            request = new HttpRequest(request.method(), request.uri(), stripped, request.body(), request.options());
        }
        HttpResponse response = transport.send(request);
        if (cookies != null) {
            try {
                cookies.store(response, hop.uri());
            } catch (RuntimeException e) {
                close(response);
                throw e;
            }
        }
        return response;
    }

    private static boolean hasHeader(List<HttpHeader> headers, String name) {
        return headers.stream().anyMatch(header -> header.name().equalsIgnoreCase(name));
    }

    // Closes the response also if closing its body fails
    private static void close(HttpResponse response) {
        try (response) {
            if (response.body() != null) {
                response.body().close();
            }
        } catch (IOException e) {
            // The response is on its way out, so nothing can be done about it
        }
    }
}

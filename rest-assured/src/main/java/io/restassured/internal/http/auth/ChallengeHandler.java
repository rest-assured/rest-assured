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

import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.RequestBody;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Answers the {@code 401} challenges of the host the credentials of {@code auth().basic(..)} or
 * {@code auth().digest(..)} are for, as REST Assured has always answered them: a challenge is answered once, so a
 * second {@code 401} is the response of the request, unless it says that the Digest nonce is stale. Digest is
 * answered when the server offers it and REST Assured can answer it, and Basic otherwise, and once a challenge has
 * been answered the credentials are sent with every request to that host.
 */
public final class ChallengeHandler {
    private static final List<String> PREFERRED_SCHEMES = List.of("digest", "basic");

    /**
     * How often one hop may answer a challenge: once per authentication scheme that REST Assured answers, or for a
     * stale Digest nonce.
     */
    public static final int MAX_ANSWERS_PER_HOP = PREFERRED_SCHEMES.size();

    private enum State {
        /**
         * No challenge has been answered.
         */
        UNCHALLENGED,
        /**
         * A challenge has been answered, and the response to the answer hasn't arrived.
         */
        ANSWERED,
        /**
         * A challenge has been answered and accepted.
         */
        AUTHENTICATED,
        /**
         * The server didn't accept the answer to its challenge.
         */
        FAILED
    }

    private final Credentials credentials;
    private State state = State.UNCHALLENGED;
    private String scheme;
    private DigestAuth digest;
    private String basic;

    /**
     * @param credentials the credentials to answer challenges with
     */
    public ChallengeHandler(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * @return whether the credentials are sent to this URI at all
     */
    public boolean appliesTo(URI uri) {
        return credentials.appliesTo(uri);
    }

    /**
     * @param method the method of the request that is about to be sent
     * @param uri    the URI of the request that is about to be sent
     * @param body   the body of the request that is about to be sent, or {@code null}
     * @return the value of its {@code Authorization} header, or {@code null} if no challenge has been answered yet, or
     * the Digest answer would have to cover a body that can't be read again
     */
    public String authorization(String method, URI uri, RequestBody body) {
        if (state != State.ANSWERED && state != State.AUTHENTICATED) {
            return null;
        }
        return "digest".equals(scheme) ? digest.authorization(method, target(uri), body) : basic;
    }

    /**
     * Takes the response of a request that the credentials apply to.
     *
     * @return whether the request is sent again, with an answer to a challenge of the response
     */
    public boolean handle(HttpResponse response) {
        if (response.statusCode() != 401) {
            if (state == State.ANSWERED) {
                state = State.AUTHENTICATED;
            }
            return false;
        }
        if (state == State.FAILED) {
            return false;
        }
        Map<String, AuthChallenge> challenges = AuthChallenge.of(response.headers());
        if (challenges.isEmpty()) {
            return false;
        }
        if (state == State.AUTHENTICATED) {
            // The server no longer accepts the credentials it accepted before, so they are offered again
            state = State.UNCHALLENGED;
            scheme = null;
        }
        if (scheme != null) {
            AuthChallenge again = challenges.get(scheme);
            if (again != null) {
                // The answer wasn't accepted, unless the server only wants it for a new Digest nonce
                boolean stale = scheme.equals("digest") && "true".equalsIgnoreCase(again.parameter("stale"));
                if (stale && answer(scheme, again)) {
                    return true;
                }
                state = State.FAILED;
                return false;
            }
            scheme = null;
        }
        for (String preferred : PREFERRED_SCHEMES) {
            AuthChallenge challenge = challenges.get(preferred);
            if (challenge != null && credentials.supports(preferred) && answer(preferred, challenge)) {
                return true;
            }
        }
        return false;
    }

    // Returns false if REST Assured can't answer the challenge
    private boolean answer(String scheme, AuthChallenge challenge) {
        if (scheme.equals("digest")) {
            DigestAuth answer = DigestAuth.of(credentials, challenge);
            if (answer == null) {
                return false;
            }
            digest = answer;
        } else {
            basic = basicAuthorization();
        }
        this.scheme = scheme;
        state = State.ANSWERED;
        return true;
    }

    // The credentials are read as US-ASCII, as REST Assured has always read them, so other characters become '?'
    private String basicAuthorization() {
        String userNameAndPassword = credentials.userName() + ":" + credentials.password();
        return "Basic " + Base64.getEncoder().encodeToString(userNameAndPassword.getBytes(StandardCharsets.US_ASCII));
    }

    // The request target, which the Digest response covers: the path and the query
    private static String target(URI uri) {
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }
}

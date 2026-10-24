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

import io.restassured.common.spi.http.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;

/**
 * Answers a Digest challenge as REST Assured has always answered it, following RFC 2617 with the {@code MD5} and
 * {@code MD5-sess} algorithms and the {@code auth} and {@code auth-int} quality of protection: the digest covers the
 * method and the request target including its query, with a client nonce and a nonce count that counts the answers to
 * the challenge. The credentials are read as US-ASCII, so other characters become {@code ?}.
 */
final class DigestAuth {
    private static final Charset CREDENTIALS_CHARSET = StandardCharsets.US_ASCII;

    private final Credentials credentials;
    private final String realm;
    private final String nonce;
    private final String algorithm;
    private final String opaque;
    // The quality of protection options the server offers, or null if it offers none
    private final Set<String> qualityOfProtection;
    private final String clientNonce;
    private long nonceCount;

    private DigestAuth(Credentials credentials, AuthChallenge challenge, String algorithm, Set<String> qualityOfProtection,
                       String clientNonce) {
        this.credentials = credentials;
        this.realm = challenge.parameter("realm");
        this.nonce = challenge.parameter("nonce");
        this.opaque = challenge.parameter("opaque");
        this.algorithm = algorithm;
        this.qualityOfProtection = qualityOfProtection;
        this.clientNonce = clientNonce;
    }

    /**
     * @return what answers the challenge, or {@code null} if REST Assured can't answer it, because it has no realm or
     * no nonce, or asks for an algorithm or offers only quality of protection options that REST Assured doesn't know
     */
    static DigestAuth of(Credentials credentials, AuthChallenge challenge) {
        return of(credentials, challenge, DigestAuth::createClientNonce);
    }

    /**
     * @param clientNonces the client nonces to use, which a test fixes
     */
    static DigestAuth of(Credentials credentials, AuthChallenge challenge, Supplier<String> clientNonces) {
        String algorithm = challenge.parameter("algorithm") == null ? "MD5" : challenge.parameter("algorithm");
        Set<String> qualityOfProtection = options(challenge.parameter("qop"));
        if (challenge.parameter("realm") == null || challenge.parameter("nonce") == null || messageDigest(algorithm) == null
                || qualityOfProtection != null && !qualityOfProtection.contains("auth") && !qualityOfProtection.contains("auth-int")) {
            return null;
        }
        return new DigestAuth(credentials, challenge, algorithm, qualityOfProtection, clientNonces.get());
    }

    /**
     * @param method the method of the request
     * @param target the request target, the path and the query of the URI
     * @param body   the body of the request, or {@code null} if it has none
     * @return the value of the {@code Authorization} header, or {@code null} if the server only offers
     * {@code auth-int} and REST Assured can't read the body more than once
     */
    String authorization(String method, String target, RequestBody body) {
        String qop = qualityOfProtection(body);
        if (qualityOfProtection != null && qop == null) {
            return null;
        }
        MessageDigest digest = messageDigest(algorithm);
        nonceCount++;
        String nonceCountValue = String.format(Locale.US, "%08x", nonceCount);

        String a1 = credentials.userName() + ":" + realm + ":" + credentials.password();
        if (algorithm.equalsIgnoreCase("MD5-sess")) {
            a1 = hash(digest, a1) + ":" + nonce + ":" + clientNonce;
        }
        String a2 = method + ":" + target;
        if ("auth-int".equals(qop)) {
            a2 = a2 + ":" + hash(digest, bytesOf(body));
        }
        String hashA1 = hash(digest, a1);
        String hashA2 = hash(digest, a2);
        String response = qop == null
                ? hash(digest, hashA1 + ":" + nonce + ":" + hashA2)
                : hash(digest, hashA1 + ":" + nonce + ":" + nonceCountValue + ":" + clientNonce + ":" + qop + ":" + hashA2);

        StringJoiner parameters = new StringJoiner(", ");
        parameters.add(quoted("username", credentials.userName()));
        parameters.add(quoted("realm", realm));
        parameters.add(quoted("nonce", nonce));
        parameters.add(quoted("uri", target));
        parameters.add(quoted("response", response));
        if (qop != null) {
            parameters.add("qop=" + qop);
            parameters.add("nc=" + nonceCountValue);
            parameters.add(quoted("cnonce", clientNonce));
        }
        parameters.add("algorithm=" + algorithm);
        if (opaque != null) {
            parameters.add(quoted("opaque", opaque));
        }
        return "Digest " + parameters;
    }

    // auth-int when the server offers it and the body can be read again, or there is no body and auth isn't offered,
    // else auth, or null when the server offers no quality of protection or none that fits
    private String qualityOfProtection(RequestBody body) {
        if (qualityOfProtection == null) {
            return null;
        }
        boolean auth = qualityOfProtection.contains("auth");
        if (qualityOfProtection.contains("auth-int") && (body == null ? !auth : body.isRepeatable())) {
            return "auth-int";
        }
        return auth ? "auth" : null;
    }

    private static Set<String> options(String offered) {
        if (offered == null) {
            return null;
        }
        Set<String> options = new HashSet<>();
        for (String option : offered.split(",")) {
            options.add(option.trim().toLowerCase(Locale.ROOT));
        }
        return options;
    }

    // The body of a request that auth-int covers, which is one that can be read again, or none
    private static byte[] bytesOf(RequestBody body) {
        if (body == null) {
            return new byte[0];
        }
        try {
            if (body.getType() == RequestBody.Type.FILE) {
                return Files.readAllBytes(body.getFile().toPath());
            }
            if (body.getType() == RequestBody.Type.WRITER) {
                ByteArrayOutputStream written = new ByteArrayOutputStream();
                body.getWriter().writeTo(written);
                return written.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Reading the request body to answer a Digest challenge failed", e);
        }
        return body.getBytes();
    }

    private static String quoted(String name, String value) {
        return name + "=\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String hash(MessageDigest digest, String value) {
        return hash(digest, value.getBytes(CREDENTIALS_CHARSET));
    }

    private static String hash(MessageDigest digest, byte[] value) {
        return hexadecimal(digest.digest(value));
    }

    // The digest of the algorithm, or null if this Java doesn't have it
    private static MessageDigest messageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm.equalsIgnoreCase("MD5-sess") ? "MD5" : algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String createClientNonce() {
        byte[] nonce = new byte[8];
        new SecureRandom().nextBytes(nonce);
        return hexadecimal(nonce);
    }

    private static String hexadecimal(byte[] bytes) {
        StringBuilder hexadecimal = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hexadecimal.append(Character.forDigit((value & 0xf0) >> 4, 16)).append(Character.forDigit(value & 0x0f, 16));
        }
        return hexadecimal.toString();
    }
}

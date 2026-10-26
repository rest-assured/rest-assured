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

import io.restassured.internal.http.Origin;

import java.net.URI;
import java.util.Locale;

/**
 * The user name and password that answer a challenge of one host and port, with the authentication schemes they may
 * be sent with: {@code auth().basic(..)} answers Basic and Digest, {@code auth().digest(..)} only Digest, so that the
 * password is never sent in clear text to a server that asks for Basic authentication.
 *
 * @param userName   the user name
 * @param password   the password
 * @param host       the host the credentials are for, or {@code null} if it can't be read, in which case they are
 *                   sent nowhere
 * @param port       the port the credentials are for, or {@code -1} for every port of the host
 * @param digestOnly whether the credentials only answer a Digest challenge
 */
public record Credentials(String userName, String password, String host, int port, boolean digestOnly) {

    /**
     * @param uri the URI of the request the credentials were set for
     * @return credentials that answer a Basic or a Digest challenge
     */
    public static Credentials basic(String userName, String password, URI uri) {
        return new Credentials(userName, password, Origin.host(uri), Origin.port(uri), false);
    }

    /**
     * @param uri the URI of the request the credentials were set for
     * @return credentials that only answer a Digest challenge
     */
    public static Credentials digest(String userName, String password, URI uri) {
        return new Credentials(userName, password, Origin.host(uri), Origin.port(uri), true);
    }

    /**
     * @return whether the credentials are sent to the host and port of this URI
     */
    public boolean appliesTo(URI uri) {
        Origin origin = Origin.of(uri);
        return host != null && host.equalsIgnoreCase(origin.host()) && (port < 0 || port == origin.port());
    }

    /**
     * @return whether the credentials may answer a challenge of this authentication scheme
     */
    public boolean supports(String authenticationScheme) {
        String name = authenticationScheme.toLowerCase(Locale.ROOT);
        return name.equals("digest") || !digestOnly && name.equals("basic");
    }
}

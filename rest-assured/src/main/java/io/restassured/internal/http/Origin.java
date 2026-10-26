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

package io.restassured.internal.http;

import java.net.URI;
import java.util.Locale;

/**
 * Where a request goes: the scheme, the host and the port of its URI, with the default port of the scheme filled in.
 * <p>
 * {@link URI#getHost()} is {@code null} for a host that isn't a valid domain name, such as {@code my_service}, so the
 * host and the port are read from the authority of the URI then. A URI whose host can't be read at all has no origin
 * in common with any other, so that nothing meant for one host is ever sent to another.
 *
 * @param scheme the scheme, in lower case, or {@code null}
 * @param host   the host, in lower case, or {@code null} if it can't be read
 * @param port   the port, or the default port of the scheme, or {@code -1} if neither is known
 */
public record Origin(String scheme, String host, int port) {

    /**
     * @return the origin of a request to this URI
     */
    public static Origin of(URI uri) {
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        int port = port(uri);
        if (port < 0) {
            port = "https".equals(scheme) ? 443 : "http".equals(scheme) ? 80 : -1;
        }
        return new Origin(scheme, host(uri), port);
    }

    /**
     * @return whether a request to this origin goes to the same scheme, host and port as one to the other, which it
     * never does when the host of either is unknown
     */
    public boolean isSameAs(Origin other) {
        return host != null && equals(other);
    }

    /**
     * @return the host of the URI in lower case, or {@code null} if it has none
     */
    public static String host(URI uri) {
        if (uri.getHost() != null) {
            return uri.getHost().toLowerCase(Locale.ROOT);
        }
        String hostAndPort = hostAndPort(uri);
        if (hostAndPort == null) {
            return null;
        }
        int portSeparator = portSeparator(hostAndPort);
        String host = portSeparator < 0 ? hostAndPort : hostAndPort.substring(0, portSeparator);
        return host.isEmpty() ? null : host.toLowerCase(Locale.ROOT);
    }

    /**
     * @return the port of the URI, or {@code -1} if it has none
     */
    public static int port(URI uri) {
        if (uri.getPort() >= 0 || uri.getHost() != null) {
            return uri.getPort();
        }
        String hostAndPort = hostAndPort(uri);
        int portSeparator = hostAndPort == null ? -1 : portSeparator(hostAndPort);
        return portSeparator < 0 ? -1 : Integer.parseInt(hostAndPort.substring(portSeparator + 1));
    }

    // The authority without the user information
    private static String hostAndPort(URI uri) {
        String authority = uri.getRawAuthority();
        return authority == null ? null : authority.substring(authority.lastIndexOf('@') + 1);
    }

    // The index of the colon that a port follows, or -1 if there is no port
    private static int portSeparator(String hostAndPort) {
        int colon = hostAndPort.lastIndexOf(':');
        return colon >= 0 && hostAndPort.substring(colon + 1).matches("[0-9]{1,5}") ? colon : -1;
    }
}

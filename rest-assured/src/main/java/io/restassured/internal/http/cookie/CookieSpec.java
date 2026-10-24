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

package io.restassured.internal.http.cookie;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * How {@code Set-Cookie} headers are read and which requests the cookies they set are sent in.
 */
public interface CookieSpec {

    /**
     * @param header the value of a {@code Set-Cookie} header
     * @param origin where the response that set the cookie came from
     * @return the cookie, or {@code null} if the header can't be read or the cookie isn't allowed for the origin
     */
    StoredCookie parse(String header, CookieOrigin origin);

    /**
     * @return whether a request to the origin sends the cookie, which a request whose host is unknown never does
     */
    default boolean matches(StoredCookie cookie, CookieOrigin origin) {
        return origin.host() != null
                && pathMatches(origin.path(), cookie.getPath())
                && domainMatches(cookie, origin)
                && (!cookie.isSecure() || origin.secure());
    }

    /**
     * @return a cookie of the name and value, with the path and the domain of the origin, or {@code null} if the
     * origin's host is unknown or the name is empty
     */
    static StoredCookie cookieOf(String name, String value, CookieOrigin origin) {
        if (origin.host() == null || name.isEmpty()) {
            return null;
        }
        StoredCookie cookie = new StoredCookie(name, value);
        cookie.setPath(defaultPath(origin));
        cookie.setDomain(origin.host());
        return cookie;
    }

    /**
     * Applies an attribute that every spec reads the same way: {@code Path}, {@code Domain}, {@code Secure} and
     * {@code Expires}. Any other attribute is ignored.
     *
     * @param value        the value of the attribute, or {@code null} if it has none
     * @param datePatterns the patterns to read an {@code Expires} attribute with
     * @return {@code false} if the attribute makes the cookie invalid
     */
    static boolean apply(StoredCookie cookie, String attribute, String value, List<String> datePatterns) {
        switch (attribute) {
            case "path":
                cookie.setPath(value == null || value.isBlank() ? "/" : value);
                return true;
            case "domain":
                if (value == null || value.isBlank()) {
                    return false;
                }
                if (!value.endsWith(".")) {
                    // A domain attribute that ends with a dot is ignored, see RFC 6265, 4.1.2.3
                    cookie.setDomainAttribute(value.startsWith(".") ? value.substring(1) : value);
                }
                return true;
            case "secure":
                cookie.setSecure(true);
                return true;
            case "expires":
                Date expires = CookieDates.parse(value, datePatterns);
                if (expires == null) {
                    return false;
                }
                cookie.setExpiryDate(expires);
                return true;
            default:
                return true;
        }
    }

    /**
     * @return the path a cookie without a {@code Path} attribute gets: the directory of the request's path
     */
    static String defaultPath(CookieOrigin origin) {
        String path = origin.path();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            return path;
        }
        return path.substring(0, lastSlash == 0 ? 1 : lastSlash);
    }

    /**
     * @return whether a request to <code>uriPath</code> sends a cookie of <code>cookiePath</code>
     */
    static boolean pathMatches(String uriPath, String cookiePath) {
        String path = cookiePath == null ? "/" : cookiePath;
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return uriPath.startsWith(path)
                && (path.equals("/") || uriPath.length() == path.length() || uriPath.charAt(path.length()) == '/');
    }

    /**
     * @return whether <code>host</code> is <code>domain</code> or a subdomain of it, which an IP address never is
     */
    static boolean domainMatches(String domain, String host) {
        if (domain == null || host.isEmpty() || isIpAddress(host)) {
            return false;
        }
        String name = domain.startsWith(".") ? domain.substring(1) : domain;
        if (!host.endsWith(name)) {
            return false;
        }
        int prefix = host.length() - name.length();
        return prefix == 0 || prefix > 1 && host.charAt(prefix - 1) == '.';
    }

    /**
     * @return whether a request to the origin sends a cookie of the domain, which it does when the domain is the
     * host of the request, or a domain of the host when the cookie has a {@code Domain} attribute
     */
    static boolean domainMatches(StoredCookie cookie, CookieOrigin origin) {
        String domain = cookie.getDomain();
        if (domain == null) {
            return false;
        }
        if (domain.startsWith(".")) {
            domain = domain.substring(1);
        }
        return origin.host().equals(domain.toLowerCase(Locale.ROOT))
                || cookie.hasDomainAttribute() && domainMatches(domain, origin.host());
    }

    private static boolean isIpAddress(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                if (part.charAt(i) < '0' || part.charAt(i) > '9') {
                    return false;
                }
            }
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }
}

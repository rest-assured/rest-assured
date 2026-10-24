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

import io.restassured.common.exception.TooManyRedirectsException;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Which responses are redirects, where they lead and what the request that follows one looks like, as REST Assured
 * has always decided it:
 * <ul>
 * <li>A {@code 303} is always followed, a {@code 301}, {@code 302}, {@code 307} or {@code 308} only for GET and HEAD,
 * or for every method with {@code RedirectConfig.followRedirectsForAllMethods(true)}.</li>
 * <li>A HEAD stays a HEAD and a GET a GET, a {@code 307} and a {@code 308} keep the method and the body, if there is
 * one, and everything else becomes a GET without a body, which still sends the headers of the original request.</li>
 * <li>A relative {@code Location} is resolved against the URI of the request, a {@code Location} that is only a query
 * replaces the query and keeps the path, and the fragment of a {@code Location} isn't sent.</li>
 * </ul>
 */
public final class Redirects {
    private static final String GET = "GET";
    private static final String HEAD = "HEAD";

    private Redirects() {
    }

    /**
     * @param method   the method of the request that got the response
     * @param response the response
     * @param settings how redirects are followed
     * @return whether the response is a redirect that is followed
     */
    public static boolean isRedirected(String method, HttpResponse response, RedirectSettings settings) {
        boolean redirectable = settings.followAllMethods() || GET.equalsIgnoreCase(method) || HEAD.equalsIgnoreCase(method);
        switch (response.statusCode()) {
            case 302:
                return redirectable && response.firstHeader("Location") != null;
            case 301:
            case 307:
            case 308:
                return redirectable;
            case 303:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param request    the request that got the response
     * @param statusCode the status code of the response
     * @param uri        where the response leads
     * @param headers    the headers of the original request, which the follow-up sends again
     * @return the request that follows the redirect
     */
    public static HttpRequest followUp(HttpRequest request, int statusCode, URI uri, List<HttpHeader> headers) {
        if (keepsMethod(request.method(), statusCode)) {
            return new HttpRequest(request.method(), uri, headers, request.body(), request.options());
        }
        return new HttpRequest(HEAD.equalsIgnoreCase(request.method()) ? HEAD : GET, uri, headers, null, request.options());
    }

    private static boolean keepsMethod(String method, int statusCode) {
        return (statusCode == 307 || statusCode == 308) && !HEAD.equalsIgnoreCase(method) && !GET.equalsIgnoreCase(method);
    }

    /**
     * @param request    the request that got the response
     * @param statusCode the status code of the response
     * @return whether the request that follows the redirect sends the body of this request again
     */
    public static boolean resendsBody(HttpRequest request, int statusCode) {
        return request.body() != null && keepsMethod(request.method(), statusCode);
    }

    /**
     * @param requestUri the URI of the request that got the response
     * @param response   the redirect response
     * @param settings   how redirects are followed
     * @param visited    the URIs the request has been redirected to so far, which this call adds to
     * @return where the response leads
     * @throws ProtocolException if the response has no {@code Location}, its {@code Location} isn't a URI, it's
     *                           relative and relative redirects are rejected, or it leads where the request has
     *                           already been and circular redirects aren't allowed
     */
    public static URI location(URI requestUri, HttpResponse response, RedirectSettings settings, List<URI> visited) throws ProtocolException {
        String location = response.firstHeader("Location");
        if (location == null) {
            throw new ProtocolException("Received redirect response " + statusLine(response) + " but no location header");
        }
        URI uri;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            throw new ProtocolException("Invalid redirect URI: " + location);
        }
        try {
            uri = normalize(uri);
            if (!uri.isAbsolute()) {
                if (settings.rejectRelative()) {
                    throw new ProtocolException("Relative redirect location '" + uri + "' not allowed");
                }
                uri = resolve(requestUri, uri);
            }
        } catch (URISyntaxException e) {
            throw new ProtocolException(e.getMessage());
        }
        if (!settings.allowCircular() && visited.contains(uri)) {
            throw new ProtocolException("Circular redirect to '" + uri + "'");
        }
        visited.add(uri);
        return uri;
    }

    /**
     * @throws TooManyRedirectsException if the request may not be redirected again
     */
    public static void checkCount(int redirectCount, RedirectSettings settings) throws TooManyRedirectsException {
        if (redirectCount >= settings.max()) {
            throw new TooManyRedirectsException("Maximum redirects (" + settings.max() + ") exceeded");
        }
    }

    /**
     * @return the URI without its fragment, which isn't sent to the server
     */
    public static URI withoutFragment(URI uri) {
        if (uri.getRawFragment() == null) {
            return uri;
        }
        String withFragment = uri.toString();
        return URI.create(withFragment.substring(0, withFragment.length() - uri.getRawFragment().length() - 1));
    }

    // Removes the . and .. segments of the path and lower-cases the scheme and the host, of a URI with an authority.
    // It works on the encoded parts of the URI, so that the escaped characters of the server's Location are sent as
    // the server wrote them.
    private static URI normalize(URI uri) throws URISyntaxException {
        if (uri.isOpaque() || uri.getRawAuthority() == null) {
            return uri;
        }
        Deque<String> segments = new ArrayDeque<>();
        for (String segment : rawPathSegments(uri)) {
            if (segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                segments.pollLast();
            } else {
                segments.addLast(segment);
            }
        }
        StringBuilder path = new StringBuilder();
        if (segments.isEmpty()) {
            path.append('/');
        }
        for (String segment : segments) {
            path.append('/').append(rawNonAsciiReplaced(segment));
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT) + ":";
        return new URI(scheme + "//" + lowerCaseHost(uri.getRawAuthority()) + path
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment()));
    }

    // A character of the path that a server sent raw, without percent-encoding it, is replaced, as REST Assured has
    // always replaced it: the characters are narrowed to bytes and read as UTF-8, so a byte that isn't UTF-8 becomes
    // U+FFFD, and the result is percent-encoded as UTF-8.
    private static String rawNonAsciiReplaced(String segment) {
        boolean rawCharacter = false;
        for (int i = 0; i < segment.length(); i++) {
            rawCharacter |= segment.charAt(i) > 127;
        }
        if (!rawCharacter) {
            return segment;
        }
        StringBuilder replaced = new StringBuilder(segment.length());
        int at = 0;
        while (at < segment.length()) {
            if (segment.charAt(at) <= 127) {
                replaced.append(segment.charAt(at++));
                continue;
            }
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            while (at < segment.length() && segment.charAt(at) > 127) {
                raw.write(segment.charAt(at++));
            }
            for (byte encoded : new String(raw.toByteArray(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)) {
                replaced.append('%').append(Character.toUpperCase(Character.forDigit((encoded & 0xf0) >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(encoded & 0x0f, 16)));
            }
        }
        return replaced.toString();
    }

    // The host of an authority is case insensitive, its user information isn't
    private static String lowerCaseHost(String authority) {
        int userInfo = authority.lastIndexOf('@');
        return authority.substring(0, userInfo + 1) + authority.substring(userInfo + 1).toLowerCase(Locale.ROOT);
    }

    private static List<String> rawPathSegments(URI uri) {
        List<String> segments = new ArrayList<>();
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        int at = path.startsWith("/") ? 1 : 0;
        while (!path.isEmpty() && at <= path.length()) {
            int next = path.indexOf('/', at);
            if (next < 0) {
                segments.add(path.substring(at));
                break;
            }
            segments.add(path.substring(at, next));
            at = next + 1;
        }
        return segments;
    }

    // Resolves a relative location against the URI of the request, as REST Assured has always resolved it
    private static URI resolve(URI requestUri, URI location) throws URISyntaxException {
        String reference = location.toASCIIString();
        if (reference.startsWith("?")) {
            String base = requestUri.toASCIIString();
            int query = base.indexOf('?');
            return URI.create((query < 0 ? base : base.substring(0, query)) + reference);
        }
        if (reference.isEmpty()) {
            String resolved = requestUri.resolve(URI.create("#")).toASCIIString();
            return normalize(URI.create(resolved.substring(0, resolved.indexOf('#'))));
        }
        return normalize(requestUri.resolve(location));
    }

    private static String statusLine(HttpResponse response) {
        return response.protocolVersion() + " " + response.statusCode()
                + (response.reasonPhrase() == null ? "" : " " + response.reasonPhrase());
    }
}

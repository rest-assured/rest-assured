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

import io.restassured.common.spi.http.HttpResponse;

import java.net.URI;
import java.util.List;
import java.util.StringJoiner;

/**
 * The cookies that {@code httpClientConfig().keepResponseCookies(true)} keeps: the ones a response set are sent in the
 * requests that follow it, within one request's redirects and, with {@code reuseHttpClientInstance()}, across the
 * requests that share the configuration.
 */
public final class KeptCookies {
    private final CookieStore store;
    private final KeptCookieSpec spec;

    /**
     * @param store the store the cookies live in
     * @param spec  how the {@code Set-Cookie} headers are read
     */
    public KeptCookies(CookieStore store, KeptCookieSpec spec) {
        this.store = store;
        this.spec = spec;
    }

    /**
     * @return the value of the {@code Cookie} header of a request to the URI, or {@code null} if it sends no cookies
     */
    public String cookieHeader(URI uri) {
        List<StoredCookie> cookies = store.match(spec, CookieOrigin.of(uri));
        if (cookies.isEmpty()) {
            return null;
        }
        StringJoiner header = new StringJoiner("; ");
        for (StoredCookie cookie : cookies) {
            header.add(cookie.getValue() == null ? cookie.getName() : cookie.getName() + "=" + cookie.getValue());
        }
        return header.toString();
    }

    /**
     * Keeps the cookies that the response of a request to the URI set.
     */
    public void store(HttpResponse response, URI uri) {
        CookieOrigin origin = CookieOrigin.of(uri);
        response.headers().stream()
                .filter(header -> "Set-Cookie".equalsIgnoreCase(header.name()))
                .map(header -> spec.parse(header.value(), origin))
                .filter(cookie -> cookie != null)
                .forEach(store::add);
    }
}

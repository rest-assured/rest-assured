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

import io.restassured.internal.http.Origin;

import java.net.URI;

/**
 * Where a request goes, which decides which cookies it sends and what a cookie it sets defaults to.
 *
 * @param host   the host of the request, in lower case, or {@code null} if it can't be read, in which case the request
 *               keeps and sends no cookies
 * @param path   the path of the request
 * @param secure whether the request is sent over https
 */
public record CookieOrigin(String host, String path, boolean secure) {

    /**
     * @return the origin of a request to this URI
     */
    public static CookieOrigin of(URI uri) {
        String path = uri.getPath();
        return new CookieOrigin(Origin.host(uri), path == null || path.isEmpty() ? "/" : path, "https".equalsIgnoreCase(uri.getScheme()));
    }
}

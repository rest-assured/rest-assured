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

package io.restassured.common.spi.http;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * A request as a client sends it.
 *
 * @param method  the method, for example {@code GET}
 * @param uri     the URI, already encoded
 * @param headers the headers in the order they're sent. A name may occur more than once.
 * @param body    the body, or {@code null} if the request has none
 * @param options how this request is sent
 */
public record HttpRequest(String method, URI uri, List<HttpHeader> headers, RequestBody body, RequestOptions options) {

    public HttpRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        headers = List.copyOf(headers);
        Objects.requireNonNull(options, "options");
    }
}

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

package io.restassured.internal.transport;

import io.restassured.internal.http.RequestBody;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A request as REST Assured hands it to an HTTP client, independent of any HTTP client library.
 *
 * @param method  the method, as given (the client upper-cases it)
 * @param uri     the URI, encoded
 * @param headers the headers in the order they're sent, where a name may occur more than once and a <code>null</code>
 *                value is sent as an empty value. The client adds the headers that come from the body
 *                (<code>Content-Length</code>, and <code>Content-Type</code> unless there is one) and its own (such as
 *                <code>Host</code>).
 * @param body    the body, or <code>null</code> if the request has none
 */
public record TransportRequest(String method, URI uri, List<Map.Entry<String, String>> headers, RequestBody body) {

    public TransportRequest {
        headers = List.copyOf(headers);
    }
}

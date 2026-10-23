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

import java.io.Closeable;
import java.io.IOException;

/**
 * A client of an HTTP client library, created by {@link HttpClientProvider#newClient(ClientSettings)}.
 * <p>
 * A client sends exactly the request it gets:
 * <ul>
 * <li>It sends the headers in the given order, as far as its library allows, and changes no header name or value.
 * It adds the content type of the body ({@link RequestBody#getContentType()}) as a {@code Content-Type} header unless
 * the request has one, and sends the content length of the body, or sends the body chunked when its length is unknown.</li>
 * <li>It sends the method as given. REST Assured has already upper-cased it where it should be.</li>
 * <li>It doesn't follow redirects, answer authentication challenges, keep cookies, retry, or decode the response
 * body. REST Assured does all of that, and adds the {@code Accept-Encoding} header itself.</li>
 * <li>Without a proxy in the {@link ClientSettings}, it uses {@link java.net.ProxySelector#getDefault()}.</li>
 * <li>It throws {@link io.restassured.common.exception.ConnectionTimeoutException} when connecting times out, and
 * {@link io.restassured.common.exception.ResponseTimeoutException} when waiting for the response times out.</li>
 * <li>It throws {@link UnsupportedFeatureException} for what its library can't do, rather than doing something else.</li>
 * </ul>
 */
public interface HttpClientInstance extends Closeable {

    /**
     * Sends the request and returns once the head of the response has arrived. The body is read from the response.
     *
     * @param request the request
     * @return the response, which the caller closes
     * @throws IOException if the request can't be sent or the response can't be read
     */
    HttpResponse send(HttpRequest request) throws IOException;

    /**
     * Closes the client and its connections. It may do nothing when the library can't close a client.
     */
    @Override
    void close();
}

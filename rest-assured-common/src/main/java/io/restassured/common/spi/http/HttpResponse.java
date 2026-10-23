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
import java.io.InputStream;
import java.util.List;

/**
 * A response as a client received it, with its body still to be read.
 *
 * @param protocolVersion the protocol version, for example {@code HTTP/1.1}
 * @param statusCode      the status code
 * @param reasonPhrase    the reason phrase, or {@code null} if the client gives none
 * @param headers         the headers in the order the client gives them
 * @param contentLength   the length of the body, or {@code -1} if it's unknown
 * @param body            the body as it was received (not decoded), or {@code null} if the response has none
 * @param onClose         tells the client that REST Assured is done with the response, or {@code null}
 */
public record HttpResponse(String protocolVersion, int statusCode, String reasonPhrase, List<HttpHeader> headers,
                           long contentLength, InputStream body, Closeable onClose) implements Closeable {

    public HttpResponse {
        headers = List.copyOf(headers);
    }

    /**
     * @param name the name of the header, in any case
     * @return the value of the first header with this name, or {@code null} if there is none
     */
    public String firstHeader(String name) {
        for (HttpHeader header : headers) {
            if (header.name().equalsIgnoreCase(name)) {
                return header.value();
            }
        }
        return null;
    }

    /**
     * Tells the client that REST Assured is done with this response: after it has closed the body, or when the user
     * closes a body stream that was handed out unread, before that stream is closed. It may be called more than once.
     */
    @Override
    public void close() throws IOException {
        if (onClose != null) {
            onClose.close();
        }
    }
}

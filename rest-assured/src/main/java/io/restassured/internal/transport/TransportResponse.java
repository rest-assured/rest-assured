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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * A response as an HTTP client received it, independent of any HTTP client library. The body is unread and undecoded.
 *
 * @param protocolVersion the protocol of the status line, such as <code>HTTP/1.1</code>
 * @param statusCode      the status code
 * @param reasonPhrase    the reason phrase of the status line, possibly empty or <code>null</code>
 * @param headers         the headers in the order they were received
 * @param contentLength   the length of the body, or <code>-1</code> if it isn't known
 * @param body            the body, or <code>null</code> if the response can't have one (for example a response to HEAD,
 *                        a 204 or a 304). Closing it releases the connection.
 * @param onClose         run by {@link #close()}, may be <code>null</code>
 */
public record TransportResponse(String protocolVersion, int statusCode, String reasonPhrase, List<Map.Entry<String, String>> headers,
                                long contentLength, InputStream body, Closeable onClose) implements Closeable {

    public TransportResponse {
        headers = List.copyOf(headers);
    }

    /**
     * @return the value of the first header with this name (ignoring case), or <code>null</code> if there is none
     */
    public String firstHeader(String name) {
        for (Map.Entry<String, String> header : headers) {
            if (header.getKey().equalsIgnoreCase(name)) {
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * Tells the client that REST Assured is done with this response: after it has closed the body, or when the user
     * closes the body stream that was handed out unread (before that stream is closed). May be called more than once.
     */
    @Override
    public void close() throws IOException {
        if (onClose != null) {
            onClose.close();
        }
    }
}

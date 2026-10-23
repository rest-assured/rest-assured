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

package io.restassured.itest.java.wire;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * A request as {@link WireServer} received it: the request line, the headers in the order they were sent and the
 * body bytes (after the transfer encoding is removed, but before any content encoding is). {@code host} is the host of
 * the {@code Host} header, {@code localAddress} and {@code localPort} are where the connection arrived.
 */
public record RecordedRequest(String method, String target, String protocol, String host, String localAddress, int localPort,
                              List<Map.Entry<String, String>> headers, byte[] body) {

    public RecordedRequest {
        headers = List.copyOf(headers);
    }

    /**
     * @return the request line, for example {@code GET /path?a=b HTTP/1.1}
     */
    public String requestLine() {
        return method + " " + target + " " + protocol;
    }

    /**
     * @return the value of the first header with this name, ignoring case, or {@code null}
     */
    public String header(String name) {
        return headers.stream().filter(h -> h.getKey().equalsIgnoreCase(name)).map(Map.Entry::getValue).findFirst().orElse(null);
    }

    /**
     * @return the values of all headers with this name, ignoring case, in the order they were sent
     */
    public List<String> headers(String name) {
        return headers.stream().filter(h -> h.getKey().equalsIgnoreCase(name)).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public boolean hasHeader(String name) {
        return header(name) != null;
    }

    public String bodyAsString(Charset charset) {
        return new String(body, charset);
    }

    /**
     * @return the body as ISO-8859-1, which maps every byte to one char, so binary bodies can be compared too
     */
    public String bodyAsString() {
        return bodyAsString(ISO_8859_1);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(requestLine()).append('\n');
        headers.forEach(h -> builder.append(h.getKey()).append(": ").append(h.getValue()).append('\n'));
        return builder.append('\n').append(bodyAsString()).toString();
    }
}

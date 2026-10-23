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

package io.restassured.internal.http;

import io.restassured.internal.transport.TransportResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import java.io.Closeable;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sends requests with HttpClient 4.
 */
public class HttpClient4Transport {

    private HttpClient4Transport() {
    }

    /**
     * @param body the body of the response, or <code>null</code>
     * @return the response as a {@link TransportResponse}
     */
    public static TransportResponse toTransportResponse(HttpResponse response, InputStream body, Closeable onClose) {
        List<Map.Entry<String, String>> headers = new ArrayList<>();
        for (Header header : response.getAllHeaders()) {
            headers.add(new AbstractMap.SimpleImmutableEntry<>(header.getName(), header.getValue()));
        }
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        long contentLength = entity == null ? -1 : entity.getContentLength();
        return new TransportResponse(statusLine.getProtocolVersion().toString(), statusLine.getStatusCode(), statusLine.getReasonPhrase(),
                headers, contentLength, body, onClose);
    }
}

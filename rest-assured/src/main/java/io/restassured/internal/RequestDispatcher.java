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

package io.restassured.internal;

import io.restassured.internal.http.ContentDecoding;
import io.restassured.internal.http.ResponseParseException;
import io.restassured.internal.transport.TransportResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Hands the response of an HTTP client to the REST Assured response.
 */
final class RequestDispatcher {

    private RequestDispatcher() {
    }

    /**
     * Parses the response into the REST Assured response of the assertion closure. With body assertions the body is
     * read right away, otherwise it's read when the REST Assured response is asked for it.
     */
    static void receive(TransportResponse response, ContentDecoding contentDecoding, boolean hasBodyAssertions,
                        ResponseSpecificationImpl.HamcrestAssertionClosure assertionClosure) throws IOException {
        InputStream body = response.body();
        boolean bodyIsReadLater = false;
        try {
            try {
                String contentEncoding = response.firstHeader(ContentDecoding.CONTENT_ENCODING);
                // The decoded length of an encoded body isn't known, so it's parsed even when the encoded body is empty
                if (body == null || response.contentLength() == 0 && !contentDecoding.decodes(contentEncoding)) {
                    // Closing the empty body releases its connection
                    closeQuietly(body);
                    assertionClosure.call(response, EMPTY);
                } else {
                    InputStream content = contentDecoding.decode(body, contentEncoding);
                    bodyIsReadLater = !hasBodyAssertions;
                    assertionClosure.call(response, content);
                }
            } catch (Exception ex) {
                throw new ResponseParseException(response.statusCode(), response.reasonPhrase(), ex);
            }
        } finally {
            if (hasBodyAssertions) {
                closeQuietly(body);
            }
            // A body that is read later closes the response once it has been read instead
            if (!bodyIsReadLater) {
                response.close();
            }
        }
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // Like HttpClient 4's EntityUtils.consumeQuietly
        }
    }
}

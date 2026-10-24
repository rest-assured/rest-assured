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

package io.restassured.http.apache5;

import io.restassured.common.spi.http.RequestBody;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Turns a {@link RequestBody} into an {@link HttpEntity}. The entities have no content type, since the adapter sends
 * the body's as a header, so that HttpClient sends the {@code Content-Type} exactly as given. A body of unknown length
 * is sent chunked.
 */
final class Entities {

    private Entities() {
    }

    static HttpEntity of(RequestBody body) {
        switch (body.getType()) {
            case BYTES:
                return new ByteArrayEntity(body.getBytes(), null);
            case FILE:
                return new FileEntity(body.getFile(), (ContentType) null);
            case STREAM:
                return new InputStreamEntity(body.getStream(), body.getContentLength(), null);
            default:
                return new WriterEntity(body);
        }
    }

    static HttpEntity empty() {
        return new ByteArrayEntity(new byte[0], null);
    }

    /**
     * A body that its writer writes to the connection, such as a multipart body.
     */
    static final class WriterEntity extends AbstractHttpEntity {
        private final RequestBody body;

        WriterEntity(RequestBody body) {
            super((ContentType) null, null);
            this.body = body;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            body.getWriter().writeTo(out);
        }

        /**
         * HttpClient doesn't read a request body this way, but an interceptor of a customizer may
         */
        @Override
        public InputStream getContent() throws IOException {
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            writeTo(content);
            return new ByteArrayInputStream(content.toByteArray());
        }

        @Override
        public long getContentLength() {
            return body.getContentLength();
        }

        @Override
        public boolean isRepeatable() {
            return body.isRepeatable();
        }

        @Override
        public boolean isStreaming() {
            return !body.isRepeatable();
        }

        @Override
        public void close() {
        }
    }
}

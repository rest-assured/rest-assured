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

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Maps a {@link RequestBody} onto the HttpClient 4 entity that is sent. HttpClient 4 sends an entity chunked when its
 * content length is unknown, and otherwise with a <code>Content-Length</code> header.
 */
public class RequestBodyToHttpClient4Entity {

    private RequestBodyToHttpClient4Entity() {
    }

    /**
     * @return an entity with the body's content, content-type, content length and repeatability
     */
    public static HttpEntity toHttpEntity(RequestBody body) {
        final AbstractHttpEntity entity;
        switch (body.getType()) {
            case BYTES:
                entity = new ByteArrayEntity(body.getBytes());
                break;
            case STREAM:
                entity = new InputStreamEntity(body.getStream(), body.getContentLength());
                break;
            case FILE:
                // Streamed once like an InputStream, so a request with a file body can't be repeated, as before
                entity = new InputStreamEntity(open(body.getFile()), body.getContentLength());
                break;
            case WRITER:
                entity = new WriterEntity(body);
                break;
            default:
                throw new IllegalStateException("Unknown request body type " + body.getType());
        }
        entity.setContentType(body.getContentType());
        return entity;
    }

    private static InputStream open(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File " + file.getPath() + " not found", e);
        }
    }

    private static class WriterEntity extends AbstractHttpEntity {
        private final RequestBody body;

        private WriterEntity(RequestBody body) {
            this.body = body;
        }

        @Override
        public boolean isRepeatable() {
            return body.isRepeatable();
        }

        @Override
        public long getContentLength() {
            return body.getContentLength();
        }

        @Override
        public InputStream getContent() {
            throw new UnsupportedOperationException("A request body written by a writer can't be read as a stream");
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            body.getWriter().writeTo(out);
        }

        @Override
        public boolean isStreaming() {
            return !body.isRepeatable();
        }
    }
}

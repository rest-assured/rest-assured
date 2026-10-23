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

package io.restassured.internal.multipart;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The content of one part of a multipart request: text, a file or a stream, with its content-type and file name.
 * {@link MultiPartInternal#getContentBody()} creates it and {@link MultipartWriter} writes it.
 */
public final class PartBody {
    private static final int BUFFER_SIZE = 4096;

    private final byte[] text;
    private final File file;
    private final InputStream stream;
    private final MediaType contentType;
    private final String fileName;

    private PartBody(byte[] text, File file, InputStream stream, MediaType contentType, String fileName) {
        this.text = text;
        this.file = file;
        this.stream = stream;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    /**
     * @return a part without file name whose text is encoded in the charset of the content-type, or US-ASCII without one
     */
    static PartBody text(String text, MediaType contentType) {
        Charset charset = contentType.getCharset() == null ? StandardCharsets.US_ASCII : contentType.getCharset();
        return new PartBody(text.getBytes(charset), null, null, contentType, null);
    }

    /**
     * @return a part with the file's contents, named after the file unless a file name is given
     */
    static PartBody file(File file, MediaType contentType, String fileName) {
        return new PartBody(null, file, null, contentType, fileName == null ? file.getName() : fileName);
    }

    /**
     * @return a part of unknown length that is written once, and closes the stream when written
     */
    static PartBody stream(InputStream stream, MediaType contentType, String fileName) {
        return new PartBody(null, null, stream, contentType, fileName);
    }

    /**
     * @return the value of the part's Content-Type header
     */
    public String getContentType() {
        return contentType.toString();
    }

    /**
     * @return the file name of the Content-Disposition header, or {@code null} for none
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the length in bytes, or -1 if unknown
     */
    public long getContentLength() {
        if (text != null) {
            return text.length;
        } else if (file != null) {
            return file.length();
        }
        return -1;
    }

    /**
     * Writes the content in writes of at most 4096 bytes and flushes after it, like httpmime did, so a chunked request
     * gets the same chunks.
     */
    public void writeTo(OutputStream out) throws IOException {
        if (text != null) {
            copy(new ByteArrayInputStream(text), out);
        } else if (file != null) {
            try (InputStream in = new FileInputStream(file)) {
                copy(in, out);
            }
        } else {
            try {
                copy(stream, out);
            } finally {
                stream.close();
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }
}

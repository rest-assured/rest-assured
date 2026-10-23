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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * A request body as the HTTP client sends it, independent of any HTTP client library: its content (bytes, a stream,
 * a file or a {@link ContentWriter writer}), its content-type, its content length and whether it can be sent more than
 * once. A body with an unknown length ({@code -1}) is sent chunked.
 * <p>
 * Instances are immutable, but the content is not copied: a byte array must not be modified after it's passed in, and
 * a stream can only be read once.
 */
public final class RequestBody {

    /**
     * What a {@link RequestBody} holds.
     */
    public enum Type {
        /**
         * Bytes held in memory, see {@link RequestBody#getBytes()}.
         */
        BYTES,
        /**
         * A stream that's read once, see {@link RequestBody#getStream()}.
         */
        STREAM,
        /**
         * A file that's read when the body is sent, see {@link RequestBody#getFile()}.
         */
        FILE,
        /**
         * A writer that writes the body to the connection, see {@link RequestBody#getWriter()}.
         */
        WRITER
    }

    /**
     * Writes a request body to an output stream, for example a multipart body.
     */
    @FunctionalInterface
    public interface ContentWriter {
        /**
         * @param out the stream to write the body to, not closed by the writer
         */
        void writeTo(OutputStream out) throws IOException;
    }

    /**
     * The content length of a body whose length isn't known up front.
     */
    public static final long UNKNOWN_LENGTH = -1;

    private final Type type;
    private final Object content;
    private final String contentType;
    private final long contentLength;
    private final boolean repeatable;

    private RequestBody(Type type, Object content, String contentType, long contentLength, boolean repeatable) {
        this.type = type;
        this.content = content;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.repeatable = repeatable;
    }

    /**
     * @param content     the body, not copied
     * @param contentType the content-type, or {@code null} for none
     * @return a repeatable body of the given bytes
     */
    public static RequestBody bytes(byte[] content, String contentType) {
        notNull(content, "Request body content");
        return new RequestBody(Type.BYTES, content, contentType, content.length, true);
    }

    /**
     * @param content       the body
     * @param contentLength the number of bytes in the stream, or {@link #UNKNOWN_LENGTH}
     * @param contentType   the content-type, or {@code null} for none
     * @return a body that's read from the stream, and so can't be repeated
     */
    public static RequestBody stream(InputStream content, long contentLength, String contentType) {
        notNull(content, "Request body content");
        return new RequestBody(Type.STREAM, content, contentType, contentLength, false);
    }

    /**
     * @param file        the file to send
     * @param contentType the content-type, or {@code null} for none
     * @return a repeatable body of the file's content, with the file's current length
     */
    public static RequestBody file(File file, String contentType) {
        notNull(file, "Request body file");
        return new RequestBody(Type.FILE, file, contentType, file.length(), true);
    }

    /**
     * @param writer        writes the body
     * @param contentLength the number of bytes the writer writes, or {@link #UNKNOWN_LENGTH}
     * @param repeatable    whether the writer can write the body more than once
     * @param contentType   the content-type, or {@code null} for none
     * @return a body that's written by the writer
     */
    public static RequestBody writer(ContentWriter writer, long contentLength, boolean repeatable, String contentType) {
        notNull(writer, "Request body writer");
        return new RequestBody(Type.WRITER, writer, contentType, contentLength, repeatable);
    }

    /**
     * @return what the body holds, which tells which of the content getters can be used
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the content-type, or {@code null} if the body is sent without one
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the number of bytes in the body, or {@link #UNKNOWN_LENGTH}
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * @return whether the body can be sent more than once, for example again after an authentication challenge
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * @return the bytes of a {@link Type#BYTES} body, not a copy
     */
    public byte[] getBytes() {
        return (byte[]) contentOf(Type.BYTES);
    }

    /**
     * @return the stream of a {@link Type#STREAM} body
     */
    public InputStream getStream() {
        return (InputStream) contentOf(Type.STREAM);
    }

    /**
     * @return the file of a {@link Type#FILE} body
     */
    public File getFile() {
        return (File) contentOf(Type.FILE);
    }

    /**
     * @return the writer of a {@link Type#WRITER} body
     */
    public ContentWriter getWriter() {
        return (ContentWriter) contentOf(Type.WRITER);
    }

    private static void notNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    private Object contentOf(Type expected) {
        if (type != expected) {
            throw new IllegalStateException("The request body is of type " + type + ", not " + expected + ".");
        }
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestBody)) return false;
        RequestBody that = (RequestBody) o;
        return type == that.type && contentLength == that.contentLength && repeatable == that.repeatable &&
                Objects.equals(contentType, that.contentType) && Objects.deepEquals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[]{type, content, contentType, contentLength, repeatable});
    }

    @Override
    public String toString() {
        return "RequestBody{type=" + type + ", contentType=" + contentType + ", contentLength=" + contentLength +
                ", repeatable=" + repeatable + "}";
    }
}

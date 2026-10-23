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

import io.restassured.config.MultipartMode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static io.restassured.internal.util.GroovyStringConversion.castToString;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Writes the body of a multipart request, byte for byte as httpmime 4.5 did when REST Assured used it:
 * <ul>
 * <li>each part starts with {@code --boundary}, then its headers: the custom ones in the order given, then a generated
 * {@code Content-Disposition} and {@code Content-Type} unless a custom header has that name;</li>
 * <li>{@link MultipartMode#STRICT} writes the headers as US-ASCII (other characters become {@code ?}),
 * {@link MultipartMode#RFC6532} as UTF-8, and {@link MultipartMode#BROWSER_COMPATIBLE} writes only the
 * {@code Content-Disposition} header, and the {@code Content-Type} header of parts with a file name, in the charset;</li>
 * <li>the boundary in the body is encoded in the charset, or US-ASCII without one.</li>
 * </ul>
 */
public final class MultipartWriter {
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private static final byte[] FIELD_SEPARATOR = ascii(": ");
    private static final byte[] CR_LF = ascii("\r\n");
    private static final byte[] TWO_DASHES = ascii("--");

    private final MediaType contentType;
    private final Charset charset;
    private final MultipartMode mode;
    private final String boundary;
    private final List<Part> parts = new ArrayList<>();

    /**
     * @param subType  the subtype of the multipart content-type, such as {@code form-data}
     * @param charset  the charset of the content-type, or {@code null} for none
     * @param mode     how the part headers are written
     * @param boundary the boundary between the parts
     */
    public MultipartWriter(String subType, String charset, MultipartMode mode, String boundary) {
        notNull(subType, "Multipart sub type");
        notNull(mode, "Multipart mode");
        notNull(boundary, "Multipart boundary");
        Charset contentCharset = charset == null ? null : Charset.forName(charset.trim());
        String trimmedSubType = subType.trim();
        if (isBlank(trimmedSubType)) {
            throw new IllegalArgumentException("MIME subtype may not be blank");
        }
        MediaType multipartType = MediaType.create("multipart/" + trimmedSubType).withParameter("boundary", boundary);
        this.contentType = contentCharset == null ? multipartType : multipartType.withParameter("charset", contentCharset.name());
        this.charset = contentCharset == null ? StandardCharsets.US_ASCII : contentCharset;
        this.mode = mode;
        this.boundary = boundary;
    }

    /**
     * Adds a part. A {@code Content-Transfer-Encoding} header is never written, not even a custom one: httpmime added
     * one that servers following RFC 7578 (section 4.7) reject, so REST Assured removed them all.
     *
     * @param name    the control name
     * @param body    the content
     * @param headers custom headers, written before the generated ones
     * @throws IllegalArgumentException if a header name is {@code null}
     * @throws IllegalStateException    if the name is blank
     */
    public void addPart(String name, PartBody body, Map<String, String> headers) {
        List<Field> fields = new ArrayList<>();
        if (headers != null) {
            // Groovy callers may use GString names and values, which Groovy converted to String
            for (Map.Entry<?, ?> header : headers.entrySet()) {
                String headerName = castToString(header.getKey());
                if (headerName == null) {
                    throw new IllegalArgumentException("Field name may not be null");
                }
                fields.add(new Field(headerName, castToString(header.getValue())));
            }
        }
        if (isBlank(name)) {
            throw new IllegalStateException("Name is blank");
        }
        if (field(fields, CONTENT_DISPOSITION) == null) {
            fields.add(new Field(CONTENT_DISPOSITION, contentDisposition(name, body.getFileName())));
        }
        if (field(fields, CONTENT_TYPE) == null) {
            fields.add(new Field(CONTENT_TYPE, body.getContentType()));
        }
        fields.removeIf(field -> field.name.equalsIgnoreCase(CONTENT_TRANSFER_ENCODING));
        parts.add(new Part(body, fields));
    }

    /**
     * @return the value of the request's Content-Type header, such as {@code multipart/form-data; boundary=abc}:
     * the subtype in lower case, then the boundary, quoted only if it needs to be, then the charset if there is one
     */
    public String getContentType() {
        return contentType.toString();
    }

    /**
     * @return the length of the body in bytes, or -1 when a part has unknown length, which makes the request chunked
     */
    public long getContentLength() {
        long length = 0;
        for (Part part : parts) {
            long partLength = part.body.getContentLength();
            if (partLength < 0) {
                return -1;
            }
            length += partLength;
        }
        CountingOutputStream framing = new CountingOutputStream();
        try {
            write(framing, false);
        } catch (IOException e) {
            // Can't happen: counting doesn't do I/O
            return -1;
        }
        return length + framing.count;
    }

    /**
     * @return {@code true} if the body can be written more than once, which is when its length is known
     */
    public boolean isRepeatable() {
        return getContentLength() != -1;
    }

    /**
     * Writes the body. A stream part can only be written once.
     */
    public void writeTo(OutputStream out) throws IOException {
        write(out, true);
    }

    // Each piece is a separate write, as in httpmime, so a chunked request is split into the same chunks
    private void write(OutputStream out, boolean writeContent) throws IOException {
        byte[] encodedBoundary = encode(charset, boundary);
        for (Part part : parts) {
            out.write(TWO_DASHES);
            out.write(encodedBoundary);
            out.write(CR_LF);
            writeHeaders(part, out);
            out.write(CR_LF);
            if (writeContent) {
                part.body.writeTo(out);
            }
            out.write(CR_LF);
        }
        out.write(TWO_DASHES);
        out.write(encodedBoundary);
        out.write(TWO_DASHES);
        out.write(CR_LF);
    }

    private void writeHeaders(Part part, OutputStream out) throws IOException {
        switch (mode) {
            case BROWSER_COMPATIBLE:
                writeField(field(part.fields, CONTENT_DISPOSITION), charset, out);
                if (part.body.getFileName() != null) {
                    writeField(field(part.fields, CONTENT_TYPE), charset, out);
                }
                break;
            case RFC6532:
                for (Field field : part.fields) {
                    writeField(field, StandardCharsets.UTF_8, out);
                }
                break;
            default:
                for (Field field : part.fields) {
                    writeField(field, StandardCharsets.US_ASCII, out);
                }
        }
    }

    private static void writeField(Field field, Charset charset, OutputStream out) throws IOException {
        out.write(encode(charset, field.name));
        out.write(FIELD_SEPARATOR);
        out.write(encode(charset, field.value));
        out.write(CR_LF);
    }

    private static Field field(List<Field> fields, String name) {
        for (Field field : fields) {
            if (field.name.equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    private static String contentDisposition(String name, String fileName) {
        StringBuilder value = new StringBuilder("form-data; name=\"").append(escape(name)).append('"');
        if (fileName != null) {
            value.append("; filename=\"").append(escape(fileName)).append('"');
        }
        return value.toString();
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (ch == '"' || ch == '\\' || ch == '\r') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    // Charset.encode replaces what the charset can't encode, e.g. with '?' in US-ASCII
    private static byte[] encode(Charset charset, String value) {
        ByteBuffer encoded = charset.encode(CharBuffer.wrap(value));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static final class Part {
        private final PartBody body;
        private final List<Field> fields;

        private Part(PartBody body, List<Field> fields) {
            this.body = body;
            this.fields = fields;
        }
    }

    private static final class Field {
        private final String name;
        private final String value;

        private Field(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private static final class CountingOutputStream extends OutputStream {
        private long count;

        @Override
        public void write(int b) {
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            count += len;
        }
    }
}

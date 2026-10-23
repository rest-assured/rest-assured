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

import io.restassured.config.DecoderConfig;
import io.restassured.config.DecoderConfig.ContentDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * The content decoders of a {@link DecoderConfig}: which encodings a request accepts, and how a response body is
 * decoded according to its {@code Content-Encoding} header.
 */
public class ContentDecoding {
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String CONTENT_ENCODING = "Content-Encoding";

    private final List<ContentDecoder> decoders;
    private final boolean useNoWrapForInflateDecoding;

    public ContentDecoding(DecoderConfig decoderConfig) {
        DecoderConfig config = decoderConfig == null ? DecoderConfig.decoderConfig() : decoderConfig;
        this.decoders = List.copyOf(config.contentDecoders());
        this.useNoWrapForInflateDecoding = config.shouldUseNoWrapForInflateDecoding();
    }

    /**
     * Adds each decoder, in the configured order, to the {@code Accept-Encoding} header the request has. A decoder that
     * is already part of the header (as a substring) isn't added again. With several {@code Accept-Encoding} headers,
     * the merged value replaces the first one and the others are kept as they are.
     *
     * @param acceptEncodings the values of the request's {@code Accept-Encoding} headers, in order
     * @return the value for the first {@code Accept-Encoding} header, or {@code null} if there are no decoders and the
     * headers stay as they are
     */
    public String acceptEncoding(List<String> acceptEncodings) {
        List<String> values = new ArrayList<>(acceptEncodings);
        for (ContentDecoder decoder : decoders) {
            String encoding = nameOf(decoder);
            if (values.isEmpty()) {
                values.add(encoding);
            } else {
                String joined = String.join(",", values);
                values.set(0, joined.contains(encoding) ? joined : joined + "," + encoding);
            }
        }
        return decoders.isEmpty() ? null : values.get(0);
    }

    /**
     * @param contentEncoding the value of the response's first {@code Content-Encoding} header, or {@code null}
     * @return whether {@link #decode(InputStream, String)} decodes a body with this content encoding
     */
    public boolean decodes(String contentEncoding) {
        return decoders.stream().anyMatch(decoder -> hasEncoding(contentEncoding, decoder));
    }

    /**
     * Wraps the body in a decoder for each configured decoder, in the configured order, whose encoding is listed in
     * the {@code Content-Encoding} header. Other encodings, such as {@code x-gzip} or {@code br}, are left as they are.
     *
     * @param body            the body as it was received
     * @param contentEncoding the value of the response's first {@code Content-Encoding} header, or {@code null}
     * @return the decoded body
     * @throws IOException if reading a gzip body fails
     */
    public InputStream decode(InputStream body, String contentEncoding) throws IOException {
        InputStream decoded = body;
        for (ContentDecoder decoder : decoders) {
            if (hasEncoding(contentEncoding, decoder)) {
                decoded = decoder == ContentDecoder.GZIP ? gunzip(decoded) : inflate(decoded);
            }
        }
        return decoded;
    }

    // Reads the whole body first, so that an empty body gives an empty stream instead of failing on a missing gzip header
    private static InputStream gunzip(InputStream body) throws IOException {
        byte[] bytes = body.readAllBytes();
        ByteArrayInputStream buffered = new ByteArrayInputStream(bytes);
        return bytes.length == 0 ? buffered : new GZIPInputStream(buffered);
    }

    private InputStream inflate(InputStream body) {
        return new InflaterInputStream(body, new Inflater(useNoWrapForInflateDecoding));
    }

    private static boolean hasEncoding(String contentEncoding, ContentDecoder decoder) {
        if (contentEncoding == null) {
            return false;
        }
        for (String element : contentEncoding.split(",")) {
            if (codingName(element).equalsIgnoreCase(nameOf(decoder))) {
                return true;
            }
        }
        return false;
    }

    // The name of a header element such as "gzip;q=1", without its parameters
    private static String codingName(String element) {
        return element.split("[;=]", 2)[0].trim();
    }

    private static String nameOf(ContentDecoder decoder) {
        return decoder.name().toLowerCase(Locale.ENGLISH);
    }
}

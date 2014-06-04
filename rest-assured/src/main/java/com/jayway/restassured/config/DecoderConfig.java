/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.config;

import org.apache.commons.lang3.Validate;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Allows you to specify configuration for the decoder.
 */
public class DecoderConfig {

    private static final boolean DEFAULT_NO_WRAP_FOR_INFLATE_ENCODED_STREAMS = false;

    private final String defaultContentCharset;
    private final List<ContentDecoder> contentDecoders;
    private final boolean useNoWrapForInflateDecoding;

    /**
     * Configure the decoder config to use the default charset as specified by {@link java.nio.charset.Charset#defaultCharset()} for content decoding.
     */
    public DecoderConfig() {
        this(Charset.defaultCharset().toString(), DEFAULT_NO_WRAP_FOR_INFLATE_ENCODED_STREAMS, defaultContentEncoders());
    }

    /**
     * Configure the decoder config to use supplied <code>defaultContentCharset</code> for content decoding if a charset is not specified in the response.
     *
     * @param defaultContentCharset The charset to use if not specifically specified in the response.
     */
    public DecoderConfig(String defaultContentCharset) {
        this(defaultContentCharset, DEFAULT_NO_WRAP_FOR_INFLATE_ENCODED_STREAMS, defaultContentEncoders());
    }

    /**
     * Configure the decoder config to use {@link java.nio.charset.Charset#defaultCharset()} for content decoding.
     * Also specify the content decoders that will be presented to the server when making a request (using the <code>Accept-Encoding</code> header).
     * If the server supports any of these encodings then REST Assured will automatically perform decoding of the response accordingly.
     * <p>
     * By default {@link ContentDecoder#GZIP} and {@link ContentDecoder#DEFLATE} are used.
     * </p>
     *
     * @param contentDecoder            The first content decoder
     * @param additionalContentDecoders Optional additional content decoders
     * @return A new instance of the DecoderConfig.
     */
    public DecoderConfig(ContentDecoder contentDecoder, ContentDecoder... additionalContentDecoders) {
        this(Charset.defaultCharset().toString(), DEFAULT_NO_WRAP_FOR_INFLATE_ENCODED_STREAMS, merge(contentDecoder, additionalContentDecoders));
    }

    private DecoderConfig(String defaultContentCharset, boolean useNoWrapForInflateDecoding, ContentDecoder... contentDecoders) {
        this(defaultContentCharset, useNoWrapForInflateDecoding, contentDecoders == null ? Collections.<ContentDecoder>emptyList() : Arrays.asList(contentDecoders));
    }

    private DecoderConfig(String defaultContentCharset, boolean useNoWrapForInflateDecoding, List<ContentDecoder> contentDecoders) {
        Validate.notBlank(defaultContentCharset, "Default decoder content charset to cannot be blank");
        this.defaultContentCharset = defaultContentCharset;
        this.contentDecoders = Collections.unmodifiableList(contentDecoders == null ? Collections.<ContentDecoder>emptyList() : contentDecoders);
        this.useNoWrapForInflateDecoding = useNoWrapForInflateDecoding;
    }

    /**
     * @return The configured default content charset that'll be used by REST Assured if no charset is specified in the response.
     */
    public String defaultContentCharset() {
        return defaultContentCharset;
    }

    /**
     * @return The configured content decoders.
     */
    public List<ContentDecoder> contentDecoders() {
        return contentDecoders;
    }

    /**
     * If the parameter 'nowrap' is true then the ZLIB header and checksum fields will not be used. This provides compatibility with the
     * compression format used by both GZIP and PKZIP.
     * <p/>
     * Note: When using the 'nowrap' option it is also necessary to provide
     * an extra "dummy" byte as input. This is required by the ZLIB native
     * library in order to support certain optimizations.
     * <p/>
     * Setting no wrap to <code>true</code> is required when communicating with servers not using RFC 1950 (such as PHP which uses RFC 1951).
     * See <a href=" http://stackoverflow.com/a/11401785">stackoverflow</a> for more details.
     * <p/>
     * Default is {@value #DEFAULT_NO_WRAP_FOR_INFLATE_ENCODED_STREAMS}.
     *
     * @param nowrap if true then support GZIP compatible compression
     * @return A new instance of the DecoderConfig.
     */
    public DecoderConfig useNoWrapForInflateDecoding(boolean nowrap) {
        return new DecoderConfig(defaultContentCharset, nowrap, contentDecoders);
    }

    /**
     * @return <code>true</code> if no wrap should be used for inflate encoded streams.
     * @see #useNoWrapForInflateDecoding(boolean)
     */
    public boolean shouldUseNoWrapForInflateDecoding() {
        return useNoWrapForInflateDecoding;
    }

    /**
     * Specify the default charset of the content in the response that's assumed if no charset is explicitly specified in the response.
     *
     * @param charset The expected charset
     * @return A new instance of the DecoderConfig.
     */
    public DecoderConfig defaultContentCharset(String charset) {
        return new DecoderConfig(charset, useNoWrapForInflateDecoding, contentDecoders);
    }

    /**
     * Specify the content decoders that will be presented to the server when making a request (using the <code>Accept-Encoding</code> header).
     * If the server supports any of these encodings then REST Assured will automatically perform decoding of the response accordingly.
     * <p>
     * By default {@link ContentDecoder#GZIP} and {@link ContentDecoder#DEFLATE} are used.
     * </p>
     *
     * @param contentDecoder            The first content decoder
     * @param additionalContentDecoders Optional additional content decoders
     * @return A new instance of the DecoderConfig.
     */
    public DecoderConfig contentDecoders(ContentDecoder contentDecoder, ContentDecoder... additionalContentDecoders) {
        return new DecoderConfig(defaultContentCharset, useNoWrapForInflateDecoding, merge(contentDecoder, additionalContentDecoders));
    }

    /**
     * Specify that no content decoders should be used by REST Assured.
     *
     * @return A new instance of the DecoderConfig.
     * @see #contentDecoders(com.jayway.restassured.config.DecoderConfig.ContentDecoder, com.jayway.restassured.config.DecoderConfig.ContentDecoder...)
     */
    public DecoderConfig noContentDecoders() {
        return new DecoderConfig(defaultContentCharset, useNoWrapForInflateDecoding);
    }

    /**
     * @return A static way to create a new DecoderConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static DecoderConfig decoderConfig() {
        return new DecoderConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same decoder config instance.
     */
    public DecoderConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same decoder config instance.
     */
    public DecoderConfig with() {
        return this;
    }


    private static ContentDecoder[] defaultContentEncoders() {
        return new ContentDecoder[]{ContentDecoder.GZIP, ContentDecoder.DEFLATE};
    }

    private static ContentDecoder[] merge(ContentDecoder contentDecoder, ContentDecoder[] additionalContentDecoders) {
        Validate.notNull(contentDecoder, "Content decoder cannot be null");
        final ContentDecoder[] contentDecoders;
        if (additionalContentDecoders == null || additionalContentDecoders.length == 0) {
            contentDecoders = new ContentDecoder[]{contentDecoder};
        } else {
            contentDecoders = new ContentDecoder[additionalContentDecoders.length + 1];
            contentDecoders[0] = contentDecoder;
            System.arraycopy(additionalContentDecoders, 0, contentDecoders, 1, additionalContentDecoders.length);
        }
        return contentDecoders;
    }

    /**
     * Predefined content encoders in REST Assured. Will also automatically specify the Accept-Encoder header.
     */
    public static enum ContentDecoder {
        /**
         * GZIP compression support for both requests and responses.
         */
        GZIP, DEFLATE
    }
}
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

    private final String defaultContentCharset;
    private final List<ContentDecoder> contentDecoders;

    /**
     * Configure the decoder config to use the default charset as specified by {@link java.nio.charset.Charset#defaultCharset()} for content decoding.
     */
    public DecoderConfig() {
        this(Charset.defaultCharset().toString(), defaultContentEncoders());
    }

    /**
     * Configure the decoder config to use supplied <code>defaultContentCharset</code> for content decoding if a charset is not specified in the response.
     *
     * @param defaultContentCharset The charset to use if not specifically specified in the response.
     */
    public DecoderConfig(String defaultContentCharset) {
        this(defaultContentCharset, defaultContentEncoders());
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
        this(Charset.defaultCharset().toString(), merge(contentDecoder, additionalContentDecoders));
    }

    private DecoderConfig(String defaultContentCharset, ContentDecoder... contentDecoders) {
        Validate.notBlank(defaultContentCharset, "Default decoder content charset to cannot be blank");
        this.defaultContentCharset = defaultContentCharset;
        this.contentDecoders = Collections.unmodifiableList(contentDecoders == null ? Collections.<ContentDecoder>emptyList() : Arrays.asList(contentDecoders));
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
     * Specify the default charset of the content in the response that's assumed if no charset is explicitly specified in the response.
     *
     * @param charset The expected charset
     * @return A new instance of the DecoderConfig.
     */
    public DecoderConfig defaultContentCharset(String charset) {
        return new DecoderConfig(charset);
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
        return new DecoderConfig(defaultContentCharset, merge(contentDecoder, additionalContentDecoders));
    }

    /**
     * Specify that no content decoders should be used by REST Assured.
     *
     * @return A new instance of the DecoderConfig.
     * @see #contentDecoders(com.jayway.restassured.config.DecoderConfig.ContentDecoder, com.jayway.restassured.config.DecoderConfig.ContentDecoder...)
     */
    public DecoderConfig noContentDecoders() {
        return new DecoderConfig(defaultContentCharset, new ContentDecoder[0]);
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
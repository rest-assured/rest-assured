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
import org.apache.http.protocol.HTTP;

/**
 * Allows you to specify configuration for the encoder
 */
public class EncoderConfig {

    private static final String UTF_8 = "UTF-8";
    private final String defaultContentCharset;
    private final String defaultQueryParameterCharset;
    private final boolean shouldAppendDefaultContentCharsetToContentTypeIfUndefined;

    /**
     * Configure the encoder config to use {@value org.apache.http.protocol.HTTP#DEFAULT_CONTENT_CHARSET} for content encoding and <code>UTF-8</code>.
     * for query parameter encoding.
     * <p>
     * The reason for choosing UTF-8 as default for query parameters even though US-ASCII is standard according to the URI Syntax specification is
     * that it's nowadays <a href="http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">recommended</a> by w3 to use UTF-8. Different web servers
     * seem to take different approaches though, for example Jetty uses UTF-8 as default but Tomcat uses US-ASCII. Since REST Assured is a test
     * framework first and Jetty is more popular for testing REST Assured uses UTF-8.
     * </p>
     */
    public EncoderConfig() {
        this(HTTP.DEF_CONTENT_CHARSET.toString(), UTF_8);
    }

    public EncoderConfig(String defaultContentCharset, String defaultQueryParameterCharset) {
        this(defaultContentCharset, defaultQueryParameterCharset, true);
    }

    private EncoderConfig(String defaultContentCharset, String defaultQueryParameterCharset, boolean shouldAppendDefaultContentCharsetToContentTypeIfUndefined) {
        Validate.notBlank(defaultContentCharset, "Default encoder content charset to cannot be blank. See \"appendDefaultContentCharsetToContentTypeIfMissing\" method if you like to disable automatically appending the charset to the content-type.");
        Validate.notBlank(defaultQueryParameterCharset, "Default protocol charset to cannot be blank.");
        this.defaultContentCharset = defaultContentCharset;
        this.defaultQueryParameterCharset = defaultQueryParameterCharset;
        this.shouldAppendDefaultContentCharsetToContentTypeIfUndefined = shouldAppendDefaultContentCharsetToContentTypeIfUndefined;
    }

    public String defaultContentCharset() {
        return defaultContentCharset;
    }

    public String defaultQueryParameterCharset() {
        return defaultQueryParameterCharset;
    }

    public EncoderConfig defaultContentCharset(String charset) {
        return new EncoderConfig(charset, defaultQueryParameterCharset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined);
    }

    public EncoderConfig defaultQueryParameterCharset(String charset) {
        return new EncoderConfig(defaultContentCharset, charset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined);
    }

    /**
     * Tells whether REST Assured should automatically append the content charset to the content-type header if not defined explicitly.
     * <p>
     * Note that this does not affect multipart form data.
     * </p>
     * <p>
     * Default is <code>true</code>.
     * </p>
     *
     * @param shouldAddDefaultContentCharsetToContentTypeIfMissing Whether REST Assured should automatically append the content charset to the content-type header if not defined explicitly.
     * @return A new {@link com.jayway.restassured.config.EncoderConfig} instance
     */
    public EncoderConfig appendDefaultContentCharsetToContentTypeIfUndefined(boolean shouldAddDefaultContentCharsetToContentTypeIfMissing) {
        return new EncoderConfig(defaultContentCharset, defaultQueryParameterCharset, shouldAddDefaultContentCharsetToContentTypeIfMissing);
    }

    /**
     * Tells whether REST Assured should automatically append the content charset to the content-type header if not defined explicitly.
     *
     * @param shouldAddDefaultContentCharsetToContentTypeIfMissing Whether REST Assured should automatically append the content charset to the content-type header if not defined explicitly.
     * @return A new {@link com.jayway.restassured.config.EncoderConfig} instance
     * @deprecated Use {@link #appendDefaultContentCharsetToContentTypeIfUndefined(boolean)} instead.
     */
    @Deprecated
    public EncoderConfig appendDefaultContentCharsetToStreamingContentTypeIfUndefined(boolean shouldAddDefaultContentCharsetToContentTypeIfMissing) {
        return new EncoderConfig(defaultContentCharset, defaultQueryParameterCharset, shouldAddDefaultContentCharsetToContentTypeIfMissing);
    }

    /**
     * Tells whether REST Assured should automatically append the content charset to the content-type header if not defined explicitly.
     * <p>
     * Note that this does not affect multipart form data.
     * </p>
     *
     * @return <code>true</code> if REST Assured should automatically append the content charset to the content-type header if not defined explicitly.
     */
    public boolean shouldAppendDefaultContentCharsetToContentTypeIfUndefined() {
        return shouldAppendDefaultContentCharsetToContentTypeIfUndefined;
    }

    /**
     * @return A static way to create a new EncoderConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static EncoderConfig encoderConfig() {
        return new EncoderConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same encoder config instance.
     */
    public EncoderConfig and() {
        return this;
    }
}
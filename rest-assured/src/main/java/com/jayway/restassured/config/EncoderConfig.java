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

    /**
     * Configure the encoder config to use {@value HTTP#DEFAULT_CONTENT_CHARSET} for content encoding and {@value #UTF_8}.
     * for query parameter encoding.
     */
    public EncoderConfig() {
        this(HTTP.DEFAULT_CONTENT_CHARSET, UTF_8);
    }

    public EncoderConfig(String defaultContentCharset, String defaultQueryParameterCharset) {
        Validate.notBlank(defaultContentCharset, "Default encoder content charset to cannot be blank");
        Validate.notBlank(defaultQueryParameterCharset, "Default protocol charset to cannot be blank");
        this.defaultContentCharset = defaultContentCharset;
        this.defaultQueryParameterCharset = defaultQueryParameterCharset;
    }

    public String defaultContentCharset() {
        return defaultContentCharset;
    }

    public String defaultQueryParameterCharset() {
        return defaultQueryParameterCharset;
    }

    public EncoderConfig defaultContentCharset(String charset) {
        return new EncoderConfig(charset, defaultQueryParameterCharset);
    }

    public EncoderConfig defaultQueryParameterCharset(String charset) {
        return new EncoderConfig(defaultContentCharset, charset);
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
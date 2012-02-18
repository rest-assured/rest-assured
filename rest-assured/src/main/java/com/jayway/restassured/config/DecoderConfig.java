/*
 * Copyright 2012 the original author or authors.
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
 * Allows you to specify configuration for the decoder.
 */
public class DecoderConfig {

    private final String defaultContentCharset;

    /**
     * Configure the decoder config to use {@value org.apache.http.protocol.HTTP#DEFAULT_CONTENT_CHARSET} for content decoding.
     */
    public DecoderConfig() {
        this(HTTP.DEFAULT_CONTENT_CHARSET);
    }

    public DecoderConfig(String defaultContentCharset) {
        Validate.notBlank(defaultContentCharset, "Default decoder content charset to cannot be blank");
        this.defaultContentCharset = defaultContentCharset;
    }

    public String defaultContentCharset() {
        return defaultContentCharset;
    }

    public DecoderConfig defaultContentCharset(String charset) {
        return new DecoderConfig(charset);
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
     * @return The same encoder config instance.
     */
    public DecoderConfig and() {
        return this;
    }
}
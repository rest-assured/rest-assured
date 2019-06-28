/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.config;

import io.restassured.http.ContentType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Allows you to specify configuration for the encoder
 */
public class EncoderConfig implements Config {

    private static final String UTF_8 = "UTF-8";
    private static final String ISO_8859_1 = "ISO-8859-1";

    private static final Map<String, String> DEFAULT_CHARSET_FOR_CONTENT_TYPE = new HashMap<String, String>() {{
        put(ContentType.JSON.toString(), UTF_8);
        put("text/json", UTF_8);
    }};

    private final String defaultContentCharset;
    private final String defaultQueryParameterCharset;
    private final boolean shouldAppendDefaultContentCharsetToContentTypeIfUndefined;
    private final Map<String, ContentType> contentEncoders;
    private final Map<String, String> contentTypeToDefaultCharset;
    private final boolean isUserDefined;

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
        this(ISO_8859_1, UTF_8, true, new HashMap<>(), DEFAULT_CHARSET_FOR_CONTENT_TYPE, true);
    }

    public EncoderConfig(String defaultContentCharset, String defaultQueryParameterCharset) {
        this(defaultContentCharset, defaultQueryParameterCharset, true, new HashMap<>(), DEFAULT_CHARSET_FOR_CONTENT_TYPE, true);
    }

    private EncoderConfig(String defaultContentCharset, String defaultQueryParameterCharset,
                          boolean shouldAppendDefaultContentCharsetToContentTypeIfUndefined,
                          Map<String, ContentType> encoders, Map<String, String> contentTypeToDefaultCharset,
                          boolean isUserDefined) {
        Validate.notBlank(defaultContentCharset, "Default encoder content charset to cannot be blank. See \"appendDefaultContentCharsetToContentTypeIfMissing\" method if you like to disable automatically appending the charset to the content-type.");
        Validate.notBlank(defaultQueryParameterCharset, "Default protocol charset to cannot be blank.");
        this.contentTypeToDefaultCharset = new HashMap<>(contentTypeToDefaultCharset);
        this.defaultContentCharset = defaultContentCharset;
        this.defaultQueryParameterCharset = defaultQueryParameterCharset;
        this.shouldAppendDefaultContentCharsetToContentTypeIfUndefined = shouldAppendDefaultContentCharsetToContentTypeIfUndefined;
        this.contentEncoders = encoders;
        this.isUserDefined = isUserDefined;
    }

    /**
     * @return The default charset for a specific content-type. It will have precedence over {@link #defaultContentCharset()}.
     */
    public String defaultCharsetForContentType(String contentType) {
        if (StringUtils.isEmpty(contentType)) {
            return defaultContentCharset();
        }
        String charset = contentTypeToDefaultCharset.get(trim(contentType).toLowerCase());
        if (charset == null) {
            return defaultContentCharset();
        }
        return charset;
    }

    /**
     * @return A map that contains default charset for a specific content-type. It will have precedence over {@link #defaultContentCharset()}.
     */
    public String defaultCharsetForContentType(ContentType contentType) {
        if (contentType == null) {
            return defaultContentCharset();
        }
        return defaultCharsetForContentType(contentType.toString());
    }

    /**
     * @return A map that contains default charset for a specific content-type. It will have precedence over {@link #defaultContentCharset()}.
     */
    public boolean hasDefaultCharsetForContentType(String contentType) {
        return !StringUtils.isBlank(contentType) && contentTypeToDefaultCharset.containsKey(trim(contentType).toLowerCase());
    }

    /**
     * @return The default content charset for all content-types (unless specified in {@link #defaultCharsetForContentType(String)}.
     */
    public String defaultContentCharset() {
        return defaultContentCharset;
    }

    /**
     * @return The default query parameter charset
     */
    public String defaultQueryParameterCharset() {
        return defaultQueryParameterCharset;
    }

    /**
     * Specify the default charset to use for the specific content-type if it's not specified in the content-type header explicitly
     *
     * @param charset     The charset to use as default (unless specified explicitly)
     * @param contentType The content-type
     * @return A new instance of {@link EncoderConfig}
     */
    public EncoderConfig defaultCharsetForContentType(String charset, String contentType) {
        notNull(charset, "Charset");
        notNull(contentType, "ContentType");
        Map<String, String> map = new HashMap<>(contentTypeToDefaultCharset);
        map.put(trim(contentType).toLowerCase(), trim(charset));
        return new EncoderConfig(charset, defaultQueryParameterCharset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined, contentEncoders, map, true);
    }

    /**
     * Specify the default charset to use for the specific content-type if it's not specified in the content-type header explicitly
     *
     * @param charset     The charset to use as default (unless specified explicitly)
     * @param contentType The content-type
     * @return A new instance of {@link EncoderConfig}
     */
    public EncoderConfig defaultCharsetForContentType(String charset, ContentType contentType) {
        notNull(charset, "Charset");
        notNull(contentType, ContentType.class);
        Map<String, String> map = new HashMap<>(contentTypeToDefaultCharset);
        for (String ct : contentType.getContentTypeStrings()) {
            map.put(ct.toLowerCase(), trim(charset));
        }
        return new EncoderConfig(charset, defaultQueryParameterCharset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined, contentEncoders, map, true);
    }

    /**
     * Specify the default charset to use for the specific content-type if it's not specified in the content-type header explicitly
     *
     * @param charset     The charset to use as default (unless specified explicitly)
     * @param contentType The content-type
     * @return A new instance of {@link EncoderConfig}
     */
    public EncoderConfig defaultCharsetForContentType(Charset charset, ContentType contentType) {
        notNull(charset, "Charset");
        return defaultCharsetForContentType(charset.toString(), contentType);
    }

    /**
     * Specify the default charset to use for the specific content-type if it's not specified in the content-type header explicitly
     *
     * @param charset     The charset to use as default (unless specified explicitly)
     * @param contentType The content-type
     * @return A new instance of {@link EncoderConfig}
     */
    public EncoderConfig defaultCharsetForContentType(Charset charset, String contentType) {
        notNull(charset, "Charset");
        return defaultCharsetForContentType(charset.toString(), contentType);
    }

    /**
     * Specify the default charset for the body/content in the request specification
     *
     * @param charset The charset to use.
     * @return A new instance of {@link EncoderConfig}
     */
    public EncoderConfig defaultContentCharset(Charset charset) {
        String charsetAsString = notNull(charset, Charset.class).toString();
        return new EncoderConfig(charsetAsString, defaultQueryParameterCharset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined, contentEncoders, contentTypeToDefaultCharset, true);
    }

    /**
     * Specify the default charset for query parameters
     *
     * @param charset The charset to use.
     * @return A new instance of {@link EncoderConfig}
     */
    public EncoderConfig defaultQueryParameterCharset(String charset) {
        return new EncoderConfig(defaultContentCharset, charset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined, contentEncoders, contentTypeToDefaultCharset, true);
    }

    /**
     * Specify the default charset for query parameters
     *
     * @param charset The charset to use.
     * @return A new instance of {@link EncoderConfig}
     */
    @SuppressWarnings("UnusedDeclaration")
    public EncoderConfig defaultQueryParameterCharset(Charset charset) {
        String charsetAsString = notNull(charset, Charset.class).toString();
        return new EncoderConfig(defaultContentCharset, charsetAsString, shouldAppendDefaultContentCharsetToContentTypeIfUndefined, contentEncoders, contentTypeToDefaultCharset, true);
    }

    /**
     * Specify the default charset for the body/content in the request specification
     *
     * @param charset The charset to use.
     * @return A new instance of {@link EncoderConfig}
     */
    public EncoderConfig defaultContentCharset(String charset) {
        return new EncoderConfig(charset, defaultQueryParameterCharset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined, contentEncoders, contentTypeToDefaultCharset, true);
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
     * @return A new {@link EncoderConfig} instance
     */
    public EncoderConfig appendDefaultContentCharsetToContentTypeIfUndefined(boolean shouldAddDefaultContentCharsetToContentTypeIfMissing) {
        return new EncoderConfig(defaultContentCharset, defaultQueryParameterCharset, shouldAddDefaultContentCharsetToContentTypeIfMissing, contentEncoders, contentTypeToDefaultCharset, true);
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

    public boolean isUserConfigured() {
        return isUserDefined;
    }

    /**
     * @see #encodeContentTypeAs(String, ContentType)
     */
    public boolean hasContentEncoders() {
        return !contentEncoders.isEmpty();
    }

    /**
     * @return A map of all specified content encoders
     * @see #encodeContentTypeAs(String, ContentType)
     */
    public Map<String, ContentType> contentEncoders() {
        return Collections.unmodifiableMap(contentEncoders);
    }

    /**
     * Encodes the content (body) of the request specified with the given <code>contentType</code> with the same
     * encoder used by the supplied <code>encoder</code>. This is useful only if REST Assured picks the wrong
     * encoder (or can't recognize it) for the given content-type.
     *
     * @param contentType The content-type to encode with a specific encoder.
     * @param encoder     The encoder to use for the given content-type.
     * @return A new {@link EncoderConfig} instance
     */
    public EncoderConfig encodeContentTypeAs(String contentType, ContentType encoder) {
        notNull(contentType, "Content-Type to encode");
        notNull(encoder, ContentType.class);
        Map<String, ContentType> newMap = new HashMap<>(contentEncoders);
        newMap.put(contentType, encoder);
        return new EncoderConfig(defaultContentCharset, defaultQueryParameterCharset, shouldAppendDefaultContentCharsetToContentTypeIfUndefined, newMap, contentTypeToDefaultCharset, true);
    }
}
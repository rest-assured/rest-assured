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

import io.restassured.internal.common.assertion.AssertParameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.mime.HttpMultipartMode;

import java.nio.charset.Charset;

/**
 * Enables default configuration of how to handle multi-parts.
 */
public class MultiPartConfig implements Config {

    private static final String DEFAULT_CONTROL_NAME = "file";
    private static final String DEFAULT_FILE_NAME = "file";
    private static final String DEFAULT_SUBTYPE = "form-data";
    private static final String DEFAULT_CHARSET = null;
    private static final String DEFAULT_BOUNDARY = null;

    private final String defaultControlName;
    private final String defaultFileName;
    private final String defaultSubtype;
    private final String defaultBoundary;
    private final String defaultCharset;

    private final boolean isUserConfigured;

    /**
     * Create a new MultiPartConfig with default control name equal to {@value #DEFAULT_CONTROL_NAME} and
     * default file name equal to {@value #DEFAULT_FILE_NAME} and default subtype {@value #DEFAULT_SUBTYPE} and
     * default boundary <code>null</code> (which means it'll be automatically generated) and default charset US-ASCII.
     */
    public MultiPartConfig() {
        this(DEFAULT_CONTROL_NAME, DEFAULT_FILE_NAME, DEFAULT_SUBTYPE, DEFAULT_BOUNDARY, DEFAULT_CHARSET, false);
    }

    private MultiPartConfig(String defaultControlName, String defaultFileName, String defaultSubtype, String defaultBoundary,
                            String defaultCharset, boolean isUserConfigured) {
        this.defaultControlName = defaultControlName;
        this.defaultBoundary = defaultBoundary;
        this.defaultFileName = StringUtils.trimToNull(defaultFileName);
        this.defaultSubtype = StringUtils.trimToNull(defaultSubtype);
        this.defaultCharset = StringUtils.trimToNull(defaultCharset);
        AssertParameter.notNull(this.defaultControlName, "Default control name");
        AssertParameter.notNull(this.defaultSubtype, "Default subtype");
        this.isUserConfigured = isUserConfigured;
    }

    /**
     * Specify the default control name to use if not defined explicitly in multi-part request.
     * <p>
     * Default is {@value #DEFAULT_CONTROL_NAME}
     * </p>
     *
     * @param defaultControlName The control name to use
     * @return A new instance of {@link MultiPartConfig}
     */
    public MultiPartConfig defaultControlName(String defaultControlName) {
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, defaultBoundary, defaultCharset, true);
    }

    /**
     * Specify the default filename to use if not defined explicitly in multi-part request.
     * <p>
     * Default is {@value #DEFAULT_FILE_NAME}
     * </p>
     *
     * @param defaultFileName The file name to use
     * @return A new instance of {@link MultiPartConfig}
     */
    public MultiPartConfig defaultFileName(String defaultFileName) {
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, defaultBoundary, defaultCharset, true);
    }

    /**
     * Specify the default subtype to use if not defined explicitly in when making the multi-part request.
     * This will control how the Content-Type will be constructed for multipart requests when using REST Assured
     * when no Content-Type header has been explicitly defined. For example if subtype is set to "mixed" then the
     * Content-Type header will be "multipart/mixed" if not specified explicitly.
     * <p>
     * Default is {@value #DEFAULT_SUBTYPE}
     * </p>
     *
     * @param defaultSubtype The default subtype to use in multipart requests. Default is {@value #DEFAULT_SUBTYPE}.
     * @return A new instance of {@link MultiPartConfig}
     */
    public MultiPartConfig defaultSubtype(String defaultSubtype) {
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, defaultBoundary, defaultCharset, true);
    }

    /**
     * Specify default filename to be empty if not defined explicitly in multi-part request.
     * This means that the "filename" field will not be added to the multi-part.
     * <p>
     * This is the same as calling {@link #defaultFileName(String)} with <code>null</code>.
     * </p>
     *
     * @return A new instance of {@link MultiPartConfig}
     */
    public MultiPartConfig emptyDefaultFileName() {
        return new MultiPartConfig(defaultControlName, null, defaultSubtype, defaultBoundary, defaultCharset, true);
    }

    /**
     * @return The default control name that'll be used unless explicitly defined in multi-part request.
     */
    public String defaultControlName() {
        return defaultControlName;
    }

    /**
     * @return The default file name that'll be used unless explicitly defined in multi-part request.
     */
    public String defaultFileName() {
        return defaultFileName;
    }

    /**
     * @return The default subtype that'll be used unless explicitly defined in the Content-Type header.
     */
    public String defaultSubtype() {
        return defaultSubtype;
    }

    /**
     * Specify an explicit default multipart boundary to use when sending multi-part data.
     *
     * @param defaultBoundary The boundary to set
     * @return An updated MultiPartConfig
     */
    public MultiPartConfig defaultBoundary(String defaultBoundary) {
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, defaultBoundary, defaultCharset, true);
    }

    /**
     * Specify a default charset to use for multi-parts (default is US-ASCII). This affects the encoding of the multipart body (such as the
     * control name) but <i>not</i> the actual <i>content</i> (such as the a JSON or String document).
     * <p>
     * <b>NOTE:</b> This setting is <i>only</i> taken into account if {@link HttpClientConfig#httpMultipartMode(HttpMultipartMode)} is set to
     * something other than {@link HttpMultipartMode#STRICT} (which is the default). So if you want this setting to apply you also need to
     * explicitly change the multipart mode, for example:
     *
     * <pre>
     * given().
     *         config(RestAssuredConfig.config()
     *                                 .httpClient(HttpClientConfig.httpClientConfig().httpMultipartMode(BROWSER_COMPATIBLE))
     *                                 .multiPartConfig(multiPartConfig().defaultCharset("UTF-8"))).
     * when().
     *        post("/x"). ..
     * </pre>
     * </p>
     * @param defaultCharset The default charset to use.
     * @return An updated MultiPartConfig
     */
    public MultiPartConfig defaultCharset(String defaultCharset) {
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, defaultBoundary, defaultCharset, true);
    }

    /**
     * Specify a default charset to use for multi-parts (default is US-ASCII). This affects the encoding of the multipart body (such as the
     * control name) but <i>not</i> the actual <i>content</i> (such as the a JSON or String document).
     * <p>
     * <b>NOTE:</b> This setting is <i>only</i> taken into account if {@link HttpClientConfig#httpMultipartMode(HttpMultipartMode)} is set to
     * something other than {@link HttpMultipartMode#STRICT} (which is the default). So if you want this setting to apply you also need to
     * explicitly change the multipart mode, for example:
     *
     * <pre>
     * given().
     *         config(RestAssuredConfig.config()
     *                                 .httpClient(HttpClientConfig.httpClientConfig().httpMultipartMode(BROWSER_COMPATIBLE))
     *                                 .multiPartConfig(multiPartConfig().defaultCharset("UTF-8"))).
     * when().
     *        post("/x"). ..
     * </pre>
     * </p>
     * @param defaultCharset The default charset to use.
     * @return An updated MultiPartConfig
     */
    public MultiPartConfig defaultCharset(Charset defaultCharset) {
        String charsetAsString = AssertParameter.notNull(defaultCharset, Charset.class).toString();
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, defaultBoundary, charsetAsString, true);
    }

    /**
     * Get the default multipart boundary to use when sending multi-part data.
     *
     * @return The boundary
     */
    public String defaultBoundary() {
        return defaultBoundary;
    }

    /**
     * Get the default multipart charset to use when sending multi-part data.
     *
     * @return The boundary
     */
    public String defaultCharset() {
        return defaultCharset;
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    /**
     * @return A static way to create a new MultiPartConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static MultiPartConfig multiPartConfig() {
        return new MultiPartConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same MultiPartConfig instance.
     */
    public MultiPartConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same MultiPartConfig instance.
     */
    public MultiPartConfig with() {
        return this;
    }
}

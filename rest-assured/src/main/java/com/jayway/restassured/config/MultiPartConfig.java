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

import com.jayway.restassured.internal.assertion.AssertParameter;
import org.apache.commons.lang3.StringUtils;

/**
 * Enables default configuration of how to handle multi-parts.
 */
public class MultiPartConfig implements Config {

    private static final String DEFAULT_CONTROL_NAME = "file";
    private static final String DEFAULT_FILE_NAME = "file";
    private static final String DEFAULT_SUBTYPE = "form-data";

    private final String defaultControlName;
    private final String defaultFileName;
    private final String defaultSubtype;

    private final boolean isUserConfigured;

    /**
     * Create a new MultiPartConfig with default control name equal to {@value #DEFAULT_CONTROL_NAME} and
     * default file name equal to {@value #DEFAULT_FILE_NAME} and default subtype {@value #DEFAULT_SUBTYPE} .
     */
    public MultiPartConfig() {
        this(DEFAULT_CONTROL_NAME, DEFAULT_FILE_NAME, DEFAULT_SUBTYPE, false);
    }

    private MultiPartConfig(String defaultControlName, String defaultFileName, String defaultSubtype, boolean isUserConfigured) {
        this.defaultControlName = defaultControlName;
        this.defaultFileName = StringUtils.trimToNull(defaultFileName);
        this.defaultSubtype = StringUtils.trimToNull(defaultSubtype);
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
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, true);
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
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, true);
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
        return new MultiPartConfig(defaultControlName, defaultFileName, defaultSubtype, true);
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
        return new MultiPartConfig(defaultControlName, null, defaultSubtype, true);
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

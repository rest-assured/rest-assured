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

import io.restassured.filter.log.LogDetail;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URL;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Configuration for CSRF related properties
 */
public class CsrfConfig implements Config {

    public static final String DEFAULT_CSRF_HEADER_NAME = "X-CSRF-TOKEN";
    public static final String DEFAULT_CSRF_INPUT_FIELD_NAME = "_csrf";
    public static final String DEFAULT_CSRF_META_TAG_NAME = "_csrf_header";

    private final boolean automaticallyApplyCookies;
    private final boolean isUserConfigured;
    private final String csrfTokenPath;
    private final String csrfInputFieldName;
    private final String csrfMetaTagName;
    private final String csrfHeaderName;
    private final CsrfPrioritization csrfPrioritization;
    private final LogConfig logConfig;
    private final LogDetail logDetail;

    /**
     * Create a default
     */
    public CsrfConfig() {
        this(null, DEFAULT_CSRF_INPUT_FIELD_NAME, DEFAULT_CSRF_META_TAG_NAME, DEFAULT_CSRF_HEADER_NAME, CsrfPrioritization.HEADER, null, null, true, false);
    }

    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     * For example: "/login"
     */
    public CsrfConfig(String csrfTokenPath) {
        this(notNull(StringUtils.trimToNull(csrfTokenPath), "csrfTokenPath"), DEFAULT_CSRF_INPUT_FIELD_NAME, DEFAULT_CSRF_META_TAG_NAME, DEFAULT_CSRF_HEADER_NAME, CsrfPrioritization.HEADER, null, null, true, true);
    }

    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public CsrfConfig(URI csrfTokenPath) {
        this(notNull(csrfTokenPath, "csrfTokenPath").toString());
    }

    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public CsrfConfig(URL csrfTokenPath) {
        this(notNull(csrfTokenPath, "csrfTokenPath").toString());
    }

    private CsrfConfig(String csrfTokenPath, String csrfInputFieldName, String csrfMetaTagName, String csrfHeaderName, CsrfPrioritization csrfPrioritization, LogConfig logConfig, LogDetail logDetail, boolean automaticallyApplyCookies, boolean isUserConfigured) {
        notNull(csrfPrioritization, CsrfPrioritization.class);
        notNull(StringUtils.trimToNull(csrfInputFieldName), "csrfInputFieldName");
        notNull(StringUtils.trimToNull(csrfMetaTagName), "csrfMetaTagName");
        notNull(StringUtils.trimToNull(csrfHeaderName), "csrfHeaderName");

        this.csrfTokenPath = StringUtils.trimToNull(csrfTokenPath);
        this.csrfInputFieldName = StringUtils.trimToNull(csrfInputFieldName);
        this.csrfMetaTagName = StringUtils.trimToNull(csrfMetaTagName);
        this.csrfHeaderName = StringUtils.trimToNull(csrfHeaderName);
        this.csrfPrioritization = csrfPrioritization;
        this.logConfig = logConfig;
        this.logDetail = logDetail;
        this.isUserConfigured = isUserConfigured;
        this.automaticallyApplyCookies = automaticallyApplyCookies;
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    public boolean isCsrfEnabled() {
        return csrfTokenPath != null;
    }

    public static CsrfConfig csrfConfig() {
        return new CsrfConfig();
    }

    /**
     * Enable Cross-site request forgery (csrf) support by including the csrf token specified in a meta tag as a header.
     * For example, if you've specified the {@link #csrfTokenPath} to {@code "/login"} and the login page looks like this:
     * <pre>
     * &lt;html&gt;
     * &lt;head&gt;
     *     &lt;title&gt;Login&lt;/title&gt;
     *     &lt;meta name="_csrf_header" content="ab8722b1-1f23-4dcf-bf63-fb8b94be4107"/&gt;
     * &lt;/head&gt;
     * &lt;body&gt;
     *          ..
     * &lt;/body&gt;
     * &lt;/html&gt;
     * </pre>
     * The csrf meta tag name is called <code>_csrf_header</code> (which is the default meta tag name used by REST Assured). If the server returns a different name
     * you can specify it with this method. REST Assured will then send the CSRF token as a header with name {@link #csrfHeaderName} (default {@value #DEFAULT_CSRF_HEADER_NAME}).
     * <p/>
     * <b>Important:</b> When enabling csrf support then REST Assured <b>must always</b> make an additional request to the server in order to
     * be able to include in the csrf value which will slow down the tests.
     *
     * @param csrfMetaTagName The name of the meta tag containing the CSRF token
     * @return A new CsrfConfig instance.
     * @see #csrfHeaderName
     */
    public CsrfConfig csrfMetaTagName(String csrfMetaTagName) {
        notNull(StringUtils.trimToNull(csrfMetaTagName), "CSRF meta tag name");
        return new CsrfConfig(csrfTokenPath, csrfMetaTagName, csrfMetaTagName, csrfHeaderName, csrfPrioritization, logConfig, logDetail, automaticallyApplyCookies, true);
    }

    /**
     * Enable Cross-site request forgery (csrf) support by including the csrf value of the input field with the specified name.
     * For example, if you've specified the {@link #csrfTokenPath} to {@code "/login"} and the login page looks like this:
     * <pre>
     * &lt;html&gt;
     * &lt;head&gt;
     *     &lt;title&gt;Login&lt;/title&gt;
     * &lt;/head&gt;
     * &lt;body&gt;
     * &lt;form action=&quot;j_spring_security_check_with_csrf&quot; method=&quot;POST&quot;&gt;
     *     &lt;table&gt;
     *         &lt;tr&gt;
     *             &lt;td&gt;User:&amp;nbsp;&lt;/td&gt;
     *             &lt;td&gt;&lt;input type=&quot;text&quot; name=&quot;j_username&quot;&gt;&lt;/td&gt;
     *         &lt;/tr&gt;
     *         &lt;tr&gt;
     *             &lt;td&gt;Password:&lt;/td&gt;
     *             &lt;td&gt;&lt;input type=&quot;password&quot; name=&quot;j_password&quot;&gt;&lt;/td&gt;
     *         &lt;/tr&gt;
     *         &lt;tr&gt;
     *             &lt;td colspan=&quot;2&quot;&gt;&lt;input name=&quot;submit&quot; type=&quot;submit&quot;/&gt;&lt;/td&gt;
     *         &lt;/tr&gt;
     *     &lt;/table&gt;
     *     &lt;input type=&quot;hidden&quot; name=&quot;_csrf&quot; value=&quot;8adf2ea1-b246-40aa-8e13-a85fb7914341&quot;/&gt;
     * &lt;/form&gt;
     * &lt;/body&gt;
     * &lt;/html&gt;
     * </pre>
     * The csrf field name is called <code>_csrf</code> (which is the default input field name used by REST Assured).
     * <p/>
     * <b>Important:</b> When enabling csrf support then REST Assured <b>must always</b> make an additional request to the server in order to
     * be able to include in the csrf value which will slow down the tests.
     *
     * @param inputFieldName The name of the input field containing the CSRF token
     * @return A new CsrfConfig instance.
     */
    public CsrfConfig csrfInputFieldName(String inputFieldName) {
        notNull(StringUtils.trimToNull(inputFieldName), "CSRF input field name");
        return new CsrfConfig(csrfTokenPath, inputFieldName, csrfMetaTagName, csrfHeaderName, csrfPrioritization, logConfig, logDetail, automaticallyApplyCookies, true);
    }

    /**
     * Enables logging with log level {@link LogDetail#ALL} of the request made to {@link #csrfTokenPath(String)}.
     * Both the request and the response are logged.
     *
     * @return A new CsrfConfig instance.
     */
    public CsrfConfig loggingEnabled() {
        return loggingEnabled(LogDetail.ALL);
    }

    /**
     * Enables logging with the supplied logDetail of the request made to {@link #csrfTokenPath(String)}.
     * Both the request and the response are logged.
     *
     * @return A new CsrfConfig instance.
     */
    public CsrfConfig loggingEnabled(LogDetail logDetail) {
        return loggingEnabled(logDetail, new LogConfig());
    }

    /**
     * Enables logging with log level {@link LogDetail#ALL} of the request made to {@link #csrfTokenPath(String)}
     * using the specified {@link LogConfig}. Both the request and the response are logged.
     *
     * @return A new CsrfConfig instance.
     */
    public CsrfConfig loggingEnabled(LogConfig logConfig) {
        return loggingEnabled(LogDetail.ALL, logConfig);
    }

    /**
     * Enables logging with the supplied log detail of the request made to {@link #csrfTokenPath(String)} using the
     * specified {@link LogConfig}. Both the request and the response are logged.
     *
     * @return A new CsrfConfig instance.
     */
    public CsrfConfig loggingEnabled(LogDetail logDetail, LogConfig logConfig) {
        notNull(logDetail, LogDetail.class);
        notNull(logConfig, LogConfig.class);
        return new CsrfConfig(csrfTokenPath, csrfInputFieldName, csrfMetaTagName, csrfHeaderName, csrfPrioritization, logConfig, logDetail, automaticallyApplyCookies, true);
    }

    /**
     * Specify the name of the header that REST Assured will send the CSRF token <i>if</i> REST Assured detects that it should send the token in a header.
     * REST Assured detects this by looking for a <code>&lt;meta&gt;</code> tag (in the <code>&lt;head&gt;</code>) with the name specified by {@link  #csrfMetaTagName} (default is {@value #DEFAULT_CSRF_META_TAG_NAME}).
     * If this meta tag exist, REST Assured will send the CSRF token in the header.
     *
     * @param csrfHeaderName The name of the header that'll convey the CSRF token to the server, default is {@value #DEFAULT_CSRF_HEADER_NAME}.
     * @return A new CsrfConfig instance.
     * @see #csrfMetaTagName(String)
     */
    public CsrfConfig csrfHeaderName(String csrfHeaderName) {
        notNull(StringUtils.trimToNull(csrfHeaderName), "CSRF header name");
        return new CsrfConfig(csrfTokenPath, csrfInputFieldName, csrfMetaTagName, csrfHeaderName, csrfPrioritization, logConfig, logDetail, automaticallyApplyCookies, true);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same failure config instance.
     */
    public CsrfConfig with() {
        return this;
    }


    /**
     * Syntactic sugar
     *
     * @return The same CsrfConfig instance
     */
    public CsrfConfig and() {
        return this;
    }

    /**
     * Get the configured {@link CsrfPrioritization} strategy
     *
     * @return A new CsrfConfig instance.
     */
    public CsrfPrioritization getCsrfPrioritization() {
        return csrfPrioritization;
    }

    /**
     * Check if the {@link CsrfPrioritization} is equal to the supplied <code>csrfPrioritization</code>.
     *
     * @return <code>true</code> if match, <code>false</code> otherwise.
     */
    public boolean isCsrfPrioritization(CsrfPrioritization csrfPrioritization) {
        return this.csrfPrioritization == csrfPrioritization;
    }

    /**
     * Check if the cookies returned in the GET response from the page/header containing the CSRF token should be automatically applied to the subsequent request.
     *
     * @return <code>true</code> if cookies are applied, <code>false</code> otherwise.
     */
    public boolean isAutomaticallyApplyCookies() {
        return this.automaticallyApplyCookies;
    }

    /*
     * Configure if the cookies returned in the GET response from the page/header containing the CSRF token should be automatically applied to the subsequent request.
     *
     * @param automaticallyApplyCookies The setting
     * @return A new CsrfConfig instance.
     */
    public CsrfConfig automaticallyApplyCookies(boolean automaticallyApplyCookies) {
        return new CsrfConfig(csrfTokenPath, csrfInputFieldName, csrfMetaTagName, csrfHeaderName, csrfPrioritization, logConfig, logDetail, automaticallyApplyCookies, true);
    }


    /**
     * Defines how REST Assured should prioritize form vs header csrf tokens if both are present in the response page. Default is {@link CsrfPrioritization#HEADER}.
     *
     * @param csrfPrioritization The csrf prioritization
     * @return A new CsrfConfig instance.
     */
    public CsrfConfig csrfPrioritization(CsrfPrioritization csrfPrioritization) {
        return new CsrfConfig(csrfTokenPath, csrfInputFieldName, csrfMetaTagName, csrfHeaderName, csrfPrioritization, logConfig, logDetail, automaticallyApplyCookies, true);
    }

    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public CsrfConfig csrfTokenPath(String csrfTokenPath) {
        return new CsrfConfig(notNull(StringUtils.trimToNull(csrfTokenPath), "csrfTokenPath"), csrfInputFieldName, csrfMetaTagName, csrfHeaderName, csrfPrioritization, logConfig, logDetail, automaticallyApplyCookies, true);
    }

    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public CsrfConfig csrfTokenPath(URI csrfTokenPath) {
        return csrfTokenPath(notNull(csrfTokenPath, "csrfTokenPath").toString());
    }

    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public CsrfConfig csrfTokenPath(URL csrfTokenPath) {
        return csrfTokenPath(notNull(csrfTokenPath, "csrfTokenPath").toString());
    }

    /*
     * The path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public String getCsrfTokenPath() {
        return csrfTokenPath;
    }

    /*
     * @return The specified csrf meta field name or <code>null</code> if undefined
     */
    public String getCsrfMetaTagName() {
        return csrfMetaTagName;
    }

    /*
     * @return The name of the header in which REST Assured will send the CSRF token (if applicable).
     */
    public String getCsrfHeaderName() {
        return csrfHeaderName;
    }

    /**
     * @return The specified csrf input field name or <code>null</code> if undefined
     */
    public String getCsrfInputFieldName() {
        return csrfInputFieldName;
    }

    /**
     * @return The logging configuration
     */
    public LogConfig getLogConfig() {
        return logConfig;
    }

    /**
     * @return <code>true</code> if logging is enabled or <code>false</code> otherwise.
     */
    public boolean isLoggingEnabled() {
        return logConfig != null && logDetail != null;
    }

    /**
     * @return The specified log detail or <code>null</code> if undefined
     */
    public LogDetail getLogDetail() {
        return logDetail;
    }


    /**
     * Defines how REST Assured should prioritize form vs header csrf tokens if both are present in the response page.
     */
    public enum CsrfPrioritization {
        FORM, HEADER
    }
}
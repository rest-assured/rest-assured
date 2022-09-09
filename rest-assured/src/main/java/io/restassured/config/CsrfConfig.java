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

    private final boolean isUserConfigured;
    private final String csrfTokenPath;
    private final String csrfInputFieldName;
    private final boolean autoDetectCsrfInputFieldName;
    private final boolean sendCsrfTokenAsFormParam;
    private final LogConfig logConfig;
    private final LogDetail logDetail;

    /**
     * Create a default
     */
    public CsrfConfig() {
        this(null, null, true, true, null, null, false);
    }

    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     * For example: "/login"
     */
    public CsrfConfig(String csrfTokenPath) {
        this(notNull(StringUtils.trimToNull(csrfTokenPath), "csrfTokenPath"), null, true, true, null, null, true);
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

    private CsrfConfig(String csrfTokenPath, String csrfInputFieldName, boolean autoDetectCsrfInputFieldName, boolean sendCsrfTokenAsFormParam, LogConfig logConfig, LogDetail logDetail, boolean isUserConfigured) {
        this.csrfTokenPath = StringUtils.trimToNull(csrfTokenPath);
        if (StringUtils.isNotBlank(csrfInputFieldName) && autoDetectCsrfInputFieldName) {
            throw new IllegalArgumentException("You cannot provide an explicit csrf input field name and autoDetectCsrfFieldName=true at the same time.");
        }
        this.csrfInputFieldName = StringUtils.trimToNull(csrfInputFieldName);
        this.autoDetectCsrfInputFieldName = autoDetectCsrfInputFieldName;
        this.sendCsrfTokenAsFormParam = sendCsrfTokenAsFormParam;
        this.logConfig = logConfig;
        this.logDetail = logDetail;
        this.isUserConfigured = isUserConfigured;
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    public boolean isCsrfEnabled() {
        return csrfTokenPath != null && (StringUtils.isNotBlank(csrfInputFieldName) || autoDetectCsrfInputFieldName);
    }

    public static CsrfConfig csrfConfig() {
        return new CsrfConfig();
    }

    /**
     * Enable Cross-site request forgery (csrf) support by automatically trying to find the name and value of the csrf input field.
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
     * The csrf field name is called <code>_csrf</code> and REST Assured will autodetect its name since the field name is the only <code>hidden</code> field on this page.
     * If auto-detection fails you can consider using {@link #csrfInputFieldName(String)}.
     * <p/>
     * <b>Important:</b> When enabling csrf support then REST Assured <b>must always</b> make an additional request to the server in order to
     * be able to include in the csrf value which will slow down the tests.
     *
     * @return A new CsrfConfig instance.
     * @see #csrfInputFieldName(String)
     */
    public CsrfConfig autoDetectCsrfInputFieldName() {
        return new CsrfConfig(csrfTokenPath, null, true, sendCsrfTokenAsFormParam, logConfig, logDetail, true);
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
     * The csrf field name is called <code>_csrf</code>.
     * <p/>
     * <b>Important:</b> When enabling csrf support then REST Assured <b>must always</b> make an additional request to the server in order to
     * be able to include in the csrf value which will slow down the tests.
     *
     * @param inputFieldName The name of the input field containing the CSRF value
     * @return A new CsrfConfig instance.
     * @see #autoDetectCsrfInputFieldName()
     */
    public CsrfConfig csrfInputFieldName(String inputFieldName) {
        notNull(StringUtils.trimToNull(inputFieldName), "CSRF input field name");
        return new CsrfConfig(csrfTokenPath, inputFieldName, false, sendCsrfTokenAsFormParam, logConfig, logDetail, true);
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
        return new CsrfConfig(csrfTokenPath, csrfInputFieldName, autoDetectCsrfInputFieldName, sendCsrfTokenAsFormParam, logConfig, logDetail, true);
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


    /*
     * Specify a path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public CsrfConfig csrfTokenPath(String csrfTokenPath) {
        return new CsrfConfig(notNull(StringUtils.trimToNull(csrfTokenPath), "csrfTokenPath"), csrfInputFieldName, autoDetectCsrfInputFieldName, sendCsrfTokenAsFormParam, logConfig, logDetail, true);
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

    /**
     * @return Configure form authentication to send the csrf token in a header.
     */
    public CsrfConfig sendCsrfTokenAsHeader() {
        return new CsrfConfig(csrfTokenPath, csrfInputFieldName, autoDetectCsrfInputFieldName, false, logConfig, logDetail, true);
    }

    /**
     * @return Configure form authentication to send the csrf token as a form parameter (default setting).
     */
    public CsrfConfig sendCsrfTokenAsFormParam() {
        return new CsrfConfig(csrfTokenPath, csrfInputFieldName, autoDetectCsrfInputFieldName, true, logConfig, logDetail, true);
    }

    /*
     * The path that is used to get the CSRF token. This token will be used automatically by REST Assured for subsequent calls to the API.
     */
    public String getCsrfTokenPath() {
        return csrfTokenPath;
    }

    /**
     * @return The specified csrf input field name or <code>null</code> if undefined
     */
    public String getCsrfInputFieldName() {
        return csrfInputFieldName;
    }

    /**
     * @return <code>true</code> if csrf input field name is defined or <code>false</code> otherwise.
     */
    public boolean hasCsrfInputFieldName() {
        return StringUtils.isNotBlank(csrfInputFieldName);
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
     * @return <code>true</code> if auto detection of csrf field name is enabled, <code>false</code> otherwise.
     */
    public boolean isAutoDetectCsrfInputFieldName() {
        return autoDetectCsrfInputFieldName;
    }

    /**
     * @return <code>true</code> if the csrf token should be sent as a form param or <code>false</code> if it's sent as a header.
     */
    public boolean shouldSendCsrfTokenAsFormParam() {
        return sendCsrfTokenAsFormParam;
    }
}
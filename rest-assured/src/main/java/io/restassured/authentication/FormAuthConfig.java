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

package io.restassured.authentication;

import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static java.lang.String.format;

/**
 * Configuration of form authentication to correctly identify which form that contains the username and password
 * and the action of the form.
 */
public class FormAuthConfig {
    private final String formAction;
    private final String userInputTagName;
    private final String passwordInputTagName;
    private final LogConfig logConfig;
    private final LogDetail logDetail;
    private final List<String> additionalInputFieldNames;
    private final String csrfFieldName;
    private final boolean autoDetectCsrfFieldName;
    private final boolean sendCsrfTokenAsFormParam;

    /**
     * Create a form auth config with a pre-defined form action, username input tag, password input tag.
     * E.g. let's say that the login form on your login page looks like this:
     * <pre>
     *  &lt;form action=&quot;/j_spring_security_check&quot;&gt;
     *     &lt;label for=&quot;j_username&quot;&gt;Username&lt;/label&gt;
     *     &lt;input type=&quot;text&quot; name=&quot;j_username&quot; id=&quot;j_username&quot;/&gt;
     *     &lt;br/&gt;
     *     &lt;label for=&quot;j_password&quot;&gt;Password&lt;/label&gt;
     *     &lt;input type=&quot;password&quot; name=&quot;j_password&quot; id=&quot;j_password&quot;/&gt;
     *     &lt;br/&gt;
     *     &lt;input type=&#39;checkbox&#39; name=&#39;_spring_security_remember_me&#39;/&gt; Remember me on this computer.
     *     &lt;br/&gt;
     *     &lt;input type=&quot;submit&quot; value=&quot;Login&quot;/&gt;
     * &lt;/form&gt;
     * </pre>
     * <p/>
     * This means that <code>formAction</code> should be set to <code>/j_spring_security_check</code>, <code>userNameInputTagName</code>
     * should be set to <code>j_username</code> and <code>passwordInputTagName</code> should be set to <code>j_password</code>.
     *
     * @param formAction           The action of the form
     * @param userNameInputTagName The name of the username input tag in the login form
     * @param passwordInputTagName The name of the password input tag in the login form
     */
    public FormAuthConfig(String formAction, String userNameInputTagName, String passwordInputTagName) {
        this(formAction, userNameInputTagName, passwordInputTagName, null, null, null, false, true,
                Collections.<String>emptyList());
    }

    /**
     * Creates a new empty {@link FormAuthConfig}.
     */
    public FormAuthConfig() {
        this(null, null, null);
    }

    private FormAuthConfig(String formAction, String userNameInputTagName, String passwordInputTagName, LogDetail logDetail, LogConfig logConfig,
                           String csrfFieldName, boolean autoDetectCsrfFieldName, boolean sendCsrfTokenAsFormParam,
                           List<String> additionalInputFieldNames) {
        this.formAction = formAction;
        this.userInputTagName = userNameInputTagName;
        this.passwordInputTagName = passwordInputTagName;
        this.logDetail = logDetail;
        this.logConfig = logConfig;
        this.csrfFieldName = csrfFieldName;
        this.autoDetectCsrfFieldName = autoDetectCsrfFieldName;
        this.sendCsrfTokenAsFormParam = sendCsrfTokenAsFormParam;
        this.additionalInputFieldNames = Collections.unmodifiableList(additionalInputFieldNames);
    }

    /**
     * @return A predefined form authentication config for default Spring Security configuration (tested in version 3.0.5) (no CSRF detection).
     */
    public static FormAuthConfig springSecurity() {
        return new FormAuthConfig("/j_spring_security_check", "j_username", "j_password");
    }

    /**
     * Enable Cross-site request forgery (csrf) support when using form authentication by including the csrf value of the input field with the specified name.
     * For example if the login page looks like this:
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
     * @param fieldName The name of the input field
     * @return A new FormAuthConfig instance.
     * @see #withAutoDetectionOfCsrf()
     */
    public FormAuthConfig withCsrfFieldName(String fieldName) {
        notNull(fieldName, "CSRF field name");
        if (autoDetectCsrfFieldName) {
            throw new IllegalStateException("Cannot defined a CSRF field name since the CSRF field name has been marked as auto-detected.");
        }
        return new FormAuthConfig(formAction, userInputTagName, passwordInputTagName, logDetail, logConfig, fieldName, false, sendCsrfTokenAsFormParam, additionalInputFieldNames);
    }


    /**
     * Include additional field when using form authentication by including input field value with the specified name.
     * For example if the login page looks like this:
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
     *     &lt;input type=&quot;hidden&quot; name=&quot;something&quot; value=&quot;8adf2ea1-b246-40aa-8e13-a85fb7914341&quot;/&gt;
     * &lt;/form&gt;
     * &lt;/body&gt;
     * &lt;/html&gt;
     * </pre>
     * and you'd like to include the field named <code>something</code> as an additional form parameter in the request you can do like this:
     *
     * <pre>
     * given().auth().form(..., new FormAuthConfig(..).withAdditionalField("something"). ..
     * </pre>
     * and then REST Assured will send the form parameter <code>something=8adf2ea1-b246-40aa-8e13-a85fb7914341</code> 
     *
     * <p/>
     * <b>Important:</b> When including an additional field without specifying a value then REST Assured <b>must always</b> make an additional request to the server in order to
     * be able to figure out the field value. This will slow down the tests.
     *
     * @param fieldName The first field name to include
     * @return A new FormAuthConfig instance.
     */
    public FormAuthConfig withAdditionalField(String fieldName) {
        notNull(fieldName, "Additional field name");
        List<String> list = new ArrayList<String>(additionalInputFieldNames);
        list.add(fieldName);
        return new FormAuthConfig(formAction, userInputTagName, passwordInputTagName, logDetail, logConfig, csrfFieldName, autoDetectCsrfFieldName, sendCsrfTokenAsFormParam, list);
    }

    /**
     * Include multiple additional fields when using form authentication by including input field values with the specified name.
     * This is the same as {@link #withAdditionalField(String)} but for multiple fields.
     *
     * <p/>
     * <b>Important:</b> When including an additional field without specifying a value then REST Assured <b>must always</b> make an additional request to the server in order to
     * be able to figure out the field value. This will slow down the tests.
     *
     * @param firstFieldName The first additional input field to include
     * @param secondFieldName The second additional input field to include
     * @param additionalFieldNames Additional field name to include (optional)
     * @return A new FormAuthConfig instance.
     */
    public FormAuthConfig withAdditionalFields(String firstFieldName, String secondFieldName, String... additionalFieldNames) {
        notNull(firstFieldName, "First additional field name");
        notNull(secondFieldName, "Second additional field name");
        List<String> list = new ArrayList<String>(additionalInputFieldNames);
        list.add(firstFieldName);
        list.add(secondFieldName);
        if (additionalFieldNames != null && additionalFieldNames.length > 0) {
            list.addAll(Arrays.asList(additionalFieldNames));
        }
        return new FormAuthConfig(formAction, userInputTagName, passwordInputTagName, logDetail, logConfig, csrfFieldName, autoDetectCsrfFieldName, sendCsrfTokenAsFormParam, list);
    }

    /**
     * @return Configure form authentication to send the csrf token in a header.
     */
    public FormAuthConfig sendCsrfTokenAsHeader() {
        return new FormAuthConfig(formAction, userInputTagName, passwordInputTagName, logDetail, logConfig, csrfFieldName, autoDetectCsrfFieldName, false, additionalInputFieldNames);
    }

    /**
     * @return Configure form authentication to send the csrf token as a form parameter (default setting).
     */
    public FormAuthConfig sendCsrfTokenAsFormParam() {
        return new FormAuthConfig(formAction, userInputTagName, passwordInputTagName, logDetail, logConfig, csrfFieldName, autoDetectCsrfFieldName, true, additionalInputFieldNames);
    }

    /**
     * Enable Cross-site request forgery (csrf) support when using form authentication by automatically trying to find the name and value of the csrf input field.
     * For example if the login page looks like this:
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
     * If auto-detection fails you can consider using {@link #withCsrfFieldName(String)}.
     * <p/>
     * <b>Important:</b> When enabling csrf support then REST Assured <b>must always</b> make an additional request to the server in order to
     * be able to include in the csrf value which will slow down the tests.
     *
     * @return A new FormAuthConfig instance.
     * @see #withCsrfFieldName(String)
     */
    public FormAuthConfig withAutoDetectionOfCsrf() {
        if (hasCsrfFieldName()) {
            throw new IllegalStateException(format("Cannot use auto-detection of CSRF field name since a CSRF field name was already defined as '%s'", csrfFieldName));
        }
        return new FormAuthConfig(formAction, userInputTagName, passwordInputTagName, logDetail, logConfig, csrfFieldName, true, sendCsrfTokenAsFormParam, additionalInputFieldNames);
    }

    /**
     * Enables logging with log level {@link LogDetail#ALL} of the request made to authenticate using
     * form authentication. Both the request and the response is logged.
     *
     * @return A new FormAuthConfig instance.
     */

    public FormAuthConfig withLoggingEnabled() {
        return withLoggingEnabled(LogDetail.ALL);
    }

    /**
     * Enables logging with the supplied logDetail of the request made to authenticate using form authentication.
     * Both the request and the response is logged.
     *
     * @return A new FormAuthConfig instance.
     */
    public FormAuthConfig withLoggingEnabled(LogDetail logDetail) {
        return withLoggingEnabled(logDetail, new LogConfig());
    }

    /**
     * Enables logging with log level {@link LogDetail#ALL} of the request made to authenticate using
     * form authentication using the specified {@link LogConfig}. Both the request and the response is logged.
     *
     * @return A new FormAuthConfig instance.
     */
    public FormAuthConfig withLoggingEnabled(LogConfig logConfig) {
        return withLoggingEnabled(LogDetail.ALL, logConfig);
    }

    /**
     * Enables logging with the supplied log detail of the request made to authenticate using form authentication using the
     * specified {@link LogConfig}. Both the request and the response is logged.
     *
     * @return A new FormAuthConfig instance.
     */
    public FormAuthConfig withLoggingEnabled(LogDetail logDetail, LogConfig logConfig) {
        notNull(logDetail, LogDetail.class);
        notNull(logConfig, LogConfig.class);
        return new FormAuthConfig(formAction, userInputTagName, passwordInputTagName, logDetail, logConfig, csrfFieldName, autoDetectCsrfFieldName, sendCsrfTokenAsFormParam, additionalInputFieldNames);
    }

    /**
     * Creates a new empty {@link FormAuthConfig}.
     *
     * @return A new FormAuthConfig instance.
     */
    public static FormAuthConfig formAuthConfig() {
        return new FormAuthConfig(null, null, null);
    }

    /**
     * Syntactic sugar
     *
     * @return The same FormAuthConfig instance
     */
    public FormAuthConfig and() {
        return this;
    }

    public String getFormAction() {
        return formAction;
    }

    public String getUserInputTagName() {
        return userInputTagName;
    }

    /**
     * @return The password input tag name or <code>null</code> if undefined
     */
    public String getPasswordInputTagName() {
        return passwordInputTagName;
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
     * @return The specified csrf field name or <code>null</code> if undefined
     */
    public String getCsrfFieldName() {
        return csrfFieldName;
    }

    /**
     * @return The additional input field names
     */
    public List<String> getAdditionalInputFieldNames() {
        return additionalInputFieldNames;
    }

    /**
     * @return <code>true</code> if csrf field name is defined or <code>false</code> otherwise.
     */
    public boolean hasCsrfFieldName() {
        return StringUtils.isNotBlank(csrfFieldName);
    }

    /**
     * @return <code>true</code> if additional input field name have been specified or <code>false</code> otherwise.
     */
    public boolean hasAdditionalInputFieldNames() {
        return !additionalInputFieldNames.isEmpty();
    }

    /**
     * @return <code>true</code> if auto detection of csrf field name is enabled, <code>false</code> otherwise.
     */
    public boolean isAutoDetectCsrfFieldName() {
        return autoDetectCsrfFieldName;
    }

    /**
     * @return <code>true</code> if the user input tag name is defined or <code>false</code> otherwise.
     */
    public boolean hasUserInputTagName() {
        return StringUtils.isNotBlank(userInputTagName);
    }

    /**
     * @return <code>true</code> if the password input tag name is defined or <code>false</code> otherwise.
     */
    public boolean hasPasswordInputTagName() {
        return StringUtils.isNotBlank(passwordInputTagName);
    }

    /**
     * @return <code>true</code> if the form action is defined or <code>false</code> otherwise.
     */
    public boolean hasFormAction() {
        return StringUtils.isNotBlank(formAction);
    }

    /**
     * @return <code>true</code> if the {@link FormAuthConfig} instance contains settings that require REST Assured to make a request to the server before applying form authentication, <code>false</code> otherwise.
     */
    public boolean requiresParsingOfLoginPage() {
        return !hasFormAction() || !hasUserInputTagName() || !hasPasswordInputTagName() || isAutoDetectCsrfFieldName() || hasCsrfFieldName() ||hasAdditionalInputFieldNames();
    }

    /**
     * @return <code>true</code> if the csrf token should be sent as a form param or <code>false</code> if it's sent as a header.
     */
    public boolean shouldSendCsrfTokenAsFormParam() {
        return sendCsrfTokenAsFormParam;
    }
}

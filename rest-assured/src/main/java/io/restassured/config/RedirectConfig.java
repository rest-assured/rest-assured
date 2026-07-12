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

/**
 * Configures the redirect settings that can be used with RestAssured.
 *
 * @see RestAssuredConfig
 * @see org.apache.http.client.params.ClientPNames
 */
public class RedirectConfig implements Config {

    private final boolean followRedirects;
    private final boolean allowCircularRedirects;
    private final boolean rejectRelativeRedirect;
    private final int maxRedirects;
    private final boolean stripSensitiveHeadersOnCrossHostRedirect;
    private final boolean isUserConfigured;

    /**
     * Create a new RedirectConfig instance with the following configuration by default:
     * <ol>
     * <li>followRedirects = true</li>
     * <li>allowCircularRedirects = false</li>
     * <li>rejectRelativeRedirect = false</li>
     * <li>maxRedirects = 100 </li>
     * <li>stripSensitiveHeadersOnCrossHostRedirect = true </li>
     * </ol>
     */
    public RedirectConfig() {
        this(true, false, false, 100, true, false);
    }

    /**
     * Create a new instance of a RedirectConfig with the supplied settings.
     *
     * @param followRedirects        Configure if REST Assured should follow redirects
     * @param allowCircularRedirects Configure if REST Assured should allow circular redirects
     * @param rejectRelativeRedirect Configure if REST Assured should reject relative redirects
     * @param maxRedirects           Configure the REST Assured maximum number of redirect
     */
    public RedirectConfig(boolean followRedirects, boolean allowCircularRedirects,
                          boolean rejectRelativeRedirect, int maxRedirects) {
        this(followRedirects, allowCircularRedirects, rejectRelativeRedirect, maxRedirects, true, true);
    }

    private RedirectConfig(boolean followRedirects, boolean allowCircularRedirects,
                           boolean rejectRelativeRedirect, int maxRedirects,
                           boolean stripSensitiveHeadersOnCrossHostRedirect, boolean isUserConfigured) {
        this.followRedirects = followRedirects;
        this.allowCircularRedirects = allowCircularRedirects;
        this.rejectRelativeRedirect = rejectRelativeRedirect;
        this.maxRedirects = maxRedirects;
        this.stripSensitiveHeadersOnCrossHostRedirect = stripSensitiveHeadersOnCrossHostRedirect;
        this.isUserConfigured = isUserConfigured;
    }

    /**
     * Configure if REST Assured should follow redirects
     *
     * @param value <code>true</code> if it should follow redirects, <code>false</code> otherwise.
     * @return An updated RedirectConfig
     */
    public RedirectConfig followRedirects(boolean value) {
        return new RedirectConfig(value, allowCircularRedirects, rejectRelativeRedirect, maxRedirects, stripSensitiveHeadersOnCrossHostRedirect, true);
    }

    /**
     * Configure if REST Assured should allow circular redirects
     *
     * @param value <code>true</code> if it should allow circular redirects, <code>false</code> otherwise.
     * @return An updated RedirectConfig
     */
    public RedirectConfig allowCircularRedirects(boolean value) {
        return new RedirectConfig(followRedirects, value, rejectRelativeRedirect, maxRedirects, stripSensitiveHeadersOnCrossHostRedirect, true);
    }

    /**
     * Configure if REST Assured should reject relative redirects
     *
     * @param value <code>true</code> if it should reject relative redirects, <code>false</code> otherwise.
     * @return An updated RedirectConfig
     */
    public RedirectConfig rejectRelativeRedirect(boolean value) {
        return new RedirectConfig(followRedirects, allowCircularRedirects, value, maxRedirects, stripSensitiveHeadersOnCrossHostRedirect, true);
    }

    /**
     * Configure the maximum number of redirects.
     *
     * @param value The maximum number of redirects
     * @return An updated RedirectConfig
     */
    public RedirectConfig maxRedirects(int value) {
        return new RedirectConfig(followRedirects, allowCircularRedirects, rejectRelativeRedirect, value, stripSensitiveHeadersOnCrossHostRedirect, true);
    }

    /**
     * Configure whether REST Assured should strip sensitive headers (<code>Authorization</code>, <code>Cookie</code> and
     * <code>Proxy-Authorization</code>) when a redirect crosses to a different host, so that a credential or session is not
     * leaked to the redirect target. The target is considered a different host when its scheme, host or port differs from
     * the request being redirected. Enabled by default.
     *
     * @param value <code>true</code> if sensitive headers should be stripped on a cross-host redirect, <code>false</code> to forward them as before.
     * @return An updated RedirectConfig
     */
    public RedirectConfig stripSensitiveHeadersOnCrossHostRedirect(boolean value) {
        return new RedirectConfig(followRedirects, allowCircularRedirects, rejectRelativeRedirect, maxRedirects, value, true);
    }

    /**
     * The same RedirectConfig instance. This method is only provided as syntactic sugar.
     */
    public RedirectConfig and() {
        return this;
    }

    /**
     * @return <code>true</code> if configured to follow redirects
     */
    public boolean followsRedirects() {
        return followRedirects;
    }

    /**
     * @return <code>true</code> if configured to allow circular redirects
     */
    public boolean allowsCircularRedirects() {
        return allowCircularRedirects;
    }

    /**
     * @return <code>true</code> if configured to reject relative redirects
     */
    public boolean rejectRelativeRedirects() {
        return rejectRelativeRedirect;
    }

    /**
     * @return The maximum number of redirects.
     */
    public int maxRedirects() {
        return maxRedirects;
    }

    /**
     * @return <code>true</code> if configured to strip sensitive headers on a cross-host redirect
     */
    public boolean stripsSensitiveHeadersOnCrossHostRedirect() {
        return stripSensitiveHeadersOnCrossHostRedirect;
    }

    /**
     * @return A static way to create a new RedirectConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static RedirectConfig redirectConfig() {
        return new RedirectConfig();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUserConfigured() {
        return isUserConfigured;
    }
}

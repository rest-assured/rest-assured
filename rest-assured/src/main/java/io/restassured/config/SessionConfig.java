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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Configure session management for REST Assured. Here you can define a default session id value that'll be used for each request as well as
 * defining the default session id name (by default it's {@value SessionConfig#DEFAULT_SESSION_ID_NAME}).
 */
public class SessionConfig implements Config {

    public static final String DEFAULT_SESSION_ID_NAME = "JSESSIONID";
    private final String sessionIdName;
    private final String sessionIdValue;
    private final boolean isUserDefined;

    /**
     * Create a new session configuration  with session id name {@value #DEFAULT_SESSION_ID_NAME} and no session id value.
     */
    public SessionConfig() {
        this(DEFAULT_SESSION_ID_NAME, null, false);
    }

    /**
     * Create a new session configuration  with session id name {@value #DEFAULT_SESSION_ID_NAME} and with the supplied session id value.
     *
     * @param sessionIdValue The session id to use for each request.
     */
    public SessionConfig(String sessionIdValue) {
        this(DEFAULT_SESSION_ID_NAME, sessionIdValue, true);
    }

    /**
     * Create a new session config with the given session id name and value.
     *
     * @param sessionIdName  The name of the session id, by default it's {@value #DEFAULT_SESSION_ID_NAME}
     * @param sessionIdValue The value of the session id. This session id will be used for each request that uses this session configuration instance (unless it's overwritten by the DSL).
     *                       Default is <code>null</code>.
     */
    public SessionConfig(String sessionIdName, String sessionIdValue) {
        this(sessionIdName, sessionIdValue, true);
    }

    private SessionConfig(String sessionIdName, String sessionIdValue, boolean isUserDefined) {
        Validate.notEmpty(sessionIdName, "Session id name cannot be empty.");
        this.sessionIdName = sessionIdName;
        this.sessionIdValue = sessionIdValue;
        this.isUserDefined = isUserDefined;
    }

    public boolean isSessionIdValueDefined() {
        return !StringUtils.isBlank(sessionIdValue);
    }

    /**
     * Specify the default session id to use for each request..
     *
     * @param defaultSessionId The stream
     * @return A new SessionConfig instance
     */
    public SessionConfig sessionIdValue(String defaultSessionId) {
        return new SessionConfig(sessionIdName, defaultSessionId, true);
    }


    /**
     * Set the session id name. This is the name of the cookie that contains the session id. By default it's {@value #DEFAULT_SESSION_ID_NAME}.
     *
     * @param sessionIdName The name of the session id variable
     * @return A new SessionConfig instance
     */
    public SessionConfig sessionIdName(String sessionIdName) {
        return new SessionConfig(sessionIdName, sessionIdValue, true);
    }

    /**
     * @return The session id name
     */
    public String sessionIdName() {
        return sessionIdName;
    }

    /**
     * @return The session id value
     */
    public String sessionIdValue() {
        return sessionIdValue;
    }

    /**
     * @return A static way to create a new SessionConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static SessionConfig sessionConfig() {
        return new SessionConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same session config instance.
     */
    public SessionConfig and() {
        return this;
    }

    public boolean isUserConfigured() {
        return isUserDefined;
    }
}
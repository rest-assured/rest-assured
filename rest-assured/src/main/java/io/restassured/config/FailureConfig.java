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

import io.restassured.internal.LogRequestAndResponseOnFailListener;
import io.restassured.listener.ResponseValidationFailureListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Configure the failure listeners. It allows registering instances of {@link ResponseValidationFailureListener}
 * that are invoked when validation fails for any response with relevant parameters.
 * Listeners solve the problem when you want to access response after failure - normally it is impossible
 * because validation failure ends with exception rather then returning anything that could be used to extract
 * response. So if you need to access data in response, register a listener here.
 */
public class FailureConfig implements Config {

    private static ResponseValidationFailureListener DEFAULT_LOG_LISTENER = new LogRequestAndResponseOnFailListener();

    private List<ResponseValidationFailureListener> failureListeners = new ArrayList<ResponseValidationFailureListener>();
    private final boolean isUserConfigured;

    /**
     * Configure the default stream to use the System.out stream (default).
     */
    public FailureConfig() {
        this(new ArrayList<ResponseValidationFailureListener>(), false);
    }

    public FailureConfig(List<ResponseValidationFailureListener> failureListeners) {
        this(failureListeners, true);
    }

    private FailureConfig(List<ResponseValidationFailureListener> failureListeners, boolean isUserConfigured) {
        this.isUserConfigured = isUserConfigured;
        this.failureListeners = new ArrayList<ResponseValidationFailureListener>();
        this.failureListeners.addAll(failureListeners);
        this.failureListeners.add(DEFAULT_LOG_LISTENER);
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    public List<ResponseValidationFailureListener> getFailureListeners() {
        return failureListeners;
    }

    public static FailureConfig failureConfig() {
        return new FailureConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same failure config instance.
     */
    public FailureConfig with() {
        return this;
    }

    public FailureConfig failureListeners(Collection<ResponseValidationFailureListener> responseValidationFailureListeners) {
        return new FailureConfig(new ArrayList<ResponseValidationFailureListener>(responseValidationFailureListeners), true);
    }

    public FailureConfig failureListeners(ResponseValidationFailureListener first, ResponseValidationFailureListener... more) {
        notNull(first, ResponseValidationFailureListener.class.getSimpleName());
        List<ResponseValidationFailureListener> listeners = new ArrayList<ResponseValidationFailureListener>();
        listeners.add(first);
        if (more != null) {
            Collections.addAll(listeners, more);
        }
        return failureListeners(listeners);
    }
}
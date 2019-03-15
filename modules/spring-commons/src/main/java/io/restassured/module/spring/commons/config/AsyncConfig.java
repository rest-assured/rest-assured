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

package io.restassured.module.spring.commons.config;

import io.restassured.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for async requests
 */
public class AsyncConfig implements Config {

    private static final long DEFAULT_TIMEOUT_IN_MILLISECONDS = TimeUnit.SECONDS.toMillis(1);

    private final boolean userConfigured;
    private final long duration;
    private final TimeUnit timeUnit;

    /**
     * Creates a default {@link AsyncConfig} with timeout equal 1000 milliseconds (1 second).
     */
    public AsyncConfig() {
        this(DEFAULT_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS, false);
    }

    /**
     * Creates a new {@link AsyncConfig} with timeout equal to the given number of milliseconds.
     *
     * @param timeoutInMs The timeunit in milliseconds.
     */
    public AsyncConfig(long timeoutInMs) {
        this(timeoutInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new {@link AsyncConfig} with timeout.
     *
     * @param duration The duration
     * @param timeUnit The time unit
     */
    public AsyncConfig(long duration, TimeUnit timeUnit) {
        this(duration, timeUnit, true);
    }

    private AsyncConfig(long duration, TimeUnit timeUnit, boolean isUserConfigured) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.userConfigured = isUserConfigured;
    }

    public static AsyncConfig withTimeout(long duration, TimeUnit timeUnit) {
        return new AsyncConfig(timeUnit.toMillis(duration));
    }

    /**
     * Just syntactic sugar.
     *
     * @return A new instance of {@link AsyncConfig}.
     */
    public static AsyncConfig asyncConfig() {
        return new AsyncConfig();
    }

    /**
     * Specify the timeout for the async request in milliseconds.
     *
     * @param timeoutInMs The timeout in milliseconds.
     * @return A new instance of the MockMvcAsyncConfig
     */
    public AsyncConfig timeout(long timeoutInMs) {
        return timeout(timeoutInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Specify the timeout for the async request in milliseconds.
     *
     * @param duration The duration.
     * @param timeUnit The time unit for the duration.
     * @return A new instance of the MockMvcAsyncConfig
     */
    public AsyncConfig timeout(long duration, TimeUnit timeUnit) {
        return new AsyncConfig(duration, timeUnit, true);
    }

    /**
     * @return The timeout converted to milliseconds.
     */
    public long timeoutInMs() {
        return TimeUnit.MILLISECONDS.convert(duration, timeUnit);
    }

    public boolean isUserConfigured() {
        return userConfigured;
    }

    /**
     * Just syntactic sugar to make the DSL more english like.
     */
    public AsyncConfig with() {
        return this;
    }
}

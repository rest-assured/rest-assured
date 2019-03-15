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

import org.apache.commons.lang3.Validate;

import java.util.concurrent.TimeUnit;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Lets you configure connection settings for REST Assured. For example if you want to force-close the Apache HTTP Client connection
 * after each response. You may want to do this if you make a lot of fast consecutive requests with small amount of data in the response.
 * How ever if you're downloading large amount of (chunked) data you must not close connections after each response. By default
 * connections are <i>not</i> closed after each response.
 */
public class ConnectionConfig implements Config {

    private final CloseIdleConnectionConfig closeIdleConnectionConfig;
    private final boolean isUserConfigured;

    /**
     * Create a new connection configuration that doesn't close the HTTP connections after each response.
     */
    public ConnectionConfig() {
        this(null, false);
    }

    /**
     * Create a new Connection configuration with the supplied settings.
     *
     * @param closeIdleConnectionConfig Configures REST Assured to close idle connections after each response.
     *                                  If <code>null</code> (default) then connections are not close after each response.
     */
    public ConnectionConfig(CloseIdleConnectionConfig closeIdleConnectionConfig) {
        this(notNull(closeIdleConnectionConfig, CloseIdleConnectionConfig.class), true);
    }

    private ConnectionConfig(CloseIdleConnectionConfig closeIdleConnectionConfig, boolean isUserConfigured) {
        this.closeIdleConnectionConfig = closeIdleConnectionConfig;
        this.isUserConfigured = isUserConfigured;
    }

    /**
     * Close open connections after each response. This is required if you plan to make a lot of
     * consecutive requests with small response bodies. It can also be enabled if you never receive
     * chunked HTTP responses.
     */
    public ConnectionConfig closeIdleConnectionsAfterEachResponse() {
        return new ConnectionConfig(new CloseIdleConnectionConfig(0, NANOSECONDS));
    }

    /**
     * Close open connections that have idled for the amount of time specified in this config after each response.
     *
     * @param idleTime The idle time of connections to be closed
     * @param timeUnit The time unit to for <code>idleTime</code>
     * @return A new ConnectionConfig instance with the updated configuration
     */
    public ConnectionConfig closeIdleConnectionsAfterEachResponseAfter(long idleTime, TimeUnit timeUnit) {
        return new ConnectionConfig(new CloseIdleConnectionConfig(idleTime, timeUnit));
    }

    /**
     * Close open connections that have idled for the amount of time specified in this config after each response.
     *
     * @param closeIdleConnectionConfig The close connection configuration.
     * @return A new ConnectionConfig instance with the updated configuration
     */
    public ConnectionConfig closeIdleConnectionsAfterEachResponseAfter(CloseIdleConnectionConfig closeIdleConnectionConfig) {
        return new ConnectionConfig(closeIdleConnectionConfig);
    }

    /**
     * Don't close idle connections after each request. This is the default configuration. If you downloading
     * large amount of data using HTTP chunking this setting is required.
     *
     * @return A new ConnectionConfig instance with the updated configuration
     */
    public ConnectionConfig dontCloseIdleConnectionsAfterEachResponse() {
        return new ConnectionConfig(null);
    }

    /**
     * @return The close connection configuration
     */
    public CloseIdleConnectionConfig closeIdleConnectionConfig() {
        return closeIdleConnectionConfig;
    }

    public boolean shouldCloseIdleConnectionsAfterEachResponse() {
        return closeIdleConnectionConfig() != null;
    }

    /**
     * @return A static way to create a new ConnectionConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static ConnectionConfig connectionConfig() {
        return new ConnectionConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same connection config instance.
     */
    public ConnectionConfig and() {
        return this;
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    /**
     * Close open connections that have idled for the amount of time specified in this config.
     */
    public static class CloseIdleConnectionConfig {
        private final long idleTime;
        private final TimeUnit timeUnit;

        /**
         * Close connections that have idled for the amount of time specified in this config.
         *
         * @param idleTime The idle time of connections to be closed
         * @param timeUnit The time unit to for <code>idleTime</code>
         */
        public CloseIdleConnectionConfig(long idleTime, TimeUnit timeUnit) {
            if (idleTime < 0) {
                throw new IllegalArgumentException("Idle time cannot be less than 0.");
            }
            Validate.notNull(timeUnit, "Timeunit cannot be null");
            this.idleTime = idleTime;
            this.timeUnit = timeUnit;
        }

        public long getIdleTime() {
            return idleTime;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }
    }
}

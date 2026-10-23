/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static io.restassured.config.ConnectionConfig.connectionConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ConnectionConfigTest {

    @Test public void
    dont_close_idle_connections_after_each_response_turns_off_closing_idle_connections() {
        ConnectionConfig config = connectionConfig().closeIdleConnectionsAfterEachResponse().dontCloseIdleConnectionsAfterEachResponse();

        assertThat(config.shouldCloseIdleConnectionsAfterEachResponse(), is(false));
        assertThat(config.closeIdleConnectionConfig(), nullValue());
        assertThat(config.isUserConfigured(), is(true));
    }
}

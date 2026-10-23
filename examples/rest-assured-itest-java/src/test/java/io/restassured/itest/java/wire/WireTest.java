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

package io.restassured.itest.java.wire;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

/**
 * Base class for tests that check what REST Assured sends on the wire. Each test class gets its own
 * {@link WireServer} on a free port, and REST Assured and the server are reset before each test.
 * <p>
 * The tests describe what REST Assured sends today, so that replacing the HTTP client underneath doesn't change it
 * by accident. They only check what REST Assured decides, not what the HTTP client adds on its own (such as
 * {@code User-Agent} or {@code Connection}).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class WireTest {
    protected WireServer server;

    @BeforeAll
    void startWireServer() throws Exception {
        server = startServer();
    }

    /**
     * Override to start the server with TLS.
     */
    protected WireServer startServer() throws Exception {
        return WireServer.start();
    }

    @AfterAll
    void stopWireServer() throws Exception {
        server.close();
    }

    @BeforeEach
    void resetRestAssuredAndWireServer() {
        RestAssured.reset();
        server.reset();
    }
}

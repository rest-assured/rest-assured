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
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.util.Base64;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Base class for tests that check what REST Assured sends on the wire. Each test class gets its own
 * {@link WireServer} on a free port. The server is reset before each test, and REST Assured's static configuration
 * both before and after it, so that none of it leaks into the next test or into the other test classes in the JVM.
 * <p>
 * The tests describe what REST Assured sends today, so that replacing the HTTP client underneath doesn't change it
 * by accident. They only check what REST Assured decides, not what the HTTP client adds on its own (such as
 * {@code User-Agent} or {@code Connection}).
 * <p>
 * The tests run with the JDK client, and with Apache HttpClient 5 under the {@code apache5} profile. Where the clients
 * send something differently, a test branches on {@link io.restassured.itest.java.support.TestClient}, also on the
 * version of HttpClient 5 where that matters. A test of what only one client can do is tagged
 * {@value io.restassured.itest.java.support.TestClient#APACHE5_ONLY} or
 * {@value io.restassured.itest.java.support.TestClient#JDK_ONLY}, and on the other client expects the message that
 * says it can't, with the fix that it names.
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
        if (server != null) {
            server.close();
        }
    }

    @BeforeEach
    void resetRestAssuredAndWireServer() {
        RestAssured.reset();
        server.reset();
    }

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    /**
     * @return the value of a {@code Basic} {@code Authorization} or {@code Proxy-Authorization} header
     */
    protected static String basicAuthorization(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(UTF_8));
    }

    /**
     * Sends a GET to a path answered with {@link Responder#withRemotePort()}, reads the body so that a reused HTTP client
     * can send the next request, and tells which connection the request arrived on.
     *
     * @return the client port of the connection
     */
    protected static String remotePortOfReadResponse(RestAssuredConfig config, String url) {
        Response response = given().config(config).get(url);
        response.asString();
        return response.header(Responder.REMOTE_PORT_HEADER);
    }
}

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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies that {@link io.restassured.config.JsonConfig#numberLengthLimit(int)} is wired through the
 * response-body parsing path (ContentParser to ConfigurableJsonSlurper) used by the {@code body(...)} matchers,
 * not just the standalone JsonPath API.
 */
public class JsonNumberLengthLimitITest {

    // Longer than the default limit (1000) but small enough to parse instantly when the cap is disabled.
    private static final String OVERSIZED_NUMBER = "9".repeat(2000);

    @AfterEach
    public void reset() {
        RestAssured.reset();
    }

    @Test public void
    rejects_oversized_response_number_with_the_default_limit() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse.Builder().code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("{\"n\":" + OVERSIZED_NUMBER + "}").build());
            server.start();
            String url = "http://localhost:" + server.getPort() + "/";

            assertThatThrownBy(() -> given().when().get(url).then().body("n", notNullValue()))
                    .hasStackTraceContaining("exceeds the maximum allowed length");
        }
    }

    @Test public void
    accepts_oversized_response_number_when_the_limit_is_disabled() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse.Builder().code(200)
                    .addHeader("Content-Type", "application/json")
                    .body("{\"n\":" + OVERSIZED_NUMBER + "}").build());
            server.start();
            String url = "http://localhost:" + server.getPort() + "/";

            given().config(config().jsonConfig(jsonConfig().numberLengthLimit(-1))).
            when().
                    get(url).
            then().
                    body("n", equalTo(new BigInteger(OVERSIZED_NUMBER)));
        }
    }
}

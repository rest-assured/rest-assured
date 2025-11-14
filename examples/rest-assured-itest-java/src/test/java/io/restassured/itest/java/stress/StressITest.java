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

package io.restassured.itest.java.stress;

import io.restassured.RestAssured;
import io.restassured.config.ConnectionConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.restlet.Component;
import org.restlet.data.Protocol;

import java.time.Duration;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class StressITest {
    Component component;
    static final int wait = 60 * 1000;
    int iterations = 30;
    String post = "TEST";
    String expect = "HANG-TEST";
    String url = null;

    @BeforeEach
    public void setUp() throws Exception {
        url = "http://localhost:8081/restlet/test";
        component = new Component();
        component.getLogService().setEnabled(true);
        component.getServers().add(Protocol.HTTP, 8081);
        component.getDefaultHost().attach("/restlet", new StressApp());
        component.start();
        RestAssured.config = RestAssuredConfig.config().connectionConfig(new ConnectionConfig().closeIdleConnectionsAfterEachResponse());
    }

    @AfterEach
    public void tearDown() throws Exception {
        component.stop();
        RestAssured.reset();
    }

    @Test
    void stressWithRestAssuredGet() {
        assertTimeoutPreemptively(Duration.ofMillis(wait), () -> {
            for (int i = 0, n = iterations; i < n; i++) {
                given().
                        expect().body(equalTo(expect)).
                        when().get(url);
            }
        });
    }

    @Test
    void stressWithRestAssuredPost() {
        assertTimeoutPreemptively(Duration.ofMillis(wait), () -> {
            for (int i = 0, n = iterations; i < n; i++) {
                given().contentType("text/plain;charset=utf-8").body(post.getBytes(java.nio.charset.StandardCharsets.UTF_8)).
                        expect().body(equalTo(expect)).
                        when().post(url);
            }
        });
    }

    @Test
    void stressWithRestAssuredPostWhenSameHttpClientInstanceIsReused() {
        RestAssured.config = RestAssuredConfig.newConfig().httpClient(HttpClientConfig.httpClientConfig().reuseHttpClientInstance());
        try {
            assertTimeoutPreemptively(Duration.ofMillis(wait), () -> {
                for (int i = 0, n = iterations; i < n; i++) {
                    given().contentType("text/plain;charset=utf-8").body(post.getBytes(java.nio.charset.StandardCharsets.UTF_8)).
                            expect().body(equalTo(expect)).
                            when().post(url);
                }
            });
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    void stressWithRestAssuredGetManualClose() {
        RestAssured.config = RestAssuredConfig.newConfig().httpClient(HttpClientConfig.httpClientConfig().reuseHttpClientInstance());
        try {
            assertTimeoutPreemptively(Duration.ofMillis(wait), () -> {
                for (int i = 0, n = iterations; i < n; i++) {
                    String body = IOUtils.toString(get(url).andReturn().body().asInputStream(), java.nio.charset.StandardCharsets.UTF_8);
                    assertThat(body).isEqualTo(expect);
                }
            });
        } finally {
            RestAssured.reset();
        }
    }
}

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

package io.restassured.itest.java.support;

import io.restassured.RestAssured;
import io.restassured.internal.http.client.HttpClientProviders;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.VersionInfo;
import org.junit.jupiter.api.Assumptions;

/**
 * Which HTTP client the tests send their requests with: the JDK client by default, or Apache HttpClient 5 when the
 * build runs with the {@code apache5} profile, which adds http-client-apache5. It's the client that REST Assured picks,
 * so it's also right with {@code -Drestassured.http.client}. A test that expects what a client documents as different
 * names the row of the differences table.
 */
public final class TestClient {

    private TestClient() {
    }

    /**
     * @return Whether the requests are sent with the JDK HTTP client
     */
    public static boolean isJdk() {
        return "jdk".equals(name());
    }

    /**
     * @return Whether the requests are sent with Apache HttpClient 5
     */
    public static boolean isApache5() {
        return "apache5".equals(name());
    }

    /**
     * @param minor a minor release of HttpClient 5, such as 4 for 5.4
     * @return Whether the requests are sent with an Apache HttpClient 5 that's older than 5.{@code minor}
     */
    public static boolean isApache5Before(int minor) {
        if (!isApache5()) {
            return false;
        }
        String release = VersionInfo.loadVersionInfo("org.apache.hc.client5", HttpClientBuilder.class.getClassLoader()).getRelease();
        String[] parts = release.split("[.\\-]");
        return Integer.parseInt(parts[0]) == 5 && Integer.parseInt(parts[1]) < minor;
    }

    /**
     * Skips a test of what only the JDK client has
     */
    public static void assumeJdk() {
        Assumptions.assumeTrue(isJdk(), "Tests what only the JDK client has");
    }

    /**
     * Skips a test of what only Apache HttpClient 5 has
     */
    public static void assumeApache5() {
        Assumptions.assumeTrue(isApache5(), "Tests what only Apache HttpClient 5 has");
    }

    private static String name() {
        return HttpClientProviders.select(RestAssured.config().getHttpClientConfig()).provider().name();
    }
}

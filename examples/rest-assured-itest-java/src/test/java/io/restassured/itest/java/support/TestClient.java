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

import org.junit.jupiter.api.Assumptions;

/**
 * Which HTTP client the tests send their requests with: Apache HttpClient 4, the default until REST Assured 7.0.0 makes
 * the JDK client the default, or the JDK client when the build runs with {@code -Drestassured.http.client=jdk}. A test
 * that expects what the JDK client documents as different names the row of the JDK differences table.
 */
public final class TestClient {

    private TestClient() {
    }

    /**
     * @return Whether the requests are sent with the JDK HTTP client
     */
    public static boolean isJdk() {
        return "jdk".equalsIgnoreCase(System.getProperty("restassured.http.client"));
    }

    /**
     * Skips a test of what only HttpClient 4 has, such as its own client factory or the deprecated
     * {@code authenticate(HTTPBuilder)}.
     */
    public static void assumeHttpClient4() {
        Assumptions.assumeFalse(isJdk(), "Tests what only Apache HttpClient 4 has");
    }
}

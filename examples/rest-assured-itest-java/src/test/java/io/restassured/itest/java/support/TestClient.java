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
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.VersionInfo;

/**
 * Which HTTP client the tests send their requests with: the JDK client by default, or Apache HttpClient 5 when the
 * build runs with the {@code apache5} profile, which adds http-client-apache5. It's the client that REST Assured picks,
 * so it's also right with {@code -Drestassured.http.client}. A test that expects what a client documents as different
 * names the row of the differences table.
 * <p>
 * A test of what only one client can do is tagged {@value #APACHE5_ONLY} or {@value #JDK_ONLY}. It runs on both
 * clients, and on the other one expects the message that says the client can't do it ({@link #cantMessage(String)}), or
 * for a customizer of the other client's builder, the message that says which client REST Assured uses.
 */
public final class TestClient {

    /**
     * The tag of a test of what only Apache HttpClient 5 can do
     */
    public static final String APACHE5_ONLY = "apache5-only";
    /**
     * The tag of a test of what only the JDK client can do
     */
    public static final String JDK_ONLY = "jdk-only";

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
        return isApache5() && minorOf("org.apache.hc.client5", HttpClientBuilder.class) < minor;
    }

    /**
     * @param minor a minor release of HttpCore 5, such as 4 for 5.4
     * @return Whether the requests are sent with Apache HttpClient 5 on an HttpCore 5 that's older than 5.{@code minor}
     */
    public static boolean isApache5OnHttpCoreBefore(int minor) {
        return isApache5() && minorOf("org.apache.hc.core5", HttpHost.class) < minor;
    }

    /**
     * @param feature what the client can't do, as the client says it
     * @return The start of the message of a request that the client can't send, up to the fix that it names, when the
     * client is the default JDK client or Apache HttpClient 5 found on the classpath, as the tests run
     */
    public static String cantMessage(String feature) {
        if (isJdk()) {
            return "The JDK HTTP client (the default module, http-client-jdk) can't " + feature + ". "
                    + "Add io.rest-assured:http-client-apache5 to your test dependencies and REST Assured uses it";
        }
        return "Apache HttpClient 5 (found on the classpath, http-client-apache5) can't " + feature + ". "
                + "Choose another client with httpClientConfig().client(..)";
    }

    // The minor release of a 5.x library, read from its version file
    private static int minorOf(String packageName, Class<?> libraryClass) {
        String release = VersionInfo.loadVersionInfo(packageName, libraryClass.getClassLoader()).getRelease();
        String[] parts = release.split("[.\\-]");
        if (!parts[0].equals("5")) {
            throw new IllegalStateException(packageName + " " + release + " isn't a 5.x release");
        }
        return Integer.parseInt(parts[1]);
    }

    private static String name() {
        return HttpClientProviders.select(RestAssured.config().getHttpClientConfig()).provider().name();
    }
}

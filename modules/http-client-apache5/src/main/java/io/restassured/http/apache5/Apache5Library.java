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

package io.restassured.http.apache5;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.VersionInfo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Checks that the Apache HttpClient 5 on the classpath is one that the module supports, 5.2.1 or later with HttpCore 5.2
 * or later, httpcore5-h2 included. The client is checked by a class that's new in 5.2, since a BOM may pin an older
 * version, and an uber-jar may lose the version files. The version files only go into the messages, and into the check
 * of HttpCore.
 * <p>
 * Nothing here uses an HttpClient 5 type before the check has found it.
 */
final class Apache5Library {
    static final String CLIENT_BUILDER = "org.apache.hc.client5.http.impl.classic.HttpClientBuilder";
    // New in HttpClient 5.2
    static final String CONNECTION_CONFIG = "org.apache.hc.client5.http.config.ConnectionConfig";
    static final String HTTP_HOST = "org.apache.hc.core5.http.HttpHost";
    // In httpcore5-h2, which the classic API loads at each connect too (TlsConfig)
    static final String HTTP_VERSION_POLICY = "org.apache.hc.core5.http2.HttpVersionPolicy";

    private static final String NEEDS = "http-client-apache5 needs Apache HttpClient 5.2.1 or later";
    private static final String PINNED = ", probably set by a BOM such as Spring Boot's.";
    private static final AtomicBoolean WARNED_ABOUT_UNKNOWN_VERSION = new AtomicBoolean();

    private Apache5Library() {
    }

    /**
     * @throws IllegalStateException if HttpClient 5 or HttpCore 5 is missing or too old
     */
    static void check() {
        check(Apache5Library::isPresent, Apache5Library::clientRelease, () -> release("org.apache.hc.core5", HttpHost.class));
    }

    /**
     * @param classPresent  whether a class is on the classpath
     * @param clientRelease the release of HttpClient 5, or {@code null} if it can't be read
     * @param coreRelease   the release of HttpCore 5, or {@code null} if it can't be read
     */
    static void check(Predicate<String> classPresent, Supplier<String> clientRelease, Supplier<String> coreRelease) {
        if (!classPresent.test(CLIENT_BUILDER)) {
            throw new IllegalStateException(NEEDS + " (org.apache.httpcomponents.client5:httpclient5), but it isn't on the classpath.");
        }
        if (!classPresent.test(HTTP_HOST)) {
            throw new IllegalStateException(NEEDS + " with Apache HttpCore 5.2 or later (org.apache.httpcomponents.core5:httpcore5), "
                    + "but HttpCore 5 isn't on the classpath.");
        }
        if (!classPresent.test(CONNECTION_CONFIG)) {
            String release = clientRelease.get();
            throw new IllegalStateException(NEEDS + ", but " + (release == null ? "an older version" : release) + " is on the classpath" + PINNED);
        }
        if (!classPresent.test(HTTP_VERSION_POLICY)) {
            throw new IllegalStateException(NEEDS + " with Apache HttpCore 5.2 or later, including org.apache.httpcomponents.core5:httpcore5-h2, "
                    + "which HttpClient 5 uses for HTTP/1.1 too, but httpcore5-h2 isn't on the classpath. Don't exclude it from the dependencies.");
        }
        String core = coreRelease.get();
        if (core != null && isOlderThan5_2(core)) {
            throw new IllegalStateException(NEEDS + " with Apache HttpCore 5.2 or later (org.apache.httpcomponents.core5:httpcore5), but HttpCore "
                    + core + " is on the classpath" + PINNED);
        }
    }

    /**
     * @return "Apache HttpClient" and its release, which is "(version unknown)", with a warning logged once, when the
     * version file of HttpClient 5 can't be read, for example in an uber-jar that left it out
     */
    static String description() {
        String release = clientRelease();
        if (release == null && WARNED_ABOUT_UNKNOWN_VERSION.compareAndSet(false, true)) {
            System.getLogger("io.restassured").log(System.Logger.Level.WARNING, "REST Assured can't read the version of Apache HttpClient 5, "
                    + "since org/apache/hc/client5/version.properties is missing. http-client-apache5 supports 5.2.1 and later.");
        }
        return "Apache HttpClient " + (release == null ? "(version unknown)" : release);
    }

    static boolean isOlderThan5_2(String release) {
        String[] parts = release.split("[.\\-]", 3);
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return major < 5 || major == 5 && minor < 2;
        } catch (NumberFormatException e) {
            // A release that can't be read isn't held against the library
            return false;
        }
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, Apache5Library.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static String clientRelease() {
        return release("org.apache.hc.client5", HttpClientBuilder.class);
    }

    // Only called once the library is known to be on the classpath. The version file is read with the class loader of
    // the library, which in OSGi is the only one that sees it.
    private static String release(String packageName, Class<?> libraryClass) {
        VersionInfo info = VersionInfo.loadVersionInfo(packageName, libraryClass.getClassLoader());
        return info == null || VersionInfo.UNAVAILABLE.equals(info.getRelease()) ? null : info.getRelease();
    }
}

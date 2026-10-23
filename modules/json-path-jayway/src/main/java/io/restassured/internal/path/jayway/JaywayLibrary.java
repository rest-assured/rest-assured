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

package io.restassured.internal.path.jayway;

import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Manifest;

/**
 * Checks that Jayway JsonPath 2.8.0 or later is on the classpath, before json-path-jayway uses it. Dependency management,
 * such as Spring Boot's, can set an older version than the one this module was built with.
 */
public final class JaywayLibrary {
    static final String JSON_PATH = "com.jayway.jsonpath.JsonPath";
    // A class that Jayway JsonPath has from 2.8.0 (the first() function)
    static final String SINCE_2_8_0 = "com.jayway.jsonpath.internal.function.sequence.First";

    private static volatile boolean verified;

    private JaywayLibrary() {
    }

    /**
     * @throws IllegalStateException If Jayway JsonPath is missing or older than 2.8.0
     */
    public static void verify() {
        if (verified) {
            return;
        }
        ClassLoader classLoader = JaywayLibrary.class.getClassLoader();
        String problem = problem(name -> exists(name, classLoader), name -> bundleVersion(name, classLoader));
        if (problem != null) {
            throw new IllegalStateException(problem);
        }
        verified = true;
    }

    /**
     * @param exists  Whether a class exists
     * @param version The Bundle-Version of the jar of a class, or {@code null} if it can't be read
     * @return What is wrong with the Jayway JsonPath on the classpath, or {@code null} if nothing is
     */
    static String problem(Predicate<String> exists, Function<String, String> version) {
        if (!exists.test(JSON_PATH)) {
            return "io.rest-assured:json-path-jayway needs Jayway JsonPath (com.jayway.jsonpath:json-path) 2.8.0 or later on the classpath.";
        } else if (!exists.test(SINCE_2_8_0)) {
            String found = version.apply(JSON_PATH);
            return "io.rest-assured:json-path-jayway needs Jayway JsonPath 2.8.0 or later, but " + (found == null ? "an older version" : found)
                    + " is on the classpath, probably set by dependency management such as Spring Boot's.";
        }
        return null;
    }

    private static boolean exists(String className, ClassLoader classLoader) {
        try {
            // Other classes through Jayway's own class loader, since in OSGi the Jayway bundle doesn't export its internal
            // packages, such as the one of SINCE_2_8_0
            ClassLoader loader = className.equals(JSON_PATH) ? classLoader : Class.forName(JSON_PATH, false, classLoader).getClassLoader();
            Class.forName(className, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    // Jayway's Implementation-Version is wrong, and only its Bundle-Version is right
    private static String bundleVersion(String className, ClassLoader classLoader) {
        try {
            CodeSource codeSource = Class.forName(className, false, classLoader).getProtectionDomain().getCodeSource();
            URL manifest = new URL("jar:" + codeSource.getLocation() + "!/META-INF/MANIFEST.MF");
            try (InputStream in = manifest.openStream()) {
                return new Manifest(in).getMainAttributes().getValue("Bundle-Version");
            }
        } catch (Exception e) {
            return null;
        }
    }
}

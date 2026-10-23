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

package io.restassured.internal.support;

import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the version files that REST Assured's artifacts have in {@code META-INF/rest-assured}, and compares versions.
 * It uses only JDK types, so that it can run before anything from rest-assured-common is loaded.
 */
public final class VersionFiles {
    static final String VERSION = "version";
    static final String PATH_SPI_REVISION = "path-spi-revision";

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private VersionFiles() {
    }

    /**
     * @return The version file of the artifact, or {@code null} if the class loader can't find it or it wasn't filtered,
     * as when an IDE builds the module without Maven's resource filtering.
     */
    static Properties read(ClassLoader classLoader, String artifactId) {
        Properties versionFile = load(classLoader, "META-INF/rest-assured/" + artifactId + ".properties");
        return versionFile == null || versionFile.getProperty(VERSION, "").contains("${") ? null : versionFile;
    }

    /**
     * @return The version of the artifact from its version file, or else from the {@code pom.properties} that Maven
     * puts in the jar, which artifacts older than REST Assured 7.0.0 also have, or {@code null} if neither is found.
     */
    static String version(ClassLoader classLoader, String artifactId) {
        Properties versionFile = read(classLoader, artifactId);
        Properties properties = versionFile != null ? versionFile : load(classLoader, "META-INF/maven/io.rest-assured/" + artifactId + "/pom.properties");
        return properties == null ? null : properties.getProperty(VERSION);
    }

    /**
     * @return The version of REST Assured from the version file of rest-assured, or {@code null} if it wasn't filtered
     */
    public static String restAssuredVersion() {
        Properties versionFile = read(VersionFiles.class.getClassLoader(), "rest-assured");
        return versionFile == null ? null : versionFile.getProperty(VERSION);
    }

    /**
     * @return Whether both versions have the same major and minor version, such as 7.0.0 and 7.0.1-SNAPSHOT.
     */
    static boolean sameMinorVersion(String version, String otherVersion) {
        return majorAndMinor(version).equals(majorAndMinor(otherVersion));
    }

    /**
     * @return Whether the class loader is an OSGi bundle's, which can't read the resources of other bundles.
     */
    static boolean isOsgiBundle(ClassLoader classLoader) {
        for (Class<?> type = classLoader == null ? null : classLoader.getClass(); type != null; type = type.getSuperclass()) {
            for (Class<?> implemented : type.getInterfaces()) {
                if (implemented.getName().equals("org.osgi.framework.BundleReference")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Logs a warning, once for each message.
     */
    static void warnOnce(String message) {
        if (WARNED.add(message)) {
            System.getLogger("io.restassured").log(System.Logger.Level.WARNING, message);
        }
    }

    private static String majorAndMinor(String version) {
        String[] parts = version.split("[.-]", 3);
        return parts.length < 2 ? version : parts[0] + "." + parts[1];
    }

    private static Properties load(ClassLoader classLoader, String resource) {
        try (InputStream in = classLoader == null ? null : classLoader.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (Exception e) {
            return null;
        }
    }
}

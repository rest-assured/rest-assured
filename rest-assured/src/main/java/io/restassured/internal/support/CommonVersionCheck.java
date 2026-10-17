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

/**
 * Checks that rest-assured-common is recent enough for this version of REST Assured, so that an older common on the
 * classpath gives a message instead of a {@link NoClassDefFoundError}. It uses only JDK types, so that it can run
 * before anything that needs common is loaded.
 */
public class CommonVersionCheck {
    // The newest type in rest-assured-common that REST Assured needs
    static final String REQUIRED_TYPE = "io.restassured.common.config.NumberReturnType";

    private CommonVersionCheck() {
    }

    public static void verify() {
        verify(CommonVersionCheck.class.getClassLoader());
    }

    static void verify(ClassLoader classLoader) {
        try {
            Class.forName(REQUIRED_TYPE, false, classLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException(message(version(classLoader, "rest-assured-common"), version(classLoader, "rest-assured")), e);
        }
    }

    static String message(String commonVersion, String restAssuredVersion) {
        String common = commonVersion == null ? "An older rest-assured-common" : "rest-assured-common " + commonVersion;
        String restAssured = restAssuredVersion == null ? "" : " " + restAssuredVersion;
        return common + " is on the classpath but REST Assured is" + (restAssuredVersion == null ? " newer" : restAssured)
                + ". If you use Spring Boot 3, set the rest-assured.version property instead of a version on the dependency."
                + " Otherwise import io.rest-assured:rest-assured-bom.";
    }

    private static String version(ClassLoader classLoader, String artifactId) {
        try (InputStream in = classLoader == null ? null : classLoader.getResourceAsStream("META-INF/maven/io.rest-assured/" + artifactId + "/pom.properties")) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version");
        } catch (Exception e) {
            return null;
        }
    }
}

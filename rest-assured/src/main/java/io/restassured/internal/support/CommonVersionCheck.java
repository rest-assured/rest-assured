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

/**
 * Checks that rest-assured-common has the same version as REST Assured, so that a different common on the classpath
 * gives a message instead of a {@link NoClassDefFoundError}. It uses only JDK types, so that it can run before anything
 * that needs common is loaded.
 * <p>
 * The versions come from the version files in {@code META-INF/rest-assured}, or else from Maven's {@code pom.properties}.
 * A different major or minor version fails, and a different patch version logs a warning. When a version can't be
 * read, it checks that common has the newest type REST Assured needs.
 */
public class CommonVersionCheck {
    // The newest type in rest-assured-common that REST Assured needs
    static final String REQUIRED_TYPE = "io.restassured.common.config.NumberReturnType";
    // A type that every version of rest-assured-common has, whose class loader can read common's version file, also in OSGi
    static final String COMMON_TYPE = "io.restassured.common.mapper.TypeRef";

    private CommonVersionCheck() {
    }

    public static void verify() {
        verify(CommonVersionCheck.class.getClassLoader());
    }

    static void verify(ClassLoader classLoader) {
        verify(classLoader, commonClassLoader(classLoader));
    }

    static void verify(ClassLoader classLoader, ClassLoader commonClassLoader) {
        String restAssuredVersion = VersionFiles.version(classLoader, "rest-assured");
        String commonVersion = VersionFiles.version(commonClassLoader, "rest-assured-common");
        if (restAssuredVersion != null && commonVersion != null && !restAssuredVersion.equals(commonVersion)) {
            if (!VersionFiles.sameMinorVersion(commonVersion, restAssuredVersion)) {
                throw new IllegalStateException(message(commonVersion, restAssuredVersion));
            }
            VersionFiles.warnOnce(message(commonVersion, restAssuredVersion));
        }
        try {
            Class.forName(REQUIRED_TYPE, false, classLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException(message(commonVersion, restAssuredVersion), e);
        }
    }

    static String message(String commonVersion, String restAssuredVersion) {
        String common = commonVersion == null ? "An older rest-assured-common" : "rest-assured-common " + commonVersion;
        String restAssured = restAssuredVersion == null ? "" : " " + restAssuredVersion;
        return common + " is on the classpath but REST Assured is" + (restAssuredVersion == null ? " newer" : restAssured)
                + ". If you use Spring Boot 3, set the rest-assured.version property instead of a version on the dependency."
                + " Otherwise import io.rest-assured:rest-assured-bom.";
    }

    private static ClassLoader commonClassLoader(ClassLoader classLoader) {
        try {
            return Class.forName(COMMON_TYPE, false, classLoader).getClassLoader();
        } catch (ClassNotFoundException | LinkageError e) {
            return classLoader;
        }
    }
}

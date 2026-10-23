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

import java.util.Objects;
import java.util.Properties;

/**
 * Checks that a REST Assured module, such as json-path, has the same version as REST Assured before its classes are
 * loaded, so that a different version gives a message instead of a {@link NoClassDefFoundError}. A different major or
 * minor version, or a different revision of the SPI, fails, and a different patch version logs a warning.
 * <p>
 * In OSGi, REST Assured can't read the resources of other bundles, and the version ranges of the bundles' imports do
 * the check instead.
 */
public final class ModuleVersionCheck {

    private ModuleVersionCheck() {
    }

    public static void verify(String artifactId) {
        verify(ModuleVersionCheck.class.getClassLoader(), artifactId);
    }

    static void verify(ClassLoader classLoader, String artifactId) {
        Properties restAssured = VersionFiles.read(classLoader, "rest-assured");
        if (restAssured == null || VersionFiles.isOsgiBundle(classLoader)) {
            return;
        }
        String restAssuredVersion = restAssured.getProperty(VersionFiles.VERSION);
        Properties module = VersionFiles.read(classLoader, artifactId);
        String moduleVersion = module == null ? VersionFiles.version(classLoader, artifactId) : module.getProperty(VersionFiles.VERSION);
        if (moduleVersion == null || moduleVersion.equals(restAssuredVersion)) {
            return;
        }
        String message = "io.rest-assured:" + artifactId + " " + moduleVersion + " doesn't match REST Assured " + restAssuredVersion
                + ". Use one version for all REST Assured artifacts, for example by importing io.rest-assured:rest-assured-bom.";
        // A module without a version file is older than REST Assured 7.0.0
        if (module == null || !VersionFiles.sameMinorVersion(moduleVersion, restAssuredVersion)
                || !Objects.equals(module.getProperty(VersionFiles.PATH_SPI_REVISION), restAssured.getProperty(VersionFiles.PATH_SPI_REVISION))) {
            throw new IllegalStateException(message);
        }
        VersionFiles.warnOnce(message);
    }
}

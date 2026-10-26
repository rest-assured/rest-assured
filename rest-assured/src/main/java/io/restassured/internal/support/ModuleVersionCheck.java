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

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Checks that a REST Assured module, such as json-path-gpath, has the same version as REST Assured before its classes are
 * loaded, so that a different version gives a message instead of a {@link NoClassDefFoundError}. A different major or
 * minor version, or a different revision of the SPI, fails, and a different patch version logs a warning.
 * <p>
 * In OSGi, REST Assured can't read the resources of other bundles, and the version ranges of the bundles' imports do
 * the check instead.
 */
public final class ModuleVersionCheck {
    // The names of the modules before REST Assured 7.0.0
    private static final Map<String, String> FORMER_NAMES = Map.of("json-path-gpath", "json-path", "xml-path-gpath", "xml-path");

    private ModuleVersionCheck() {
    }

    /**
     * Checks a path engine module, such as json-path-gpath.
     */
    public static void verify(String artifactId) {
        verify(ModuleVersionCheck.class.getClassLoader(), artifactId);
    }

    static void verify(ClassLoader classLoader, String artifactId) {
        verify(classLoader, artifactId, VersionFiles.PATH_SPI_REVISION);
    }

    /**
     * @param spiRevisionKey The key of the SPI revision that the module and REST Assured must agree on, such as
     *                       {@link VersionFiles#HTTP_SPI_REVISION} for an HTTP client module
     */
    public static void verify(ClassLoader classLoader, String artifactId, String spiRevisionKey) {
        Properties restAssured = VersionFiles.read(classLoader, "rest-assured");
        if (restAssured == null || VersionFiles.isOsgiBundle(classLoader)) {
            return;
        }
        String restAssuredVersion = restAssured.getProperty(VersionFiles.VERSION);
        verifyNoJarOfFormerName(classLoader, artifactId, restAssuredVersion);
        Properties module = VersionFiles.read(classLoader, artifactId);
        String moduleVersion = module == null ? VersionFiles.version(classLoader, artifactId) : module.getProperty(VersionFiles.VERSION);
        if (moduleVersion == null || moduleVersion.equals(restAssuredVersion)) {
            return;
        }
        String message = "io.rest-assured:" + artifactId + " " + moduleVersion + " doesn't match REST Assured " + restAssuredVersion
                + ". Use one version for all REST Assured artifacts, for example by importing io.rest-assured:rest-assured-bom.";
        // A module without a version file is older than REST Assured 7.0.0
        if (module == null || !VersionFiles.sameMinorVersion(moduleVersion, restAssuredVersion)
                || !Objects.equals(module.getProperty(spiRevisionKey), restAssured.getProperty(spiRevisionKey))) {
            throw new IllegalStateException(message);
        }
        VersionFiles.warnOnce(message);
    }

    /**
     * Fails when a jar of the module's name before REST Assured 7.0.0, such as json-path, is on the classpath. Its
     * packages are the same as the renamed module's, so whichever comes first on the classpath would be used. In 7.x
     * the former name is a relocation pom without a jar, so such a jar is always an older version, for example one that
     * the Spring Boot 3 dependency management pins.
     */
    private static void verifyNoJarOfFormerName(ClassLoader classLoader, String artifactId, String restAssuredVersion) {
        String formerName = FORMER_NAMES.get(artifactId);
        String formerVersion = formerName == null ? null : VersionFiles.version(classLoader, formerName);
        if (formerVersion != null) {
            throw new IllegalStateException("io.rest-assured:" + formerName + " " + formerVersion + " is on the classpath, but REST Assured "
                    + restAssuredVersion + " uses io.rest-assured:" + artifactId + " instead. Replace " + formerName + " with " + artifactId
                    + " in your dependencies, or set the rest-assured.version property if you use Spring Boot 3.");
        }
    }
}

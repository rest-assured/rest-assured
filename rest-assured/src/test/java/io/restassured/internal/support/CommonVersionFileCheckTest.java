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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The version file part of {@link CommonVersionCheck}. Core's version is read from the fixture, and the newest type
 * that REST Assured needs is found by this test's own class loader.
 */
class CommonVersionFileCheckTest {

    @TempDir
    Path core;

    @TempDir
    Path common;

    private ClassLoader core(String version) {
        return new VersionFileFixture(core).versionFile("rest-assured", version, 1).classLoader();
    }

    private static ClassLoader withTypes(ClassLoader versionFiles) {
        // Classes from this test's class loader, version files from the fixture
        return new ClassLoader(CommonVersionFileCheckTest.class.getClassLoader()) {
            @Override
            public java.net.URL getResource(String name) {
                return versionFiles.getResource(name);
            }

            @Override
            public java.io.InputStream getResourceAsStream(String name) {
                return versionFiles.getResourceAsStream(name);
            }
        };
    }

    @Test
    void passes_for_the_same_version() {
        ClassLoader commonLoader = new VersionFileFixture(common).versionFile("rest-assured-common", "7.0.0", 1).classLoader();

        assertThatNoException().isThrownBy(() -> CommonVersionCheck.verify(withTypes(core("7.0.0")), commonLoader));
    }

    @Test
    void passes_with_a_warning_for_another_patch_version() {
        ClassLoader commonLoader = new VersionFileFixture(common).versionFile("rest-assured-common", "7.0.1", 1).classLoader();

        assertThatNoException().isThrownBy(() -> CommonVersionCheck.verify(withTypes(core("7.0.0")), commonLoader));
    }

    @Test
    void fails_for_another_minor_version() {
        ClassLoader commonLoader = new VersionFileFixture(common).versionFile("rest-assured-common", "7.1.0", 1).classLoader();

        assertThatThrownBy(() -> CommonVersionCheck.verify(withTypes(core("7.0.0")), commonLoader))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("rest-assured-common 7.1.0 is on the classpath but REST Assured is 7.0.0. If you use Spring Boot 3, set the rest-assured.version property instead of a version on the dependency. Otherwise import io.rest-assured:rest-assured-bom.");
    }

    @Test
    void fails_for_a_common_older_than_7_0_0_that_has_the_required_type() {
        // rest-assured-common 6.3.0 has NumberReturnType but no version file
        ClassLoader commonLoader = new VersionFileFixture(common).pomProperties("rest-assured-common", "6.3.0").classLoader();

        assertThatThrownBy(() -> CommonVersionCheck.verify(withTypes(core("7.0.0")), commonLoader))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("rest-assured-common 6.3.0 is on the classpath but REST Assured is 7.0.0. If you use Spring Boot 3, set the rest-assured.version property instead of a version on the dependency. Otherwise import io.rest-assured:rest-assured-bom.");
    }

    @Test
    void passes_for_a_stale_snapshot_of_common_without_a_version_file() {
        ClassLoader commonLoader = new VersionFileFixture(common).pomProperties("rest-assured-common", "7.0.0-SNAPSHOT").classLoader();

        assertThatNoException().isThrownBy(() -> CommonVersionCheck.verify(withTypes(core("7.0.0-SNAPSHOT")), commonLoader));
    }
}

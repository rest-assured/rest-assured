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

class ModuleVersionCheckTest {

    @TempDir
    Path directory;

    private VersionFileFixture restAssured(String version) {
        return new VersionFileFixture(directory).versionFile("rest-assured", version, 1);
    }

    @Test
    void passes_for_the_same_version() {
        ClassLoader classLoader = restAssured("7.0.0").versionFile("json-path", "7.0.0", 1).classLoader();

        assertThatNoException().isThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"));
    }

    @Test
    void passes_for_the_classpath_of_this_build() {
        assertThatNoException().isThrownBy(() -> ModuleVersionCheck.verify("json-path"));
        assertThatNoException().isThrownBy(() -> ModuleVersionCheck.verify("xml-path"));
    }

    @Test
    void passes_with_a_warning_for_another_patch_version() {
        ClassLoader classLoader = restAssured("7.0.0").versionFile("json-path", "7.0.1", 1).classLoader();

        assertThatNoException().isThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"));
    }

    @Test
    void fails_for_another_minor_version() {
        ClassLoader classLoader = restAssured("7.0.1").versionFile("json-path", "7.1.0", 1).classLoader();

        assertThatThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("io.rest-assured:json-path 7.1.0 doesn't match REST Assured 7.0.1. Use one version for all REST Assured artifacts, for example by importing io.rest-assured:rest-assured-bom.");
    }

    @Test
    void fails_for_another_revision_of_the_path_spi() {
        ClassLoader classLoader = restAssured("7.0.0").versionFile("json-path", "7.0.1", 2).classLoader();

        assertThatThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("io.rest-assured:json-path 7.0.1 doesn't match REST Assured 7.0.0.");
    }

    @Test
    void fails_for_a_module_older_than_7_0_0_that_has_no_version_file() {
        ClassLoader classLoader = restAssured("7.0.0").pomProperties("json-path", "6.3.0").classLoader();

        assertThatThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("io.rest-assured:json-path 6.3.0 doesn't match REST Assured 7.0.0. Use one version for all REST Assured artifacts, for example by importing io.rest-assured:rest-assured-bom.");
    }

    @Test
    void passes_when_a_version_file_wasnt_filtered_by_maven() {
        ClassLoader classLoader = restAssured("7.0.0").versionFile("json-path", "${project.version}", 1).classLoader();

        assertThatNoException().isThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"));
    }

    @Test
    void passes_when_the_module_isnt_on_the_classpath() {
        ClassLoader classLoader = restAssured("7.0.0").classLoader();

        assertThatNoException().isThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"));
    }

    @Test
    void passes_when_the_version_of_rest_assured_is_unknown() {
        ClassLoader classLoader = new VersionFileFixture(directory).versionFile("json-path", "6.3.0", 1).classLoader();

        assertThatNoException().isThrownBy(() -> ModuleVersionCheck.verify(classLoader, "json-path"));
    }
}

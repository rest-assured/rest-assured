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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A class loader that sees only the JDK and the version files written to a directory.
 */
class VersionFileFixture {
    private final Path directory;

    VersionFileFixture(Path directory) {
        this.directory = directory;
    }

    VersionFileFixture versionFile(String artifactId, String version, int pathSpiRevision) {
        return write("META-INF/rest-assured/" + artifactId + ".properties", "version=" + version + "\npath-spi-revision=" + pathSpiRevision + "\n");
    }

    VersionFileFixture pomProperties(String artifactId, String version) {
        return write("META-INF/maven/io.rest-assured/" + artifactId + "/pom.properties", "groupId=io.rest-assured\nartifactId=" + artifactId + "\nversion=" + version + "\n");
    }

    ClassLoader classLoader() {
        try {
            return new URLClassLoader(new URL[]{directory.toUri().toURL()}, null);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private VersionFileFixture write(String resource, String content) {
        try {
            Path file = directory.resolve(resource);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

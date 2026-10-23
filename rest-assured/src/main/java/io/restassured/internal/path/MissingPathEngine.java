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

package io.restassured.internal.path;

import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.support.VersionFiles;

/**
 * The message for a path that can't be evaluated because no path engine for the document is on the classpath. It names
 * the dependency to add, with REST Assured's own version, since the path engines are separate dependencies from
 * REST Assured 7.0.0.
 */
public final class MissingPathEngine {
    private static final String FALLBACK_VERSION = "7.0.0";
    private static final String NL = System.lineSeparator();

    private MissingPathEngine() {
    }

    /**
     * @param path The path that can't be evaluated, or {@code null} if it isn't known
     */
    public static String message(String path, DocumentType type) {
        return message(path, type, version());
    }

    static String message(String path, DocumentType type, String version) {
        boolean json = type == DocumentType.JSON;
        String artifactId = json ? "json-path-gpath" : "xml-path-gpath";
        String what = (path == null ? "a path" : "the path \"" + path + "\"");
        return "REST Assured can't evaluate " + what + " because no " + (json ? "JSON" : type) + " path engine is on the classpath." + NL
                + NL
                + "Since REST Assured 7.0.0 the path engine is a separate dependency. Add GPath, the path syntax REST Assured has always used, "
                + "to your test dependencies. If you're upgrading from 6.x, your existing paths keep working unchanged:" + NL
                + "  Maven:" + NL
                + "    <dependency>" + NL
                + "        <groupId>io.rest-assured</groupId>" + NL
                + "        <artifactId>" + artifactId + "</artifactId>" + NL
                + "        <version>" + version + "</version>" + NL
                + "        <scope>test</scope>" + NL
                + "    </dependency>" + NL
                + "  Gradle:" + NL
                + "    testImplementation(\"io.rest-assured:" + artifactId + ":" + version + "\")" + NL
                + NL
                + "Or let OpenRewrite update your build and code (Gradle: see the link):" + NL
                + "  mvn org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=io.rest-assured:rest-assured-openrewrite:"
                + version + " -Drewrite.activeRecipes=io.restassured.UpgradeToRestAssured7" + NL
                + NL
                + "More: https://github.com/rest-assured/rest-assured/wiki/ReleaseNotes70#path-engines";
    }

    /**
     * @return REST Assured's version from its version file, which isn't filtered in an IDE build, or else from the
     * manifest, or else the first version that has separate path engines
     */
    static String version() {
        String version = VersionFiles.restAssuredVersion();
        if (version == null) {
            version = MissingPathEngine.class.getPackage().getImplementationVersion();
        }
        return version == null ? FALLBACK_VERSION : version;
    }
}

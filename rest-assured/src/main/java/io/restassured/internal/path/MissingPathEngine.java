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

    /**
     * @param method The method that needs a path engine, such as {@code jsonPath()}
     */
    public static String forMethod(String method, DocumentType type) {
        return forMethod(method, type, version(), PathProviders.isPresent(PathProviders.JAYWAY_JSON));
    }

    static String message(String path, DocumentType type, String version) {
        String action = "evaluate " + (path == null ? "a path" : "the path \"" + path + "\"");
        if (type != DocumentType.JSON) {
            return text(action, type, gpathOnly("xml-path-gpath", version), version);
        }
        String engines = "Since REST Assured 7.0.0 the path engine is a separate dependency. Add one of these to your test dependencies." + NL
                + NL
                + "GPath, the path syntax REST Assured has always used. Pick this if you're upgrading from 6.x, and your existing paths keep working unchanged:" + NL
                + snippets("json-path-gpath", version) + NL
                + NL
                + "JSONPath (Jayway), for example \"$.lotto.winners[*].winnerId\":" + NL
                + snippets("json-path-jayway", version);
        return text(action, type, engines, version);
    }

    /**
     * jsonPath(), xmlPath() and htmlPath() only work with GPath, so the message names only GPath.
     *
     * @param jaywayIsPresent Whether json-path-jayway is on the classpath, whose JaywayPath can take the place of jsonPath()
     */
    static String forMethod(String method, DocumentType type, String version, boolean jaywayIsPresent) {
        String engines = gpathOnly(type == DocumentType.JSON ? "json-path-gpath" : "xml-path-gpath", version);
        if (jaywayIsPresent && type == DocumentType.JSON) {
            engines += NL + NL + "With io.rest-assured:json-path-jayway, use JaywayPath.from(response.asString()) instead of " + method + ".";
        }
        return text("run " + method, type, engines, version);
    }

    private static String gpathOnly(String artifactId, String version) {
        return "Since REST Assured 7.0.0 the path engine is a separate dependency. Add GPath, the path syntax REST Assured has always used, "
                + "to your test dependencies. If you're upgrading from 6.x, your existing paths keep working unchanged:" + NL
                + snippets(artifactId, version);
    }

    private static String snippets(String artifactId, String version) {
        return "  Maven:" + NL
                + "    <dependency>" + NL
                + "        <groupId>io.rest-assured</groupId>" + NL
                + "        <artifactId>" + artifactId + "</artifactId>" + NL
                + "        <version>" + version + "</version>" + NL
                + "        <scope>test</scope>" + NL
                + "    </dependency>" + NL
                + "  Gradle:" + NL
                + "    testImplementation(\"io.rest-assured:" + artifactId + ":" + version + "\")";
    }

    private static String text(String action, DocumentType type, String engines, String version) {
        return "REST Assured can't " + action + " because no " + (type == DocumentType.JSON ? "JSON" : type) + " path engine is on the classpath." + NL
                + NL
                + engines + NL
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

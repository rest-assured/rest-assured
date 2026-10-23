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

import io.restassured.config.JsonPathEngine;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathProvider;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.support.ModuleVersionCheck;
import io.restassured.internal.util.SafeExceptionRethrower;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the path engine for a document. REST Assured's own path modules are found by class name, so that only they
 * count, and each is loaded once, after checking that its version matches REST Assured's.
 */
public final class PathProviders {
    static final String GPATH_JSON = "io.restassured.internal.path.json.GPathJsonProvider";
    static final String GPATH_XML = "io.restassured.internal.path.xml.GPathXmlProvider";

    // The JSON engines in the order they're picked when none is configured
    private static final List<JsonEngine> JSON_ENGINES = List.of(new JsonEngine(JsonPathEngine.GPATH, GPATH_JSON, "json-path"));

    private static final Map<String, Optional<PathProvider>> LOADED = new ConcurrentHashMap<>();

    private PathProviders() {
    }

    /**
     * @return The provider that parses and evaluates paths on documents of this type. For JSON, it's the engine of the
     * {@link io.restassured.config.JsonConfig}, or else the first engine in a fixed order that is on the classpath.
     */
    public static PathProvider forType(DocumentType type, RestAssuredConfig config) {
        if (type == DocumentType.JSON) {
            return forJson(config.getJsonConfig().pathEngine(), JSON_ENGINES);
        }
        return load(GPATH_XML, "xml-path")
                .orElseThrow(() -> new IllegalStateException("REST Assured found no XML path engine. Add io.rest-assured:xml-path to your test dependencies."));
    }

    static PathProvider forJson(JsonPathEngine configured, List<JsonEngine> engines) {
        if (configured != null) {
            JsonEngine engine = engines.stream().filter(it -> it.engine() == configured).findFirst().orElseThrow();
            return load(engine.className(), engine.artifactId()).orElseThrow(() -> new IllegalStateException("JsonConfig.pathEngine is " + configured
                    + " but io.rest-assured:" + engine.artifactId() + " isn't on the classpath."));
        }
        return engines.stream().map(engine -> load(engine.className(), engine.artifactId())).flatMap(Optional::stream).findFirst()
                .orElseThrow(() -> new IllegalStateException("REST Assured found no JSON path engine. Add io.rest-assured:json-path to your test dependencies."));
    }

    /**
     * @return The value of a path result, {@code null} for a missing path.
     * @throws IllegalArgumentException If the path can't be evaluated on the document.
     */
    public static Object valueOf(PathResult result) {
        switch (result.kind()) {
            case VALUE:
                return result.value();
            case MISSING:
                return null;
            default:
                throw new IllegalArgumentException(result.message());
        }
    }

    static Optional<PathProvider> load(String className, String artifactId) {
        return LOADED.computeIfAbsent(className, name -> newInstance(name, artifactId));
    }

    private static Optional<PathProvider> newInstance(String className, String artifactId) {
        // Before the class is loaded, so that a module of another version fails with a message
        ModuleVersionCheck.verify(artifactId);
        Class<?> providerClass;
        try {
            providerClass = Class.forName(className, true, PathProviders.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
        try {
            return Optional.of((PathProvider) providerClass.getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    record JsonEngine(JsonPathEngine engine, String className, String artifactId) {
    }
}

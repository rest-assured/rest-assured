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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Finds the path engine for a document. REST Assured's own path modules are found by class name, so that only they
 * count, and each is loaded once, after checking that its version matches REST Assured's.
 */
public final class PathProviders {
    static final String GPATH_JSON = "io.restassured.internal.path.json.GPathJsonProvider";
    static final String GPATH_XML = "io.restassured.internal.path.xml.GPathXmlProvider";
    static final String JAYWAY_JSON = "io.restassured.internal.path.jayway.JaywayJsonProvider";

    // The JSON engines in the order they're picked when none is configured
    static final List<JsonEngine> JSON_ENGINES = List.of(new JsonEngine(JsonPathEngine.GPATH, GPATH_JSON, "json-path-gpath"),
            new JsonEngine(JsonPathEngine.JAYWAY, JAYWAY_JSON, "json-path-jayway"));

    private static final Map<String, Optional<PathProvider>> LOADED = new ConcurrentHashMap<>();
    private static final OnceLog INFO = new OnceLog(message -> System.getLogger("io.restassured").log(System.Logger.Level.INFO, message));

    private PathProviders() {
    }

    /**
     * @return The provider that parses and evaluates paths on documents of this type. For JSON, it's the engine of the
     * {@link io.restassured.config.JsonConfig}, or else the first engine in a fixed order that is on the classpath.
     */
    public static PathProvider forType(DocumentType type, RestAssuredConfig config) {
        return forType(type, config, null);
    }

    /**
     * @param path The path to evaluate, for the message when no engine is on the classpath, or {@code null} if it isn't known
     * @see #forType(DocumentType, RestAssuredConfig)
     */
    public static PathProvider forType(DocumentType type, RestAssuredConfig config, String path) {
        if (type == DocumentType.JSON) {
            return forJson(config.getJsonConfig().pathEngine(), JSON_ENGINES, path, INFO);
        }
        return gpath(type).orElseThrow(() -> new IllegalStateException(MissingPathEngine.message(path, type)));
    }

    /**
     * Checks that GPath (json-path-gpath for JSON, xml-path-gpath for XML and HTML) is on the classpath, with the
     * version of REST Assured, before {@code jsonPath()}, {@code xmlPath()} or {@code htmlPath()} uses it.
     *
     * @param method The method that needs GPath, for the message when it isn't on the classpath
     * @throws IllegalStateException If GPath isn't on the classpath
     */
    public static void requireGPath(DocumentType type, String method) {
        if (gpath(type).isEmpty()) {
            throw new IllegalStateException(MissingPathEngine.forMethod(method, type));
        }
    }

    private static Optional<PathProvider> gpath(DocumentType type) {
        return type == DocumentType.JSON ? load(GPATH_JSON, "json-path-gpath") : load(GPATH_XML, "xml-path-gpath");
    }

    static PathProvider forJson(JsonPathEngine configured, List<JsonEngine> engines) {
        return forJson(configured, engines, null, INFO);
    }

    static PathProvider forJson(JsonPathEngine configured, List<JsonEngine> engines, String path) {
        return forJson(configured, engines, path, INFO);
    }

    static PathProvider forJson(JsonPathEngine configured, List<JsonEngine> engines, String path, OnceLog info) {
        if (configured != null) {
            JsonEngine engine = engines.stream().filter(it -> it.engine() == configured).findFirst().orElseThrow();
            return load(engine.className(), engine.artifactId()).orElseThrow(() -> new IllegalStateException("JsonConfig.pathEngine is " + configured
                    + " but io.rest-assured:" + engine.artifactId() + " isn't on the classpath."));
        }
        for (int i = 0; i < engines.size(); i++) {
            JsonEngine engine = engines.get(i);
            Optional<PathProvider> provider = load(engine.className(), engine.artifactId());
            if (provider.isPresent()) {
                if (!info.hasLogged()) {
                    // Only checks that the class is there, so that an engine that isn't used can't fail
                    engines.subList(i + 1, engines.size()).stream().filter(other -> isPresent(other.className())).findFirst()
                            .ifPresent(other -> info.log(bothEnginesMessage(engine, other)));
                }
                return provider.get();
            }
        }
        throw new IllegalStateException(MissingPathEngine.message(path, DocumentType.JSON));
    }

    private static String bothEnginesMessage(JsonEngine used, JsonEngine other) {
        return "REST Assured uses " + displayName(used.engine()) + " (io.rest-assured:" + used.artifactId() + ") for JSON paths, although io.rest-assured:"
                + other.artifactId() + " is also on the classpath. To use " + displayName(other.engine()) + ", set jsonConfig().pathEngine("
                + other.engine() + ") globally or on a specification.";
    }

    private static String displayName(JsonPathEngine engine) {
        return engine == JsonPathEngine.GPATH ? "GPath" : "Jayway";
    }

    /**
     * @return The value of a path result, {@code null} for a missing path.
     * @throws IllegalArgumentException If the path can't be evaluated on the document.
     */
    public static Object valueOf(PathProvider provider, PathResult result) {
        return valueOf(provider, result, () -> isPresent(GPATH_JSON));
    }

    static Object valueOf(PathProvider provider, PathResult result, BooleanSupplier gpathIsPresent) {
        switch (result.kind()) {
            case VALUE:
                return result.value();
            case MISSING:
                return null;
            default:
                // The path may be a GPath path from REST Assured 6.x, which another engine can't evaluate
                boolean suggestGPath = !JsonPathEngine.GPATH.name().equals(provider.engineName())
                        && provider.documentTypes().contains(DocumentType.JSON) && !gpathIsPresent.getAsBoolean();
                throw new IllegalArgumentException(result.message() + (suggestGPath ? " If this is a GPath path from REST Assured 6.x, add "
                        + "io.rest-assured:json-path-gpath:" + MissingPathEngine.version() + " to your test dependencies." : ""));
        }
    }

    /**
     * @return Whether the module of the class is on the classpath. The class isn't initialized, and the module's version isn't checked.
     */
    static boolean isPresent(String className) {
        try {
            Class.forName(className, false, PathProviders.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
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
        } catch (InvocationTargetException e) {
            // Such as the message of a provider whose library is missing or too old
            return SafeExceptionRethrower.safeRethrow(e.getCause());
        } catch (ReflectiveOperationException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    record JsonEngine(JsonPathEngine engine, String className, String artifactId) {
    }

    /**
     * Logs a message the first time only.
     */
    static final class OnceLog {
        private final AtomicBoolean logged = new AtomicBoolean();
        private final Consumer<String> sink;

        OnceLog(Consumer<String> sink) {
            this.sink = sink;
        }

        boolean hasLogged() {
            return logged.get();
        }

        void log(String message) {
            if (logged.compareAndSet(false, true)) {
                sink.accept(message);
            }
        }
    }
}

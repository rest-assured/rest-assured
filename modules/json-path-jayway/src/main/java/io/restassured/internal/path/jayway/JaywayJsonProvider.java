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

package io.restassured.internal.path.jayway;

import io.restassured.common.config.NumberReturnType;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathProvider;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.common.spi.path.PathSettings;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * Evaluates JSONPath expressions with Jayway JsonPath for REST Assured. REST Assured finds it by class name.
 * <p>
 * Its methods name no Jayway type, so that it can check that Jayway is on the classpath, and recent enough, before
 * {@link JaywayEvaluator} uses it.
 */
public class JaywayJsonProvider implements PathProvider {
    private final JaywayEvaluator evaluator;

    public JaywayJsonProvider() {
        JaywayLibrary.verify();
        evaluator = new JaywayEvaluator();
    }

    @Override
    public String artifactId() {
        return "json-path-jayway";
    }

    @Override
    public String engineName() {
        return "JAYWAY";
    }

    @Override
    public Set<DocumentType> documentTypes() {
        return Set.of(DocumentType.JSON);
    }

    @Override
    public String description(DocumentType type) {
        return JaywayEvaluator.DESCRIPTION;
    }

    @Override
    public Object parse(PathInput input, DocumentType type, PathSettings settings) {
        NumberReturnType numberReturnType = settings.numberReturnType();
        if (input.isText()) {
            return JsonParser.parse(input.text(), numberReturnType, settings.numberLengthLimit());
        }
        // JsonParser reads with its own buffer
        return JsonParser.parse(new InputStreamReader(input.stream(), Charset.forName(input.charset())), numberReturnType, settings.numberLengthLimit());
    }

    @Override
    public RuntimeException parseFailure(DocumentType type, Exception cause) {
        return evaluator.parseFailure(cause);
    }

    @Override
    public PathResult evaluate(Object document, DocumentType type, String path) {
        return evaluator.evaluate(document, path);
    }

    /**
     * Joins with a dot, unless the key starts with a dot or a bracket, or the root path ends with a dot, so that
     * {@code ..} stays a deep scan. When both have the dot, one is dropped.
     */
    @Override
    public String joinRootPath(String rootPath, String key) {
        if (rootPath == null || rootPath.isEmpty()) {
            return key;
        } else if (key.isEmpty()) {
            return rootPath;
        } else if (rootPath.endsWith(".") && key.startsWith(".")) {
            return rootPath + key.substring(1);
        } else if (rootPath.endsWith(".") || key.startsWith(".") || key.startsWith("[")) {
            return rootPath + key;
        }
        return rootPath + "." + key;
    }
}

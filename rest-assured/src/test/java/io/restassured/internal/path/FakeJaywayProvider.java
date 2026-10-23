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
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathProvider;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.common.spi.path.PathSettings;

import java.util.Set;

/**
 * A JSON path provider that stands in for json-path-jayway, which core doesn't depend on.
 */
public class FakeJaywayProvider implements PathProvider {

    @Override
    public String artifactId() {
        return "fake-jayway";
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
        return "Fake JSON path";
    }

    @Override
    public Object parse(PathInput input, DocumentType type, PathSettings settings) {
        return input.text();
    }

    @Override
    public RuntimeException parseFailure(DocumentType type, Exception cause) {
        return new IllegalStateException(cause);
    }

    @Override
    public PathResult evaluate(Object document, DocumentType type, String path) {
        return PathResult.value(path);
    }

    @Override
    public String joinRootPath(String rootPath, String key) {
        return rootPath == null || rootPath.isEmpty() ? key : rootPath + "/" + key;
    }
}

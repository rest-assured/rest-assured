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

package io.restassured.internal.path.json;

import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathProvider;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.common.spi.path.PathSettings;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.path.json.config.JsonPathConfig;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Evaluates GPath expressions on JSON documents for REST Assured.
 */
public class GPathJsonProvider implements PathProvider {

    @Override
    public String artifactId() {
        return "json-path";
    }

    @Override
    public String engineName() {
        return "GPATH";
    }

    @Override
    public Set<DocumentType> documentTypes() {
        return Set.of(DocumentType.JSON);
    }

    @Override
    public String description(DocumentType type) {
        return "JSON path";
    }

    @Override
    public Object parse(PathInput input, DocumentType type, PathSettings settings) throws IOException {
        JsonPathConfig.NumberReturnType numberReturnType = JsonPathConfig.NumberReturnType.valueOf(settings.numberReturnType().name());
        ConfigurableJsonSlurper slurper = new ConfigurableJsonSlurper(numberReturnType, settings.numberLengthLimit());
        if (input.isText()) {
            return slurper.parseText(input.text());
        }
        return slurper.parse(new InputStreamReader(new BufferedInputStream(input.stream()), input.charset()));
    }

    /**
     * @return The exception that {@code jsonPath} throws when it can't parse the document.
     */
    @Override
    public RuntimeException parseFailure(DocumentType type, Exception cause) {
        return new JsonPathException("Failed to parse the JSON document", cause);
    }

    @Override
    public PathResult evaluate(Object document, DocumentType type, String path) {
        JSONAssertion assertion = new JSONAssertion();
        assertion.setKey(path);
        return PathResult.value(assertion.getResult(document, null));
    }
}

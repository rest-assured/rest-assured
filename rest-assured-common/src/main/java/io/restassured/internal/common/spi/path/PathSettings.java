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

package io.restassured.internal.common.spi.path;

import io.restassured.common.config.NumberReturnType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The settings a provider parses a body with, as plain values.
 *
 * @param numberReturnType        How JSON numbers with a fraction are returned.
 * @param numberLengthLimit       The maximum number of characters of a JSON number, or a negative number for no limit.
 * @param validating              Whether the XML parser validates the document.
 * @param namespaceAware          Whether the XML parser is namespace aware.
 * @param allowDocTypeDeclaration Whether the XML parser allows a DOCTYPE declaration.
 * @param features                Features to set on the XML or HTML parser.
 * @param properties              Properties to set on the XML or HTML parser.
 * @param declaredNamespaces      Namespaces to declare on an XML or HTML document, by prefix.
 */
public record PathSettings(NumberReturnType numberReturnType, int numberLengthLimit, boolean validating, boolean namespaceAware,
                           boolean allowDocTypeDeclaration, Map<String, Boolean> features, Map<String, Object> properties,
                           Map<String, String> declaredNamespaces) {

    public PathSettings {
        features = copy(features);
        properties = copy(properties);
        declaredNamespaces = copy(declaredNamespaces);
    }

    // Keeps the order of the given map, in which the parser gets the features and properties
    private static <V> Map<String, V> copy(Map<String, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }
}

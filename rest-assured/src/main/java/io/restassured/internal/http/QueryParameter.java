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

package io.restassured.internal.http;

import io.restassured.internal.NoParameterValue;

/**
 * A query parameter of a {@link URIBuilder}. A parameter without a value is written without an equals sign.
 */
public final class QueryParameter {

    private final String name;
    private final String value;
    private final boolean hasValue;

    private QueryParameter(String name, String value, boolean hasValue) {
        this.name = name;
        this.value = value;
        this.hasValue = hasValue;
    }

    /**
     * A parameter given by the user: <code>null</code> means an empty value and {@link NoParameterValue} means no value.
     */
    public static QueryParameter of(String name, Object value) {
        return new QueryParameter(name, value == null ? "" : value.toString(), !(value instanceof NoParameterValue));
    }

    /**
     * A parameter read from a query string, where <code>value</code> is <code>null</code> when there was no equals sign.
     * It's written with an equals sign either way.
     */
    public static QueryParameter parsed(String name, String value) {
        return new QueryParameter(name, value, true);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean hasValue() {
        return hasValue;
    }
}

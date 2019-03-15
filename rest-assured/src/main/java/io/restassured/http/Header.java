/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.http;

import io.restassured.internal.NameAndValue;
import io.restassured.internal.common.assertion.AssertParameter;

/**
 * Represents a HTTP header
 */
public class Header implements NameAndValue {

    private final String name;
    private final String value;

    /**
     * Create a new header with the given name and value.
     *
     * @param name  The header name, cannot be null.
     * @param value The value (can be null)
     */
    public Header(String name, String value) {
        AssertParameter.notNull(name, "Header name");
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean hasSameNameAs(Header header) {
        AssertParameter.notNull(header, Header.class);
        return this.name.equalsIgnoreCase(header.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Header header = (Header) o;

        // HTTP header names are always case-insensitive. Values are usually case-insensitive.
        if (name != null ? !name.equalsIgnoreCase(header.name) : header.name != null) return false;
        if (value != null ? !value.equalsIgnoreCase(header.value) : header.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(name);
        if (value != null) {
            builder.append("=").append(value);
        }
        return builder.toString();
    }
}

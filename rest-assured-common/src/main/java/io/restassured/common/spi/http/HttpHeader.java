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

package io.restassured.common.spi.http;

import java.util.Objects;

/**
 * A request or response header.
 *
 * @param name  the name, as it's sent or as it was received
 * @param value the value, which may be empty. {@code null} becomes an empty value.
 */
public record HttpHeader(String name, String value) {

    public HttpHeader {
        Objects.requireNonNull(name, "name");
        value = value == null ? "" : value;
    }
}

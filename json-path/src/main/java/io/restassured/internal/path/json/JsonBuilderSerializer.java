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

import groovy.json.JsonBuilder;

/**
 * Serializes a Map or Collection request body to JSON exactly like Groovy's {@link JsonBuilder} does, so that
 * modules without a Groovy dependency can produce the same JSON. Internal API.
 */
public class JsonBuilderSerializer {

    private JsonBuilderSerializer() {
    }

    /**
     * @param mapOrCollection the Map or Collection to serialize
     * @return the JSON produced by {@code new JsonBuilder(mapOrCollection).toString()}
     */
    public static String toJson(Object mapOrCollection) {
        return new JsonBuilder(mapOrCollection).toString();
    }
}

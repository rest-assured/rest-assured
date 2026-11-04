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

package io.restassured.path.json;

import groovy.json.JsonException;
import groovy.lang.GroovySystem;
import io.restassured.path.json.exception.JsonPathException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A document that is just a string with an invalid escape. Groovy's JSON parser reads it as null up to Groovy 5, and fails on
 * it from Groovy 6. The same escape inside an object or an array fails on every Groovy. The groovy CI job runs this on each.
 */
class JsonPathInvalidEscapeTest {

    private static final boolean GROOVY_6_OR_LATER = Integer.parseInt(GroovySystem.getVersion().split("\\.")[0]) >= 6;

    @ParameterizedTest
    @ValueSource(strings = {"\"\\x\"", "\"\\u12\""})
    void a_string_with_an_invalid_escape_as_the_document_fails_on_groovy_6_and_is_null_before(String json) {
        if (GROOVY_6_OR_LATER) {
            assertThatThrownBy(() -> new JsonPath(json).get())
                    .isInstanceOf(JsonPathException.class)
                    .hasMessage("Failed to parse the JSON document")
                    .cause().isInstanceOf(JsonException.class);
        } else {
            assertThat((Object) new JsonPath(json).get()).isNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"[\"\\x\"]", "{\"a\":\"\\u12\"}"})
    void an_invalid_escape_inside_an_array_or_object_fails_on_every_groovy(String json) {
        assertThatThrownBy(() -> new JsonPath(json).get())
                .isInstanceOf(JsonPathException.class)
                .cause().isInstanceOf(JsonException.class);
    }
}

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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Pins how JsonPath renders JSON objects and arrays when they are converted to a String.
 */
class JsonPathToStringFormatTest {

    private static final String JSON = "{\"obj\":{\"b\":1,\"c\":[1,2]},\"list\":[{\"x\":1},{\"x\":2}],\"empty\":{},\"emptyList\":[],\"m\":{\"k\":{\"n\":1}}}";

    private final JsonPath jsonPath = new JsonPath(JSON);

    @Test
    void get_string_renders_an_object_in_groovy_map_format() {
        assertThat(jsonPath.getString("obj")).isEqualTo("[b:1, c:[1, 2]]");
    }

    @Test
    void get_string_renders_a_list_of_objects_in_groovy_format() {
        assertThat(jsonPath.getString("list")).isEqualTo("[[x:1], [x:2]]");
    }

    @Test
    void get_string_renders_empty_object_and_list_in_groovy_format() {
        assertThat(jsonPath.getString("empty")).isEqualTo("[:]");
        assertThat(jsonPath.getString("emptyList")).isEqualTo("[]");
    }

    @Test
    void get_map_with_string_values_renders_nested_objects_in_groovy_format() {
        Map<String, String> map = jsonPath.getMap("m", String.class, String.class);

        assertThat(map).containsExactly(Map.entry("k", "[n:1]"));
    }

    @Test
    void conversion_error_shows_the_object_in_groovy_format() {
        Throwable t = catchThrowable(() -> jsonPath.getInt("obj"));

        assertThat(t).isInstanceOf(NumberFormatException.class).hasMessage("For input string: \"[b:1, c:[1, 2]]\"");
    }
}

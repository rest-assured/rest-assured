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

package io.restassured.internal.util;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class GroovyStringConversionTest {

    @Test
    void call_to_string_returns_what_groovy_returns_for_to_string() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", Arrays.asList("x", null));

        List<Object> values = Arrays.asList(
                42, "text", new StringBuilder("sb"), 'c', true,
                new byte[]{1, 2}, new int[]{3}, new char[]{'h', 'i'}, new Object[]{"a", new int[]{1}, null}, new String[0],
                map, Collections.emptyMap(), new TreeMap<>(map), new ConcurrentHashMap<>(Collections.singletonMap("a", 1)),
                Collections.unmodifiableMap(map), Map.of("a", 1), Collections.singletonMap("a", 1),
                Arrays.asList(1, "two"), new ArrayList<>(List.of("a")), Collections.unmodifiableList(List.of("a")), List.of("a"),
                new CopyOnWriteArrayList<>(List.of("a")), new ArrayDeque<>(List.of("a")), Collections.singleton("a"),
                new PublicOwnToStringList(), new PrivateOwnToStringList(), new PackagePrivateOwnToStringMap(),
                new OwnToString());

        for (Object value : values) {
            assertThat(GroovyStringConversion.callToString(value))
                    .as(value.getClass().getName())
                    .isEqualTo(InvokerHelper.invokeMethod(value, "toString", null));
        }
    }

    public static class PublicOwnToStringList extends ArrayList<String> {
        PublicOwnToStringList() {
            add("a");
        }

        @Override
        public String toString() {
            return "own";
        }
    }

    private static class PrivateOwnToStringList extends ArrayList<String> {
        PrivateOwnToStringList() {
            add("a");
        }

        @Override
        public String toString() {
            return "own";
        }
    }

    static class PackagePrivateOwnToStringMap extends LinkedHashMap<String, String> {
        PackagePrivateOwnToStringMap() {
            put("a", "b");
        }

        @Override
        public String toString() {
            return "own";
        }
    }

    private static class OwnToString {
        @Override
        public String toString() {
            return "own";
        }
    }
}

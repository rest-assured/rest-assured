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

import groovy.lang.IntRange;
import io.restassured.internal.common.util.GroovyStyleToString;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compares {@link GroovyStyleToString} with what Groovy itself returns for a dynamically dispatched
 * {@code value.toString()}. It lives in core because Groovy is on core's test classpath.
 */
class GroovyStyleToStringTest {

    @Test
    void renders_like_groovy_dynamic_to_string() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", Arrays.asList("x", null));

        List<Object> values = Arrays.asList(
                42, "text", new StringBuilder("sb"), 'c', true,
                new byte[]{1, 2}, new int[]{3}, new char[]{'h', 'i'}, new Object[]{"a", new int[]{1}, null}, new String[0],
                map, Collections.emptyMap(), new TreeMap<>(map), new ConcurrentHashMap<>(Collections.singletonMap("a", 1)),
                Collections.unmodifiableMap(map), Map.of("a", 1), Collections.singletonMap("a", 1),
                Collections.synchronizedMap(new HashMap<>(map)), new LinkedHashMap<>(Map.of("k", new byte[]{1})),
                Arrays.asList(1, "two"), new ArrayList<>(List.of("a")), Collections.unmodifiableList(List.of("a")), List.of("a"),
                Collections.emptyList(), Collections.checkedList(new ArrayList<>(List.of("a")), String.class),
                new CopyOnWriteArrayList<>(List.of("a")), new ArrayDeque<>(List.of("a")), Collections.singleton("a"),
                new IntRange(1, 3), Arrays.asList(new IntRange(1, 3), Map.of("r", new IntRange(2, 4))),
                Arrays.asList(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("nested")),
                new PublicOwnToStringList(), new PrivateOwnToStringList(), new PackagePrivateOwnToStringMap(),
                new HashMap<String, String>() {
                    @Override
                    public String toString() {
                        return "anonymous";
                    }
                },
                new AbstractMap<String, String>() {
                    @Override
                    public Set<Entry<String, String>> entrySet() {
                        return Collections.singletonMap("a", "b").entrySet();
                    }
                },
                new OwnToString(),
                DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("a"));

        List<String> differences = new ArrayList<>();
        for (Object value : values) {
            String actual = GroovyStyleToString.toString(value);
            Object expected = InvokerHelper.invokeMethod(value, "toString", null);
            if (!actual.equals(expected)) {
                differences.add(value.getClass().getName() + ": expected <" + expected + "> but was <" + actual + ">");
            }
        }
        assertThat(differences).isEmpty();
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

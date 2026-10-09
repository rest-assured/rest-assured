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

package io.restassured.internal;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterizes how a collection header value is flattened to the strings that are sent as separate headers.
 */
class HeaderValueFlatteningTest {

    @Test
    void flattens_nested_collections_and_arrays_recursively_and_keeps_nulls() {
        List<Object> value = new ArrayList<>();
        value.add("a");
        value.add(Arrays.asList("b", Arrays.asList("c", null)));
        value.add(new Object[]{"d", new String[]{"e"}, Collections.singleton(1)});
        value.add(new int[]{2, 3});
        value.add(Collections.emptyList());
        value.add(null);
        value.add(4L);

        Collection<String> flattened = flatten(value);

        assertThat(flattened).containsExactly("a", "b", "c", null, "d", "e", "1", "2", "3", null, "4");
    }

    @Test
    void returns_an_empty_collection_for_an_empty_collection() {
        assertThat(flatten(Collections.emptyList())).isEmpty();
    }

    @Test
    void keeps_duplicates_of_a_list() {
        assertThat(flatten(Arrays.asList("a", "a", Collections.singletonList("a")))).containsExactly("a", "a", "a");
    }

    @Test
    void drops_duplicates_of_a_set_and_keeps_its_order() {
        Set<Object> value = new LinkedHashSet<>();
        value.add("b");
        value.add(Arrays.asList("a", "b", "c"));

        assertThat(flatten(value)).containsExactly("b", "a", "c");
    }

    @Test
    void sorts_a_sorted_set_with_its_comparator() {
        SortedSet<Object> value = new TreeSet<>(Comparator.comparing(Object::toString).reversed());
        value.add("a");
        value.add("c");

        assertThat(flatten(value)).containsExactly("c", "a");
    }

    @Test
    void flattens_iterators_and_unwraps_present_optionals_without_flattening_their_value() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", 1);
        List<Object> value = Arrays.asList(
                Arrays.asList("a", "b").iterator(),
                Optional.of("c"),
                Optional.empty(),
                Optional.of(Arrays.asList("d", "e")),
                Optional.of(new String[]{"f"}),
                map);

        assertThat(flatten(value)).containsExactly("a", "b", "c", "[d, e]", "[f]", "[k:1]");
    }

    private static Collection<String> flatten(Collection<?> value) {
        return RestAssuredHttpBuilder.flattenToString(value);
    }
}

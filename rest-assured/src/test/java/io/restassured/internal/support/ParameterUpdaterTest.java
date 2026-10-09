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
package io.restassured.internal.support;

import groovy.lang.GString;
import io.restassured.internal.NoParameterValue;
import io.restassured.specification.FilterableRequestSpecification;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ParamConfig.UpdateStrategy.MERGE;
import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterUpdaterTest {

    private final List<Object> serialized = new ArrayList<>();
    private final ParameterUpdater updater = new ParameterUpdater(value -> {
        serialized.add(value);
        return "s(" + (value instanceof Object[] ? Arrays.toString((Object[]) value) : value) + ")";
    });
    private final Map<String, Object> to = new LinkedHashMap<>();

    // updateStandardParameter

    @Test
    void standard_parameter_without_value_is_a_no_parameter_value() {
        updater.updateStandardParameter(REPLACE, to, "a");

        assertThat(to.get("a")).isExactlyInstanceOf(NoParameterValue.class);
        assertThat(serialized).isEmpty();
    }

    @Test
    void standard_parameter_with_null_value_replaces_an_existing_value_with_a_no_parameter_value_even_when_merging() {
        to.put("a", "existing");

        updater.updateStandardParameter(MERGE, to, "a", null);

        assertThat(to.get("a")).isExactlyInstanceOf(NoParameterValue.class);
    }

    @Test
    void standard_parameter_is_serialized_and_replaces_the_existing_value() {
        to.put("a", "existing");

        updater.updateStandardParameter(REPLACE, to, "a", 1);

        assertThat(to).containsExactly(Map.entry("a", "s(1)"));
        assertThat(serialized).containsExactly(1);
    }

    @Test
    void merging_a_standard_parameter_with_no_existing_value_puts_the_serialized_value() {
        updater.updateStandardParameter(MERGE, to, "a", 1);

        assertThat(to.get("a")).isEqualTo("s(1)");
    }

    @Test
    void merging_a_standard_parameter_with_a_non_list_value_creates_an_array_list_of_both() {
        to.put("a", "existing");

        updater.updateStandardParameter(MERGE, to, "a", 1);

        assertThat(to.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList("existing", "s(1)"));
    }

    @Test
    void merging_a_standard_parameter_with_an_existing_null_value_creates_a_list_with_null() {
        to.put("a", null);

        updater.updateStandardParameter(MERGE, to, "a", 1);

        assertThat(to.get("a")).isEqualTo(Arrays.asList(null, "s(1)"));
    }

    @Test
    void merging_a_standard_parameter_appends_to_the_existing_list() {
        List<Object> existing = new LinkedList<>(Collections.singletonList("existing"));
        to.put("a", existing);

        updater.updateStandardParameter(MERGE, to, "a", 1);

        assertThat(to.get("a")).isSameAs(existing);
        assertThat(existing).containsExactly("existing", "s(1)");
    }

    @Test
    void merging_a_standard_parameter_with_an_existing_set_nests_the_set_in_a_new_list() {
        Set<Object> existing = new LinkedHashSet<>(Arrays.asList("x", "y"));
        to.put("a", existing);

        updater.updateStandardParameter(MERGE, to, "a", 1);

        assertThat(to.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList(existing, "s(1)"));
    }

    @Test
    void merging_a_standard_parameter_into_an_unmodifiable_list_fails() {
        to.put("a", Collections.unmodifiableList(Arrays.asList("x")));

        assertThatThrownBy(() -> updater.updateStandardParameter(MERGE, to, "a", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    // updateCollectionParameter

    @Test
    void null_or_empty_collection_parameter_is_a_no_parameter_value() {
        updater.updateCollectionParameter(MERGE, to, "a", null);
        updater.updateCollectionParameter(REPLACE, to, "b", Collections.emptyList());

        assertThat(to.get("a")).isExactlyInstanceOf(NoParameterValue.class);
        assertThat(to.get("b")).isExactlyInstanceOf(NoParameterValue.class);
    }

    @Test
    void replacing_a_collection_parameter_puts_an_array_list_of_serialized_values() {
        to.put("a", "existing");

        updater.updateCollectionParameter(REPLACE, to, "a", Arrays.asList(1, null, "x"));

        assertThat(to.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList("s(1)", "s(null)", "s(x)"));
        assertThat(serialized).containsExactly(1, null, "x");
    }

    @Test
    void merging_a_collection_parameter_without_existing_value_puts_a_linked_list() {
        updater.updateCollectionParameter(MERGE, to, "a", new LinkedHashSet<>(Arrays.asList(1, 2)));

        assertThat(to.get("a")).isExactlyInstanceOf(LinkedList.class).isEqualTo(Arrays.asList("s(1)", "s(2)"));
    }

    @Test
    void merging_a_collection_parameter_adds_all_to_an_existing_collection() {
        Set<Object> existing = new LinkedHashSet<>(Arrays.asList("x", "s(1)"));
        to.put("a", existing);

        updater.updateCollectionParameter(MERGE, to, "a", Arrays.asList(1, 2));

        assertThat(to.get("a")).isSameAs(existing);
        assertThat(existing).containsExactly("x", "s(1)", "s(2)");
    }

    @Test
    void merging_a_collection_parameter_with_an_existing_scalar_flattens_them_into_an_array_list() {
        to.put("a", "existing");

        updater.updateCollectionParameter(MERGE, to, "a", Arrays.asList(1, 2));

        assertThat(to.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList("existing", "s(1)", "s(2)"));
    }

    @Test
    void merging_a_collection_parameter_with_an_existing_null_keeps_the_null() {
        to.put("a", null);

        updater.updateCollectionParameter(MERGE, to, "a", Arrays.asList(1, 2));

        assertThat(to.get("a")).isEqualTo(Arrays.asList(null, "s(1)", "s(2)"));
    }

    @Test
    void merging_a_collection_parameter_with_an_existing_array_flattens_the_array() {
        to.put("a", new Object[]{"x", new int[]{1, 2}, Arrays.asList("y", new String[]{"z"})});

        updater.updateCollectionParameter(MERGE, to, "a", Arrays.asList(1, 2));

        assertThat(to.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList("x", 1, 2, "y", "z", "s(1)", "s(2)"));
    }

    @Test
    void merging_a_collection_parameter_with_an_existing_iterator_flattens_the_iterator() {
        to.put("a", Arrays.asList("x", Arrays.asList("y", "z")).iterator());

        updater.updateCollectionParameter(MERGE, to, "a", Arrays.asList(1));

        assertThat(to.get("a")).isEqualTo(Arrays.asList("x", "y", "z", "s(1)"));
    }

    @Test
    void merging_a_collection_parameter_with_an_existing_optional_unwraps_the_optional() {
        List<String> inOptional = Arrays.asList("x", "y");
        to.put("a", Optional.of(inOptional));
        to.put("b", Optional.empty());

        updater.updateCollectionParameter(MERGE, to, "a", Arrays.asList(1));
        updater.updateCollectionParameter(MERGE, to, "b", Arrays.asList(1));

        assertThat(to.get("a")).isEqualTo(Arrays.asList(inOptional, "s(1)"));
        assertThat(to.get("b")).isEqualTo(Arrays.asList("s(1)"));
    }

    @Test
    void merging_a_collection_parameter_with_an_existing_map_does_not_flatten_the_map() {
        Map<String, Object> existing = Collections.singletonMap("k", "v");
        to.put("a", existing);

        updater.updateCollectionParameter(MERGE, to, "a", Arrays.asList(1));

        assertThat(to.get("a")).isEqualTo(Arrays.asList(existing, "s(1)"));
    }

    // updateZeroToManyParameters

    @Test
    void zero_to_many_without_values_is_a_no_parameter_value() {
        to.put("a", "existing");
        to.put("b", "existing");
        to.put("c", "existing");
        NoParameterValue noValue = new NoParameterValue();

        updater.updateZeroToManyParameters(MERGE, to, "a");
        updater.updateZeroToManyParameters(MERGE, to, "b", (Object[]) null);
        updater.updateZeroToManyParameters(MERGE, to, "c", noValue);

        assertThat(to.get("a")).isExactlyInstanceOf(NoParameterValue.class);
        assertThat(to.get("b")).isExactlyInstanceOf(NoParameterValue.class);
        assertThat(to.get("c")).isExactlyInstanceOf(NoParameterValue.class).isNotSameAs(noValue);
        assertThat(serialized).isEmpty();
    }

    @Test
    void zero_to_many_with_one_value_is_a_standard_parameter() {
        to.put("a", "existing");

        updater.updateZeroToManyParameters(MERGE, to, "a", Arrays.asList(1, 2));

        assertThat(to.get("a")).isEqualTo(Arrays.asList("existing", "s([1, 2])"));
    }

    @Test
    void zero_to_many_with_one_null_value_is_a_no_parameter_value() {
        updater.updateZeroToManyParameters(MERGE, to, "a", new Object[]{null});

        assertThat(to.get("a")).isExactlyInstanceOf(NoParameterValue.class);
    }

    @Test
    void zero_to_many_with_several_values_is_a_collection_parameter() {
        to.put("a", "existing");

        updater.updateZeroToManyParameters(MERGE, to, "a", 1, 2);

        assertThat(to.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList("existing", "s(1)", "s(2)"));
    }

    @Test
    void zero_to_many_with_several_values_replaces_with_an_array_list() {
        updater.updateZeroToManyParameters(REPLACE, to, "a", 1, 2);

        assertThat(to.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList("s(1)", "s(2)"));
    }

    // updateParameters

    @Test
    void update_parameters_treats_collections_as_collection_parameters_and_everything_else_as_standard_parameters() {
        Map<String, Object> from = new LinkedHashMap<>();
        from.put("list", Arrays.asList(1, 2));
        from.put("emptySet", Collections.emptySet());
        from.put("array", new Object[]{3, 4});
        from.put("null", null);
        from.put("scalar", 5);
        to.put("scalar", "existing");

        updater.updateParameters(MERGE, from, to);

        assertThat(to).containsOnlyKeys("scalar", "list", "emptySet", "array", "null");
        assertThat(to.get("list")).isExactlyInstanceOf(LinkedList.class).isEqualTo(Arrays.asList("s(1)", "s(2)"));
        assertThat(to.get("emptySet")).isExactlyInstanceOf(NoParameterValue.class);
        assertThat(to.get("array")).isEqualTo("s([3, 4])");
        assertThat(to.get("null")).isExactlyInstanceOf(NoParameterValue.class);
        assertThat(to.get("scalar")).isEqualTo(Arrays.asList("existing", "s(5)"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void update_parameters_converts_gstring_keys_to_strings() {
        GString key = new GStringImpl(new Object[]{"b"}, new String[]{"a", ""});
        Map from = new LinkedHashMap();
        from.put(key, 1);
        from.put(new GStringImpl(new Object[]{"d"}, new String[]{"c", ""}), Arrays.asList(1));

        updater.updateParameters(REPLACE, from, to);

        assertThat(new ArrayList<Object>(to.keySet())).containsExactly("ab", "cd");
        assertThat(to.keySet()).allMatch(String.class::isInstance);
    }

    @Test
    void update_parameters_keeps_a_null_key() {
        Map<String, Object> from = new LinkedHashMap<>();
        from.put(null, "x");

        updater.updateParameters(REPLACE, from, to);

        assertThat(to).containsOnlyKeys((String) null);
        assertThat(to.get(null)).isEqualTo("s(x)");
    }

    /**
     * The Groovy version failed with a groovy.lang.MissingMethodException for these keys.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void update_parameters_uses_the_string_form_of_keys_that_are_neither_strings_nor_gstrings() {
        Map from = new LinkedHashMap();
        from.put(1, "x");
        from.put('c', Arrays.asList("y"));
        from.put(Arrays.asList("a", "b"), "z");

        updater.updateParameters(REPLACE, from, to);

        assertThat(new ArrayList<Object>(to.entrySet())).containsExactly(
                Map.entry("1", "s(x)"), Map.entry("c", Arrays.asList("s(y)")), Map.entry("[a, b]", "s(z)"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void request_specification_uses_the_string_form_of_parameter_names_that_are_neither_strings_nor_gstrings() {
        Map params = new LinkedHashMap();
        params.put(1, "x");

        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().queryParams(params);

        assertThat(spec.getQueryParams()).containsExactly(Map.entry("1", "x"));
    }

    @Test
    void update_parameters_validates_its_arguments() {
        assertThatThrownBy(() -> updater.updateParameters(MERGE, null, to)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Map to copy from cannot be null");
        assertThatThrownBy(() -> updater.updateParameters(MERGE, to, null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Map to copy to cannot be null");
        assertThatThrownBy(() -> updater.updateParameters(null, to, to)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("UpdateStrategy cannot be null");
    }
}

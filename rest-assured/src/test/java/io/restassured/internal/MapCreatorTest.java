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
package io.restassured.internal;

import groovy.lang.GString;
import io.restassured.internal.MapCreator.ArgsAndValue;
import io.restassured.internal.MapCreator.CollisionStrategy;
import io.restassured.specification.Argument;
import org.codehaus.groovy.runtime.GStringImpl;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.withArgs;
import static io.restassured.internal.MapCreator.CollisionStrategy.MERGE;
import static io.restassured.internal.MapCreator.CollisionStrategy.OVERWRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class MapCreatorTest {

    @Test public void
    can_merge_map_keys_with_parameters() {
        Map<String, Object> map = MapCreator.createMapFromObjects(CollisionStrategy.MERGE, "key1.%s", withArgs("hello1"), equalTo("value1"), "key2.%s", withArgs("hello2"), equalTo("value2"));

        assertThat(map).isNotEmpty();
    }

    // Regression guard for #1867: Integer values used to trigger a Groovy operator-dispatch
    // failure on the `parameters.length % 2` check under Groovy 4.0.29.
    @Test public void
    can_create_map_from_integer_values() {
        Map<String, Object> map = MapCreator.createMapFromObjects(CollisionStrategy.MERGE, "a", 1, "b", 2);

        assertThat(map).containsEntry("a", 1).containsEntry("b", 2);
    }

    // createMapFromObjects

    @Test public void
    requires_at_least_one_key_and_one_value() {
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, (Object[]) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply at least one key and one value.");
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply at least one key and one value.");
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "a")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply at least one key and one value.");
    }

    @Test public void
    requires_the_same_number_of_keys_as_values() {
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "a", 1, "b", 2, "c")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply the same number of keys as values.");
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "a", 1, "b")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply the same number of keys as values.");
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "a", withArgs(1), "b")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply the same number of keys as values.");
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "a", 1, "b", 2, "c", 3, "d", 4, "e")).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply the same number of keys as values.");
    }

    @Test public void
    overwrite_keeps_the_last_value_of_a_duplicate_key_in_the_first_position() {
        Map<String, Object> map = MapCreator.createMapFromObjects(OVERWRITE, "a", 1, "b", 2, "a", 3);

        assertThat(map).isExactlyInstanceOf(LinkedHashMap.class);
        assertThat(new ArrayList<Object>(map.entrySet())).containsExactly(Map.entry("a", 3), Map.entry("b", 2));
    }

    @Test public void
    merge_collects_the_values_of_a_duplicate_key_in_an_array_list() {
        Map<String, Object> map = MapCreator.createMapFromObjects(MERGE, "a", 1, "b", 2, "a", 3, "a", null);

        assertThat(map.get("a")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList(1, 3, null));
        assertThat(map.get("b")).isEqualTo(2);
    }

    @Test public void
    merge_appends_to_a_list_value() {
        List<Object> value = new ArrayList<>(Arrays.asList("x", "y"));

        Map<String, Object> map = MapCreator.createMapFromObjects(MERGE, "a", value, "a", 3);

        assertThat(map.get("a")).isSameAs(value);
        assertThat(value).containsExactly("x", "y", 3);
    }

    @Test public void
    merge_fails_to_append_to_an_unmodifiable_list_value() {
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "a", Collections.singletonList("x"), "a", 3)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test public void
    keeps_keys_that_are_not_strings_as_they_are() {
        GString gStringKey = new GStringImpl(new Object[]{"b"}, new String[]{"a", ""});
        List<String> listKey = Arrays.asList("x", "y");

        Map<?, ?> map = MapCreator.createMapFromObjects(OVERWRITE, gStringKey, 1, 2, 3, listKey, 4, null, 5);

        assertThat(new ArrayList<Object>(map.keySet())).containsExactly(gStringKey, 2, listKey, null);
        assertThat(map.keySet().iterator().next()).isSameAs(gStringKey);
    }

    @Test public void
    uses_arguments_when_the_second_parameter_is_a_list_of_arguments_and_the_third_a_matcher() {
        List<Argument> args1 = withArgs("hello1");
        List<Argument> args2 = withArgs("hello2");
        Matcher<String> matcher1 = equalTo("value1");

        Map<String, Object> map = MapCreator.createMapFromObjects(OVERWRITE, "key1.%s", args1, matcher1, "key2.%s", args2, "not a matcher");

        assertThat(map).containsOnlyKeys("key1.%s", "key2.%s");
        ArgsAndValue first = (ArgsAndValue) map.get("key1.%s");
        assertThat(first.getArgs()).isSameAs(args1);
        assertThat(first.getValue()).isSameAs(matcher1);
        ArgsAndValue second = (ArgsAndValue) map.get("key2.%s");
        assertThat(second.getArgs()).isSameAs(args2);
        assertThat(second.getValue()).isEqualTo("not a matcher");
    }

    @Test public void
    an_empty_list_counts_as_a_list_of_arguments() {
        Matcher<String> matcher = equalTo("value");

        Map<String, Object> map = MapCreator.createMapFromObjects(OVERWRITE, "key", Collections.emptyList(), matcher);

        assertThat(map.get("key")).isEqualTo(new ArgsAndValue(Collections.emptyList(), matcher));
    }

    @Test public void
    a_list_with_something_else_than_arguments_is_a_value() {
        List<Object> notOnlyArguments = Arrays.asList(new Argument(1), "x");

        assertThatThrownBy(() -> MapCreator.createMapFromObjects(OVERWRITE, "key", notOnlyArguments, equalTo("value")))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply the same number of keys as values.");
        assertThat(MapCreator.createMapFromObjects(OVERWRITE, "key", notOnlyArguments)).containsExactly(Map.entry("key", notOnlyArguments));
    }

    @Test public void
    merge_collects_args_and_values_of_a_duplicate_key() {
        Matcher<String> matcher1 = equalTo("value1");
        Matcher<Integer> matcher2 = greaterThan(2);

        Map<String, Object> map = MapCreator.createMapFromObjects(MERGE, "key", withArgs(1), matcher1, "key", withArgs(2), matcher2);

        assertThat(map.get("key")).isEqualTo(Arrays.asList(new ArgsAndValue(withArgs(1), matcher1), new ArgsAndValue(withArgs(2), matcher2)));
    }

    @Test public void
    rejects_arguments_that_are_not_a_list_of_arguments_after_the_first_key() {
        Map<String, Object> notArguments = new LinkedHashMap<>();
        notArguments.put("a", 1);
        notArguments.put("b", new int[]{1, 2});

        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "key1", withArgs(1), equalTo(1), Arrays.asList("k", 2), notArguments, equalTo(2)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Illegal argument '[a:1, b:[1, 2]]' passed to body expectation '[k, 2]', a list of io.restassured.specification.Argument is required.");
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "key1", withArgs(1), equalTo(1), "key2", null, equalTo(2)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Illegal argument 'null' passed to body expectation 'key2', a list of io.restassured.specification.Argument is required.");
    }

    @Test public void
    arguments_with_a_parameter_count_that_is_not_a_multiple_of_three_run_out_of_parameters() {
        assertThatThrownBy(() -> MapCreator.createMapFromObjects(MERGE, "key1", withArgs(1), equalTo(1), "key2"))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    // createMapFromParams

    @Test public void
    creates_map_from_first_parameter_and_value_and_additional_pairs() {
        Map<String, Object> map = MapCreator.createMapFromParams(MERGE, "a", 1, new Object[]{"b", 2, "a", 3});

        assertThat(map).containsOnlyKeys("a", "b");
        assertThat(map.get("a")).isEqualTo(Arrays.asList(1, 3));
    }

    @Test public void
    creates_map_from_first_parameter_and_value_without_additional_pairs() {
        assertThat(MapCreator.createMapFromParams(MERGE, "a", 1, new Object[0])).containsExactly(Map.entry("a", 1));
        assertThat(MapCreator.createMapFromParams(MERGE, "a", 1, (Object[]) null)).containsExactly(Map.entry("a", 1));
    }

    @Test public void
    does_not_flatten_additional_pairs() {
        Object[] nested = {"c", 3};

        Map<String, Object> map = MapCreator.createMapFromParams(OVERWRITE, "a", 1, new Object[]{"b", nested});

        assertThat(map.get("b")).isSameAs(nested);
    }

    @Test public void
    first_parameter_and_value_must_not_be_null() {
        assertThatThrownBy(() -> MapCreator.createMapFromParams(MERGE, null, 1, new Object[]{"b", 2})).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("firstParam cannot be null");
        assertThatThrownBy(() -> MapCreator.createMapFromParams(MERGE, "a", null, new Object[]{"b", 2})).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("firstValue cannot be null");
    }

    @Test public void
    creates_map_from_first_parameter_and_parameters() {
        Map<String, Object> map = MapCreator.createMapFromParams(OVERWRITE, "a", new Object[]{1, "b", 2});

        assertThat(new ArrayList<Object>(map.entrySet())).containsExactly(Map.entry("a", 1), Map.entry("b", 2));
    }

    @Test public void
    creating_map_from_first_parameter_without_parameters_fails() {
        assertThatThrownBy(() -> MapCreator.createMapFromParams(OVERWRITE, "a", new Object[0])).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply at least one key and one value.");
        assertThatThrownBy(() -> MapCreator.createMapFromParams(OVERWRITE, "a", (Object[]) null)).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply at least one key and one value.");
        assertThatThrownBy(() -> MapCreator.createMapFromParams(OVERWRITE, null, new Object[0])).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("firstParam cannot be null");
    }

    // ArgsAndValue

    @Test public void
    args_and_value_has_tuple_constructors_and_properties() {
        List<Argument> args = withArgs(1);

        ArgsAndValue empty = new ArgsAndValue();
        ArgsAndValue onlyArgs = new ArgsAndValue(args);
        ArgsAndValue both = new ArgsAndValue(args, "value");

        assertThat(empty.getArgs()).isNull();
        assertThat(empty.getValue()).isNull();
        assertThat(onlyArgs.getArgs()).isSameAs(args);
        assertThat(onlyArgs.getValue()).isNull();
        assertThat(both.getArgs()).isSameAs(args);
        assertThat(both.getValue()).isEqualTo("value");

        empty.setArgs(args);
        empty.setValue("value");
        assertThat(empty.getArgs()).isSameAs(args);
        assertThat(empty.getValue()).isEqualTo("value");
    }

    @Test public void
    args_and_value_to_string_is_the_class_name_and_the_formatted_properties() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("a", new int[]{1, 2});

        assertThat(new ArgsAndValue(Arrays.asList(new Argument("x")), value).toString())
                .matches("io\\.restassured\\.internal\\.MapCreator\\$ArgsAndValue\\(\\[io\\.restassured\\.specification\\.Argument@[0-9a-f]+\\], \\[a:\\[1, 2\\]\\]\\)");
        assertThat(new ArgsAndValue().toString()).isEqualTo("io.restassured.internal.MapCreator$ArgsAndValue(null, null)");
        assertThat(new ArgsAndValue(Collections.emptyList(), equalTo("x")).toString()).isEqualTo("io.restassured.internal.MapCreator$ArgsAndValue([], \"x\")");
    }

    @Test public void
    args_and_value_equals_compares_the_properties() {
        Matcher<String> matcher = equalTo("x");
        ArgsAndValue argsAndValue = new ArgsAndValue(withArgs(1, "a"), matcher);

        assertThat(argsAndValue).isEqualTo(argsAndValue);
        assertThat(argsAndValue).isEqualTo(new ArgsAndValue(new ArrayList<>(withArgs(1, "a")), matcher));
        assertThat(argsAndValue).isNotEqualTo(new ArgsAndValue(withArgs(1, "b"), matcher));
        assertThat(argsAndValue).isNotEqualTo(new ArgsAndValue(withArgs(1, "a"), equalTo("x")));
        assertThat(argsAndValue).isNotEqualTo(new ArgsAndValue(withArgs(1, "a"), null));
        assertThat(argsAndValue).isNotEqualTo(null);
        assertThat(argsAndValue).isNotEqualTo("x");
        assertThat(new ArgsAndValue()).isEqualTo(new ArgsAndValue());
        assertThat(new ArgsAndValue(null, new int[]{1, 2})).isEqualTo(new ArgsAndValue(null, new int[]{1, 2}));
        assertThat(argsAndValue.canEqual(new ArgsAndValue())).isTrue();
        assertThat(argsAndValue.canEqual("x")).isFalse();
    }

    @Test public void
    args_and_value_hash_code_uses_groovys_hash_code_helper_algorithm() {
        List<Argument> args = withArgs(1, "a");

        assertThat(new ArgsAndValue().hashCode()).isEqualTo(59 * 59 * 127);
        assertThat(new ArgsAndValue(args, "value").hashCode()).isEqualTo(59 * (59 * 127 + args.hashCode()) + "value".hashCode());
        assertThat(new ArgsAndValue(null, true).hashCode()).isEqualTo(59 * 59 * 127 + 79);
        assertThat(new ArgsAndValue(null, false).hashCode()).isEqualTo(59 * 59 * 127 + 97);
        assertThat(new ArgsAndValue(null, new int[]{1, 2}).hashCode()).isEqualTo(59 * 59 * 127 + Arrays.hashCode(new int[]{1, 2}));
    }
}

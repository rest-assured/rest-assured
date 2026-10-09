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

package io.restassured.internal.common.path;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static io.restassured.internal.common.path.ObjectConverter.canConvert;
import static io.restassured.internal.common.path.ObjectConverter.convertObjectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the conversions json-path and xml-path rely on.
 */
class ObjectConverterConversionTest {

    @Test
    void null_stays_null() {
        assertThat(convertObjectTo(null, Integer.class), nullValue());
        assertThat(convertObjectTo(null, String.class), nullValue());
    }

    @Test
    void returns_same_instance_when_already_of_the_requested_type() {
        Integer value = 1234567;
        ArrayList<Object> list = new ArrayList<>();

        assertThat(convertObjectTo(value, Integer.class), sameInstance(value));
        assertThat(convertObjectTo(list, ArrayList.class), sameInstance(list));
    }

    @Test
    void converts_to_boxed_and_primitive_types_from_their_string_representation() {
        assertThat(convertObjectTo("5", Integer.class), is(5));
        assertThat(convertObjectTo("5", int.class), is(5));
        assertThat(convertObjectTo(5L, Integer.class), is(5));
        assertThat(convertObjectTo("true", Boolean.class), is(true));
        assertThat(convertObjectTo("yes", boolean.class), is(false));
        assertThat(convertObjectTo("abc", Character.class), is('a'));
        assertThat(convertObjectTo(7, char.class), is('7'));
        assertThat(convertObjectTo("1", Byte.class), is((byte) 1));
        assertThat(convertObjectTo("1", byte.class), is((byte) 1));
        assertThat(convertObjectTo("2", Short.class), is((short) 2));
        assertThat(convertObjectTo("2", short.class), is((short) 2));
        assertThat(convertObjectTo("1.5", Float.class), is(1.5f));
        assertThat(convertObjectTo(1.5d, float.class), is(1.5f));
        assertThat(convertObjectTo("2.5", Double.class), is(2.5d));
        assertThat(convertObjectTo(2.5f, double.class), is(2.5d));
        assertThat(convertObjectTo("3", Long.class), is(3L));
        assertThat(convertObjectTo(3, long.class), is(3L));
    }

    @Test
    void converts_to_big_decimal_string_and_uuid() {
        UUID uuid = UUID.randomUUID();

        assertThat(convertObjectTo("1.50", BigDecimal.class), is(new BigDecimal("1.50")));
        assertThat(convertObjectTo(1.5d, BigDecimal.class), is(new BigDecimal("1.5")));
        assertThat(convertObjectTo(5, String.class), is("5"));
        assertThat(convertObjectTo(5, CharSequence.class), is("5"));
        assertThat(convertObjectTo(uuid.toString(), UUID.class), is(uuid));
    }

    @Test
    void super_types_of_integer_are_parsed_as_integers() {
        assertThat(convertObjectTo(5L, Number.class), instanceOf(Integer.class));
        assertThat(convertObjectTo("5", Object.class), is(5));
        assertThrows(NumberFormatException.class, () -> convertObjectTo("abc", Object.class));
        assertThrows(NumberFormatException.class, () -> convertObjectTo(5.5d, Number.class));
    }

    @Test
    void throws_class_cast_exception_with_both_types_when_not_convertible() {
        ClassCastException e = assertThrows(ClassCastException.class, () -> convertObjectTo(new ArrayList<>(), Map.class));

        assertThat(e.getMessage(), is("Cannot convert class java.util.ArrayList to interface java.util.Map."));
    }

    @Test
    void propagates_parse_failures() {
        assertThrows(NumberFormatException.class, () -> convertObjectTo("abc", Integer.class));
        assertThrows(StringIndexOutOfBoundsException.class, () -> convertObjectTo("", Character.class));
        assertThrows(IllegalArgumentException.class, () -> convertObjectTo("not-a-uuid", UUID.class));
    }

    @Test
    void can_convert_reports_whether_conversion_throws() {
        assertThat(canConvert("5", Integer.class), is(true));
        assertThat(canConvert("abc", Integer.class), is(false));
        assertThat(canConvert("", Character.class), is(false));
        assertThat(canConvert(new ArrayList<>(), Map.class), is(false));
        assertThat(canConvert(null, Map.class), is(true));
    }
}

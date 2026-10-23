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

package io.restassured.internal.http;

import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the JSON that a Map or Collection request body with a JSON content-type is sent as, value type by value type.
 */
class JsonBodyCharacterizationTest {

    private final EncoderRegistry registry = new EncoderRegistry();

    private String json(Object body) throws IOException {
        return new String(registry.encodeJSON("application/json", body).getBytes(), StandardCharsets.UTF_8);
    }

    private String jsonOf(Object value) throws IOException {
        return json(Collections.singletonList(value));
    }

    @Test
    void escapes_quotes_backslashes_control_characters_and_everything_outside_printable_ascii() throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("så", "a\"b\\c/d\n\t\b\f\r\u0001\u001f \u007få€😀");

        assertThat(json(map)).isEqualTo("{\"s\\u00e5\":\"a\\\"b\\\\c/d\\n\\t\\b\\f\\r\\u0001\\u001f \\u007f\\u00e5\\u20ac\\ud83d\\ude00\"}");
    }

    @Test
    void writes_numbers_with_their_to_string() throws IOException {
        List<Object> numbers = List.of(1, 1L, 1.5f, 2.50d, new BigDecimal("1.50"), BigInteger.TEN, (short) 3, (byte) 4,
                new BigDecimal("1E+3"), 1e20d, 1e-7d, Long.MAX_VALUE, new AtomicInteger(3));

        assertThat(json(numbers)).isEqualTo("[1,1,1.5,2.5,1.50,10,3,4,1E+3,1.0E20,1.0E-7,9223372036854775807,3]");
    }

    @Test
    void fails_for_numbers_that_json_has_no_notation_for() {
        assertThatThrownBy(() -> jsonOf(Double.NaN)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Number NaN can't be serialized as JSON: NaN are not allowed in JSON.");
        assertThatThrownBy(() -> jsonOf(Float.POSITIVE_INFINITY)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Number Infinity can't be serialized as JSON: infinite are not allowed in JSON.");
    }

    @Test
    void writes_elements_of_primitive_arrays_without_checking_them() throws IOException {
        assertThat(json(Arrays.asList(new double[]{Double.NaN}, new float[]{Float.POSITIVE_INFINITY, 2f}, new boolean[]{true}, new long[]{1L},
                new short[]{2}, new int[0])))
                .isEqualTo("[[NaN],[Infinity,2.0],[true],[1],[2],[]]");
    }

    @Test
    void writes_enum_names_and_empty_strings_and_keys_without_escaping_them() throws IOException {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("", new StringBuilder());
        map.put(new StringBuilder("kå"), Unescaped.Å);

        assertThat(json(map)).isEqualTo("{\"\":\"\",\"k\\u00e5\":\"Å\"}");
    }

    @Test
    void writes_booleans_characters_char_sequences_and_null() throws IOException {
        assertThat(json(Arrays.asList(true, false, 'c', '"', new StringBuilder("sb"), null))).isEqualTo("[true,false,\"c\",\"\\\"\",\"sb\",null]");
    }

    @Test
    void writes_enums_by_name() throws IOException {
        assertThat(jsonOf(Thread.State.NEW)).isEqualTo("[\"NEW\"]");
    }

    @Test
    void writes_uuids_and_urls_as_strings() throws IOException {
        assertThat(json(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001"), new URL("http://localhost/a?b=c"))))
                .isEqualTo("[\"00000000-0000-0000-0000-000000000001\",\"http://localhost/a?b=c\"]");
    }

    @Test
    void writes_dates_and_calendars_in_gmt() throws IOException {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"));
        calendar.setTimeInMillis(0);

        assertThat(json(List.of(new Date(0), calendar, new java.sql.Timestamp(1000))))
                .isEqualTo("[\"1970-01-01T00:00:00+0000\",\"1970-01-01T00:00:00+0000\",\"1970-01-01T00:00:01+0000\"]");
    }

    @Test
    void writes_arrays_iterables_iterators_and_enumerations_as_json_arrays() throws IOException {
        Iterable<Integer> iterable = () -> List.of(1, 2).iterator();

        assertThat(json(Arrays.asList(new int[]{1, 2}, new String[]{"a"}, new byte[]{1, 2}, new char[]{'a', 'b'}, new Object[]{null},
                iterable, List.of(3).iterator(), Collections.enumeration(List.of(4)), Set.of("z"))))
                .isEqualTo("[[1,2],[\"a\"],[1,2],[\"a\",\"b\"],[null],[1,2],[3],[4],[\"z\"]]");
    }

    @Test
    void writes_nested_maps_and_collections_and_map_keys_as_strings() throws IOException {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(1, List.of(Map.of("a", List.of())));
        map.put(Thread.State.NEW, new LinkedHashMap<>());

        assertThat(json(map)).isEqualTo("{\"1\":[{\"a\":[]}],\"NEW\":{}}");
        assertThat(json(new LinkedHashMap<>())).isEqualTo("{}");
        assertThat(json(List.of())).isEqualTo("[]");
    }

    @Test
    void fails_for_a_map_with_a_null_key() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, "x");

        assertThatThrownBy(() -> json(map)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maps with null keys can't be converted to JSON");
    }

    @Test
    void writes_a_nested_bean_with_the_object_mapper() throws IOException {
        assertThat(jsonOf(new Bean())).isEqualTo("[{\"age\":3,\"color\":\"RED\",\"name\":\"n\",\"nested\":null}]");
    }

    @Test
    void writes_a_record_with_the_object_mapper() throws IOException {
        assertThat(jsonOf(new Point("x", 1))).isEqualTo("[{\"a\":\"x\",\"b\":1}]");
    }

    @Test
    void writes_uris_optionals_and_java_time_values_with_the_object_mapper() throws IOException {
        assertThat(jsonOf(URI.create("http://x/y"))).isEqualTo("[\"http://x/y\"]");
        assertThat(json(Arrays.asList(Optional.of("x"), Optional.empty()))).isEqualTo("[\"x\",null]");
        assertThat(jsonOf(java.time.Instant.EPOCH)).isEqualTo("[\"1970-01-01T00:00:00Z\"]");
    }

    @Test
    void writes_other_values_with_the_object_mapper_of_the_config() throws IOException {
        registry.setObjectMapperConfig(new ObjectMapperConfig().defaultObjectMapper(new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                return "{\"mapped\":\"" + context.getObjectToSerialize().getClass().getSimpleName() + "\"}";
            }
        }));

        assertThat(json(Arrays.asList("s", new Bean()))).isEqualTo("[\"s\",{\"mapped\":\"Bean\"}]");
    }

    public enum Color {
        RED
    }

    public enum Unescaped {
        Å
    }

    public static class Bean {
        public String getName() {
            return "n";
        }

        public int getAge() {
            return 3;
        }

        public Color getColor() {
            return Color.RED;
        }

        public Object getNested() {
            return null;
        }
    }

    public record Point(String a, int b) {
    }
}

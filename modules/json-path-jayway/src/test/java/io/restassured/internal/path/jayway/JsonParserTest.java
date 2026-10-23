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

package io.restassured.internal.path.jayway;

import com.jayway.jsonpath.InvalidJsonException;
import io.restassured.common.config.NumberReturnType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.common.config.NumberReturnType.BIG_DECIMAL;
import static io.restassured.common.config.NumberReturnType.BIG_INTEGER;
import static io.restassured.common.config.NumberReturnType.DOUBLE;
import static io.restassured.common.config.NumberReturnType.FLOAT_AND_DOUBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonParserTest {

    private static Object parse(String json) {
        return parse(json, FLOAT_AND_DOUBLE);
    }

    private static Object parse(String json, NumberReturnType numberReturnType) {
        return JsonParser.parse(json, numberReturnType, 1000);
    }

    @Test
    void reads_objects_into_linked_hash_maps_and_arrays_into_array_lists() {
        Object document = parse("{\"b\":[1,\"x\",true,false,null,{}],\"a\":{\"c\":[]}}");

        assertThat(document).isExactlyInstanceOf(LinkedHashMap.class);
        Map<?, ?> map = (Map<?, ?>) document;
        assertThat(new ArrayList<>(map.keySet())).isEqualTo(Arrays.asList("b", "a"));
        assertThat(map.get("b")).isExactlyInstanceOf(ArrayList.class).isEqualTo(Arrays.asList(1, "x", true, false, null, Map.of()));
        assertThat(map.get("a")).isEqualTo(Map.of("c", new ArrayList<>()));
    }

    @Test
    void the_last_of_duplicate_keys_wins() {
        assertThat(parse("{\"a\":1,\"a\":2}")).isEqualTo(Map.of("a", 2));
    }

    @Test
    void reads_a_scalar_document() {
        assertThat(parse(" \"x\" ")).isEqualTo("x");
        assertThat(parse("1")).isEqualTo(1);
        assertThat(parse("null")).isNull();
    }

    @Test
    void integers_are_integers_longs_or_big_integers_whichever_fits() {
        assertThat(parse("[2147483647,-2147483648,2147483648,9223372036854775807,9223372036854775808]")).isEqualTo(Arrays.asList(
                Integer.MAX_VALUE, Integer.MIN_VALUE, 2147483648L, Long.MAX_VALUE, new BigInteger("9223372036854775808")));
    }

    @Test
    void big_integer_reads_every_integer_as_big_integer() {
        assertThat(parse("[1,2147483648,1.5]", BIG_INTEGER)).isEqualTo(Arrays.asList(BigInteger.ONE, new BigInteger("2147483648"), new BigDecimal("1.5")));
    }

    @Test
    void decimals_follow_the_number_return_type() {
        assertThat(parse("[1.50,1e2,3.5E+39]", FLOAT_AND_DOUBLE)).isEqualTo(Arrays.asList(1.5f, 100f, 3.5E39));
        assertThat(parse("[1.50,1e2]", DOUBLE)).isEqualTo(Arrays.asList(1.5d, 100d));
        assertThat(parse("[1.50,-1E-50,12.300]", BIG_DECIMAL)).isEqualTo(Arrays.asList(new BigDecimal("1.50"), new BigDecimal("-1E-50"), new BigDecimal("12.300")));
    }

    @Test
    void big_decimal_keeps_the_scale() {
        assertThat(((BigDecimal) parse("1.50", BIG_DECIMAL)).scale()).isEqualTo(2);
    }

    @Test
    void numbers_longer_than_the_limit_fail() {
        assertThat(JsonParser.parse("12345", FLOAT_AND_DOUBLE, 5)).isEqualTo(12345);
        assertThatThrownBy(() -> JsonParser.parse("[123456]", FLOAT_AND_DOUBLE, 5))
                .isExactlyInstanceOf(InvalidJsonException.class)
                .hasMessage("JSON number length (6 chars) exceeds the maximum allowed length of 5 chars. Raise or disable it via JsonConfig.numberLengthLimit(int).");
        assertThat(JsonParser.parse("1234567890", FLOAT_AND_DOUBLE, -1)).isEqualTo(1234567890);
    }

    @Test
    void reads_escapes() {
        assertThat(parse("\"\\\" \\\\ \\/ \\b \\f \\n \\r \\t \\u00e5 \\ud83d\\ude00\"")).isEqualTo("\" \\ / \b \f \n \r \t å \uD83D\uDE00");
    }

    @Test
    void empty_text_fails_while_whitespace_or_an_empty_stream_is_no_document() {
        assertThatThrownBy(() -> parse("")).isExactlyInstanceOf(InvalidJsonException.class)
                .hasMessage("The JSON input text should neither be null nor empty.");
        assertThat(parse(" \n")).isNull();
        assertThat(JsonParser.parse(new StringReader(""), FLOAT_AND_DOUBLE, 1000)).isNull();
    }

    @Test
    void reads_from_a_reader() {
        assertThat(JsonParser.parse(new StringReader("{\"a\":1.50}"), BIG_DECIMAL, 1000)).isEqualTo(Map.of("a", new BigDecimal("1.50")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{a:1}", "{'a':1}", "[1,]", "{\"a\":1,}", "[01]", "[1.]", "[.5]", "[+1]", "[-]", "[1e]", "tru", "nul",
            "\"a", "[1", "{\"a\"", "{\"a\":", "\"\t\"", "\"\\x\"", "\"\\u12\"", "{} x", "[1] [2]", "NaN", "// c\n1"})
    void invalid_json_fails(String json) {
        assertThatThrownBy(() -> parse(json)).isExactlyInstanceOf(InvalidJsonException.class);
    }

    @Test
    void failures_name_the_line_and_column() {
        assertThatThrownBy(() -> parse("{\n  \"a\": x\n}"))
                .hasMessage("Expected a value on line 2, column 8, but found 'x'.");
        assertThatThrownBy(() -> parse("[1"))
                .hasMessage("Expected ']' on line 1, column 3, but found the end of the document.");
    }
}

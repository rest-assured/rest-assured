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

package io.restassured.itest.java;

import com.jayway.jsonpath.InvalidJsonException;
import io.restassured.common.config.NumberReturnType;
import io.restassured.internal.path.jayway.JsonParser;
import io.restassured.internal.path.json.ConfigurableJsonSlurper;
import io.restassured.path.json.config.JsonPathConfig;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Compares the JSON parser of json-path-jayway with the one of json-path-gpath, so that a body gives the same values,
 * with the same number types, whichever engine reads it. The design of phase 5 lists the known differences too.
 */
public class JsonParserParityITest {
    private static final List<String> VALID = List.of(
            "{\"a\":1,\"b\":[true,false,null],\"c\":{\"d\":\"e\"}}",
            "[2147483647,-2147483648,2147483648,9223372036854775807,9223372036854775808,12345678901234567890]",
            "[0,-0,1.5,1.50,-1.5e-3,1E+2,1e5,3.4028235E38,3.4028236E38,1E-50,12.300]",
            "\"\\\" \\\\ \\/ \\b \\f \\n \\r \\t \\u00e5 \\ud83d\\ude00\"",
            "{\"a\":1,\"a\":2}",
            "  [ ]  ", "{}", "1", "\"x\"", "null", "true", "", " \n ");

    // GPath reads these, json-path-jayway fails on them, since it follows RFC 8259
    private static final List<String> ONLY_GPATH_READS = List.of(
            "[1,]", "{\"a\":1,}",                  // a trailing comma
            "[01]",                                // a leading zero
            "\"\t\"",                              // a control character in a string
            "{\"a\":1}garbage", "{} x", "[1] [2]", // anything after the document
            "\"a", "\"\\x\"", "\"\\u12\"");         // a broken string as the document, which GPath reads as null

    @Test
    public void valid_json_gives_the_same_values_and_number_types() {
        for (String json : VALID) {
            for (NumberReturnType type : NumberReturnType.values()) {
                assertThat(jayway(json, type)).as(type + " " + json).isEqualTo(gpath(json, type));
            }
        }
    }

    @Test
    public void known_differences() {
        for (String json : ONLY_GPATH_READS) {
            gpath(json, NumberReturnType.FLOAT_AND_DOUBLE);
            assertThatThrownBy(() -> jayway(json, NumberReturnType.FLOAT_AND_DOUBLE)).as(json).isInstanceOf(InvalidJsonException.class);
        }
    }

    private static Object gpath(String json, NumberReturnType type) {
        return new ConfigurableJsonSlurper(JsonPathConfig.NumberReturnType.valueOf(type.name()), 1000).parse(new StringReader(json));
    }

    private static Object jayway(String json, NumberReturnType type) {
        return JsonParser.parse(new StringReader(json), type, 1000);
    }
}

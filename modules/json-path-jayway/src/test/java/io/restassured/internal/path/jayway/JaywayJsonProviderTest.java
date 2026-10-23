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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import io.restassured.common.config.NumberReturnType;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.common.spi.path.PathSettings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.internal.common.spi.path.PathResult.Kind.INVALID_PATH;
import static io.restassured.internal.common.spi.path.PathResult.Kind.LIST_ADDRESSED_AS_OBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JaywayJsonProviderTest {
    private static final String STORE = "{\"store\":{\"book\":[{\"title\":\"t0\",\"price\":0.5,\"tag\":\"a\"},{\"title\":\"t1\",\"price\":1.5,\"tag\":\"b\"}],"
            + "\"nil\":null,\"num\":1,\"str\":\"s\",\"my-key\":\"dash\",\"$schema\":\"sch\",\"a.b\":\"dotted\"},\"$schema\":\"root\"}";

    private final JaywayJsonProvider provider = new JaywayJsonProvider();
    private final Object store = parse(STORE, NumberReturnType.FLOAT_AND_DOUBLE);

    private Object parse(String json, NumberReturnType numberReturnType) {
        return provider.parse(PathInput.ofText(json), DocumentType.JSON, settings(numberReturnType));
    }

    private static PathSettings settings(NumberReturnType numberReturnType) {
        return new PathSettings(numberReturnType, 1000, false, true, false, Map.of(), Map.of(), Map.of());
    }

    private PathResult evaluate(String path) {
        return provider.evaluate(store, DocumentType.JSON, path);
    }

    private Object value(String path) {
        PathResult result = evaluate(path);
        assertThat(result.kind()).as(path).isEqualTo(PathResult.Kind.VALUE);
        return result.value();
    }

    @Test
    void describes_itself() {
        assertThat(provider.artifactId()).isEqualTo("json-path-jayway");
        assertThat(provider.engineName()).isEqualTo("JAYWAY");
        assertThat(provider.documentTypes()).isEqualTo(Set.of(DocumentType.JSON));
        assertThat(provider.description(DocumentType.JSON)).isEqualTo("JSONPath (Jayway)");
    }

    @Test
    void evaluates_jsonpath() {
        assertThat(value("$.store.book[0].title")).isEqualTo("t0");
        assertThat(value("store.book[1].title")).isEqualTo("t1");
        assertThat(value("store.book[-1].price")).isEqualTo(1.5f);
        assertThat(value("$.store.book[*].title")).isEqualTo(List.of("t0", "t1"));
        assertThat(value("$..title")).isEqualTo(List.of("t0", "t1"));
        assertThat(value("store.book[?(@.price > 1)].title")).isEqualTo(List.of("t1"));
        assertThat(value("store.book.length()")).isEqualTo(2);
        assertThat(value("store.book.size()")).isEqualTo(2);
        assertThat(value("store.my-key")).isEqualTo("dash");
        assertThat(value("store['a.b']")).isEqualTo("dotted");
        assertThat(value("store.nil")).isNull();
    }

    @Test
    void filters_with_literals_work_since_json_smart_is_on_the_classpath() {
        assertThat(value("store.book[?(@.tag in ['a','x'])].title")).isEqualTo(List.of("t0"));
        assertThat(value("store.book[?(@.tag nin ['a'])].title")).isEqualTo(List.of("t1"));
    }

    @Test
    void results_of_indefinite_paths_are_array_lists_and_empty_when_nothing_matches() {
        assertThat(value("$.store.book[*].title")).isExactlyInstanceOf(ArrayList.class);
        assertThat(value("store.book[?(@.price > 10)].title")).isEqualTo(List.of());
        assertThat(value("store.book[*].missing")).isEqualTo(List.of());
    }

    @Test
    void the_root() {
        assertThat(value("$")).isSameAs(store);
        assertThat(value("")).isSameAs(store);
    }

    @Test
    void a_leading_key_that_starts_with_a_dollar_is_a_key() {
        assertThat(value("$schema")).isEqualTo("root");
        assertThat(value("store.$schema")).isEqualTo("sch");
        assertThat(value("$['$schema']")).isEqualTo("root");
    }

    @Test
    void a_leading_index_on_an_array() {
        Object array = parse("[{\"a\":1},{\"a\":2}]", NumberReturnType.FLOAT_AND_DOUBLE);

        assertThat(provider.evaluate(array, DocumentType.JSON, "[1].a").value()).isEqualTo(2);
        assertThat(provider.evaluate(array, DocumentType.JSON, "$[0].a").value()).isEqualTo(1);
    }

    @Test
    void missing_properties_parents_and_indexes_are_missing() {
        for (String path : List.of("store.missing", "$.store.missing.deeper", "store.book[5]", "store.book[5].title", "store.a.b")) {
            assertThat(evaluate(path)).as(path).isEqualTo(PathResult.missing());
        }
    }

    @Test
    void a_property_of_null_a_string_or_a_number_is_missing_as_in_gpath() {
        for (String path : List.of("store.nil.x", "store.nil.x.y", "store.num.x", "store.str.x", "store.book[0].title.x")) {
            assertThat(evaluate(path)).as(path).isEqualTo(PathResult.missing());
        }
    }

    @Test
    void a_property_of_a_list_fails_with_how_to_address_its_elements() {
        assertThat(evaluate("store.book.title")).isEqualTo(PathResult.listAddressedAsObject(
                "JSONPath (Jayway) can't evaluate store.book.title: store.book is a list, so use store.book[*].title."));
        assertThat(evaluate("$.store.book.title.size()").message()).isEqualTo(
                "JSONPath (Jayway) can't evaluate $.store.book.title.size(): store.book is a list, so use store.book[*].title.");
        assertThat(provider.evaluate(parse("[{\"a\":1}]", NumberReturnType.FLOAT_AND_DOUBLE), DocumentType.JSON, "a")).isEqualTo(
                PathResult.listAddressedAsObject("JSONPath (Jayway) can't evaluate a: $ is a list, so use $[*].a."));
    }

    @Test
    void the_list_is_written_with_brackets_for_keys_that_arent_identifiers() {
        Object document = parse("{\"a b\":{\"c\":[{\"d\":1}]}}", NumberReturnType.FLOAT_AND_DOUBLE);

        assertThat(provider.evaluate(document, DocumentType.JSON, "['a b'].c.d").message())
                .isEqualTo("JSONPath (Jayway) can't evaluate ['a b'].c.d: ['a b'].c is a list, so use ['a b'].c[*].d.");
    }

    @Test
    void gpath_closures_are_invalid() {
        PathResult result = evaluate("store.book.findAll { it.price > 1 }.title");

        assertThat(result.kind()).isEqualTo(INVALID_PATH);
        assertThat(result.message()).startsWith("JSONPath (Jayway) can't evaluate store.book.findAll { it.price > 1 }.title: ");
    }

    @Test
    void a_quoted_key_after_a_dot_is_gpath_and_invalid() {
        assertThat(evaluate("store.'my-key'")).isEqualTo(PathResult.invalidPath(
                "JSONPath (Jayway) can't evaluate store.'my-key': a quoted key after a dot is GPath syntax. Use store['my-key'] instead."));
        assertThat(evaluate("'store'.\"my-key\"")).isEqualTo(PathResult.invalidPath(
                "JSONPath (Jayway) can't evaluate 'store'.\"my-key\": a quoted key after a dot is GPath syntax. Use ['store'][\"my-key\"] instead."));
    }

    @Test
    void quoted_keys_in_brackets_and_filters_are_jsonpath() {
        assertThat(value("$['store']['my-key']")).isEqualTo("dash");
        assertThat(value("store[\"my-key\"]")).isEqualTo("dash");
        assertThat(value("['store'].my-key")).isEqualTo("dash");
        assertThat(value("store.book[?(@.title == 'Mr.')].title")).isEqualTo(List.of());
        assertThat(value("store.book[?(@.tag in ['.a','b'])].title")).isEqualTo(List.of("t1"));
    }

    @Test
    void invalid_jsonpath_is_invalid() {
        PathResult result = evaluate("store.book[?(@.price > 1]");

        assertThat(result.kind()).isEqualTo(INVALID_PATH);
        assertThat(result.message()).startsWith("JSONPath (Jayway) can't evaluate store.book[?(@.price > 1]: ");
    }

    @Test
    void other_failures_of_jayway_are_illegal_arguments() {
        assertThatThrownBy(() -> evaluate("store.book.max()"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("JSONPath (Jayway) can't evaluate store.book.max(): ");
    }

    @Test
    void numbers_follow_the_settings() {
        Object document = parse("{\"price\":1.50,\"count\":1}", NumberReturnType.BIG_DECIMAL);

        assertThat(provider.evaluate(document, DocumentType.JSON, "$.price").value()).isEqualTo(new BigDecimal("1.50"));
        assertThat(provider.evaluate(document, DocumentType.JSON, "$.count").value()).isEqualTo(1);
    }

    @Test
    void parses_a_stream_in_its_charset() {
        PathInput input = PathInput.ofStream(new ByteArrayInputStream("{\"a\":\"å\"}".getBytes(StandardCharsets.ISO_8859_1)), "ISO-8859-1");

        assertThat(provider.parse(input, DocumentType.JSON, settings(NumberReturnType.FLOAT_AND_DOUBLE))).isEqualTo(Map.of("a", "å"));
    }

    @Test
    void parse_failure_is_an_invalid_json_exception() {
        Exception cause = new InvalidJsonException("x");

        assertThat(provider.parseFailure(DocumentType.JSON, cause)).isExactlyInstanceOf(InvalidJsonException.class)
                .hasMessage("Failed to parse the JSON document").hasCause(cause);
    }

    @Test
    void joins_the_root_path_with_a_dot_unless_the_key_starts_with_a_dot_or_a_bracket() {
        assertThat(provider.joinRootPath(null, "a")).isEqualTo("a");
        assertThat(provider.joinRootPath("", "a")).isEqualTo("a");
        assertThat(provider.joinRootPath("store", "")).isEqualTo("store");
        assertThat(provider.joinRootPath("store", "book")).isEqualTo("store.book");
        assertThat(provider.joinRootPath("$.store", "book[0]")).isEqualTo("$.store.book[0]");
        assertThat(provider.joinRootPath("store", "[0]")).isEqualTo("store[0]");
        assertThat(provider.joinRootPath("store", "..title")).isEqualTo("store..title");
        assertThat(provider.joinRootPath("store.", "title")).isEqualTo("store.title");
        assertThat(provider.joinRootPath("store.", ".title")).isEqualTo("store.title");
        assertThat(provider.joinRootPath("store", "*.title")).isEqualTo("store.*.title");
    }

    @Test
    void its_configuration_doesnt_depend_on_jayways_defaults() {
        Configuration.Defaults before = defaults();
        try {
            Configuration.setDefaults(new Configuration.Defaults() {
                @Override
                public JsonProvider jsonProvider() {
                    throw new AssertionError("Jayway's defaults were used");
                }

                @Override
                public java.util.Set<com.jayway.jsonpath.Option> options() {
                    return java.util.EnumSet.of(com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS);
                }

                @Override
                public MappingProvider mappingProvider() {
                    throw new AssertionError("Jayway's defaults were used");
                }
            });

            assertThat(value("store.book[0].title")).isEqualTo("t0");
            assertThat(evaluate("store.book.title").kind()).isEqualTo(LIST_ADDRESSED_AS_OBJECT);
        } finally {
            Configuration.setDefaults(before);
        }
    }

    private static Configuration.Defaults defaults() {
        Configuration configuration = Configuration.defaultConfiguration();
        return new Configuration.Defaults() {
            @Override
            public JsonProvider jsonProvider() {
                return configuration.jsonProvider();
            }

            @Override
            public java.util.Set<com.jayway.jsonpath.Option> options() {
                return configuration.getOptions();
            }

            @Override
            public MappingProvider mappingProvider() {
                return configuration.mappingProvider();
            }
        };
    }
}

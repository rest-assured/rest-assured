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

import io.restassured.RestAssured;
import io.restassured.common.config.NumberReturnType;
import io.restassured.config.JsonPathEngine;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.path.jayway.JaywayPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.matcher.RestAssuredMatchers.equalToPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/**
 * JSON paths evaluated by json-path-jayway. GPath is on this module's classpath too, so each test sets the engine.
 */
public class JaywayJsonITest extends WithJetty {

    @BeforeEach
    public void useJayway() {
        RestAssured.config = config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY));
    }

    @Test
    public void reads_properties_indexes_and_wildcards() {
        expect().
                body("$.lotto.lottoId", equalTo(5)).
                body("lotto.winners[0].winnerId", equalTo(23)).
                body("lotto.winners[*].winnerId", contains(23, 54)).
                body("lotto.winning-numbers", hasItems(2, 45, 34)).
                body("$['lotto']['winning-numbers'][0]", equalTo(2)).
        when().
                get("/lotto");
    }

    @Test
    public void reads_filters_deep_scans_and_functions() {
        expect().
                body("store.book[?(@.price < 10)].title", contains("Sayings of the Century", "Moby Dick")).
                body("store.book[?(@.isbn)].author", contains("Herman Melville", "J. R. R. Tolkien")).
                body("store.book[?(@.category in ['reference'])].author", contains("Nigel Rees")).
                body("$..price", hasSize(5)).
                body("$..author", hasItem("Evelyn Waugh")).
                body("store.book.length()", equalTo(4)).
                body("store.book[?(@.price > 100)]", empty()).
                body("store.bicycle.color", equalTo("red")).
        when().
                get("/jsonStore");
    }

    @Test
    public void a_missing_path_is_null() {
        expect().
                body("store.missing", nullValue()).
                body("store.missing.deeper", nullValue()).
                body("store.book[10]", nullValue()).
                body("store.bicycle.color.shade", nullValue()).
        when().
                get("/jsonStore");
    }

    @Test
    public void root_paths_and_arguments() {
        expect().
                rootPath("store").
                body("book[0].author", equalTo("Nigel Rees")).
                body("..title", hasItem("Moby Dick")).
                body("book[%d].author", withArgs(2), equalTo("Herman Melville")).
                appendRootPath("bicycle").
                body("color", equalTo("red")).
                detachRootPath("bicycle").
                body("bicycle.price", equalTo(19.95f)).
        when().
                get("/jsonStore");
    }

    @Test
    public void path_and_extract_path() {
        assertThat(get("/jsonStore").<List<String>>path("store.book[?(@.price > 20)].title")).containsExactly("The Lord of the Rings");
        assertThat(get("/lotto").then().extract().<Integer>path("lotto.winners[1].winnerId")).isEqualTo(54);
    }

    @Test
    public void response_aware_matchers() {
        get("/lotto").then().
                body("lotto.winners[0].winnerId", equalToPath("lotto.winners[0].winnerId")).
                body("lotto.lottoId", response -> equalTo(response.path("$.lotto.lottoId")));
    }

    @Test
    public void big_decimal_keeps_the_scale() {
        given().
                config(config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY).numberReturnType(NumberReturnType.BIG_DECIMAL))).
                contentType("application/json").
                body("{\"price\":12.50,\"tiny\":1E-50}").
        expect().
                body("price", equalTo(new BigDecimal("12.50"))).
                body("tiny", equalTo(new BigDecimal("1E-50"))).
        when().
                post("/reflect");
    }

    @Test
    public void floats_and_doubles_by_default() {
        expect().body("values.pi", equalTo(3.14f)).body("values.answer", equalTo(42)).when().get("/numbers");
    }

    @Test
    public void a_key_that_starts_with_a_dollar() {
        given().
                contentType("application/json").
                body("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"a\":{\"$id\":1}}").
        expect().
                body("$schema", equalTo("https://json-schema.org/draft/2020-12/schema")).
                body("a.$id", equalTo(1)).
        when().
                post("/reflect");
    }

    @Test
    public void a_failed_expectation_names_jsonpath() {
        assertThatThrownBy(() -> expect().body("values.pi", equalTo(3.14)).when().get("/numbers"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\n" +
                        "JSONPath (Jayway) values.pi doesn't match.\n" +
                        "Expected: <3.14>\n" +
                        "  Actual: <3.14F>\n");
    }

    @Test
    public void a_gpath_spread_fails_without_a_gpath_hint_since_gpath_is_on_the_classpath() {
        assertThatThrownBy(() -> expect().body("lotto.winners.winnerId", hasItems(23, 54)).when().get("/lotto"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("JSONPath (Jayway) can't evaluate lotto.winners.winnerId: lotto.winners is a list, so use lotto.winners[*].winnerId.");
    }

    @Test
    public void jayway_path_reads_a_response() {
        assertThat(JaywayPath.from(get("/jsonStore").asString()).<List<String>>read("$.store.book[*].category"))
                .containsExactly("reference", "fiction", "fiction", "fiction");
    }

    @Test
    public void gpath_and_jayway_give_the_same_results_for_paths_that_both_read_alike() {
        List<String> paths = List.of("store.bicycle.color", "store.book[1].price", "store.missing", "store.book[2].isbn", "$.store.bicycle.price");
        for (String path : paths) {
            Object jayway = get("/jsonStore").path(path);
            RestAssured.config = config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.GPATH));
            Object gpath = get("/jsonStore").path(path);
            RestAssured.config = config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY));
            assertThat(jayway).as(path).isEqualTo(gpath);
        }
        assertThat(get("/jsonStore").<List<Object>>path("store.book[*].author"))
                .containsExactlyElementsOf(given().config(config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.GPATH))).get("/jsonStore").path("store.book.author"));
    }
}

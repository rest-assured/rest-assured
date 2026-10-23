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

package io.restassured.path.jayway;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.common.config.NumberReturnType;
import io.restassured.config.JsonPathEngine;
import io.restassured.filter.Filter;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.path.MissingPathEngine;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.matcher.RestAssuredMatchers.equalToPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;

/**
 * REST Assured with json-path-jayway as the only JSON path engine: rest-assured's GPath dependencies are optional, so
 * they aren't on this module's test classpath.
 */
class OnlyJaywayTest {
    private static final String LOTTO = "{\"lotto\":{\"lottoId\":5,\"winning-numbers\":[2,45,34,23,7,5,3],\"winners\":[{\"winnerId\":23,\"numbers\":[2,45,34,23,3,5]},"
            + "{\"winnerId\":54,\"numbers\":[52,3,12,11,18,22]}]},\"price\":12.50}";

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    @Test
    void gpath_isnt_on_the_classpath() {
        assertThatThrownBy(() -> Class.forName("io.restassured.internal.path.json.GPathJsonProvider")).isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void body_expectations_use_jayway() {
        response().then()
                .body("$.lotto.winners[*].winnerId", contains(23, 54))
                .body("lotto.winners[?(@.winnerId > 30)].numbers[0]", contains(52))
                .body("lotto.winning-numbers.length()", equalTo(7))
                .body("$..winnerId", hasItems(23, 54))
                .body("lotto.missing", nullValue())
                .body("lotto.winners[0].winnerId", equalToPath("lotto.winners[0].winnerId"));
    }

    @Test
    void path_and_extract_path_use_jayway() {
        assertThat(response().<List<Integer>>path("lotto.winners[*].winnerId")).containsExactly(23, 54);
        assertThat(response().then().extract().<Integer>path("$.lotto.winners[%s].winnerId", "1")).isEqualTo(54);
    }

    @Test
    void root_paths_and_arguments() {
        response().then().rootPath("lotto").body("winners[0].winnerId", equalTo(23))
                .appendRootPath("winners[%d]").body("winnerId", withArgs(1), equalTo(54))
                .rootPath("lotto").body("..winnerId", contains(23, 54));
    }

    @Test
    void numbers_follow_the_json_config() {
        response().then().body("price", equalTo(12.5f));
        given().filter(respond()).config(config().jsonConfig(jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL)))
                .expect().body("price", equalTo(new BigDecimal("12.50"))).when().get("/");
    }

    @Test
    void a_failed_expectation_names_jsonpath() {
        assertThatThrownBy(() -> response().then().body("lotto.lottoId", equalTo(6)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("JSONPath (Jayway) lotto.lottoId doesn't match.");
    }

    @Test
    void a_gpath_spread_fails_with_how_to_write_it_in_jsonpath_and_how_to_add_gpath() {
        assertThatThrownBy(() -> response().then().body("lotto.winners.winnerId", contains(23, 54)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("\\QJSONPath (Jayway) can't evaluate lotto.winners.winnerId: lotto.winners is a list, so use lotto.winners[*].winnerId. "
                        + "If this is a GPath path from REST Assured 6.x, add io.rest-assured:json-path-gpath:\\E7\\.\\d+\\.\\d+(-SNAPSHOT)?\\Q to your test dependencies.\\E");
    }

    @Test
    void a_gpath_closure_fails_with_how_to_add_gpath() {
        assertThatThrownBy(() -> response().path("lotto.winners.find { it.winnerId == 23 }.numbers"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("JSONPath (Jayway) can't evaluate lotto.winners.find { it.winnerId == 23 }.numbers: ")
                .hasMessageContaining("If this is a GPath path from REST Assured 6.x, add io.rest-assured:json-path-gpath:");
    }

    @Test
    void a_gpath_quoted_key_fails_but_brackets_work() {
        assertThatThrownBy(() -> response().then().body("lotto.'winning-numbers'[0]", equalTo(2)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("JSONPath (Jayway) can't evaluate lotto.'winning-numbers'[0]: a quoted key after a dot is GPath syntax. "
                        + "Use lotto['winning-numbers'][0] instead.");
        response().then().body("lotto['winning-numbers'][0]", equalTo(2)).body("$['lotto']['winning-numbers'][1]", equalTo(45));
    }

    @Test
    void gpath_as_engine_fails_without_gpath() {
        assertThatThrownBy(() -> given().filter(respond()).config(config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.GPATH)))
                .expect().body("lotto.lottoId", equalTo(5)).when().get("/"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("JsonConfig.pathEngine is GPATH but io.rest-assured:json-path-gpath isn't on the classpath.");
    }

    @Test
    void jayway_as_engine_works_globally_and_for_a_request() {
        given().filter(respond()).config(config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY)))
                .expect().body("lotto.winners[1].winnerId", equalTo(54)).when().get("/");
        RestAssured.config = config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY));
        given().filter(respond()).expect().body("lotto.winners[1].winnerId", equalTo(54)).when().get("/");
    }

    // Code that calls jsonPath() doesn't compile without json-path-gpath, so this checks the message it throws
    @Test
    void json_path_points_to_jayway_path() {
        assertThat(MissingPathEngine.forMethod("jsonPath()", DocumentType.JSON))
                .contains("With io.rest-assured:json-path-jayway, use JaywayPath.from(response.asString()) instead of jsonPath().");
        assertThat(JaywayPath.from(response().asString()).<Integer>read("lotto.lottoId")).isEqualTo(5);
    }

    private static Response response() {
        return new ResponseBuilder().setStatusCode(200).setContentType("application/json").setBody(LOTTO).build();
    }

    // Answers the request without sending it. The response has the default config, so expectations that depend on the
    // request's config are given with expect()
    private static Filter respond() {
        return (requestSpec, responseSpec, ctx) -> response();
    }
}

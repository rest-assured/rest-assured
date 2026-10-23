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
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.JsonPathEngine;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Which JSON path engine REST Assured uses with both json-path-gpath and json-path-jayway on the classpath: GPath, unless
 * an engine is configured globally, on the request or on the response specification, in that order of precedence.
 * A GPath spread such as {@code lotto.winners.winnerId} only works with GPath, and a JSONPath wildcard such as
 * {@code lotto.winners[*].winnerId} only with Jayway.
 */
public class PathEngineSelectionITest extends WithJetty {
    private static final String GPATH_SPREAD = "lotto.winners.winnerId";
    private static final String JSONPATH_WILDCARD = "lotto.winners[*].winnerId";

    private static RestAssuredConfig engine(JsonPathEngine engine) {
        return config().jsonConfig(jsonConfig().pathEngine(engine));
    }

    private static ResponseSpecification expectWinners(JsonPathEngine engine, String path) {
        return new ResponseSpecBuilder().setConfig(engine(engine)).expectBody(path, contains(23, 54)).build();
    }

    @Test
    public void gpath_is_used_by_default() {
        expect().body(GPATH_SPREAD, contains(23, 54)).when().get("/lotto");
        assertThat(get("/lotto").<List<Integer>>path(GPATH_SPREAD)).containsExactly(23, 54);
    }

    @Test
    public void jayway_can_be_set_globally() {
        RestAssured.config = engine(JsonPathEngine.JAYWAY);

        expect().body(JSONPATH_WILDCARD, contains(23, 54)).when().get("/lotto");
        assertThat(get("/lotto").<List<Integer>>path(JSONPATH_WILDCARD)).containsExactly(23, 54);
    }

    @Test
    public void jayway_can_be_set_on_a_request() {
        given().config(engine(JsonPathEngine.JAYWAY)).expect().body(JSONPATH_WILDCARD, contains(23, 54)).when().get("/lotto");
        given().config(engine(JsonPathEngine.JAYWAY)).when().get("/lotto").then().body(JSONPATH_WILDCARD, contains(23, 54));
        assertThat(given().config(engine(JsonPathEngine.JAYWAY)).get("/lotto").<List<Integer>>path(JSONPATH_WILDCARD)).containsExactly(23, 54);
        given().spec(new RequestSpecBuilder().setConfig(engine(JsonPathEngine.JAYWAY)).build()).when().get("/lotto")
                .then().body(JSONPATH_WILDCARD, contains(23, 54));
    }

    @Test
    public void jayway_can_be_set_on_a_response_specification() {
        expect().spec(expectWinners(JsonPathEngine.JAYWAY, JSONPATH_WILDCARD)).when().get("/lotto");
        get("/lotto").then().spec(expectWinners(JsonPathEngine.JAYWAY, JSONPATH_WILDCARD));
        given(new RequestSpecBuilder().build(), expectWinners(JsonPathEngine.JAYWAY, JSONPATH_WILDCARD)).get("/lotto");
    }

    @Test
    public void the_response_specification_outranks_the_request_which_outranks_the_global_config() {
        RestAssured.config = engine(JsonPathEngine.JAYWAY);

        given().config(engine(JsonPathEngine.GPATH)).expect().body(GPATH_SPREAD, contains(23, 54)).when().get("/lotto");
        given().config(engine(JsonPathEngine.GPATH)).expect().spec(expectWinners(JsonPathEngine.JAYWAY, JSONPATH_WILDCARD)).when().get("/lotto");
        given().config(engine(JsonPathEngine.JAYWAY)).when().get("/lotto").then().spec(expectWinners(JsonPathEngine.GPATH, GPATH_SPREAD));
        given(new RequestSpecBuilder().setConfig(engine(JsonPathEngine.JAYWAY)).build(), expectWinners(JsonPathEngine.GPATH, GPATH_SPREAD)).get("/lotto");
    }

    @Test
    public void engines_can_be_mixed_in_one_test_class() {
        expect().body(GPATH_SPREAD, contains(23, 54)).when().get("/lotto");
        given().config(engine(JsonPathEngine.JAYWAY)).expect().body(JSONPATH_WILDCARD, contains(23, 54)).when().get("/lotto");
        expect().body(GPATH_SPREAD, contains(23, 54)).when().get("/lotto");
    }
}

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


package io.restassured.module.webtestclient;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.JsonPathEngine;
import io.restassured.config.RestAssuredConfig;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Which JSON path engine WebTestClient uses with both json-path-gpath and json-path-jayway on the classpath: GPath, unless
 * an engine is configured statically, on the request or on the response specification. A GPath spread such as
 * {@code lotto.winners.winnerId} only works with GPath, and a JSONPath wildcard such as
 * {@code lotto.winners[*].winnerId} only with Jayway.
 */
public class WebTestClientPathEngineTest {
    private static final String GPATH_SPREAD = "lotto.winners.winnerId";
    private static final String JSONPATH_WILDCARD = "lotto.winners[*].winnerId";

    @RestController
    static class LottoController {
        @GetMapping(value = "/lotto", produces = "application/json")
        public Map<String, Object> lotto() {
            return Map.of("lotto", Map.of("winners", List.of(Map.of("winnerId", 23), Map.of("winnerId", 54))));
        }
    }

    private static RestAssuredWebTestClientConfig engine(JsonPathEngine engine) {
        return RestAssuredWebTestClientConfig.config().jsonConfig(jsonConfig().pathEngine(engine));
    }

    private static ResponseSpecification expectWinners(JsonPathEngine engine, String path) {
        return new ResponseSpecBuilder().setConfig(RestAssuredConfig.config().jsonConfig(jsonConfig().pathEngine(engine)))
                .expectBody(path, contains(23, 54)).build();
    }

    @AfterEach
    void reset() {
        RestAssuredWebTestClient.reset();
    }

    @Test
    void gpath_is_used_by_default() {
        given().standaloneSetup(new LottoController()).when().get("/lotto").then().body(GPATH_SPREAD, contains(23, 54));
        List<Integer> ids = given().standaloneSetup(new LottoController()).when().get("/lotto").path(GPATH_SPREAD);
        assertThat(ids).containsExactly(23, 54);
    }

    @Test
    void jayway_can_be_set_statically() {
        RestAssuredWebTestClient.config = engine(JsonPathEngine.JAYWAY);

        given().standaloneSetup(new LottoController()).when().get("/lotto").then().body(JSONPATH_WILDCARD, contains(23, 54));
        List<Integer> ids = given().standaloneSetup(new LottoController()).when().get("/lotto").then().extract().path(JSONPATH_WILDCARD);
        assertThat(ids).containsExactly(23, 54);
    }

    @Test
    void jayway_can_be_set_on_a_request() {
        given().config(engine(JsonPathEngine.JAYWAY)).standaloneSetup(new LottoController()).when().get("/lotto")
                .then().body(JSONPATH_WILDCARD, contains(23, 54));
        List<Integer> ids = given().config(engine(JsonPathEngine.JAYWAY)).standaloneSetup(new LottoController()).when().get("/lotto").path(JSONPATH_WILDCARD);
        assertThat(ids).containsExactly(23, 54);
    }

    @Test
    void jayway_can_be_set_on_a_response_specification() {
        given().standaloneSetup(new LottoController()).when().get("/lotto").then().spec(expectWinners(JsonPathEngine.JAYWAY, JSONPATH_WILDCARD));

        RestAssuredWebTestClient.responseSpecification = expectWinners(JsonPathEngine.JAYWAY, JSONPATH_WILDCARD);
        given().standaloneSetup(new LottoController()).when().get("/lotto");
    }

    @Test
    void the_response_specification_outranks_the_request() {
        given().config(engine(JsonPathEngine.GPATH)).standaloneSetup(new LottoController()).when().get("/lotto")
                .then().spec(expectWinners(JsonPathEngine.JAYWAY, JSONPATH_WILDCARD));
        given().config(engine(JsonPathEngine.JAYWAY)).standaloneSetup(new LottoController()).when().get("/lotto")
                .then().spec(expectWinners(JsonPathEngine.GPATH, GPATH_SPREAD));
    }
}

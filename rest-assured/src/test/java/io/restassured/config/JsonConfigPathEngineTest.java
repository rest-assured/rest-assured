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

package io.restassured.config;

import io.restassured.builder.ResponseBuilder;
import io.restassured.common.config.NumberReturnType;
import io.restassured.filter.Filter;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class JsonConfigPathEngineTest {

    @Test
    void no_engine_is_configured_by_default() {
        assertThat(JsonConfig.jsonConfig().pathEngine()).isNull();
        assertThat(JsonConfig.jsonConfig().isUserConfigured()).isFalse();
    }

    @Test
    void configuring_the_engine_keeps_the_other_settings() {
        JsonConfig config = JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL).numberLengthLimit(5).pathEngine(JsonPathEngine.GPATH);

        assertThat(config.pathEngine()).isEqualTo(JsonPathEngine.GPATH);
        assertThat(config.numberReturnType().name()).isEqualTo("BIG_DECIMAL");
        assertThat(config.numberLengthLimit()).isEqualTo(5);
        assertThat(config.isUserConfigured()).isTrue();
    }

    @Test
    void the_other_settings_keep_the_engine() {
        JsonConfig config = JsonConfig.jsonConfig().pathEngine(JsonPathEngine.GPATH);

        assertThat(config.numberReturnType(NumberReturnType.BIG_DECIMAL).pathEngine()).isEqualTo(JsonPathEngine.GPATH);
        assertThat(config.numberLengthLimit(5).pathEngine()).isEqualTo(JsonPathEngine.GPATH);
        assertThat(config.pathEngine(null).pathEngine()).isNull();
    }

    @Test
    void the_configured_engine_evaluates_body_paths_and_path() {
        Filter json = (requestSpec, responseSpec, ctx) -> new ResponseBuilder().setStatusCode(200).setContentType("application/json").setBody("{\"a\":{\"b\":1}}").build();
        RestAssuredConfig config = RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().pathEngine(JsonPathEngine.GPATH));

        int b = given().config(config).filter(json).expect().rootPath("a").body("b", equalTo(1)).when().get("http://localhost:8080/").path("a.b");

        assertThat(b).isEqualTo(1);
    }
}

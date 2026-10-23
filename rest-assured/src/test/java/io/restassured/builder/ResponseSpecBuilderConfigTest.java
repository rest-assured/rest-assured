/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.builder;

import io.restassured.RestAssured;
import io.restassured.config.JsonPathEngine;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.response.Response;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.XmlConfig.xmlConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * The config of a response specification from {@link ResponseSpecBuilder#setConfig(RestAssuredConfig)} outranks the
 * request's config and {@code RestAssured.config}, per config type. Core has only GPath, so a specification that picks
 * Jayway is seen by the message that json-path-jayway isn't on the classpath.
 */
class ResponseSpecBuilderConfigTest {
    private static final String JAYWAY_IS_MISSING = "JsonConfig.pathEngine is JAYWAY but io.rest-assured:json-path-jayway isn't on the classpath.";

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    @Test
    void then_spec_uses_the_config_of_the_specification() {
        assertThatThrownBy(() -> response().then().spec(expectB(JsonPathEngine.JAYWAY)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(JAYWAY_IS_MISSING);
    }

    @Test
    void expect_spec_uses_the_config_of_the_specification() {
        assertThatThrownBy(() -> given().filter(respond()).expect().spec(expectB(JsonPathEngine.JAYWAY)).when().get("/"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(JAYWAY_IS_MISSING);
    }

    @Test
    void given_request_and_response_specification_uses_the_config_of_the_response_specification() {
        assertThatThrownBy(() -> given(new RequestSpecBuilder().addFilter(respond()).build(), expectB(JsonPathEngine.JAYWAY)).get("/"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage(JAYWAY_IS_MISSING);
    }

    @Test
    void the_specification_outranks_the_config_of_the_request() {
        RestAssuredConfig jayway = config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY));

        assertThatThrownBy(() -> given().filter(respond()).config(jayway).expect().body("a", equalTo("b")).when().get("/"))
                .hasMessage(JAYWAY_IS_MISSING);
        given().filter(respond()).config(jayway).expect().spec(expectB(JsonPathEngine.GPATH)).when().get("/");
        given(new RequestSpecBuilder().addFilter(respond()).setConfig(jayway).build(), expectB(JsonPathEngine.GPATH)).get("/");
    }

    @Test
    void the_specification_outranks_rest_assured_config() {
        RestAssured.config = config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY));

        response().then().spec(expectB(JsonPathEngine.GPATH));
        given().filter(respond()).expect().spec(expectB(JsonPathEngine.GPATH)).when().get("/");
    }

    @Test
    void config_types_that_the_specification_doesnt_configure_come_from_the_request() {
        ResponseSpecification xmlOnly = new ResponseSpecBuilder().setConfig(config().xmlConfig(xmlConfig().namespaceAware(false)))
                .expectBody("a", equalTo("b")).build();

        assertThatThrownBy(() -> given().filter(respond()).config(config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY)))
                .expect().spec(xmlOnly).when().get("/"))
                .hasMessage(JAYWAY_IS_MISSING);
    }

    @Test
    void a_specification_merged_into_a_builder_keeps_its_config() {
        ResponseSpecification merged = new ResponseSpecBuilder().addResponseSpecification(expectB(JsonPathEngine.JAYWAY)).build();

        assertThatThrownBy(() -> response().then().spec(merged)).hasMessage(JAYWAY_IS_MISSING);
    }

    @Test
    void the_last_config_set_on_the_builder_is_used() {
        ResponseSpecification spec = new ResponseSpecBuilder().setConfig(config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.JAYWAY)))
                .setConfig(null).expectBody("a", equalTo("b")).build();

        given().filter(respond()).config(config().jsonConfig(jsonConfig().pathEngine(JsonPathEngine.GPATH))).expect().spec(spec).when().get("/");
    }

    private static ResponseSpecification expectB(JsonPathEngine engine) {
        return new ResponseSpecBuilder().setConfig(config().jsonConfig(jsonConfig().pathEngine(engine))).expectBody("a", equalTo("b")).build();
    }

    private static Response response() {
        return new ResponseBuilder().setStatusCode(200).setContentType("application/json").setBody("{\"a\":\"b\"}").build();
    }

    // Answers the request without sending it
    private static Filter respond() {
        return (requestSpec, responseSpec, ctx) -> response();
    }
}

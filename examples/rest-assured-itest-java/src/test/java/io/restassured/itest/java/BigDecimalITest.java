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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.path.json.config.JsonPathConfig;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.config.JsonConfig.jsonConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class BigDecimalITest extends WithJetty {

    @After public void
    rest_assured_is_reset_after_each_test() {
        RestAssured.reset();
    }

    @Before public void
    given_rest_assured_is_configured_with_big_decimal_as_return_type() {
        RestAssured.config = RestAssured.config().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    @Test public void
    big_decimal_works_when_configured_in_given_clause() {
        given().
                config(RestAssured.config().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL))).
        when().
                get("/amount").
        then().
                body("amount", equalTo(new BigDecimal("250.00")));
    }

    @Test public void
    big_decimal_works_when_configured_statically() {
        RestAssured.config = RestAssured.config().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));

        when().
                get("/amount").
        then().
                body("amount", equalTo(new BigDecimal("250.00")));
    }

    @Test public void
    floats_are_used_as_big_decimal_in_anonymous_list_with_numbers_when_configured_accordingly() {
        RestAssured.config = RestAssured.config().jsonConfig(jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));

        when().
                get("/anonymous_list_with_numbers").
        then().
                statusCode(HttpStatus.SC_OK).
                body("$", hasItems(100, 50, BigDecimal.valueOf(31.0)));
    }
}

/*
 * Copyright 2014 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.After;
import org.junit.Test;

import java.math.BigDecimal;

import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.config.JsonConfig.jsonConfig;
import static com.jayway.restassured.config.MatcherConfig.ErrorDescriptionType.HAMCREST;
import static com.jayway.restassured.config.MatcherConfig.matcherConfig;
import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static org.hamcrest.Matchers.equalTo;

public class BigDecimalITest extends WithJetty {

    @After public void
    rest_assured_is_reset_after_each_test() {
        RestAssured.reset();
    }

    @Test public void
    big_decimal_works() {
        RestAssured.config = RestAssured.config().jsonConfig(jsonConfig().numberReturnType(BIG_DECIMAL)).matcherConfig(matcherConfig().errorDescriptionType(HAMCREST));

        when().
                get("/amount").
        then().
                body("amount", equalTo(new BigDecimal("250.00")));
    }
}

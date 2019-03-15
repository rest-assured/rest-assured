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
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.when;
import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.DOUBLE;
import static org.hamcrest.Matchers.*;

public class DoubleITest extends WithJetty {

    @After public void
    rest_assured_is_reset_after_each_test() {
        RestAssured.reset();
    }

    @Before public void
    given_rest_assured_is_configured_with_double_as_return_type() {
        RestAssured.config = RestAssured.config().jsonConfig(jsonConfig().numberReturnType(DOUBLE));
    }

    @Test public void
    double_works() {
        when().
                get("/amount").
        then().
                body("amount", equalTo(250.00d));
    }

    @Test public void
    floats_are_used_as_doubles_in_anonymous_list_with_numbers_when_configured_accordingly() {
        when().
                get("/anonymous_list_with_numbers").
        then().
                statusCode(HttpStatus.SC_OK).
                body("$", hasItems(100, 50, 31.0d));
    }

    @Test public void
    can_use_the_close_to_hamcrest_matcher_when_number_return_type_is_double() {
        when().
                get("/amount").
        then().
                body("amount", closeTo(250.00d, 0.001d));
    }
}

/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.path.json;

import com.jayway.restassured.path.json.config.JsonPathConfig;
import org.junit.Test;

import java.math.BigDecimal;

import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class JsonPathNumberTest {

    private static final String PRICE = "{\n" +
            "\n" +
            "    \"price\":12.1 \n" +
            "\n" +
            "}";


    @Test public void
    json_path_returns_big_decimal_for_json_numbers_when_configured_accordingly() {
        // Given
        final JsonPath jsonPath = new JsonPath(PRICE).using(new JsonPathConfig(BIG_DECIMAL));

        // When
        BigDecimal price = jsonPath.get("price");

        // Then
        assertThat(price, equalTo(BigDecimal.valueOf(12.1)));
    }

    @Test public void
    json_path_returns_float_for_json_numbers_when_configured_accordingly() {
        // Given
        final JsonPath jsonPath = new JsonPath(PRICE).using(new JsonPathConfig().numberReturnType(FLOAT_AND_DOUBLE));

        // When
        float price = (Float) jsonPath.get("price");

        // Then
        assertThat(price, equalTo(12.1f));
    }

}

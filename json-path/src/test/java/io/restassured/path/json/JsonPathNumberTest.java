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

package io.restassured.path.json;

import io.restassured.path.json.config.JsonPathConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JsonPathNumberTest {

    /**
     * An expected input which is less than Integer.MAX_VALUE
     */
    private static final String EXPECTED_INTEGER = "15303030";

    /**
     * An expected input which is greater than Integer.MAX_VALUE and less than Long.MAX_VALUE
     */
    private static final String EXPECTED_LONG = "13000000000";

    private static final String ORDER_NUMBER_JSON = "{\n" +
                                                    "\n" +
                                                    "    \"orderNumber\":"+EXPECTED_INTEGER+" \n" +
                                                    "\n" +
                                                    "}";

    private static final String LIGHT_YEARS_TO_COSMIC_HORIZON_JSON = "{\n" +
                                                                     "\n" +
                                                                     "    \"lightYearsToCosmicHorizon\":" + EXPECTED_LONG + " \n" +
                                                                     "\n" +
                                                                     "}";

    private static final String PRICE_JSON = "{\n" +
                                             "\n" +
                                             "    \"price\":12.1 \n" +
                                             "\n" +
                                             "}";


    @Test public void
    json_path_returns_big_decimal_for_json_numbers_when_configured_accordingly() {
        // Given
        final JsonPath jsonPath = new JsonPath(PRICE_JSON).using(new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL));

        // When
        BigDecimal price = jsonPath.get("price");

        // Then
        assertThat(price, equalTo(BigDecimal.valueOf(12.1)));
    }

    @Test public void
    json_path_returns_float_for_json_numbers_when_configured_accordingly() {
        // Given
        final JsonPath jsonPath = new JsonPath(
            PRICE_JSON).using(new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));

        // When
        float price = (Float) jsonPath.get("price");

        // Then
        assertThat(price, equalTo(12.1f));
    }

    @Test public void
    json_path_returns_big_integer_for_json_integer_numbers_when_configured_accordingly() {
        // Given
        final JsonPath jsonPath = new JsonPath(
            ORDER_NUMBER_JSON).using(new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_INTEGER));

        // When
        BigInteger orderNumber = jsonPath.get("orderNumber");

        // Then
        assertThat(orderNumber, equalTo(new BigInteger(EXPECTED_INTEGER)));
    }

    @Test public void
    json_path_returns_big_integer_for_json_long_numbers_when_configured_accordingly() {
        // Given
        final JsonPath jsonPath = new JsonPath(
            LIGHT_YEARS_TO_COSMIC_HORIZON_JSON).using(new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_INTEGER));

        // When
        BigInteger orderNumber = jsonPath.get("lightYearsToCosmicHorizon");

        // Then
        assertThat(orderNumber, equalTo(new BigInteger(EXPECTED_LONG)));
    }

    @Test public void
    json_path_returns_finite_double_for_large_negative_number_with_default_config() {
        // Given
        final JsonPath jsonPath = new JsonPath("{\"pos\": 1e40, \"neg\": -1e40}")
                .using(new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));

        // When
        Object pos = jsonPath.get("pos");
        Object neg = jsonPath.get("neg");

        // Then
        // A magnitude above Float.MAX_VALUE must promote to double regardless of sign, so the
        // finite value survives instead of overflowing to +/-Infinity in the float branch.
        assertThat(pos, equalTo(1.0E40));
        assertThat(neg, equalTo(-1.0E40));
    }

    @Test public void
    json_path_returns_big_integer_for_primitive_number_when_configured_accordingly() {
        // Given
        final JsonPath jsonPath = new JsonPath(
                "12345").using(new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_INTEGER));

        // When
        BigInteger number = jsonPath.get("$");

        // Then
        assertThat(number, equalTo(new BigInteger("12345")));
    }

}

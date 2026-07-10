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
import io.restassured.path.json.exception.JsonPathException;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Duration;

import static io.restassured.path.json.config.JsonPathConfig.DEFAULT_NUMBER_LENGTH_LIMIT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class JsonPathNumberLengthLimitTest {

    @Test public void
    rejects_integer_number_that_exceeds_the_default_length_limit() {
        // Given a number far larger than the default limit (131072 digits, the size from the original report,
        // constructing a BigInteger from it would otherwise peg a CPU core for hundreds of milliseconds)
        final String json = "{\"n\":" + "9".repeat(131_072) + "}";
        final JsonPath jsonPath = new JsonPath(json);

        // When / Then it is rejected quickly, before any expensive number construction runs
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            JsonPathException exception = assertThrows(JsonPathException.class, () -> jsonPath.get("n"));
            assertThat(exception.getCause().getMessage(), containsString("exceeds the maximum allowed length"));
        });
    }

    @Test public void
    rejects_decimal_number_that_exceeds_the_default_length_limit() {
        // Given
        final String json = "{\"n\":" + "9".repeat(50_000) + ".5}";
        final JsonPath jsonPath = new JsonPath(json);

        // When / Then it is rejected quickly, before any expensive BigDecimal construction runs
        assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> assertThrows(JsonPathException.class, () -> jsonPath.get("n")));
    }

    @Test public void
    accepts_number_at_the_default_length_limit() {
        // Given a number exactly at the default limit
        final String number = "9".repeat(DEFAULT_NUMBER_LENGTH_LIMIT);
        final JsonPath jsonPath = new JsonPath("{\"n\":" + number + "}")
                .using(new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_INTEGER));

        // When
        BigInteger n = jsonPath.get("n");

        // Then
        assertThat(n, equalTo(new BigInteger(number)));
    }

    @Test public void
    accepts_oversized_number_when_the_limit_is_raised() {
        // Given a limit large enough to admit the number
        final String number = "9".repeat(5000);
        final JsonPath jsonPath = new JsonPath("{\"n\":" + number + "}")
                .using(new JsonPathConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_INTEGER)
                        .numberLengthLimit(10_000));

        // When
        BigInteger n = jsonPath.get("n");

        // Then
        assertThat(n, equalTo(new BigInteger(number)));
    }

    @Test public void
    accepts_oversized_number_when_the_limit_is_disabled() {
        // Given a negative limit which disables the check
        final String number = "9".repeat(5000);
        final JsonPath jsonPath = new JsonPath("{\"n\":" + number + "}")
                .using(new JsonPathConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_INTEGER)
                        .numberLengthLimit(-1));

        // When
        BigInteger n = jsonPath.get("n");

        // Then
        assertThat(n, equalTo(new BigInteger(number)));
    }
}

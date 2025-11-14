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

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ResponseTimeITest extends WithJetty {

    @BeforeEach
    void disable_tests_on_windows() {
        // These tests are flaky (fail every now and then) on Windows due to System#currentTimeMillis() precision.
        // See JavaDoc on System#currentTimeMillis() and TimingFilter.
        // Be sure to warm up the JVM and execute the tests multiple times to see the failures on Windows
        assumeFalse(SystemUtils.IS_OS_WINDOWS, "High precision dependent tests are disabled on Windows");
    }

    @Test
    void response_time_can_be_extracted() {
        long time =
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                extract().response().time();

        assertThat(time, greaterThan(0L));
    }

    @Test
    void response_time_can_be_be_converted() {
        long timeNanos = get("/lotto").timeIn(NANOSECONDS);
        long timeMillis = get("/lotto").timeIn(MILLISECONDS);
        assertThat(timeNanos, greaterThan(0L));
        assertThat(timeMillis, greaterThan(0L));
        assertThat(timeNanos, greaterThan(timeMillis));
    }

    @Test
    void response_time_can_be_validated_with_implicit_time_unit() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                time(allOf(greaterThan(0L), lessThan(2000L)));
    }

    @Test
    void response_time_can_be_validated_with_explicit_time_unit() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                time(lessThan(2L), SECONDS);
    }

    @Test
    void response_time_validation_can_fail() {
        assertThatThrownBy(() ->
            given().
                param("firstName", "John").
                param("lastName", "Doe").
            when().
                get("/greet").
            then().
                time(greaterThan(2L), DAYS)
        )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected response time was not a value greater than <2L> days, was ")
        .hasMessageContaining("milliseconds (0 days).");
    }

    @Test
    void response_time_validation_can_be_specified_in_specification() {
        ResponseSpecification spec = new ResponseSpecBuilder().expectResponseTime(lessThanOrEqualTo(3000L)).build();

        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                spec(spec);
    }

    @Test
    void response_time_validation_can_be_specified_in_specification_using_time_unit() {
        ResponseSpecification spec = new ResponseSpecBuilder().expectResponseTime(lessThanOrEqualTo(3L), SECONDS).build();

        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                spec(spec);
    }

    @Test
    void response_time_validation_can_fail_when_specified_in_specification() {
        ResponseSpecification spec = new ResponseSpecBuilder().expectResponseTime(lessThanOrEqualTo(3L), NANOSECONDS).build();
        assertThatThrownBy(() ->
            given().
                param("firstName", "John").
                param("lastName", "Doe").
            when().
                get("/greet").
            then().
                spec(spec)
        )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected response time was not a value less than or equal to <3L> nanoseconds, was");
    }

    @Test
    void can_use_response_time_validation_in_legacy_syntax() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                time(lessThan(2000L)).
        when().
                get("/greet");
    }
}

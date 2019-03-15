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
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;

public class ResponseTimeITest extends WithJetty {

    @Before
    public void
    disable_tests_on_windows() {
        // These tests are flaky (fail every now and then) on Windows due to System#currentTimeMillis() precision.
        // See JavaDoc on System#currentTimeMillis() and TimingFilter.
        // Be sure to warm up the JVM and execute the tests multiple times to see the failures on Windows
        assumeFalse("High precision dependent tests are disabled on Windows", SystemUtils.IS_OS_WINDOWS);
    }

    @Test public void
    response_time_can_be_extracted() {
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

    @Test public void
    response_time_can_be_be_converted() {
        long timeNanos = get("/lotto").timeIn(NANOSECONDS);
        long timeMillis = get("/lotto").timeIn(MILLISECONDS);
        assertThat(timeNanos, greaterThan(0L));
        assertThat(timeMillis, greaterThan(0L));
        assertThat(timeNanos, greaterThan(timeMillis));
    }

    @Test public void
    response_time_can_be_validated_with_implicit_time_unit() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                time(allOf(greaterThan(0L), lessThan(2000L)));
    }

    @Test public void
    response_time_can_be_validated_with_explicit_time_unit() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                time(lessThan(2L), SECONDS);
    }

    @Test public void
    response_time_validation_can_fail() {
        exception.expect(AssertionError.class);
        exception.expectMessage(allOf(containsString("Expected response time was not a value greater than <2L> days, was "),
                endsWith(" milliseconds (0 days).")));

        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                time(greaterThan(2L), DAYS);
    }

    @Test public void
    response_time_validation_can_be_specified_in_specification() {
        ResponseSpecification spec = new ResponseSpecBuilder().expectResponseTime(lessThanOrEqualTo(3000L)).build();

        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                spec(spec);
    }

    @Test public void
    response_time_validation_can_be_specified_in_specification_using_time_unit() {
        ResponseSpecification spec = new ResponseSpecBuilder().expectResponseTime(lessThanOrEqualTo(3L), SECONDS).build();

        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                spec(spec);
    }

    @Test public void
    response_time_validation_can_fail_when_specified_in_specification() {
        exception.expect(AssertionError.class);
        exception.expectMessage("Expected response time was not a value less than or equal to <3L> nanoseconds, was");

        ResponseSpecification spec = new ResponseSpecBuilder().expectResponseTime(lessThanOrEqualTo(3L), NANOSECONDS).build();

        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                spec(spec);
    }

    @Test public void
    can_use_response_time_validation_in_legacy_syntax() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        expect().
                time(lessThan(2000L)).
        when().
                get("/greet");
    }
}

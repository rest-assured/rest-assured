/*
 * Copyright 2015 the original author or authors.
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

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ResponseTimeITest extends WithJetty {

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
                responseTime(allOf(greaterThan(0L), lessThan(2000L)));
    }

    @Test public void
    response_time_can_be_validated_with_explicit_time_unit() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                responseTime(lessThan(2L), SECONDS);
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
                responseTime(greaterThan(2L), DAYS);
    }
}

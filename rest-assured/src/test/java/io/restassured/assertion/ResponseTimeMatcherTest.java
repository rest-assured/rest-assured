/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.assertion;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "RedundantCast"})
public class ResponseTimeMatcherTest {

    private static Map<String, Object> validate(Matcher<Long> matcher, TimeUnit timeUnit, long timeInUnit, long timeInMillis) {
        Response response = mock(Response.class);
        when(response.getTimeIn(timeUnit)).thenReturn(timeInUnit);
        when(response.getTime()).thenReturn(timeInMillis);

        ResponseTimeMatcher responseTimeMatcher = new ResponseTimeMatcher();
        responseTimeMatcher.setMatcher(matcher);
        responseTimeMatcher.setTimeUnit(timeUnit);
        return (Map<String, Object>) responseTimeMatcher.validate(response);
    }

    @Test
    public void returns_success_and_empty_error_message_when_time_matches() {
        Map<String, Object> result = validate(lessThan(10L), TimeUnit.SECONDS, 2, 2000);

        assertThat(result).containsOnlyKeys("success", "errorMessage");
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(String.valueOf(result.get("errorMessage"))).isEmpty();
    }

    @Test
    public void reports_time_in_milliseconds_and_in_expected_unit_when_time_does_not_match() {
        Map<String, Object> result = validate(greaterThan(10L), TimeUnit.SECONDS, 2, 2345);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(String.valueOf(result.get("errorMessage")))
                .isEqualTo("Expected response time was not a value greater than <10L> seconds, was 2345 milliseconds (2 seconds).");
    }

    @Test
    public void reports_missing_time_when_no_time_was_recorded() {
        Map<String, Object> result = validate(lessThan(10L), TimeUnit.MILLISECONDS, -1, -1);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(String.valueOf(result.get("errorMessage"))).isEqualTo("No time was recorded, cannot perform response time validation.");
    }

    @Test
    public void zero_is_a_recorded_time() {
        Map<String, Object> result = validate(lessThan(0L), TimeUnit.MILLISECONDS, 0, 0);

        assertThat(String.valueOf(result.get("errorMessage")))
                .isEqualTo("Expected response time was not a value less than <0L> milliseconds, was 0 milliseconds (0 milliseconds).");
    }

    @Test
    public void keeps_groovy_accessors() {
        Matcher<Long> matcher = lessThan(10L);
        ResponseTimeMatcher responseTimeMatcher = new ResponseTimeMatcher();
        responseTimeMatcher.setMatcher(matcher);
        responseTimeMatcher.setTimeUnit(TimeUnit.MINUTES);

        assertThat((Object) responseTimeMatcher.getMatcher()).isSameAs(matcher);
        assertThat(responseTimeMatcher.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
    }
}

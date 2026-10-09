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

package io.restassured.assertion;

import io.restassured.response.Response;
import org.hamcrest.Matcher;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ResponseTimeMatcher {

    private Matcher<Long> matcher;
    private TimeUnit timeUnit;

    public Map<String, Object> validate(Response response) {
        String errorMessage = "";
        boolean success = true;

        long time = response.getTimeIn(timeUnit);
        if (time <= -1) {
            errorMessage = "No time was recorded, cannot perform response time validation.";
            success = false;
        } else if (!matcher.matches(time)) {
            long timeMillis = response.getTime();
            String unit = timeUnit.toString().toLowerCase(Locale.ROOT);
            success = false;
            errorMessage = "Expected response time was not " + matcher + " " + unit + ", was " + timeMillis + " milliseconds (" + time + " " + unit + ").";
        }

        Map<String, Object> result = new LinkedHashMap<>(2);
        result.put("success", success);
        result.put("errorMessage", errorMessage);
        return result;
    }

    public Matcher<Long> getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher<Long> matcher) {
        this.matcher = matcher;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}

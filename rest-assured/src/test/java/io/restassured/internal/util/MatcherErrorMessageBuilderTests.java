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

package io.restassured.internal.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MatcherErrorMessageBuilderTests {

    @Test
    public void shouldIncludeMismatchedDescription() {

        final Integer expectedStatusCode = 200;
        final String body = "There was an error.";

        final TypeSafeMatcher<Integer> matcher = new TypeSafeMatcher<Integer>() {
            @Override
            protected boolean matchesSafely(Integer integer) {
                return integer.equals(expectedStatusCode);
            }

            public void describeTo(Description description) {
                description.appendText("to equal ").appendValue(expectedStatusCode);
            }

            @Override
            protected void describeMismatchSafely(Integer actual, Description mismatchDescription) {
                mismatchDescription
                        .appendText("was ")
                        .appendValue(actual)
                        .appendText(" with body: ")
                        .appendText(body);
            }
        };

        final MatcherErrorMessageBuilder<Integer, Matcher<Integer>> builder = new MatcherErrorMessageBuilder<Integer, Matcher<Integer>>("status code");
        final String error = builder.buildError(500, matcher);

        assertEquals("Expected status code to equal <200> but was <500> with body: There was an error.\n", error);
    }

    @Test
    public void shouldPrintNicelyWithoutMismatchDescription() {
        final Integer expectedStatusCode = 200;
        final TypeSafeMatcher<Integer> matcher = new TypeSafeMatcher<Integer>() {
            @Override
            protected boolean matchesSafely(Integer integer) {
                return integer.equals(expectedStatusCode);
            }

            public void describeTo(Description description) {
                description
                        .appendText("to equal ")
                        .appendValue(expectedStatusCode);
            }
        };

        final MatcherErrorMessageBuilder<Integer, Matcher<Integer>> builder = new MatcherErrorMessageBuilder<Integer, Matcher<Integer>>("status code");
        final String error = builder.buildError(500, matcher);

        assertEquals("Expected status code to equal <200> but was <500>.\n", error);
    }

    @Test
    public void shouldPrintNicelyWithDefaultEqualMatcher() {

        final Integer expectedStatusCode = 200;
        final MatcherErrorMessageBuilder<Integer, Matcher<Integer>> builder = new MatcherErrorMessageBuilder<Integer, Matcher<Integer>>("status code");
        final String error = builder.buildError(500, Matchers.equalTo(expectedStatusCode));

        assertEquals("Expected status code <200> but was <500>.\n", error);
    }
}

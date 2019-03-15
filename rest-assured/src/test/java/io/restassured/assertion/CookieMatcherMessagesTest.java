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

import io.restassured.http.Cookies;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Maciej Gawinecki
 */
@SuppressWarnings("unchecked")
public class CookieMatcherMessagesTest {

    private String[] cookies = new String[]{"DEVICE_ID=123; Domain=.test.com; Expires=Thu, 12-Oct-2023 09:34:31 GMT; Path=/; Secure; HttpOnly;"};

    @Test
    public void shouldPrintValidErrorMessageForStandardMatchers() {

        CookieMatcher cookieMatcher = new CookieMatcher();
        cookieMatcher.setCookieName("DEVICE_ID");
        cookieMatcher.setMatcher(Matchers.containsString("X"));

        Map<String, Object> result = (Map<String, Object>) cookieMatcher.validateCookies(Arrays.asList(cookies), new Cookies());
        assertThat((Boolean)result.get("success"), equalTo(false));
        assertThat(result.get("errorMessage").toString(), equalTo("Expected cookie \"DEVICE_ID\" was not a string containing \"X\", was \"123\".\n"));
    }

    @Test
    public void shouldPrintValidErrorMessageForCustomMatcher() {

        CookieMatcher cookieMatcher = new CookieMatcher();
        cookieMatcher.setCookieName("DEVICE_ID");
        cookieMatcher.setMatcher(new ContainsXMatcher());

        Map<String, Object> result = (Map<String, Object>) cookieMatcher.validateCookies(Arrays.asList(cookies), new Cookies());
        assertThat((Boolean)result.get("success"), equalTo(false));
        assertThat(result.get("errorMessage").toString(), equalTo("Expected cookie \"DEVICE_ID\" was not containing 'X', \"123\" not containing 'X'.\n"));
    }

    private static class ContainsXMatcher extends TypeSafeDiagnosingMatcher<String> {

        @Override
        protected boolean matchesSafely(String actual, Description mismatchDescription) {

            // Not this method will be called twice due to https://github.com/hamcrest/JavaHamcrest/issues/144

            if (actual.contains("X")) {
                return true;
            }

            mismatchDescription.appendValue(actual).appendText(" not containing 'X'");
            return false;
        }

        public void describeTo(Description description) {
            description.appendText("containing 'X'");
        }
    }

}

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

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.internal.assertion.CookieMatcher;
import org.hamcrest.Matcher;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DetailedCookieAssertion {

    private String cookieName;
    private Matcher<? super Cookie> matcher;

    public Map<String, Object> validateCookies(List<String> headerWithCookieList, Cookies responseCookies) {
        String errorMessage = "";

        Cookies cookiesInHeader = CookieMatcher.getCookies(headerWithCookieList);
        Cookie cookie = cookiesInHeader.get(cookieName);
        if (cookie == null) {
            cookie = responseCookies.get(cookieName);
        } else if (cookie.getExpiryDate() == null && responseCookies.get(cookieName) != null) {
            Date expiryDate = responseCookies.get(cookieName).getExpiryDate();
            if (expiryDate != null) {
                cookie = new Cookie.Builder(cookie).setExpiryDate(expiryDate).build();
            }
        }

        boolean success = matcher.matches(cookie);
        if (!success) {
            String expectedDescription = CookieMatcher.getExpectedDescription(matcher);
            String mismatchDescription = CookieMatcher.getMismatchDescription(matcher, cookie);
            errorMessage = "Expected cookie \"" + cookieName + "\" was not " + expectedDescription + ", " + mismatchDescription + ".\n";
        }

        Map<String, Object> result = new LinkedHashMap<>(2);
        result.put("success", success);
        result.put("errorMessage", errorMessage);
        return result;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public Matcher<? super Cookie> getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher<? super Cookie> matcher) {
        this.matcher = matcher;
    }
}

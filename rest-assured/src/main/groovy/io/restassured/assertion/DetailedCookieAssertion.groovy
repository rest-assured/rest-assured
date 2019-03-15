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

package io.restassured.assertion

import io.restassured.http.Cookie
import io.restassured.http.Cookies
import org.hamcrest.Matcher


class DetailedCookieAssertion {

    String cookieName
    Matcher<? super Cookie> matcher

    def validateCookies(List<String> headerWithCookieList, Cookies responseCookies) {
        def success
        def errorMessage = ""

        Cookies cookiesInHeader = CookieMatcher.getCookies(headerWithCookieList)
        Cookie cookie = cookiesInHeader.get(cookieName)
        if (cookie == null) {
            cookie = responseCookies.get(cookieName)
        }

        success = matcher.matches(cookie)
        if (!success) {
            def expectedDescription = CookieMatcher.getExpectedDescription(matcher)
            def mismatchDescription = CookieMatcher.getMismatchDescription(matcher, cookie)
            errorMessage = "Expected cookie \"$cookieName\" was not $expectedDescription, $mismatchDescription.\n"
        }

        [success: success, errorMessage: errorMessage]
    }
}

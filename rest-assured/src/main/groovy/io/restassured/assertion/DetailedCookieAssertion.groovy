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

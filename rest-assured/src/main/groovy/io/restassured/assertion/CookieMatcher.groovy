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
import org.apache.commons.lang3.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.client.utils.DateUtils
import org.hamcrest.Matcher
import org.hamcrest.StringDescription

import static io.restassured.http.Cookie.*
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase
import static org.apache.commons.lang3.StringUtils.trim

class CookieMatcher {
    private static final Log log = LogFactory.getLog(CookieMatcher.class)

    def cookieName
    Matcher<String> matcher

    def validateCookies(List<String> headerWithCookieList, Cookies responseCookies) {
        def success = true
        def errorMessage = ""
        if(!headerWithCookieList && !responseCookies.exist()) {
            success = false
            errorMessage = "No cookies defined in the response\n"
        } else {
            Cookies cookiesInHeader = getCookies(headerWithCookieList)
            List<Cookie> mergedCookies = []
            mergedCookies += cookiesInHeader
            for (Cookie responseCookie: responseCookies) {
                if (!cookiesInHeader.hasCookieWithName(responseCookie.getName())) {
                    mergedCookies << responseCookie
                }
            }

            def raCookies = new Cookies(mergedCookies)
            def cookie = raCookies.get(cookieName)
            if (cookie == null) {
                String cookiesAsString = raCookies.toString()
                success = false
                errorMessage = "Cookie \"$cookieName\" was not defined in the response. Cookies are: \n$cookiesAsString\n"
            } else {
                def value = cookie.getValue()
                if(!matcher.matches(value)) {
                    success = false
                    def expectedDescription = getExpectedDescription(matcher)
                    def mismatchDescription = getMismatchDescription(matcher, value)
                    errorMessage = "Expected cookie \"$cookieName\" was not $expectedDescription, $mismatchDescription.\n"

                }
            }
        }
        [success: success, errorMessage: errorMessage]
    }

    static String getExpectedDescription(Matcher matcher) {
        def expectedDescription = new StringDescription()
        matcher.describeTo(expectedDescription)
        return expectedDescription.toString()
    }

    static String getMismatchDescription(Matcher matcher, value) {
        def mismatchDescription = new StringDescription()
        matcher.describeMismatch(value, mismatchDescription)
        return mismatchDescription.toString()
    }

    static Cookies getCookies(headerWithCookieList) {
        def cookieList = []
        headerWithCookieList.each {
            Cookie.Builder cookieBuilder
            def cookieStrings = StringUtils.split(it, ";")
            cookieStrings.eachWithIndex { part, index ->
                if(index == 0) {
                    if(part.contains("=")) {
                        def (cookieKey, cookieValue) = getKeyAndValueOfCookie(part)
                        cookieBuilder = new Cookie.Builder(cookieKey, cookieValue)
                    } else {
                        cookieBuilder = new Cookie.Builder(part, null)
                    }
                } else if(part.contains("=")) {
                    def (cookieKey, cookieValue) = getKeyAndValueOfCookie(part)
                    setCookieProperty(cookieBuilder, cookieKey, cookieValue)
                } else {
                    setCookieProperty(cookieBuilder, part, null)
                }
            }
            cookieList << cookieBuilder?.build()
        }
        return new Cookies(cookieList)
    }

    static List getKeyAndValueOfCookie(String part) {
        def indexOfEqual = StringUtils.indexOf(part, "=")
        def cookieKey, cookieValue
        if(indexOfEqual > -1) {
            cookieKey = StringUtils.substring(part, 0, indexOfEqual)
            cookieValue = StringUtils.substring(part, indexOfEqual + 1)
        } else {
            cookieKey = part
            cookieValue = null
        }
        return [StringUtils.trim(cookieKey), StringUtils.trim(cookieValue)]
    }

    private static def setCookieProperty(Cookie.Builder builder, name, value) {
        name = trim(name)
        if(value != null || equalsIgnoreCase(name, SECURE) || equalsIgnoreCase(name, HTTP_ONLY)) {
            if(equalsIgnoreCase(name, COMMENT)) {
                builder.setComment(value)
            } else if(equalsIgnoreCase(name, VERSION)) {
              // Some servers supply the version in quotes, remove them
              value = trim(StringUtils.remove(value, "\""))
                if (value.isNumber()) {
                  builder.setVersion(value as Integer)
                }
            } else if(equalsIgnoreCase(name, PATH)) {
                builder.setPath(value)
            } else if(equalsIgnoreCase(name, DOMAIN)) {
                builder.setDomain(value)
            } else if(equalsIgnoreCase(name, MAX_AGE)) {
                builder.setMaxAge(Integer.parseInt(value))
            } else if(equalsIgnoreCase(name, SECURE)) {
                builder.setSecured(true)
            } else if(equalsIgnoreCase(name, HTTP_ONLY)) {
                builder.setHttpOnly(true)
            } else if(equalsIgnoreCase(name, EXPIRES)) {
              value = trim(StringUtils.remove(value, "\""))
              Date parsedDate = DateUtils.parseDate(value)
              if (parsedDate != null) {
                builder.setExpiryDate(parsedDate)
              } else {
                log.warn("Ignoring unparsable 'Expires' attribute value: " + value)
              }
            }
        }
    }
}
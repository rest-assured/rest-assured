/*
 * Copyright 2013 the original author or authors.
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



package com.jayway.restassured.assertion

import com.jayway.restassured.response.Cookie
import com.jayway.restassured.response.Cookies
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.DateUtils
import org.hamcrest.Matcher

import static com.jayway.restassured.response.Cookie.*
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase
import static org.apache.commons.lang3.StringUtils.trim

class CookieMatcher {

    def cookieName
    def Matcher<String> matcher

    def validateCookie(List<String> cookies) {
        def success = true
        def errorMessage = ""
        if(!cookies) {
            success = false
            errorMessage = "No cookies defined in the response\n"
        } else {
            def raCookies = getCookies(cookies)
            def cookie = raCookies.get(cookieName)
            if (cookie == null) {
                String cookiesAsString = raCookies.toString()
                success = false
                errorMessage = "Cookie \"$cookieName\" was not defined in the response. Cookies are: \n$cookiesAsString\n"
            } else {
                def value = cookie.getValue()
                if(!matcher.matches(value)) {
                    success = false
                    errorMessage = "Expected cookie \"$cookieName\" was not $matcher, was \"$value\".\n"

                }
            }
        }
        [success: success, errorMessage: errorMessage]
    }

    public static Cookies getCookies(headerWithCookieList) {
        def cookieList = []
        headerWithCookieList.each {
            def Cookie.Builder cookieBuilder
            def cookieStrings = StringUtils.split(it, ";");
            cookieStrings.eachWithIndex { part, index ->
                if(index == 0) {
                    if(part.contains("=")) {
                        def (cookieKey, cookieValue) = getKeyAndValueOfCookie(part)
                        cookieBuilder = new Cookie.Builder(cookieKey, cookieValue);
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
            cookieKey = StringUtils.substring(part, 0, indexOfEqual);
            cookieValue = StringUtils.substring(part, indexOfEqual + 1)
        } else {
            cookieKey = part
            cookieValue = null
        }
        return [StringUtils.trim(cookieKey), StringUtils.trim(cookieValue)]
    }

    private static def setCookieProperty(Cookie.Builder builder, name, value) {
        name = trim(name);
        if(value != null || equalsIgnoreCase(name, SECURE) || equalsIgnoreCase(name, HTTP_ONLY)) {
            if(equalsIgnoreCase(name, COMMENT)) {
                builder.setComment(value)
            } else if(equalsIgnoreCase(name, VERSION)) {
                builder.setVersion(value.isInteger()? value as Integer: -1)
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
                builder.setExpiryDate(DateUtils.parseDate(value))
            }
        }
    }
}

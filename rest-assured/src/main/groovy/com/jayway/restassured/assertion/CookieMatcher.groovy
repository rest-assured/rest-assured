/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.assertion

import com.jayway.restassured.exception.AssertionFailedException
import org.hamcrest.Matcher

class CookieMatcher {

  def cookieName
  def Matcher<String> matcher

  def containsCookie(String cookies) {
    def value = getCookieValueOrThrowExceptionIfCookieIsMissing(cookieName, cookies)
    if(!matcher.matches(value)) {
      throw new AssertionFailedException("Expected cookie \"$cookieName\" was not $matcher, was \"$value\".")
    }
  }

  private def getCookieValueOrThrowExceptionIfCookieIsMissing(cookieName,String cookies) {
    def cookieStrings = cookies.split(";");
    def cookieMap = [:]
    cookieStrings.each {
        def singleCookie = it.split("=")
        cookieMap.put singleCookie[0].trim(), singleCookie[1].trim()
    }

    def cookie = cookieMap.get(cookieName)
    if (cookie == null) {
      String cookiesAsString = "";
      cookieMap.each { cookiesAsString += "\n$it.key = $it.value" }
      throw new AssertionFailedException("Cookie \"$cookieName\" was not defined in the response. Cookies are: $cookiesAsString");
    }
    return cookie

  }


}

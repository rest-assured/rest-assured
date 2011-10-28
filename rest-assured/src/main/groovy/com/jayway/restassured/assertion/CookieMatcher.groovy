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

import org.hamcrest.Matcher
import com.jayway.restassured.response.Cookie
import com.jayway.restassured.response.Cookies
import org.apache.commons.lang.StringUtils
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase
import static com.jayway.restassured.response.Cookie.COMMENT
import static org.apache.commons.lang.StringUtils.trim
import org.apache.http.impl.cookie.DateUtils
import static com.jayway.restassured.response.Cookie.EXPIRES
import static com.jayway.restassured.response.Cookie.SECURED
import static com.jayway.restassured.response.Cookie.MAX_AGE
import static com.jayway.restassured.response.Cookie.DOMAIN
import static com.jayway.restassured.response.Cookie.PATH
import static com.jayway.restassured.response.Cookie.VERSION

class CookieMatcher {

  def cookieName
  def Matcher<String> matcher

  def containsCookie(String cookies) {
    def value = getCookieValueOrThrowExceptionIfCookieIsMissing(cookieName, cookies)
    if(!matcher.matches(value)) {
      throw new AssertionError("Expected cookie \"$cookieName\" was not $matcher, was \"$value\".")
    }
  }

  private def getCookieValueOrThrowExceptionIfCookieIsMissing(cookieName,String cookies) {
    def cookieMap = getCookies(cookies)
    def cookie = cookieMap.get(cookieName)
    if (cookie == null) {
      String cookiesAsString = "";
      cookieMap.each { cookiesAsString += "\n$it.key = $it.value" }
      throw new AssertionError("Cookie \"$cookieName\" was not defined in the response. Cookies are: $cookiesAsString");
    }
    return cookie

  }

  public static Cookies getCookies(cookies) {
    if(!cookies) {
      throw new AssertionError("No cookies defined in the response")
    }
    def Cookie.Builder cookieBuilder
    println cookies
    cookies.each {
      def cookieStrings = it.split(";");
      cookieStrings.eachWithIndex { part, index ->
        if(index == 0) {
          if(part.contains("=")) {
            def singleCookie = part.split("=")
            cookieBuilder = new Cookie.Builder(singleCookie[0].trim(), (singleCookie.length > 1) ? singleCookie[1].trim() : null);
          } else {
            cookieBuilder = new Cookie.Builder(part, null)
          }
        } else if(part.contains("=")) {
          def cookieAttributeAndValue = part.split("=")
          setCookieProperty(cookieBuilder, cookieAttributeAndValue[0], cookieAttributeAndValue[1])
        } else {
          setCookieProperty(cookieBuilder, part)
        }
      }
    }
    return cookieBuilder.build()
  }

  private static def setCookieProperty(Cookie.Builder builder, name, value) {
    name = trim(name);
    if(equalsIgnoreCase(name, COMMENT)) {
      builder.setComment(value)
    } else if(equalsIgnoreCase(name, VERSION)) {
      builder.setVersion()
    } else if(equalsIgnoreCase(name, PATH)) {
      builder.setPath(value)
    } else if(equalsIgnoreCase(name, DOMAIN)) {
      builder.setDomain(value)
    } else if(equalsIgnoreCase(name, MAX_AGE)) {
      builder.setMaxAge(Integer.parseInt(value))
    } else if(equalsIgnoreCase(name, SECURED)) {
      builder.setSecured(true)
    } else if(equalsIgnoreCase(name, EXPIRES)) {
      builder.setExpiryDate(DateUtils.parseDate(value))
    }
  }
}

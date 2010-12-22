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

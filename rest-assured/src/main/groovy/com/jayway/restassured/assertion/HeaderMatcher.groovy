package com.jayway.restassured.assertion

import org.hamcrest.Matcher

import com.jayway.restassured.exception.AssertionFailedException

class HeaderMatcher {

  def headerName
  def Matcher<String> matcher

  def containsHeader(headers) {
    def value = getHeaderValueOrThrowExceptionIfHeaderIsMissing(headerName, headers)
    if(!matcher.matches(value)) {
      throw new AssertionFailedException("Expected header \"$headerName\" was not $matcher, was \"$value\".")
    }
  }

  private def getHeaderValueOrThrowExceptionIfHeaderIsMissing(headerName, headers) {
    def header = headers.getAt(headerName)
    if (header == null) {
      String headersAsString = "";
      headers.each { headersAsString += "\n$it.name: $it.value" }
      throw new AssertionFailedException("Header \"$headerName\" was not defined in the response. Headers are: $headersAsString");
    }
    return header.value
  }
}

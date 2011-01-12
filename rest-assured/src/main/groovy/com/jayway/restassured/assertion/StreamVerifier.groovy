package com.jayway.restassured.assertion

import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.ContentType.HTML
import static groovyx.net.http.ContentType.JSON
import groovyx.net.http.ContentType

class StreamVerifier {

  def static newAssertion(response, key, content) {
    def contentType = response.contentType.toString().toLowerCase()
    def assertion
    if(contentTypeMatch(JSON, contentType) ) {
      assertion = new JSONAssertion(key: key)
    } else if(contentTypeMatch(XML, contentType)) {
      assertion = new XMLAssertion(key: key)
    } else if(contentTypeMatch(HTML, contentType)) {
      assertion = new XMLAssertion(key: key, ignoreCase: true)
    } else if("application/rss+xml" == contentType) {
        assertion = new XMLAssertion(key: key)
    } else {
      throw new IllegalStateException("Expected response to have JSON or XML content but got "+response.contentType+ ". Content was:\n$content\n")
    }
    assertion
  }

  private static boolean contentTypeMatch(ContentType expectedContentType, String actualContentType) {
    def types = expectedContentType.getContentTypeStrings();
    for(String type : types) {
      if(type == actualContentType) return true
    }
    return false
  }
}

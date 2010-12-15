package com.jayway.restassured.assertion

import org.hamcrest.Matcher
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import com.jayway.restassured.exception.AssertionFailedException
import static groovyx.net.http.ContentType.*
import org.hamcrest.xml.HasXPath
import java.lang.reflect.Array

class BodyMatcher {
  def key
  def Matcher matcher

  def isFulfilled(response, content) {
    if(key == null) {
      if(isXPathMatcher()) {
        Element node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(new String(content).getBytes())).getDocumentElement();
        if (matcher.matches(node) == false) {
          throw new AssertionFailedException(String.format("Body doesn't match.\nExpected:\n%s\nActual:\n%s", matcher.toString(), content))
        }
      } else if (!matcher.matches(content)) {
        throw new AssertionFailedException("Body doesn't match.\nExpected:\n$matcher\nActual:\n$content")
      }
    } else {
      def assertion;
      switch (response.contentType.toString().toLowerCase()) {
        case JSON.toString().toLowerCase():
          assertion = new JSONAssertion(key: key)
          break
        case XML.toString().toLowerCase():
          assertion = new XMLAssertion(key: key)
          break;
      }
      def result = null
      if(content != null) {
        result = assertion.getResult(content)
      }
      if (!matcher.matches(result)) {
        if(result instanceof Object[]) {
          result = result.join(",")
        }
        throw new AssertionFailedException(String.format("%s %s doesn't match %s, was <%s>.", assertion.description(), key, matcher.toString(), result))
      }
    }
  }

  private boolean isXPathMatcher() {
    matcher instanceof HasXPath
  }

  def boolean requiresContentTypeText() {
    isXPathMatcher() || key == null
  }

  def String getDescription() {
    String description = ""
    if(key) {
      description = "Body containing expression \"$key\" must match $matcher"
    } else {
      description = "Body must match $matcher"
    }
    return description
  }
}

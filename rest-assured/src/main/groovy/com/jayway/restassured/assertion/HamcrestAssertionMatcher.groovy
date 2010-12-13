package com.jayway.restassured.assertion

import com.jayway.restassured.exception.AssertionFailedException
import javax.xml.parsers.DocumentBuilderFactory
import net.sf.json.JSON
import org.hamcrest.Matcher
import org.hamcrest.xml.HasXPath
import org.w3c.dom.Element
import com.jayway.restassured.JsonXPath
import org.powermock.reflect.Whitebox

class HamcrestAssertionClosure {
  private Matcher matcher;
  private String key;
  def expectedStatusCode
  def expectedStatusLine

  HamcrestAssertionClosure(String key, Matcher matcher) {
    this.key = key
    this.matcher = matcher
  }

  def call(response, content) {
    return getClosure().call(response, content)
  }

  def call(response) {
    return getClosure().call(response, null)
  }

  boolean isXPathMatcher() {
    matcher instanceof HasXPath
  }

  boolean isRawBodyMatcher() {
    isXPathMatcher() || key == null
  }

  def getClosure() {
    return { response, content ->
      def headers = response.headers
      if(expectedStatusCode != null) {
        def actualStatusCode = response.statusLine.statusCode
        if(!expectedStatusCode.matches(actualStatusCode)) {
          throw new AssertionFailedException(String.format("Expected status code %s doesn't match actual status code <%s>.", expectedStatusCode.toString(), actualStatusCode));
        }
      }

      if(expectedStatusLine != null) {
        def actualStatusLine = response.statusLine.toString()
        if(!expectedStatusLine.matches(actualStatusLine)) {
          throw new AssertionFailedException(String.format("Expected status line %s doesn't match actual status line \"%s\".", expectedStatusLine.toString(), actualStatusLine));
        }
      }
      def result
      if(key == null) {
        if(isXPathMatcher()) {
          result = content.readLines().join()
          def contentType = headers.'Content-Type'
          if(contentType.contains("application/json")) {
            Matcher hasXPathValueMatcher = Whitebox.getInternalState(matcher, Matcher.class)
            String xPathString = Whitebox.getInternalState(matcher, String.class)
            def xpathMatcher = new JsonXPath(xPathString);
            if(hasXPathValueMatcher == null ) {
              if(xpathMatcher.getValue(result) == null) {
                throw new AssertionFailedException("XPath didn't match");
              }
            } else if(!hasXPathValueMatcher.matches(xpathMatcher.getValue(result))) {
              throw new AssertionFailedException("XPath didn't match: "+matcher.toString());
            }
          } else {
            Element node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(new String(result).getBytes())).getDocumentElement();
            if (matcher.matches(node) == false) {
              throw new AssertionFailedException(String.format("Body doesn't match.\nExpected:\n%s\nActual:\n%s", matcher.toString(), result))
            }
          }
        } else {
          if(content instanceof InputStreamReader) {
            result = content.readLines().join()
          } else {
            result = content.toString()
          }
          if (!matcher.matches(result)) {
            throw new AssertionFailedException(String.format("Body doesn't match.\nExpected:\n%s\nActual:\n%s", matcher.toString(), result))
          }
        }
      }  else {
        def assertion
        switch (response.contentType.toString().toLowerCase()) {
          case JSON.toString().toLowerCase():
            assertion = new JSONAssertion(key: key)
            break
          case XML.toString().toLowerCase():
            assertion = new XMLAssertion(key: key)
            break;
        }
        result = assertion.getResult(content)
        if (!matcher.matches(result)) {
          throw new AssertionFailedException(String.format("%s %s doesn't match %s, was <%s>.", assertion.description(), key, matcher.toString(), result))
        }
      }
    }
  }
}
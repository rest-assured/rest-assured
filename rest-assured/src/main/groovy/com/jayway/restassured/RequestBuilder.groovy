package com.jayway.restassured

import com.jayway.restassured.assertion.Assertion
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import org.hamcrest.Matcher
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static org.hamcrest.Matchers.equalTo
import com.jayway.restassured.exception.AssertionFailedException
import org.hamcrest.xml.HasXPath
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import com.jayway.restassured.assertion.XMLAssertion
import com.jayway.restassured.assertion.JSONAssertion
import com.jayway.restassured.assertion.HamcrestAssertionClosure

class RequestBuilder {

  String baseUri
  String path
  int port
  Matcher<Integer> expectedStatusCode;
  Matcher<String> expectedStatusLine;
  Method method
  Map query
  def HamcrestAssertionClosure assertionClosure;

  private Assertion assertion;

  def RequestBuilder content(String key, Matcher<?> matcher) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: method, query: query, assertionClosure: new HamcrestAssertionClosure(key, matcher), expectedStatusCode : expectedStatusCode, expectedStatusLine: expectedStatusLine)
  }
  def RequestBuilder statusCode(Matcher<Integer> expectedStatusCode) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: method, query: query, assertionClosure: assertionClosure, expectedStatusCode : expectedStatusCode, expectedStatusLine: expectedStatusLine)
  }

  def RequestBuilder statusCode(int expectedStatusCode) {
    return statusCode(equalTo(expectedStatusCode));
  }

  def RequestBuilder statusLine(Matcher<String> expectedStatusLine) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: method, query: query, assertionClosure: assertionClosure, expectedStatusCode : expectedStatusCode, expectedStatusLine: expectedStatusLine)
  }

  def RequestBuilder statusLine(String expectedStatusLine) {
    return statusLine(equalTo(expectedStatusLine))
  }

  def RequestBuilder body(Matcher<?> matcher) {
    return content(null, matcher);
  }

  def RequestBuilder body(String key, Matcher<?> matcher) {
    return content(key, matcher);
  }

  def RequestBuilder response() {
    return this;
  }

  def RequestBuilder when() {
    return this;
  }

  def get(String path) {
    sendRequest(path, GET, query, assertionClosure);
  }

  def post(String path) {
    sendRequest(path, POST, query, assertionClosure);
  }

  def then(Closure assertionClosure) {
    sendRequest(path, method, query, new GroovyAssertionClosure(assertionClosure));
  }

  def RequestBuilder parameters(Object...parameters) {
    if(parameters == null || parameters.length < 2) {
      throw new IllegalArgumentException("You must supply at least one key and one value.");
    } else if(parameters.length % 2 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    }

    Map<String, Object> map = new HashMap<String,Object>();
    for (int i = 0; i < parameters.length; i+=2) {
      map.put(parameters[i], parameters[i+1]);
    }
    return this.parameters(map)
  }

  def RequestBuilder parameters(Map<String, Object> map) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: method, query: map, assertionClosure: assertionClosure, expectedStatusCode: expectedStatusCode, expectedStatusLine: expectedStatusLine)
  }

  def RequestBuilder and() {
    return this;
  }

  def RequestBuilder with() {
    return this;
  }

  def RequestBuilder then() {
    return this;
  }

  def RequestBuilder expect() {
    return this;
  }

  def RequestBuilder port(int port) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: method, query: query, assertionClosure: assertionClosure, expectedStatusCode : expectedStatusCode, expectedStatusLine: expectedStatusLine)
  }

  private def sendRequest(path, method, query, assertionClosure) {
    if(port <= 0) {
      throw new IllegalArgumentException("Port must be greater than 0")
    }
    def url = baseUri.startsWith("http://") ? baseUri : "http://"+baseUri
    def http = new HTTPBuilder("$url:$port")
    def contentType = assertionClosure.isRawBodyMatcher() ? TEXT : ANY
    if(POST.equals(method)) {
      try {
        http.post( path: path, body: query,
                requestContentType: URLENC, contentType: contentType) { response, content ->
          if(assertionClosure != null) {
            assertionClosure.call (response, content)
          }
        }
      } catch(HttpResponseException e) {
        if(assertionClosure != null) {
          assertionClosure.call(e.getResponse())
        } else {
          throw e;
        }
      }
    } else if(GET.equals(method)) {
      http.request(method, contentType) {
        uri.path = path
        if(query != null) {
          uri.query = query
        }

        Closure closure = assertionClosure.getClosure()
        // response handler for a success response code:
        response.success = closure

        // handler for any failure status code:
        response.failure = closure
      }
    } else {
      throw new IllegalArgumentException("Only GET and POST supported")
    }
  }

  class GroovyAssertionClosure {

    Closure closure;

    GroovyAssertionClosure(Closure closure) {
      this.closure = closure
    }

    boolean isRawBodyMatcher() {
      return false;
    }

    def call(response, content) {
      return getClosure().call(response, content)
    }

    def call(response) {
      return getClosure().call(response)
    }

    def getClosure() {
      closure
    }
  }
}

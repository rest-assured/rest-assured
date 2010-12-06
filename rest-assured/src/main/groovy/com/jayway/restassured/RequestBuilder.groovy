package com.jayway.restassured

import com.jayway.restassured.assertion.Assertion
import com.jayway.restassured.assertion.JSONAssertion
import com.jayway.restassured.exception.AssertionFailedException
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import org.hamcrest.Matcher
import static groovyx.net.http.ContentType.ANY
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import com.jayway.restassured.assertion.XMLAssertion

class RequestBuilder {

  String baseUri
  String path
  int port
  Method method
  Map query
  Closure assertionClosure;

  private Assertion assertion;

  def RequestBuilder content(String key, Matcher<?> matcher) {
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: method, query: query, assertionClosure: getAssertionClosure(key, matcher))
  }

  def RequestBuilder when() {
    return this;
  }

  def get(String path) {
     sendRequest(path, method, query, assertionClosure);
  }

  def andAssertThatContent(Matcher<?> matcher) {
    def assertionClosure = { response, content ->
      if(!matcher.matches(response.getData())) {
        throw new AssertionFailedException(String.format("<%s> doesn't match %s.", response.data, matcher.toString()))
      }
    }
    sendRequest(path, method, query, assertionClosure);
  }

  def andAssertThat(String key, Matcher<?> matcher) {
    sendRequest(path, method, query, getAssertionClosure(key, matcher));
  }

  def then(Closure assertionClosure) {
    sendRequest(path, method, query, assertionClosure);
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
    return new RequestBuilder(baseUri: RestAssured.baseURI, path: path, port: port, method: method, query: map, assertionClosure: assertionClosure)
  }

  def RequestBuilder and() {
    return this;
  }

  def RequestBuilder with() {
    return this;
  }

  def RequestBuilder port(int port) {
    return new RequestBuilder(baseUri: baseUri, path: path, port: port, method: method)
  }

  private def sendRequest(path, method, query, responseHandler) {
    if(port <= 0) {
      throw new IllegalArgumentException("Port must be greater than 0")
    }
    def url = baseUri.startsWith("http://") ? baseUri : "http://"+baseUri
    def http = new HTTPBuilder("$url:$port")
    if(POST.equals(method)) {
      try {
        http.post( path: path, body: query,
                requestContentType: URLENC ) { response, json ->
          if(responseHandler != null) {
            responseHandler.call (response, json)
          }
        }
      } catch(HttpResponseException e) {
        if(responseHandler != null) {
          responseHandler.call(e.getResponse())
        } else {
          throw e;
        }
      }
    } else if(GET.equals(method)) {
      http.request(method, ANY ) {
        uri.path = path
        if(query != null) {
          uri.query = query
        }
        // response handler for a success response code:
        response.success = responseHandler

        // handler for any failure status code:
        response.failure = responseHandler
      }
    } else {
      throw new IllegalArgumentException("Only GET and POST supported")
    }
  }

  private Closure getAssertionClosure(String key, Matcher<?> matcher) {
    return { response, content ->
      switch (response.contentType.toString().toLowerCase()) {
        case JSON.toString().toLowerCase():
          assertion = new JSONAssertion(key: key)
          break
        case XML.toString().toLowerCase():
          assertion = new XMLAssertion(key: key)
          break;
      }
      def result = assertion.getResult(content)
      if (!matcher.matches(result)) {
        throw new AssertionFailedException(String.format("%s %s doesn't match %s, was <%s>.", assertion.description(), key, matcher.toString(), result))
      }
    }
  }
}

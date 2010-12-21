package com.jayway.restassured.internal

import com.jayway.restassured.authentication.AuthenticationScheme
import com.jayway.restassured.authentication.NoAuthScheme
import com.jayway.restassured.specification.AuthenticationSpecification
import com.jayway.restassured.specification.RequestSpecification
import com.jayway.restassured.specification.ResponseSpecification
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

class RequestSpecificationImpl implements RequestSpecification {

  private String baseUri
  private String path
  private int port
  private Map parameters
  private AuthenticationScheme authenticationScheme = new NoAuthScheme()
  private ResponseSpecification responseSpecification;

  def RequestSpecification when() {
    return this;
  }

  def RequestSpecification given() {
    return this;
  }

  def RequestSpecification that() {
    return this;
  }

  def ResponseSpecification response() {
    return responseSpecification;
  }

  // TODO Return response
  def void get(String path) {
    sendRequest(path, GET, parameters, responseSpecification.assertionClosure);
  }

  // TODO Return response
  def void post(String path) {
    sendRequest(path, POST, parameters, responseSpecification.assertionClosure);
  }

  def RequestSpecification parameters(String parameter, String...parameters) {
    return this.parameters(createMapFromStrings(createArgumentArray(parameter, parameters)))
  }

  def RequestSpecification parameters(Map<String, String> parametersMap) {
    this.parameters = Collections.unmodifiableMap(parametersMap)
    return this
  }

  def RequestSpecification and() {
    return this;
  }

  def RequestSpecification request() {
    return this;
  }

  def RequestSpecification with() {
    return this;
  }

  def RequestSpecification then() {
    return this;
  }

  def ResponseSpecification expect() {
    return responseSpecification;
  }

  def AuthenticationSpecification auth() {
    return new AuthenticationSpecification(this);
  }

  def AuthenticationSpecification authentication() {
    return auth();
  }

  def RequestSpecification port(int port) {
    this.port = port
    return this
  }

  private def sendRequest(path, method, parameters, assertionClosure) {
    def http = new HTTPBuilder(getTargetURI(path))
    def contentType = assertionClosure.requiresContentTypeText() ? TEXT : ANY
    authenticationScheme.authenticate(http)
    if(POST.equals(method)) {
      try {
        http.post( path: path, body: parameters,
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
    } else {
      http.request(method, contentType) {
        uri.path = path
        if(parameters != null) {
          uri.query = parameters
        }

        Closure closure = assertionClosure.getClosure()
        // response handler for a success response code:
        response.success = closure

        // handler for any failure status code:
        response.failure = closure
      }
    }
  }

  private String getTargetURI(String path) {
    if(port <= 0) {
      throw new IllegalArgumentException("Port must be greater than 0")
    }
    def uri
    def hasScheme = path.contains("://")
    if(hasScheme) {
      uri = path;
    } else {
      uri = "$baseUri:$port"
    }
    return uri
  }

  private def assertNotNull(Object ... objects) {
    objects.each {
      if(it == null) {
        throw new IllegalArgumentException("Argument cannot be null")
      }
    }
  }

  private def Map<String, Object> createMapFromStrings(... parameters) {
    if(parameters == null || parameters.length < 2) {
      throw new IllegalArgumentException("You must supply at least one key and one value.");
    } else if(parameters.length % 2 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    }

    Map<String, Object> map = new HashMap<String, Object>();
    for (int i = 0; i < parameters.length; i+=2) {
      map.put(parameters[i], parameters[i+1]);
    }
    return map;
  }

  private Object[] createArgumentArray(String firstExpectedHeaderName, Object... expectedHeaders) {
    def params = [firstExpectedHeaderName]
    expectedHeaders.each {
      params << it
    }
    return params as Object[]
  }

  Closure getResponseValidator() {

  }
}

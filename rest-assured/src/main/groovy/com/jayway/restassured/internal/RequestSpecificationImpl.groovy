package com.jayway.restassured.internal

import com.jayway.restassured.authentication.AuthenticationScheme
import com.jayway.restassured.authentication.NoAuthScheme
import com.jayway.restassured.specification.AuthenticationSpecification
import com.jayway.restassured.specification.RequestSpecification
import com.jayway.restassured.specification.ResponseSpecification
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import static groovyx.net.http.ContentType.ANY
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.DELETE

class RequestSpecificationImpl implements RequestSpecification {

  private String baseUri
  private String path
  private int port
  private Map parameters
  private AuthenticationScheme authenticationScheme = new NoAuthScheme()
  private ResponseSpecification responseSpecification;
  private ContentType requestContentType;
  private Map<String, String> requestHeaders = [:]
  private Map<String, String> cookies = [:]
  private Object requestBody;

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

  def void get(String path) {
    sendRequest(path, GET, parameters, responseSpecification.assertionClosure);
  }

  def void post(String path) {
    sendRequest(path, POST, parameters, responseSpecification.assertionClosure);
  }

  def void put(String path) {
     sendRequest(path, PUT, parameters, responseSpecification.assertionClosure);
  }

  def void delete(String path) {
    sendRequest(path, DELETE, parameters, responseSpecification.assertionClosure);
  }

  def RequestSpecification parameters(String parameter, String...parameters) {
    return this.parameters(MapCreator.createMapFromStrings(parameter, parameters))
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

  def RequestSpecification body(String body) {
    this.requestBody = body;
    return this;
  }

  RequestSpecification content(String body) {
    return body(body);
  }

  def RequestSpecification body(byte[] body) {
    this.requestBody = body;
    return this;
  }

  RequestSpecification content(byte[] body) {
    return body(body);
  }

  RequestSpecification contentType(ContentType contentType) {
    this.requestContentType = contentType
    return  this
  }

  RequestSpecification headers(Map<String, String> headers) {
    this.requestHeaders = headers;
    return this;
  }

  RequestSpecification headers(String headerName, String ... headerNameValueParis) {
    return headers(MapCreator.createMapFromStrings(headerName, headerNameValueParis))
  }

  RequestSpecification cookies(String cookieName, String... cookieNameValuePairs) {
    return cookies(MapCreator.createMapFromStrings(cookieName, cookieNameValuePairs))
    return this;
  }

  RequestSpecification cookies(Map<String, String> cookies) {
    this.cookies = cookies;
    return this;
  }

  private def sendRequest(path, method, parameters, assertionClosure) {
    def http = new HTTPBuilder(getTargetURI(path))
    http.getHeaders() << requestHeaders
    if(!cookies.isEmpty()) {
      http.getHeaders() << [Cookie : cookies.collect{it.key+"="+it.value}.join("; ")]
    }
    def responseContentType =  assertionClosure.getResponseContentType()
    authenticationScheme.authenticate(http)
    if(POST.equals(method)) {
      try {
        if(parameters != null && requestBody != null) {
          throw new IllegalStateException("You can either send parameters OR body content in the POST, not both!");
        }
        def bodyContent = parameters ?: requestBody
        http.post( path: path, body: bodyContent,
                requestContentType: defineRequestContentType(),
                contentType: responseContentType) { response, content ->
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
      if(requestBody != null) {
        throw new IllegalStateException("Can't send a "+method+" request with a request body. Use parameters instead.");
      }
      http.request(method, responseContentType) {
        uri.path = path
        if(parameters != null) {
          uri.query = parameters
        }
        requestContentType: requestContentType ?: ANY

        Closure closure = assertionClosure.getClosure()
        // response handler for a success response code:
        response.success = closure

        // handler for any failure status code:
        response.failure = closure
      }
    }
  }

  private def defineRequestContentType() {
    if (requestContentType == null) {
      if (requestBody == null) {
        requestContentType = URLENC
      } else if (requestBody instanceof byte[]) {
        requestContentType = BINARY
      } else {
        requestContentType = TEXT
      }
    }
    requestContentType
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
}
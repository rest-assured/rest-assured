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

package com.jayway.restassured.internal

import com.jayway.restassured.authentication.AuthenticationScheme
import com.jayway.restassured.authentication.NoAuthScheme
import com.jayway.restassured.filter.Filter
import com.jayway.restassured.internal.filter.FilterContextImpl
import com.jayway.restassured.internal.filter.RootFilter
import com.jayway.restassured.response.Response
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HTTPBuilder.RequestConfigDelegate
import groovyx.net.http.Method
import groovyx.net.http.Status
import java.util.Map.Entry
import org.apache.http.client.methods.HttpPost
import static com.jayway.restassured.assertion.AssertParameter.notNull
import com.jayway.restassured.specification.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import static java.util.Arrays.asList

class RequestSpecificationImpl implements FilterableRequestSpecification {
  private static String KEY_ONLY_COOKIE_VALUE = "Rest Assured Key Only Cookie Value"

  private String baseUri
  private String path  = ""
  private String basePath
  private AuthenticationScheme defaultAuthScheme
  private int port
  private Map<String, String> requestParameters = [:]
  private Map<String, String> queryParams = [:]
  def AuthenticationScheme authenticationScheme = new NoAuthScheme()
  private FilterableResponseSpecification responseSpecification;
  private def requestContentType;
  private Map<String, String> requestHeaders = [:]
  private Map<String, String> cookies = [:]
  private Object requestBody;
  private List<Filter> filters = [];

  public RequestSpecificationImpl (String baseURI, int requestPort, String basePath, AuthenticationScheme defaultAuthScheme,
                                   List<Filter> filters, defaultRequestContentType, RequestSpecification defaultSpec) {
    notNull(baseURI, "baseURI");
    notNull(basePath, "basePath");
    notNull(defaultAuthScheme, "defaultAuthScheme");
    notNull(filters, "Filters")
    this.baseUri = baseURI
    this.basePath = basePath
    this.defaultAuthScheme = defaultAuthScheme
    this.filters.addAll(filters)
    this.requestContentType = defaultRequestContentType
    port(requestPort)
    if(defaultSpec != null) {
      spec(defaultSpec)
    }
  }

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

  def Response get(String path) {
    notNull path, "path"
    invokeFilterChain(path, GET, responseSpecification.assertionClosure)
  }

  def Response post(String path) {
    notNull path, "path"
    invokeFilterChain(path, POST, responseSpecification.assertionClosure);
  }

  def Response put(String path) {
    notNull path, "path"
    invokeFilterChain(path, PUT, responseSpecification.assertionClosure);
  }

  def Response delete(String path) {
    notNull path, "path"
    invokeFilterChain(path, DELETE, responseSpecification.assertionClosure);
  }

  def Response head(String path) {
    notNull path, "path"
    invokeFilterChain(path, HEAD, responseSpecification.assertionClosure);
  }

  def RequestSpecification parameters(String parameterName, String... parameterNameValuePairs) {
    notNull parameterName, "parameterName"
    return parameters(MapCreator.createMapFromStrings(parameterName, parameterNameValuePairs))
  }

  def RequestSpecification parameters(Map<String, String> parametersMap) {
    notNull parametersMap, "parametersMap"
    appendParameters(parametersMap, requestParameters)
    return this
  }

  def RequestSpecification params(String parameterName, String... parameterNameValuePairs) {
    return parameters(parameterName, parameterNameValuePairs)
  }

  def RequestSpecification params(Map<String, String> parametersMap) {
    return parameters(parametersMap)
  }

  def RequestSpecification param(String parameterName, String parameterValue, String... additionalParameterValues) {
    return parameter(parameterName, parameterValue, additionalParameterValues)
  }

  def RequestSpecification parameter(String parameterName, List<String> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    appendListParameter(requestParameters, parameterName, parameterValues)
    return this
  }

  def RequestSpecification param(String parameterName, List<String> parameterValues) {
    return parameter(parameterName, parameterValues)
  }

  def RequestSpecification queryParameter(String parameterName, List<String> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    appendListParameter(queryParams, parameterName, parameterValues)
    return this
  }

  def RequestSpecification queryParam(String parameterName, List<String> parameterValues) {
    return queryParameter(parameterName, parameterValues)
  }

  def RequestSpecification parameter(String parameterName, String parameterValue, String... additionalParameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValue, "parameterValue"
    appendStringParameter(requestParameters, parameterName, parameterValue);
    if(additionalParameterValues != null) {
      appendListParameter(requestParameters, parameterName, asList(additionalParameterValues))
    }
    return this
  }

  def RequestSpecification queryParameters(String parameterName, String... parameterNameValuePairs) {
    notNull parameterName, "parameterName"
    return queryParameters(MapCreator.createMapFromStrings(parameterName, parameterNameValuePairs))
  }

  def RequestSpecification queryParameters(Map<String, String> parametersMap) {
    notNull parametersMap, "parametersMap"
    appendParameters(parametersMap, queryParams)
    return this
  }

  def RequestSpecification queryParameter(String parameterName, String parameterValue, String... additionalParameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValue, "parameterValue"
    appendStringParameter(queryParams, parameterName, parameterValue)
    if(additionalParameterValues != null) {
      appendListParameter(queryParams, parameterName, asList(additionalParameterValues))
    }
    return this
  }

  def RequestSpecification queryParams(String parameterName, String... parameterNameValuePairs) {
    return queryParameters(parameterName, parameterNameValuePairs);
  }

  def RequestSpecification queryParams(Map<String, String> parametersMap) {
    return queryParameters(parametersMap)
  }

  def RequestSpecification queryParam(String parameterName, String parameterValue, String... additionalParameterValue) {
    return queryParameter(parameterName, parameterValue, additionalParameterValue)
  }

  def RequestSpecification filter(Filter filter) {
    notNull filter, "Filter"
    filters << filter
    return this
  }

  def RequestSpecification filters(List<Filter> filters) {
    notNull filters, "Filters"
    this.filters.addAll(filters)
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

  def ResponseSpecification then() {
    return responseSpecification;
  }

  def ResponseSpecification expect() {
    return responseSpecification;
  }

  def AuthenticationSpecification auth() {
    return new AuthenticationSpecificationImpl(this);
  }

  def AuthenticationSpecification authentication() {
    return auth();
  }

  def RequestSpecification port(int port) {
    if(port < 1) {
      throw new IllegalArgumentException("Port must be greater than 0")
    }
    this.port = port
    return this
  }

  def RequestSpecification body(String body) {
    notNull body, "body"
    this.requestBody = body;
    return this;
  }

  RequestSpecification content(String content) {
    notNull content, "content"
    return content(content);
  }

  def RequestSpecification body(byte[] body) {
    notNull body, "body"
    this.requestBody = body;
    return this;
  }

  RequestSpecification content(byte[] content) {
    notNull content, "content"
    return content(content);
  }

  RequestSpecification contentType(ContentType contentType) {
    notNull contentType, "contentType"
    this.requestContentType = contentType
    return  this
  }

  RequestSpecification contentType(String contentType) {
    notNull contentType, "contentType"
    this.requestContentType = contentType
    return  this
  }

  RequestSpecification headers(Map<String, String> headers) {
    notNull headers, "headers"
    this.requestHeaders += headers;
    return this;
  }

  RequestSpecification header(String headerName, String headerValue) {
    notNull headerName, "headerName"
    notNull headerValue, "headerValue"
    requestHeaders.put(headerName, headerValue);
    return this
  }

  RequestSpecification headers(String headerName, String ... headerNameValueParis) {
    return headers(MapCreator.createMapFromStrings(headerName, headerNameValueParis))
  }

  RequestSpecification cookies(String cookieName, String... cookieNameValuePairs) {
    return cookies(MapCreator.createMapFromStrings(cookieName, cookieNameValuePairs))
  }

  RequestSpecification cookies(Map<String, String> cookies) {
    notNull cookies, "cookies"
    this.cookies += cookies;
    return this;
  }

  RequestSpecification cookie(String cookieName, String value) {
    notNull cookieName, "cookieName"
    notNull value, "value"
    cookies.put(cookieName, value)
    return this
  }

  def RequestSpecification cookie(String cookieName) {
    cookie(cookieName, KEY_ONLY_COOKIE_VALUE)
  }

  RequestSpecification spec(RequestSpecification requestSpecificationToMerge) {
    SpecificationMerger.merge this, requestSpecificationToMerge
    return this
  }

  RequestSpecification specification(RequestSpecification requestSpecificationToMerge) {
    return spec(requestSpecificationToMerge)
  }

  def invokeFilterChain(path, method, assertionClosure) {
    filters << new RootFilter()
    def ctx = new FilterContextImpl(path, method, assertionClosure, filters);
    return ctx.next(this, responseSpecification);
  }

  private def Response sendRequest(path, method, assertionClosure) {
    path = extractRequestParamsIfNeeded(method, path);
    def isFullyQualifiedUri = isFullyQualified(path)
    def targetUri = getTargetURI(path);
    def targetPath = isFullyQualifiedUri ? new URL(path).getPath() : "$basePath$path"
    def http = new HTTPBuilder(targetUri) {
      @Override protected Object doRequest(RequestConfigDelegate delegate) {
        // When doing POST we must add the failure handler here
        // in order to be able to return the response when doing
        // e.g. post("/ikk"); when an error occurs.
        if(delegate.getRequest() instanceof HttpPost) {
          if (assertionClosure != null ) {
            delegate.getResponse().put(
                    Status.FAILURE.toString(), { response, content ->
                      assertionClosure.call (response, content)
                    });
          }
          delegate.uri.query = queryParams
        }
        return super.doRequest(delegate)
      }
    };
    RestAssuredParserRegistry.responseSpecification = responseSpecification
    http.setParserRegistry(new RestAssuredParserRegistry())
    ResponseParserRegistrar.registerParsers(http, assertionClosure.requiresTextParsing())
    http.getHeaders() << requestHeaders

    if(!cookies.isEmpty()) {
      http.getHeaders() << [Cookie : cookies.collect{ defineRequestCookie(it) }.join("; ")]
    }

    // Allow returning a the response
    def restAssuredResponse = new RestAssuredResponseImpl()
    responseSpecification.restAssuredResponse = restAssuredResponse
    def responseContentType =  assertionClosure.getResponseContentType()

    if(authenticationScheme instanceof NoAuthScheme && !(defaultAuthScheme instanceof NoAuthScheme)) {
      // Use default auth scheme
      authenticationScheme = defaultAuthScheme
    }

    authenticationScheme.authenticate(http)

    if(POST.equals(method)) {
      if(!requestParameters.isEmpty() && requestBody != null) {
        throw new IllegalStateException("You can either send parameters OR body content in the POST, not both!");
      }
      def bodyContent = requestParameters.isEmpty() ? requestBody : requestParameters

      http.post( path: targetPath, body: bodyContent,
              requestContentType: defineRequestContentType(POST),
              contentType: responseContentType) { response, content ->
        if(assertionClosure != null) {
          assertionClosure.call (response, content)
        }
      }
    } else {
      http.request(method, responseContentType) {
        uri.path = targetPath

        setRequestContentType(defineRequestContentTypeAsString(method))

        if(requestBody != null) {
          body = requestBody
        }

        uri.query = requestParameters += queryParams

        Closure closure = assertionClosure.getClosure()
        // response handler for a success response code:
        response.success = closure

        // handler for any failure status code:
        response.failure = closure
      }
    }
    return restAssuredResponse
  }

  private def defineRequestCookie(Entry<String, String> it) {
    if(it.value == KEY_ONLY_COOKIE_VALUE) {
      return it.key
    }
    it.key + "=" + it.value
  }

  private boolean isFullyQualified(String targetUri) {
    return targetUri.contains("://")
  }

  private String extractRequestParamsIfNeeded(Method method, String path) {
    if(path.contains("?")) {
      def indexOfQuestionMark = path.indexOf("?")
      String allParamAsString = path.substring(indexOfQuestionMark+1);
      def keyValueParams = allParamAsString.split("&");
      keyValueParams.each {
        def keyValue = it.split("=")
        if(keyValue.length != 2) {
          throw new IllegalArgumentException("Illegal parameters passed to REST Assured. Parameters was: $keyValueParams")
        }
        if(method == POST) {
          queryParams.put(keyValue[0], keyValue[1])
        } else {
          param(keyValue[0], keyValue[1]);
        }
      };
      path = path.substring(0, indexOfQuestionMark);
    }
    return path;
  }

  private def defineRequestContentTypeAsString(Method method) {
    return defineRequestContentType(method).toString()
  }

  private def defineRequestContentType(Method method) {
    if (requestContentType == null) {
      if (requestBody == null) {
        requestContentType = method == POST ? URLENC : ANY
      } else if (requestBody instanceof byte[]) {
        if(method != POST && method != PUT) {
          throw new IllegalStateException("$method doesn't support binary request data.");
        }
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
    def hasScheme = isFullyQualified(path)
    if(hasScheme) {
      def url = new URL(path)
      uri = url.getProtocol()+"://"+url.getAuthority()
    } else {
      uri = "$baseUri:$port"
    }
    return uri
  }

  private def appendParameters(Map<String, String> from, Map<String, String> to) {
    from.each {key, value ->
      appendStringParameter(to, key, value)
    }
  }

  private def appendListParameter(Map<String, String> to, String key, List<String> value) {
    if (to.containsKey(key)) {
      def currentValue = to.get(key)
      if (currentValue instanceof List) {
        currentValue.addAll(value)
      } else {
        to.put(key, [currentValue, value].flatten())
      }
    } else {
      to.put(key, new LinkedList<String>(value))
    }
  }


  private def appendStringParameter(Map<String, String> to, String key, String value) {
    if (to.containsKey(key)) {
      def currentValue = to.get(key)
      if (currentValue instanceof List) {
        currentValue << value
      } else {
        to.put(key, [currentValue, value])
      }
    } else {
      to.put(key, value)
    }
  }

  def void setResponseSpecification(ResponseSpecification responseSpecification) {
    this.responseSpecification = responseSpecification
  }
}
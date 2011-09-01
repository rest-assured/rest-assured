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
import com.jayway.restassured.filter.log.ErrorLoggingFilter
import com.jayway.restassured.filter.log.ResponseLoggingFilter
import com.jayway.restassured.internal.filter.FilterContextImpl
import com.jayway.restassured.internal.filter.RootFilter
import com.jayway.restassured.response.Response
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HTTPBuilder.RequestConfigDelegate
import groovyx.net.http.Method
import groovyx.net.http.Status
import java.util.Map.Entry
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.lang.Validate
import org.apache.http.client.methods.HttpPost
import static com.jayway.restassured.assertion.AssertParameter.notNull
import com.jayway.restassured.specification.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import static java.util.Arrays.asList
import org.apache.commons.lang.StringUtils
import org.apache.http.entity.mime.MultipartEntity

import org.apache.http.entity.mime.HttpMultipartMode

class RequestSpecificationImpl implements FilterableRequestSpecification {
  private static String KEY_ONLY_COOKIE_VALUE = "Rest Assured Key Only Cookie Value"
  private static final int DEFAULT_HTTPS_PORT = 443
  private static final int DEFAULT_HTTP_PORT = 80
  private static final String MULTIPART_FORM_DATA = "multipart/form-data"

  private String baseUri
  private String path  = ""
  private String basePath
  private AuthenticationScheme defaultAuthScheme
  private int port
  private Map<String, String> requestParameters = [:]
  private Map<String, String> queryParams = [:]
  private Map<String, String> formParams = [:]
  private Map<String, Object> pathParams = [:]
  def AuthenticationScheme authenticationScheme = new NoAuthScheme()
  private FilterableResponseSpecification responseSpecification;
  private Object contentType;
  private Map<String, String> requestHeaders = [:]
  private Map<String, String> cookies = [:]
  private Object requestBody;
  private List<Filter> filters = [];
  private KeystoreSpec keyStoreSpec
  private boolean urlEncodingEnabled
  private List<MultiPart> multiParts = [];

  public RequestSpecificationImpl (String baseURI, int requestPort, String basePath, AuthenticationScheme defaultAuthScheme,
                                   List<Filter> filters, KeystoreSpec keyStoreSpec, defaultRequestContentType, RequestSpecification defaultSpec,
                                   boolean urlEncode) {
    notNull(baseURI, "baseURI");
    notNull(basePath, "basePath");
    notNull(defaultAuthScheme, "defaultAuthScheme");
    notNull(filters, "Filters")
    notNull(keyStoreSpec, "Keystore specification")
    notNull(urlEncode, "URL Encode query params option")
    this.baseUri = baseURI
    this.basePath = basePath
    this.defaultAuthScheme = defaultAuthScheme
    this.filters.addAll(filters)
    this.contentType = defaultRequestContentType
    this.keyStoreSpec = keyStoreSpec
    this.urlEncodingEnabled = urlEncode
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

  def Response get(String path, Object...pathParams) {
    applyPathParamsAndSendRequest(GET, path, pathParams)
  }

  def Response post(String path, Object...pathParams) {
    applyPathParamsAndSendRequest(POST, path, pathParams)
  }

  def Response put(String path, Object...pathParams) {
    applyPathParamsAndSendRequest(PUT, path, pathParams)
  }

  def Response delete(String path, Object...pathParams) {
    applyPathParamsAndSendRequest(DELETE, path, pathParams)
  }

  def Response head(String path, Object...pathParams) {
    applyPathParamsAndSendRequest(HEAD, path, pathParams)
  }

  def Response get(String path, Map<String, Object> pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(GET, path)
  }

  def Response post(String path, Map<String, Object> pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(POST, path)
  }

  def Response put(String path, Map<String, Object> pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(PUT, path)
  }

  def Response delete(String path, Map<String, Object> pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(DELETE, path)
  }

  def Response head(String path, Map<String, Object> pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(HEAD, path)
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
    appendStandardParameter(requestParameters, parameterName, parameterValue);
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
    appendStandardParameter(queryParams, parameterName, parameterValue)
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

  def RequestSpecification formParameter(String parameterName, List<String> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    appendListParameter(formParams, parameterName, parameterValues)
    return this
  }

  def RequestSpecification formParam(String parameterName, List<String> parameterValues) {
    return queryParameter(parameterName, parameterValues)
  }

  def RequestSpecification formParameters(String parameterName, String... parameterNameValuePairs) {
    notNull parameterName, "parameterName"
    return formParameters(MapCreator.createMapFromStrings(parameterName, parameterNameValuePairs))
  }

  def RequestSpecification formParameters(Map<String, String> parametersMap) {
    notNull parametersMap, "parametersMap"
    appendParameters(parametersMap, formParams)
    return this
  }

  def RequestSpecification formParameter(String parameterName, String parameterValue, String... additionalParameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValue, "parameterValue"
    appendStandardParameter(formParams, parameterName, parameterValue)
    if(additionalParameterValues != null) {
      appendListParameter(formParams, parameterName, asList(additionalParameterValues))
    }
    return this
  }

  def RequestSpecification formParams(String parameterName, String... parameterNameValuePairs) {
    return formParameters(parameterName, parameterNameValuePairs);
  }

  def RequestSpecification formParams(Map<String, String> parametersMap) {
    return formParameters(parametersMap)
  }

  def RequestSpecification formParam(String parameterName, String parameterValue, String... additionalParameterValue) {
    return formParameter(parameterName, parameterValue, additionalParameterValue)
  }

  def RequestSpecification urlEncodingEnabled(boolean isEnabled) {
    this.urlEncodingEnabled = isEnabled
    return this
  }

  def RequestSpecification pathParameter(String parameterName, Object parameterValue) {
    notNull parameterName, "parameterName"
    notNull parameterValue, "parameterValue"
    appendStandardParameter(pathParams, parameterName, parameterValue)
    return this
  }

  def RequestSpecification pathParameters(String parameterName, Object... parameterNameValuePairs) {
    notNull parameterName, "parameterName"
    return pathParameters(MapCreator.createMapFromStrings(parameterName, parameterNameValuePairs))
  }

  def RequestSpecification pathParameters(Map<String, Object> parameterNameValuePairs) {
    notNull parameterNameValuePairs, "parameterNameValuePairs"
    appendParameters(parameterNameValuePairs, pathParams)
    return this
  }

  def RequestSpecification pathParam(String parameterName, Object parameterValue) {
    return pathParameter(parameterName, parameterValue)
  }

  def RequestSpecification pathParams(String parameterName, Object... parameterNameValuePairs) {
    return pathParameters(parameterName, parameterNameValuePairs)
  }

  def RequestSpecification pathParams(Map<String, Object> parameterNameValuePairs) {
    return pathParameters(parameterNameValuePairs)
  }

  def RequestSpecification keystore(String pathToJks, String password) {
    Validate.notEmpty(pathToJks, "Path to java keystore cannot be empty");
    Validate.notEmpty(password, "Password cannot be empty");
    this.keyStoreSpec = new KeystoreSpecImpl(path: pathToJks, password: password)
    this
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

  def RequestSpecification log() {
    return filter(ResponseLoggingFilter.responseLogger())
  }

  def RequestSpecification logOnError() {
    return filter(ErrorLoggingFilter.errorLogger())
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
    this.requestBody = content;
    return this
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
    this.contentType = contentType
    return  this
  }

  RequestSpecification contentType(String contentType) {
    notNull contentType, "contentType"
    this.contentType = contentType
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

  def RequestSpecification multiPart(String controlName, File file) {
    multiParts << new MultiPart(name: controlName, content: file)
    this
  }

  def RequestSpecification multiPart(File file) {
    multiParts << new MultiPart(name: "file", content: file)
    this
  }

  def RequestSpecification multiPart(String name, File file, String mimeType) {
    multiParts << new MultiPart(name: name, content: file, mimeType: mimeType)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, byte[] bytes) {
    multiParts << new MultiPart(name: name, content: bytes, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, byte[] bytes, String mimeType) {
    multiParts << new MultiPart(name: name, content: bytes, mimeType: mimeType, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, InputStream stream) {
    multiParts << new MultiPart(name: name, content: stream, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, InputStream stream, String mimeType) {
    multiParts << new MultiPart(name: name, content: stream, mimeType: mimeType, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String contentBody) {
    multiParts << new MultiPart(name: name, content: contentBody)
    this
  }

  def RequestSpecification multiPart(String name, String contentBody, String mimeType) {
    multiParts << new MultiPart(name: name, content: contentBody, mimeType: mimeType)
    this
  }

  def invokeFilterChain(path, method, assertionClosure) {
    filters << new RootFilter()
    def ctx = new FilterContextImpl(path, method, assertionClosure, filters);
    def response = ctx.next(this, responseSpecification)
    responseSpecification.assertionClosure.validate(response)
    return response;
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
        } else if(!urlEncodingEnabled) {
          // Overwrite the URL encoded query parameters with the original ones
          delegate.uri.query = queryParams
        }
        return super.doRequest(delegate)
      }
    };
    registerRestAssuredEncoders(http);
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

    keyStoreSpec.apply(http, isFullyQualifiedUri == true && port == 8080 ? DEFAULT_HTTPS_PORT : port)

    validateMultiPartForPostOnly(method);

    if(shouldUrlEncode(method)) {
      if(hasFormParams() && requestBody != null) {
        throw new IllegalStateException("You can either send form parameters OR body content in $method, not both!");
      }
      def bodyContent = hasFormParams() ? requestParameters += formParams : multiParts.isEmpty() ? requestBody : new byte[0]
      if(method == POST) {
        http.post( path: targetPath, body: bodyContent,
                requestContentType: defineRequestContentType(method),
                contentType: responseContentType) { response, content ->
          if(assertionClosure != null) {
            assertionClosure.call (response, content)
          }
        }
      } else {
        requestBody = bodyContent instanceof Map ? createFormParamBody(bodyContent) : bodyContent
        sendHttpRequest(http, method, responseContentType, targetPath, assertionClosure)
      }
    } else {
      sendHttpRequest(http, method, responseContentType, targetPath, assertionClosure)
    }
    return restAssuredResponse
  }

  private def validateMultiPartForPostOnly(method) {
    if(multiParts.size() > 0 && method != POST) {
        throw new IllegalArgumentException("Sorry, multi part form data is only available for "+POST);
    }
  }

  private def registerRestAssuredEncoders(HTTPBuilder http) {
    // Multipart form-data
    if(multiParts.isEmpty()) {
      return;
    }

    if(hasFormParams()) {
      convertFormParamsToMultiPartParams()
    }
    http.encoder.putAt MULTIPART_FORM_DATA, {
      MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

      multiParts.each {
        def body = it.contentBody
        def name = it.name
        entity.addPart(name, body);
      }

      return entity;
    }
  }

  private def convertFormParamsToMultiPartParams() {
    def allFormParams = requestParameters += formParams
    allFormParams.each {
      multiPart(it.key, it.value)
    }
    requestParameters.clear()
    formParams.clear()
  }

  private def sendHttpRequest(HTTPBuilder http, method, responseContentType, targetPath, assertionClosure) {
    http.request(method, responseContentType) {
      uri.path = targetPath

      setRequestContentType(defineRequestContentTypeAsString(method))

      if (requestBody != null) {
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

  private boolean hasFormParams() {
    return !(requestParameters.isEmpty() && formParams.isEmpty())
  }

  private boolean shouldUrlEncode(method) {
    return POST.equals(method) || formParams.size() > 0
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
        def keyValue = StringUtils.split(it, "=", 2)
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
    if (contentType == null) {
      if(multiParts.size() > 0) {
        contentType = MULTIPART_FORM_DATA
      } else if (requestBody == null) {
        contentType = shouldUrlEncode(method) ? URLENC : ANY
      } else if(multiParts.size() > 0) {

      } else if (requestBody instanceof byte[]) {
        if(method != POST && method != PUT) {
          throw new IllegalStateException("$method doesn't support binary request data.");
        }
        contentType = BINARY
      } else {
        contentType = TEXT
      }
    }
    contentType
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

  private def appendParameters(Map<String, Object> from, Map<String, Object> to) {
    notNull from, "Map to copy from"
    notNull to, "Map to copy to"
    from.each {key, value ->
      appendStandardParameter(to, key, value)
    }
  }

  private def appendListParameter(Map<String, String> to, String key, List<String> value) {
    if(value == null || value.isEmpty()) {
      return;
    }

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


  private def appendStandardParameter(Map<String, Object> to, String key, Object value) {
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

  private def applyPathParamsAndSendRequest(Method method, String path, Object...pathParams) {
    notNull path, "path"
    notNull pathParams, "Path params"

    path = applyPathParamsIfNeeded(path, pathParams)
    invokeFilterChain(path, method, responseSpecification.assertionClosure)
  }

  private def String applyPathParamsIfNeeded(String path, Object... pathParams) {
    def suppliedPathParamSize = pathParams.size()
    def fieldPathParamSize = this.pathParams.size()
    if(suppliedPathParamSize == 0 && fieldPathParamSize == 0) {
      return path
    } else if(suppliedPathParamSize > 0 && fieldPathParamSize > 0) {
      throw new IllegalArgumentException("You cannot specify both named and unnamed path params at the same time")
    } else {
      def matchPattern = ~/.*\{\w+\}.*/
      if (suppliedPathParamSize > 0) {
        def replacePattern = ~/\{\w+\}/
        int current = 0;
        pathParams.each {
          if (!path.matches(matchPattern)) {
            throw new IllegalArgumentException("Illegal number of path parameters. Expected $current, was $suppliedPathParamSize.")
          }
          current++
          path = path.replaceFirst(replacePattern, Matcher.quoteReplacement(it.toString()))
        }
      } else {
        this.pathParams.each { key, value ->
          def literalizedKey = Matcher.quoteReplacement(key)
          def replacePattern = Pattern.compile("\\{$literalizedKey\\}")
          int current = 0;
          if(path.matches(".*\\{$literalizedKey\\}.*")) {
            path = path.replaceFirst(replacePattern, value.toString())
            current++
          } else {
            throw new IllegalArgumentException("You specified too many path parameters ($fieldPathParamSize).")
          }
        }
      }
      if (path.matches(matchPattern)) {
        throw new IllegalArgumentException("You specified too few path parameters to the request.")
      }
    }
    path
  }

  private String createFormParamBody(Map<String, Object> formParams)  {
    final StringBuilder body = new StringBuilder();
    for (Entry<String, Object> entry : formParams.entrySet()) {
      body.append(entry.getKey()).
              append("=").
              append(handleMultiValueParamsIfNeeded(entry)).
              append("&");
    }
    body.deleteCharAt(body.length()-1); //Delete last &
    return body.toString();
  }

  private def handleMultiValueParamsIfNeeded(Entry<String, Object> entry) {
    def value = entry.getValue()
    if (value instanceof List) {
      final StringBuilder multiValueList = new StringBuilder();
      value.each {
        multiValueList.append(it.toString()).append("&")
      }
      multiValueList.deleteCharAt(multiValueList.length()-1); //Delete last &
      value = multiValueList.toString()
    }
    return value
  }

  def void setResponseSpecification(ResponseSpecification responseSpecification) {
    this.responseSpecification = responseSpecification
  }

  String getBaseUri() {
    return baseUri
  }

  String getBasePath() {
    return basePath
  }

  int getPort() {
    return port
  }

  Map<String, String> getRequestParams() {
    return requestParameters
  }

  Map<String, String> getQueryParams() {
    return queryParams
  }

  Map<String, String> getHeaders() {
    return requestHeaders
  }

  Map<String, String> getCookies() {
    return cookies
  }

  def <T> T getBody() {
    return requestBody
  }

  List<Filter> getDefinedFilters() {
    return Collections.unmodifiableList(filters)
  }

  String getRequestContentType() {
    return contentType != null ? contentType instanceof String ? contentType : contentType.toString() : ANY.toString()
  }
}
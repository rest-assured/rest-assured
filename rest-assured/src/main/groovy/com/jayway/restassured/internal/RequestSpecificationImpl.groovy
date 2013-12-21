/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.restassured.internal

import com.jayway.restassured.authentication.AuthenticationScheme
import com.jayway.restassured.authentication.CertAuthScheme
import com.jayway.restassured.authentication.FormAuthScheme
import com.jayway.restassured.authentication.NoAuthScheme
import com.jayway.restassured.config.*
import com.jayway.restassured.filter.Filter
import com.jayway.restassured.http.ContentType
import com.jayway.restassured.internal.filter.FilterContextImpl
import com.jayway.restassured.internal.filter.FormAuthFilter
import com.jayway.restassured.internal.filter.SendRequestFilter
import com.jayway.restassured.internal.http.*
import com.jayway.restassured.internal.mapper.ObjectMapperType
import com.jayway.restassured.internal.mapping.ObjectMapperSerializationContextImpl
import com.jayway.restassured.internal.mapping.ObjectMapping
import com.jayway.restassured.mapper.ObjectMapper
import com.jayway.restassured.parsing.Parser
import com.jayway.restassured.response.*
import com.jayway.restassured.specification.*
import com.jayway.restassured.spi.AuthFilter
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.HttpEntityWrapper
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.impl.client.AbstractHttpClient
import org.apache.http.impl.conn.ProxySelectorRoutePlanner
import org.apache.http.message.BasicHeader

import java.util.Map.Entry
import java.util.regex.Matcher

import static com.jayway.restassured.http.ContentType.*
import static com.jayway.restassured.internal.assertion.AssertParameter.notNull
import static com.jayway.restassured.internal.http.Method.*
import static com.jayway.restassured.internal.serialization.SerializationSupport.isSerializableCandidate
import static java.lang.String.format
import static java.util.Arrays.asList
import static org.apache.commons.lang3.StringUtils.substringAfter
import static org.apache.http.client.params.ClientPNames.*

class RequestSpecificationImpl implements FilterableRequestSpecification, GroovyInterceptable {
  private static final int DEFAULT_HTTP_PORT = 80
  private static final int DEFAULT_HTTP_TEST_PORT = 8080
  private static final String MULTIPART_FORM_DATA = "multipart/form-data"
  private static final String SLASH = "/"
  private static final String CONTENT_TYPE = "content-type"
  private static final String DOUBLE_SLASH = "//"
  private static final String LOCALHOST = "localhost"

  private String baseUri
  private String path = ""
  private String basePath
  private AuthenticationScheme defaultAuthScheme
  private int port
  private Map<String, Object> requestParameters = [:]
  private Map<String, Object> queryParameters = [:]
  private Map<String, Object> formParameters = [:]
  private Map<String, Object> pathParameters = [:]
  private Map<String, Object> httpClientParams = [:]
  def AuthenticationScheme authenticationScheme = new NoAuthScheme()
  private FilterableResponseSpecification responseSpecification;
  private Object contentType;
  private Headers requestHeaders = new Headers([])
  private Cookies cookies = new Cookies([])
  private Object requestBody;
  private List<Filter> filters = [];
  private boolean urlEncodingEnabled
  private RestAssuredConfig restAssuredConfig;
  private List<MultiPartInternal> multiParts = [];

  // This field should be removed once http://jira.codehaus.org/browse/GROOVY-4647 is resolved, merge with sha 9619c3b when it's fixed.
  private AbstractHttpClient httpClient

  public RequestSpecificationImpl(String baseURI, int requestPort, String basePath, AuthenticationScheme defaultAuthScheme,
                                  List<Filter> filters, defaultRequestContentType, RequestSpecification defaultSpec,
                                  boolean urlEncode, RestAssuredConfig restAssuredConfig) {
    notNull(baseURI, "baseURI");
    notNull(basePath, "basePath");
    notNull(defaultAuthScheme, "defaultAuthScheme");
    notNull(filters, "Filters")
    notNull(urlEncode, "URL Encode query params option")
    this.baseUri = baseURI
    this.basePath = basePath
    this.defaultAuthScheme = defaultAuthScheme
    this.filters.addAll(filters)
    this.contentType = defaultRequestContentType
    this.urlEncodingEnabled = urlEncode
    this.restAssuredConfig = restAssuredConfig
    port(requestPort)
    if (defaultSpec != null) {
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

  def Response get(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(GET, path, pathParams)
  }

  def Response post(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(POST, path, pathParams)
  }

  def Response put(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(PUT, path, pathParams)
  }

  def Response delete(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(DELETE, path, pathParams)
  }

  def Response head(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(HEAD, path, pathParams)
  }

  def Response patch(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(PATCH, path, pathParams)
  }

  def Response options(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(OPTIONS, path, pathParams)
  }

  def Response get(URI uri) {
    get(notNull(uri, "URI").toString())
  }

  def Response post(URI uri) {
    post(notNull(uri, "URI").toString())
  }

  def Response put(URI uri) {
    put(notNull(uri, "URI").toString())
  }

  def Response delete(URI uri) {
    delete(notNull(uri, "URI").toString())
  }

  def Response head(URI uri) {
    head(notNull(uri, "URI").toString())
  }

  def Response patch(URI uri) {
    patch(notNull(uri, "URI").toString())
  }

  def Response options(URI uri) {
    options(notNull(uri, "URI").toString())
  }

  def Response get(URL url) {
    get(notNull(url, "URL").toString())
  }

  def Response post(URL url) {
    post(notNull(url, "URL").toString())
  }

  def Response put(URL url) {
    put(notNull(url, "URL").toString())
  }

  def Response delete(URL url) {
    delete(notNull(url, "URL").toString())
  }

  def Response head(URL url) {
    head(notNull(url, "URL").toString())
  }

  def Response patch(URL url) {
    patch(notNull(url, "URL").toString())
  }

  def Response options(URL url) {
    options(notNull(url, "URL").toString())
  }

  def Response get() {
    get("")
  }

  def Response post() {
    post("")
  }

  def Response put() {
    put("")
  }

  def Response delete() {
    delete("")
  }

  def Response head() {
    head("")
  }

  def Response patch() {
    patch("")
  }

  def Response options() {
    options("")
  }

  def Response get(String path, Map pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(GET, path)
  }

  def Response post(String path, Map pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(POST, path)
  }

  def Response put(String path, Map pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(PUT, path)
  }

  def Response delete(String path, Map pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(DELETE, path)
  }

  def Response head(String path, Map pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(HEAD, path)
  }

  def Response patch(String path, Map pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(PATCH, path)
  }

  def Response options(String path, Map pathParams) {
    pathParameters(pathParams)
    applyPathParamsAndSendRequest(OPTIONS, path)
  }

  def RequestSpecification parameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return parameters(MapCreator.createMapFromParams(firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  def RequestSpecification parameters(Map parametersMap) {
    notNull parametersMap, "parametersMap"
    appendParameters(parametersMap, requestParameters)
    return this
  }

  def RequestSpecification params(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    return parameters(firstParameterName, firstParameterValue, parameterNameValuePairs)
  }

  def RequestSpecification params(Map parametersMap) {
    return parameters(parametersMap)
  }

  def RequestSpecification param(String parameterName, Object... parameterValues) {
    return parameter(parameterName, parameterValues)
  }

  def RequestSpecification parameter(String parameterName, Collection<?> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    appendCollectionParameter(requestParameters, parameterName, parameterValues)
    return this
  }

  def RequestSpecification param(String parameterName, Collection<?> parameterValues) {
    return parameter(parameterName, parameterValues)
  }

  def RequestSpecification queryParameter(String parameterName, Collection<?> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    appendCollectionParameter(queryParameters, parameterName, parameterValues)
    return this
  }

  def RequestSpecification queryParam(String parameterName, Collection<?> parameterValues) {
    return queryParameter(parameterName, parameterValues)
  }

  def RequestSpecification parameter(String parameterName, Object... parameterValues) {
    notNull parameterName, "parameterName"
    addZeroToManyParameters(requestParameters, parameterName, parameterValues)
    return this
  }

  def RequestSpecification queryParameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return queryParameters(MapCreator.createMapFromParams(firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  def RequestSpecification queryParameters(Map parametersMap) {
    notNull parametersMap, "parametersMap"
    appendParameters(parametersMap, queryParameters)
    return this
  }

  def RequestSpecification queryParameter(String parameterName, Object... parameterValues) {
    notNull parameterName, "parameterName"
    addZeroToManyParameters(queryParameters, parameterName, parameterValues)
    return this
  }

  def RequestSpecification queryParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    return queryParameters(firstParameterName, firstParameterValue, parameterNameValuePairs);
  }

  def RequestSpecification queryParams(Map parametersMap) {
    return queryParameters(parametersMap)
  }

  def RequestSpecification queryParam(String parameterName, Object... parameterValues) {
    return queryParameter(parameterName, parameterValues)
  }

  def RequestSpecification formParameter(String parameterName, Collection<?> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    appendCollectionParameter(formParameters, parameterName, parameterValues)
    return this
  }

  def RequestSpecification formParam(String parameterName, Collection<?> parameterValues) {
    return formParameter(parameterName, parameterValues)
  }

  def RequestSpecification formParameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return formParameters(MapCreator.createMapFromParams(firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  def RequestSpecification formParameters(Map parametersMap) {
    notNull parametersMap, "parametersMap"
    appendParameters(parametersMap, formParameters)
    return this
  }

  def RequestSpecification formParameter(String parameterName, Object... additionalParameterValues) {
    notNull parameterName, "parameterName"
    addZeroToManyParameters(formParameters, parameterName, additionalParameterValues)
    return this
  }

  def RequestSpecification formParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    return formParameters(firstParameterName, firstParameterValue, parameterNameValuePairs);
  }

  def RequestSpecification formParams(Map parametersMap) {
    return formParameters(parametersMap)
  }

  def RequestSpecification formParam(String parameterName, Object... parameterValues) {
    return formParameter(parameterName, parameterValues)
  }

  def RequestSpecification urlEncodingEnabled(boolean isEnabled) {
    this.urlEncodingEnabled = isEnabled
    return this
  }

  def RequestSpecification pathParameter(String parameterName, Object parameterValue) {
    notNull parameterName, "parameterName"
    notNull parameterValue, "parameterValue"
    appendStandardParameter(pathParameters, parameterName, parameterValue)
    return this
  }

  def RequestSpecification pathParameters(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return pathParameters(MapCreator.createMapFromParams(firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  def RequestSpecification pathParameters(Map parameterNameValuePairs) {
    notNull parameterNameValuePairs, "parameterNameValuePairs"
    appendParameters(parameterNameValuePairs, pathParameters)
    return this
  }

  def RequestSpecification pathParam(String parameterName, Object parameterValue) {
    return pathParameter(parameterName, parameterValue)
  }

  def RequestSpecification pathParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    return pathParameters(firstParameterName, firstParameterValue, parameterNameValuePairs)
  }

  def RequestSpecification pathParams(Map parameterNameValuePairs) {
    return pathParameters(parameterNameValuePairs)
  }

  def RequestSpecification config(RestAssuredConfig config) {
    this.restAssuredConfig = config
    responseSpecification?.config = config
    this
  }

  def RequestSpecification keystore(String pathToJks, String password) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    // Allow all host names in order to be backward compatible
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keystore(pathToJks, password).allowAllHostnames())
    this
  }

  def RequestSpecification keystore(File pathToJks, String password) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    // Allow all host names in order to be backward compatible
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keystore(pathToJks, password).allowAllHostnames())
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

  def RequestSpecification filters(Filter filter, Filter... additionalFilter) {
    notNull filter, "Filter"
    this.filters.add(filter)
    additionalFilter?.each {
      this.filters.add(it)
    }
    return this
  }

  def RequestLogSpecification log() {
    return new RequestLogSpecificationImpl(requestSpecification: this)
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
    if (port < 1) {
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

  def RequestSpecification baseUri(String baseUriparam) {
    this.baseUri = baseUriparam;
    return this;
  }

  def RequestSpecification body(byte[] body) {
    notNull body, "body"
    this.requestBody = body;
    return this;
  }

  def RequestSpecification content(byte[] content) {
    notNull content, "content"
    return body(content);
  }

  def RequestSpecification body(Object object) {
    notNull object, "object"
    if (!isSerializableCandidate(object)) {
      return content(object.toString());
    }

    this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), null, objectMappingConfig());
    this
  }

  def RequestSpecification content(Object object) {
    return body(object)
  }

  def RequestSpecification body(Object object, ObjectMapper mapper) {
    notNull object, "object"
    notNull mapper, "Object mapper"
    def ctx = new ObjectMapperSerializationContextImpl();
    ctx.setObject(object)
    ctx.setCharset(findEncoderCharsetOrReturnDefault(requestContentType))
    ctx.setContentType(requestContentType)
    this.requestBody = mapper.serialize(ctx);
    this
  }

  def RequestSpecification body(Object object, ObjectMapperType mapperType) {
    notNull object, "object"
    notNull mapperType, "Object mapper type"
    this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), mapperType, objectMappingConfig())
    this
  }

  def RequestSpecification content(Object object, ObjectMapper mapper) {
    return body(object, mapper)
  }

  def RequestSpecification content(Object object, ObjectMapperType mapperType) {
    return body(object, mapperType)
  }

  def RequestSpecification contentType(ContentType contentType) {
    notNull contentType, "contentType"
    this.contentType = contentType
    return this
  }

  def RequestSpecification contentType(String contentType) {
    notNull contentType, "contentType"
    this.contentType = contentType
    return this
  }

  def RequestSpecification headers(Map headers) {
    notNull headers, "headers"
    def headerList = []
    if (this.requestHeaders.exist()) {
      headerList.addAll(this.requestHeaders.list())
    }
    headers.each {
      headerList << new Header(it.key, serializeIfNeeded(it.value))
    }
    filterContentTypeHeader(headerList)
    this.requestHeaders = new Headers(headerList)
    return this;
  }

  RequestSpecification headers(Headers headers) {
    notNull headers, "headers"
    if (headers.exist()) {
      def headerList = []
      if (this.requestHeaders.exist()) {
        headerList.addAll(this.requestHeaders.list())
      }

      headerList.addAll(headers.headers.list())
      filterContentTypeHeader(headerList)
      this.requestHeaders = new Headers(headerList)
    }
    this
  }

  private def void filterContentTypeHeader(List<Header> headerList) {
    def contentHeader = headerList.find {
      CONTENT_TYPE.equalsIgnoreCase(it.name)
    };
    if (contentHeader != null) {
      contentType(contentHeader.value)
      headerList.remove(contentHeader)
    }
  }

  RequestSpecification header(String headerName, Object headerValue, Object... additionalHeaderValues) {
    notNull headerName, "Header name"
    notNull headerValue, "Header value"

    if (CONTENT_TYPE.equalsIgnoreCase(headerName)) {
      return contentType(headerValue)
    }

    def headerList = [new Header(headerName, serializeIfNeeded(headerValue))]
    additionalHeaderValues?.each {
      headerList << new Header(headerName, serializeIfNeeded(it))
    }

    return headers(new Headers(headerList))
  }

  def RequestSpecification header(Header header) {
    notNull header, "Header"

    if (CONTENT_TYPE.equalsIgnoreCase(header.name)) {
      return contentType(header.value)
    }

    return headers(new Headers(asList(header)));
  }

  RequestSpecification headers(String firstHeaderName, Object firstHeaderValue, Object... headerNameValuePairs) {
    return headers(MapCreator.createMapFromParams(firstHeaderName, firstHeaderValue, headerNameValuePairs))
  }

  RequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs) {
    return cookies(MapCreator.createMapFromParams(firstCookieName, firstCookieValue, cookieNameValuePairs))
  }

  RequestSpecification cookies(Map cookies) {
    notNull cookies, "cookies"
    def cookieList = []
    if (this.cookies.exist()) {
      cookieList.addAll(this.cookies.list())
    }
    cookies.each {
      cookieList << new Cookie.Builder(it.key, it.value).build();
    }
    this.cookies = new Cookies(cookieList)
    return this;
  }

  def RequestSpecification cookies(Cookies cookies) {
    notNull cookies, "cookies"
    if (cookies.exist()) {
      def cookieList = []
      if (this.cookies.exist()) {
        cookieList.addAll(this.cookies.list())
      }

      cookieList.addAll(cookies.cookies.list())
      this.cookies = new Cookies(cookieList)
    }
    this
  }

  RequestSpecification cookie(String cookieName, Object value, Object... additionalValues) {
    notNull cookieName, "Cookie name"
    def cookieList = [new Cookie.Builder(cookieName, serializeIfNeeded(value)).build()]
    additionalValues?.each {
      cookieList << new Cookie.Builder(cookieName, serializeIfNeeded(it)).build()
    }

    return cookies(new Cookies(cookieList))
  }

  def RequestSpecification cookie(Cookie cookie) {
    notNull cookie, "Cookie"
    return cookies(new Cookies(asList(cookie)));
  }

  def RequestSpecification cookie(String cookieName) {
    cookie(cookieName, null)
  }

  def RedirectSpecification redirects() {
    new RedirectSpecificationImpl(this, httpClientParams)
  }

  RequestSpecification spec(RequestSpecification requestSpecificationToMerge) {
    SpecificationMerger.merge this, requestSpecificationToMerge
    return this
  }

  RequestSpecification specification(RequestSpecification requestSpecificationToMerge) {
    return spec(requestSpecificationToMerge)
  }

  RequestSpecification sessionId(String sessionIdValue) {
    def sessionIdName = config == null ? SessionConfig.DEFAULT_SESSION_ID_NAME : config.getSessionConfig().sessionIdName()
    sessionId(sessionIdName, sessionIdValue)
  }

  RequestSpecification sessionId(String sessionIdName, String sessionIdValue) {
    notNull(sessionIdName, "Session id name")
    notNull(sessionIdValue, "Session id value")
    if (cookies.hasCookieWithName(sessionIdName)) {
      def allOtherCookies = cookies.findAll { !it.getName().equalsIgnoreCase(sessionIdName) }
      allOtherCookies.add(new Cookie.Builder(sessionIdName, sessionIdValue).build());
      this.cookies = new Cookies(allOtherCookies)
    } else {
      cookie(sessionIdName, sessionIdValue)
    }
    this
  }

  def RequestSpecification multiPart(MultiPartSpecification multiPartSpec) {
    notNull multiPartSpec, "Multi-part specification"
    def mimeType = multiPartSpec.mimeType
    def content
    if (multiPartSpec.content instanceof File || multiPartSpec.content instanceof InputStream || multiPartSpec.content instanceof byte[]) {
      content = multiPartSpec.content
    } else {
      // Objects ought to be serialized
      if (mimeType == null) {
        mimeType = requestContentType == ANY ? JSON.toString() : requestContentType
      }
      content = serializeIfNeeded(multiPartSpec.content, mimeType)
    }

    multiParts << new MultiPartInternal(name: multiPartSpec.controlName, content: content, fileName: multiPartSpec.fileName, charset: multiPartSpec.charset, mimeType: mimeType)
    return this
  }

  def RequestSpecification multiPart(String controlName, File file) {
    multiParts << new MultiPartInternal(name: controlName, content: file)
    this
  }

  def RequestSpecification multiPart(File file) {
    multiParts << new MultiPartInternal(name: "file", content: file)
    this
  }

  def RequestSpecification multiPart(String name, File file, String mimeType) {
    multiParts << new MultiPartInternal(name: name, content: file, mimeType: mimeType)
    this
  }

  def RequestSpecification multiPart(String controlName, Object object) {
    def mimeType = requestContentType == ANY ? JSON.toString() : requestContentType
    return multiPart(controlName, object, mimeType)
  }

  def RequestSpecification multiPart(String controlName, Object object, String mimeType) {
    def possiblySerializedObject = serializeIfNeeded(object, mimeType)
    multiParts << new MultiPartInternal(name: controlName, content: possiblySerializedObject, mimeType: mimeType)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, byte[] bytes) {
    multiParts << new MultiPartInternal(name: name, content: bytes, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, byte[] bytes, String mimeType) {
    multiParts << new MultiPartInternal(name: name, content: bytes, mimeType: mimeType, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, InputStream stream) {
    multiParts << new MultiPartInternal(name: name, content: stream, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String fileName, InputStream stream, String mimeType) {
    multiParts << new MultiPartInternal(name: name, content: stream, mimeType: mimeType, fileName: fileName)
    this
  }

  def RequestSpecification multiPart(String name, String contentBody) {
    multiParts << new MultiPartInternal(name: name, content: contentBody)
    this
  }

  def RequestSpecification multiPart(String name, NoParameterValue contentBody) {
    multiParts << new MultiPartInternal(name: name, content: contentBody)
    this
  }

  def RequestSpecification multiPart(String name, String contentBody, String mimeType) {
    multiParts << new MultiPartInternal(name: name, content: contentBody, mimeType: mimeType)
    this
  }

  def invokeFilterChain(path, method, assertionClosure) {
    if (authenticationScheme instanceof NoAuthScheme && !(defaultAuthScheme instanceof NoAuthScheme)) {
      // Use default auth scheme
      authenticationScheme = defaultAuthScheme
    }

    if (authenticationScheme instanceof FormAuthScheme) {
      // Form auth scheme is handled a bit differently than other auth schemes since it's implemented by a filter.
      def formAuthScheme = authenticationScheme as FormAuthScheme
      filters.removeAll { AuthFilter.class.isAssignableFrom(it.getClass()) }
      filters.add(0, new FormAuthFilter(userName: formAuthScheme.userName, password: formAuthScheme.password, formAuthConfig: formAuthScheme.config, sessionConfig: sessionConfig()))
    }

    filters << new SendRequestFilter()

    String requestUriForLogging = generateRequestUriToLog(path, method)
    restAssuredConfig = config ?: new RestAssuredConfig()
    def ctx = new FilterContextImpl(requestUriForLogging, path, method, assertionClosure, filters);
    // We pass in this here because of a bug in the Groovy compiler, http://jira.codehaus.org/browse/GROOVY-4647 (when it's fixed 9619c3b should be used instead)
    httpClient = httpClientConfig().httpClientInstance()
    def response = ctx.next(this, responseSpecification)
    responseSpecification.assertionClosure.validate(response)
    return response;
  }

  private def String generateRequestUriToLog(path, method) {
    def targetPath
    def allQueryParams = [:]

    if (path.contains("?")) {
      def pathWithoutQueryParams = StringUtils.substringBefore(getTargetPath(path), "?");
      def queryParamsDefinedInPath = substringAfter(path, "?")

      targetPath = pathWithoutQueryParams

      // Add query parameters defined in path to the allQueryParams map
      if (!StringUtils.isBlank(queryParamsDefinedInPath)) {
        def splittedQueryParams = StringUtils.split(queryParamsDefinedInPath, "&");
        splittedQueryParams.each { queryNameWithPotentialValue ->
          def String[] splitted = StringUtils.split(queryNameWithPotentialValue, "=")
          def queryParamHasValue = splitted.size() > 1
          if (queryParamHasValue) {
            def value = StringUtils.join(splitted[1..splitted.size() - 1], "="); // If there are several "=" in the URL the merge them
            allQueryParams.put(splitted[0], value)
          } else {
            allQueryParams.put(splitted[0], new NoParameterValue());
          }
        }
      }
    } else {
      targetPath = path
    }

    def uri = URIBuilder.convertToURI(assembleCompleteTargetPath(targetPath))
    def uriBuilder = new URIBuilder(uri, this.urlEncodingEnabled, encoderConfig())

    if (method != POST && !requestParameters?.isEmpty()) {
      allQueryParams << requestParameters
    }

    if (!queryParameters?.isEmpty()) {
      allQueryParams << queryParameters
    }

    if (!allQueryParams.isEmpty()) {
      uriBuilder.addQueryParams(allQueryParams)
    }

    def requestUriForLogging = uriBuilder.toString()
    requestUriForLogging
  }

  private def Response sendRequest(path, method, assertionClosure, FilterableRequestSpecification requestSpecification) {
    path = extractRequestParamsIfNeeded(path);
    def isFullyQualifiedUri = isFullyQualified(path)
    def targetUri = getTargetURI(path);
    def targetPath = getTargetPath(path)

    if (!requestSpecification.getHttpClient() instanceof AbstractHttpClient) {
      throw new IllegalStateException(format("Unfortunately Rest Assured only supports Http Client instances of type %s.", AbstractHttpClient.class.getName()));
    }

    def http = new RestAssuredHttpBuilder(targetUri, assertionClosure, urlEncodingEnabled, config, requestSpecification.getHttpClient() as AbstractHttpClient);
    allowJreProxySettings(http)
    applyRestAssuredConfig(http)
    registerRestAssuredEncoders(http);
    setRequestHeadersToHttpBuilder(http)

    if (cookies.exist()) {
      http.getHeaders() << [Cookie: cookies.collect { it.toString() }.join("; ")]
    }

    // Allow returning a the response
    def restAssuredResponse = new RestAssuredResponseImpl()
    def RestAssuredConfig cfg = config ?: new RestAssuredConfig();
    restAssuredResponse.setSessionIdName(cfg.getSessionConfig().sessionIdName())
    restAssuredResponse.setDefaultCharset(cfg.getDecoderConfig().defaultContentCharset())
    restAssuredResponse.setConnectionManager(http.client.connectionManager)
    restAssuredResponse.setConfig(cfg)
    responseSpecification.restAssuredResponse = restAssuredResponse
    def responseContentType = assertionClosure.getResponseContentType()

    if (shouldApplySSLConfig(http, cfg)) {
      def sslConfig = cfg.getSSLConfig();
      new CertAuthScheme(pathToKeyStore: sslConfig.getPathToKeyStore(), password: sslConfig.getPassword(),
              keystoreType: sslConfig.getKeyStoreType(), port: sslConfig.getPort(), trustStore: sslConfig.getTrustStore(),
              x509HostnameVerifier: sslConfig.getX509HostnameVerifier()).authenticate(http)
    }

    authenticationScheme.authenticate(http)

    validateMultiPartForPostPutAndPatchOnly(method);

    if (mayHaveBody(method)) {
      if (hasFormParams() && requestBody != null) {
        throw new IllegalStateException("You can either send form parameters OR body content in $method, not both!");
      }
      def bodyContent = createBodyContent(assembleBodyContent(method))
      if (method == POST) {
        http.post(path: targetPath, body: bodyContent,
                requestContentType: defineRequestContentType(method),
                contentType: responseContentType) { response, content ->
          if (assertionClosure != null) {
            assertionClosure.call(response, content)
          }
        }
      } else if (method == PATCH) {
        http.patch(path: targetPath, body: bodyContent,
                requestContentType: defineRequestContentType(method),
                contentType: responseContentType) { response, content ->
          if (assertionClosure != null) {
            assertionClosure.call(response, content)
          }
        }
      } else {
        requestBody = bodyContent
        sendHttpRequest(http, method, responseContentType, targetPath, assertionClosure)
      }
    } else {
      sendHttpRequest(http, method, responseContentType, targetPath, assertionClosure)
    }
    return restAssuredResponse
  }

  def boolean shouldApplySSLConfig(http, RestAssuredConfig cfg) {
    URI uri = ((URIBuilder) http.getUri()).toURI();
    if (uri == null) throw new IllegalStateException("a default URI must be set");
    uri.getScheme()?.toLowerCase() == "https" && cfg.getSSLConfig().isUserConfigured() && !(authenticationScheme instanceof CertAuthScheme)
  }

  def applyRestAssuredConfig(HTTPBuilder http) {
    // Decoder config should always be applied regardless if restAssuredConfig is null or not because
    // by default we should support GZIP and DEFLATE decoding.
    applyContentDecoders(http, (restAssuredConfig?.getDecoderConfig() ?: new DecoderConfig()).contentDecoders());
    if (restAssuredConfig != null) {
      applyRedirectConfig(restAssuredConfig.getRedirectConfig())
      applyHttpClientConfig(restAssuredConfig.getHttpClientConfig())
      applyEncoderConfig(http, restAssuredConfig.getEncoderConfig())
      applySessionConfig(restAssuredConfig.getSessionConfig())
    }
    if (!httpClientParams.isEmpty()) {
      def p = http.client.getParams();

      httpClientParams.each { key, value ->
        p.setParameter(key, value)
      }
    }
  }

  private def applyContentDecoders(HTTPBuilder httpBuilder, List<DecoderConfig.ContentDecoder> contentDecoders) {
    def httpBuilderContentEncoders = contentDecoders.collect { contentDecoder -> ContentEncoding.Type.valueOf(contentDecoder.toString()) }.toArray()
    httpBuilder.setContentEncoding(httpBuilderContentEncoders)
  }

  def applySessionConfig(SessionConfig sessionConfig) {
    if (sessionConfig.isSessionIdValueDefined() && !cookies.hasCookieWithName(sessionConfig.sessionIdName())) {
      cookie(sessionConfig.sessionIdName(), sessionConfig.sessionIdValue())
    }
  }

  def applyEncoderConfig(HTTPBuilder httpBuilder, EncoderConfig encoderConfig) {
    httpBuilder.encoders.setCharset(encoderConfig.defaultContentCharset())
  }

  def applyHttpClientConfig(HttpClientConfig httpClientConfig) {
    ([:].plus(httpClientConfig.params())).each { key, value ->
      putIfAbsent(httpClientParams, key, value)
    }
  }

  def applyRedirectConfig(RedirectConfig redirectConfig) {
    putIfAbsent(httpClientParams, ALLOW_CIRCULAR_REDIRECTS, redirectConfig.allowsCircularRedirects())
    putIfAbsent(httpClientParams, HANDLE_REDIRECTS, redirectConfig.followsRedirects())
    putIfAbsent(httpClientParams, MAX_REDIRECTS, redirectConfig.maxRedirects())
    putIfAbsent(httpClientParams, REJECT_RELATIVE_REDIRECT, redirectConfig.rejectRelativeRedirects())
  }

  private def putIfAbsent(Map map, key, value) {
    if (!map.containsKey(key)) {
      map.put(key, value)
    }
  }

  def assembleBodyContent(httpMethod) {
    if (hasFormParams()) {
      httpMethod == Method.POST ? requestParameters += formParameters : formParameters
    } else if (multiParts.isEmpty()) {
      requestBody
    } else {
      new byte[0]
    }
  }

  def setRequestHeadersToHttpBuilder(HTTPBuilder http) {
    def httpHeaders = http.getHeaders();
    requestHeaders.each { header ->
      def headerName = header.getName()
      def headerValue = header.getValue()
      if (httpHeaders.containsKey(headerName)) {
        def values = [httpHeaders.get(headerName)];
        values << headerValue
        def headerVal = values.flatten()
        httpHeaders.put(headerName, headerVal)
      } else {
        httpHeaders.put(headerName, headerValue)
      }
    }
  }

  private def createBodyContent(bodyContent) {
    return bodyContent instanceof Map ? createFormParamBody(bodyContent) : bodyContent
  }

  private String getTargetPath(String path) {
    if (isFullyQualified(path)) {
      return new URL(path).getPath()
    }

    def baseUriPath = ""
    if (!(baseUri == null || baseUri == "")) {
      def uri = new URI(baseUri)
      baseUriPath = uri.getPath()
    }
    return mergeAndRemoveDoubleSlash(mergeAndRemoveDoubleSlash(baseUriPath, basePath), path)
  }

  private def validateMultiPartForPostPutAndPatchOnly(method) {
    if (multiParts.size() > 0 && method != POST && method != PUT && method != PATCH) {
      throw new IllegalArgumentException("Sorry, multi part form data is only available for POST, PUT and PATCH.");
    }
  }

  private def registerRestAssuredEncoders(HTTPBuilder http) {
    // Multipart form-data
    if (multiParts.isEmpty()) {
      return;
    }

    if (hasFormParams()) {
      convertFormParamsToMultiPartParams()
    }
    http.encoders.putAt MULTIPART_FORM_DATA, { contentType, content ->
      // TODO Add charset
      MultipartEntity entity = new MultipartEntity(httpClientConfig().httpMultipartMode());

      multiParts.each {
        def body = it.contentBody
        def name = it.name
        entity.addPart(name, body);
      }

      return entity;
    }
  }

  private def convertFormParamsToMultiPartParams() {
    def allFormParams = requestParameters += formParameters
    allFormParams.each {
      multiPart(it.key, it.value)
    }
    requestParameters.clear()
    formParameters.clear()
  }

  private def sendHttpRequest(HTTPBuilder http, method, responseContentType, targetPath, assertionClosure) {
    def allQueryParams = requestParameters += queryParameters
    http.request(method, responseContentType) {
      uri.path = targetPath

      setRequestContentType(defineRequestContentTypeAsString(method))

      if (requestBody != null) {
        body = requestBody
      }

      uri.query = allQueryParams

      Closure closure = assertionClosure.getClosure()
      // response handler for a success response code:
      response.success = closure

      // handler for any failure status code:
      response.failure = closure
    }
  }

  private boolean hasFormParams() {
    return !(requestParameters.isEmpty() && formParameters.isEmpty())
  }

  private boolean mayHaveBody(method) {
    return POST.equals(method) || formParameters.size() > 0 || multiParts.size() > 0
  }

  private boolean isFullyQualified(String targetUri) {
    if (StringUtils.isBlank(targetUri)) {
      return false
    }

    def indexOfFirstSlash = targetUri.indexOf("/");
    def indexOfScheme = targetUri.indexOf("://");
    if (indexOfScheme == -1) {
      // If we didn't find a single :// in the path then we know that the targetUri is not fully-qualified
      return false
    }
    return indexOfScheme < indexOfFirstSlash
  }

  private String extractRequestParamsIfNeeded(String path) {
    if (path.contains("?")) {
      def indexOfQuestionMark = path.indexOf("?")
      String allParamAsString = path.substring(indexOfQuestionMark + 1);
      def keyValueParams = allParamAsString.split("&");
      keyValueParams.each {
        def keyValue = StringUtils.split(it, "=", 2)
        def theKey;
        def theValue;
        if (keyValue.length < 1 || keyValue.length > 2) {
          throw new IllegalArgumentException("Illegal parameters passed to REST Assured. Parameters was: $keyValueParams")
        } else if (keyValue.length == 1) {
          theKey = keyValue[0]
          theValue = new NoParameterValue();
        } else {
          theKey = keyValue[0]
          theValue = keyValue[1]
        }
        queryParam(theKey, theValue)
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
      if (multiParts.size() > 0) {
        contentType = MULTIPART_FORM_DATA
      } else if (requestBody == null) {
        contentType = mayHaveBody(method) ? URLENC : ANY
      } else if (requestBody instanceof byte[]) {
        if (method != POST && method != PUT && method != DELETE && method != PATCH) {
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
    if (port <= 0) {
      throw new IllegalArgumentException("Port must be greater than 0")
    }
    def uri
    def pathHasScheme = isFullyQualified(path)
    if (pathHasScheme) {
      def url = new URL(path)
      uri = getTargetUriFromUrl(url, PathType.EXPLICIT)
    } else if (isFullyQualified(baseUri)) {
      def baseUriAsUrl = new URL(baseUri)
      uri = getTargetUriFromUrl(baseUriAsUrl, PathType.BASE_URI)
    } else {
      uri = "$baseUri:$port"
    }
    return uri
  }

  private String getTargetUriFromUrl(URL url, PathType pathType) {
    def builder = new StringBuilder();
    def protocol = url.getProtocol()
    def boolean useDefaultHttps = false
    if (port == DEFAULT_HTTP_TEST_PORT && protocol.equalsIgnoreCase("https")) {
      useDefaultHttps = true
    }

    builder.append(protocol)
    builder.append("://")
    builder.append(url.getAuthority())

    if (!hasPortDefined(url) && !useDefaultHttps && ((port != DEFAULT_HTTP_PORT && port != DEFAULT_HTTP_TEST_PORT)
            || pathType == PathType.BASE_URI || hasAuthorityEqualToLocalhost(url))) {
      builder.append(":")
      builder.append(port)
    }
    return builder.toString()
  }

  private def boolean hasAuthorityEqualToLocalhost(uri) {
    uri.getAuthority().trim().equalsIgnoreCase(LOCALHOST)
  }

  private def boolean hasPortDefined(uri) {
    return uri.getPort() != -1;
  }

  private def appendParameters(Map<String, Object> from, Map<String, Object> to) {
    notNull from, "Map to copy from"
    notNull to, "Map to copy to"
    from.each { key, value ->
      appendStandardParameter(to, key, value)
    }
  }

  private def appendCollectionParameter(Map<String, String> to, String key, Collection<Object> values) {
    if (values == null || values.isEmpty()) {
      to.put(key, new NoParameterValue())
      return;
    }

    def convertedValues = values.collect { serializeIfNeeded(it) }
    if (to.containsKey(key)) {
      def currentValue = to.get(key)
      if (currentValue instanceof Collection) {
        currentValue.addAll(convertedValues)
      } else {
        to.put(key, [currentValue, convertedValues].flatten())
      }
    } else {
      to.put(key, new LinkedList<Object>(convertedValues))
    }
  }

  private def appendStandardParameter(Map<String, Object> to, String key) {
    appendStandardParameter(to, key, null)
  }

  private def appendStandardParameter(Map<String, Object> to, String key, Object value) {
    if (value == null) {
      to.put(key, new NoParameterValue())
      return;
    }
    def newValue = serializeIfNeeded(value)
    if (to.containsKey(key)) {
      def currentValue = to.get(key)
      if (currentValue instanceof List) {
        currentValue << newValue
      } else {
        to.put(key, [currentValue, newValue])
      }
    } else {
      to.put(key, newValue)
    }
  }

  private def serializeIfNeeded(Object object) {
    serializeIfNeeded(object, requestContentType)
  }

  private def serializeIfNeeded(Object object, contentType) {
    isSerializableCandidate(object) ? ObjectMapping.serialize(object, contentType, findEncoderCharsetOrReturnDefault(contentType), null, objectMappingConfig()) : object
  }

  private def applyPathParamsAndSendRequest(Method method, String path, Object... pathParams) {
    notNull path, "path"
    notNull pathParams, "Path params"

    if (path?.endsWith("?")) {
      throw new IllegalArgumentException("Request URI cannot end with ?");
    }
    path = applyPathParamsAndEncodePath(path, pathParams)
    invokeFilterChain(path, method, responseSpecification.assertionClosure)
  }

  private def String applyPathParamsAndEncodePath(String path, Object... unnamedPathParams) {
    def unnamedPathParamSize = unnamedPathParams.size()
    def namedPathParamSize = this.pathParameters.size()
    def namedPathParams = this.pathParameters
    if (unnamedPathParamSize > 0 && namedPathParamSize > 0) {
      throw new IllegalArgumentException("You cannot specify both named and unnamed path params at the same time")
    } else {
      def host = getTargetURI(path)
      def pathWithoutQueryParams = StringUtils.substringBefore(getTargetPath(path), "?");
      def shouldAppendSlashAfterEncoding = pathWithoutQueryParams.endsWith("/")
      // The last slash is removed later so we may need to add it again
      def queryParams = substringAfter(path, "?")
      def usesNamedPathParameters = namedPathParamSize > unnamedPathParamSize

      int numberOfUsedPathParameters = 0;
      def pathParamNameUsageCount = [:].withDefault { 0 }

      def pathTemplate = ~/.*\{\w+\}.*/
      // If a path fragment contains double slash we need to replace it with something else to not mess up the path

      def hasPathParameterWithDoubleSlash = StringUtils.indexOf(pathWithoutQueryParams, DOUBLE_SLASH) != -1

      def tempParams;
      if (hasPathParameterWithDoubleSlash) {
        tempParams = StringUtils.replace(pathWithoutQueryParams, DOUBLE_SLASH, "RA_double_slash__");
      } else {
        tempParams = pathWithoutQueryParams
      }

      pathWithoutQueryParams = StringUtils.split(tempParams, "/").inject("") { String acc, String subresource ->
        if (subresource.startsWith("{") && subresource.endsWith("}") && subresource.length() >= 3) {
          // 3 means "{" and "}" and at least one character
          if (usesNamedPathParameters) {
            def pathParamName = subresource.substring(1, subresource.length() - 1)
            // Get path parameter name, what's between the "{" and "}"
            def pathParamValue = findNamedPathParamValue(pathParamName, pathParamNameUsageCount)
            subresource = pathParamValue
          } else { // uses unnamed path params
            if (numberOfUsedPathParameters >= unnamedPathParams.size()) {
              throw new IllegalArgumentException("You specified too few path parameters in the request.")
            }
            subresource = unnamedPathParams[numberOfUsedPathParameters].toString()
          }
          numberOfUsedPathParameters += 1
        }

        format("%s/%s", acc, encode(subresource, EncodingTarget.QUERY)).toString()
      }

      if (hasPathParameterWithDoubleSlash) {
        // Now get the double slash replacement back to normal double slashes
        pathWithoutQueryParams = StringUtils.replace(pathWithoutQueryParams, "RA_double_slash__", encode(DOUBLE_SLASH, EncodingTarget.QUERY))
      }

      if (shouldAppendSlashAfterEncoding) {
        pathWithoutQueryParams += "/"
      }

      // Remove used unnamed path parameters if all parameters haven't already been used
      if (!usesNamedPathParameters && unnamedPathParamSize != numberOfUsedPathParameters) {
        def firstUnusedIndex = Math.max(0, numberOfUsedPathParameters)
        def lastIndex = unnamedPathParams.size() - 1
        unnamedPathParams = unnamedPathParams[firstUnusedIndex..lastIndex]
      }

      if (queryParams.matches(pathTemplate)) {
        def hasAnyTemplateLeft = ~/.*\{\w+\}.*/
        def replacePattern = ~/\{\w+\}/
        def definedParams
        if (usesNamedPathParameters) {
          definedParams = namedPathParams.keySet()
        } else {
          definedParams = unnamedPathParams
        }

        def originalQueryParams = queryParams
        definedParams.eachWithIndex { pathParamName, index ->
          def subresource
          if (!queryParams.matches(hasAnyTemplateLeft)) {
            def expected = hasAnyTemplateLeft.matcher(originalQueryParams).getCount();
            throw new IllegalArgumentException("Illegal number of path parameters. Expected $expected, was $unnamedPathParamSize.")
          }
          if (usesNamedPathParameters) {
            def pathParamValue = findNamedPathParamValue(pathParamName, pathParamNameUsageCount)
            subresource = pathParamValue
          } else { // uses unnamed path params
            subresource = unnamedPathParams[index].toString()
          }
          // Note that we do NOT url encode query params here, that happens by UriBuilder at a later stage.
          def replacement = Matcher.quoteReplacement(subresource.toString())
          queryParams = queryParams.replaceFirst(replacePattern, replacement)
          numberOfUsedPathParameters += 1;
        }

        if (queryParams.matches(hasAnyTemplateLeft)) {
          def expected = hasAnyTemplateLeft.matcher(originalQueryParams).getCount();
          throw new IllegalArgumentException("Illegal number of path parameters. Expected $expected, was $unnamedPathParamSize.")
        }
      }

      path = host + (StringUtils.isEmpty(queryParams) ? pathWithoutQueryParams : pathWithoutQueryParams + "?" + queryParams)
      def expectedNumberOfUsedPathParameters = usesNamedPathParameters ? namedPathParamSize : unnamedPathParamSize
      if (numberOfUsedPathParameters != expectedNumberOfUsedPathParameters) {
        if (usesNamedPathParameters && expectedNumberOfUsedPathParameters < numberOfUsedPathParameters && !containsUnresolvedTemplates(path)) {
          /* If we're using named path parameters but multiple templates, e.g. given().pathParam("x", 1).get("/{x}/{x}") then it's ok that
           * expectedNumberOfUsedPathParameters < numberOfUsedPathParameters there's no unresolved templates left.
           */
        } else {
          throw new IllegalArgumentException("Illegal number of path parameters. Expected $numberOfUsedPathParameters, was $expectedNumberOfUsedPathParameters.")
        }
      }
    }
    path
  }

  private def boolean containsUnresolvedTemplates(String path) {
    path.matches(~/.*\{\w+\}.*/)
  }

  private def String findNamedPathParamValue(String pathParamName, pathParamNameUsageCount) {
    def pathParamValues = this.pathParameters.get(pathParamName);
    def pathParamValue
    if (pathParamValues instanceof Collection) {
      def pathParamCount = pathParamNameUsageCount[pathParamName]
      pathParamNameUsageCount[pathParamName] = pathParamCount++
      pathParamValue = pathParamValues.get(pathParamCount)
    } else {
      pathParamValue = pathParamValues
    }
    if (StringUtils.isEmpty(pathParamValue?.toString())) {
      throw new IllegalArgumentException("You specified too few path parameters to the request, failed to find path parameter with name '$pathParamName'.");
    }
    pathParamValue
  }

  private String createFormParamBody(Map<String, Object> formParams) {
    final StringBuilder body = new StringBuilder();
    for (Entry<String, Object> entry : formParams.entrySet()) {
      body.append(encode(entry.getKey(), EncodingTarget.BODY));
      if (!(entry.getValue() instanceof NoParameterValue)) {
        body.append("=").append(handleMultiValueParamsIfNeeded(entry));
      }
      body.append("&");
    }
    body.deleteCharAt(body.length() - 1); //Delete last &
    return body.toString();
  }


  private def String encode(Object string, EncodingTarget encodingType) {
    string = string.toString()
    if (urlEncodingEnabled) {
      def charset
      if (encodingType == EncodingTarget.BODY) {
        charset = encoderConfig().defaultContentCharset()
        if (contentType instanceof String) {
          def tempCharset = CharsetExtractor.getCharsetFromContentType(contentType as String)
          if (tempCharset != null) {
            charset = tempCharset
          }
        }
      } else { // Query or path parameter
        charset = encoderConfig().defaultQueryParameterCharset()
      }
      return URIBuilder.encode(string, charset)
    } else {
      return string
    }
  }

  private def handleMultiValueParamsIfNeeded(Entry<String, Object> entry) {
    def value = entry.getValue()
    if (value instanceof List) {
      def key = encode(entry.getKey(), EncodingTarget.BODY)
      final StringBuilder multiValueList = new StringBuilder();
      value.eachWithIndex { val, index ->
        multiValueList.append(encode(val.toString(), EncodingTarget.BODY))
        if (index != value.size() - 1) {
          multiValueList.append("&").append(key).append("=")
        }
      }
      value = multiValueList.toString()
    } else {
      value = encode(value, EncodingTarget.BODY)
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

  def Map<String, Object> getFormParams() {
    return Collections.unmodifiableMap(formParameters)
  }

  def Map<String, Object> getPathParams() {
    return Collections.unmodifiableMap(pathParameters)
  }

  Map<String, Object> getRequestParams() {
    return Collections.unmodifiableMap(requestParameters)
  }

  Map<String, Object> getQueryParams() {
    return Collections.unmodifiableMap(queryParameters)
  }

  Headers getHeaders() {
    return requestHeaders
  }

  Cookies getCookies() {
    return cookies
  }

  def <T> T getBody() {
    return requestBody
  }

  List<Filter> getDefinedFilters() {
    return Collections.unmodifiableList(filters)
  }

  def RestAssuredConfig getConfig() {
    return restAssuredConfig
  }

  def HttpClient getHttpClient() {
    return httpClient
    // @Delegate doesn't work because of http://jira.codehaus.org/browse/GROOVY-4647 (when it's fixed 9619c3b should be used instead)
  }

  String getRequestContentType() {
    return contentType != null ? contentType instanceof String ? contentType : contentType.toString() : ANY.toString()
  }

  def RequestSpecification noFilters() {
    this.filters.clear()
    this
  }

  def <T extends Filter> RequestSpecification noFiltersOfType(Class<T> filterType) {
    notNull filterType, "Filter type"
    this.filters.removeAll { filterType.isAssignableFrom(it.getClass()) }
    this
  }

  def private String mergeAndRemoveDoubleSlash(String thisOne, String otherOne) {
    thisOne = thisOne.trim()
    otherOne = otherOne.trim()
    final boolean otherOneStartsWithSlash = otherOne.startsWith(SLASH);
    final boolean thisOneEndsWithSlash = thisOne.endsWith(SLASH)
    if (thisOneEndsWithSlash && otherOneStartsWithSlash) {
      return thisOne + substringAfter(otherOne, SLASH);
    } else if (thisOneEndsWithSlash && isFullyQualified(otherOne)) {
      thisOne = ""
    } else if (!thisOneEndsWithSlash && !otherOneStartsWithSlash && !(otherOne == "" ^ thisOne == "")) {
      return "$thisOne/$otherOne"
    }
    return thisOne + otherOne;
  }

  private class RestAssuredHttpBuilder extends HTTPBuilder {
    def assertionClosure

    RestAssuredHttpBuilder(Object defaultURI, assertionClosure, boolean urlEncodingEnabled, RestAssuredConfig config, AbstractHttpClient client) throws URISyntaxException {
      super(defaultURI, urlEncodingEnabled, config?.getEncoderConfig(), client)
      this.assertionClosure = assertionClosure
    }

    /**
     * A copy of HTTP builders doRequest method with two exceptions.
     * <ol>
     *  <li>The exception is that the entity's content is not closed if no body matchers are specified.</li>
     *  <li>If headers contain a list of elements the headers are added and not overridden</li>
     *  </ol>
     */
    protected Object doRequest(HTTPBuilder.RequestConfigDelegate delegate) {
      if (delegate.getRequest() instanceof HttpPost) {
        if (assertionClosure != null) {
          delegate.getResponse().put(
                  Status.FAILURE.toString(), { response, content ->
            assertionClosure.call(response, content)
          });
        }
        delegate.uri.query = queryParameters
      }
      final HttpRequestBase reqMethod = delegate.getRequest()
      Object contentType1 = delegate.getContentType()
      String acceptContentTypes = contentType1.toString()
      if (contentType1 instanceof ContentType)
        acceptContentTypes = ((ContentType) contentType1).getAcceptHeader()
      reqMethod.setHeader("Accept", acceptContentTypes)
      reqMethod.setURI(delegate.getUri().toURI())
      if (reqMethod.getURI() == null)
        throw new IllegalStateException("Request URI cannot be null")
      Map<?, ?> headers1 = delegate.getHeaders()
      for (Object key : headers1.keySet()) {
        Object val = headers1.get(key);
        if (key == null) continue;
        if (val == null) {
          reqMethod.removeHeaders(key.toString())
        } else {
          def keyAsString = key.toString()
          if (val instanceof Collection) {
            val = val.flatten().collect { it?.toString() }
            val.each {
              reqMethod.addHeader(keyAsString, it)
            }
          } else {
            reqMethod.setHeader(keyAsString, val.toString());
          }
        }
      }
      final HttpResponseDecorator resp = new HttpResponseDecorator(
              this.client.execute(reqMethod, delegate.getContext()),
              delegate.getContext(), null)
      try {
        int status = resp.getStatusLine().getStatusCode();
        Closure responseClosure = delegate.findResponseHandler(status);

        final Object returnVal;
        Object[] closureArgs = null;
        switch (responseClosure.getMaximumNumberOfParameters()) {
          case 1:
            returnVal = responseClosure.call(resp);
            break;
          case 2: // parse the response entity if the response handler expects it:
            HttpEntity entity = resp.getEntity();
            try {
              if (entity == null || entity.getContentLength() == 0) {
                returnVal = responseClosure.call(resp, null);
              } else {
                returnVal = responseClosure.call(resp, this.parseResponse(resp, contentType1));
              }
            } catch (Exception ex) {
              org.apache.http.Header h = entity.getContentType();
              String respContentType = h != null ? h.getValue() : null;
              throw new ResponseParseException(resp, ex);
            }
            break;
          default:
            throw new IllegalArgumentException(
                    "Response closure must accept one or two parameters");
        }
        return returnVal;
      }
      finally {
        if (responseSpecification.hasBodyAssertionsDefined()) {
          HttpEntity entity = resp.getEntity();
          if (entity != null) entity.consumeContent()
        }
        // Close idle connections to the server
        def connectionConfig = connectionConfig()
        if (connectionConfig.shouldCloseIdleConnectionsAfterEachResponse()) {
          def closeConnectionConfig = connectionConfig.closeIdleConnectionConfig()
          client.getConnectionManager().closeIdleConnections(closeConnectionConfig.getIdleTime(), closeConnectionConfig.getTimeUnit());
        }
      }
    }

    /**
     * We override this method because ParserRegistry.getContentType(..) called by
     * the super method throws an exception if no content-type is available in the response
     * and then HTTPBuilder for some reason uses the streaming octet parser instead of the
     * defaultParser in the ParserRegistry to parse the response. To fix this we set the
     * content-type of the defaultParser if registered to Rest Assured to the response if no
     * content-type is defined.
     */
    protected Object parseResponse(HttpResponse resp, Object contentType) {
      def Parser definedDefaultParser = responseSpecification.rpr.defaultParser
      if (definedDefaultParser != null && ANY.toString().equals(contentType.toString())) {
        try {
          HttpResponseContentTypeFinder.findContentType(resp);
        } catch (IllegalArgumentException e) {
          // This means that no content-type is defined the response
          def entity = resp?.entity
          if (entity != null) {
            resp.entity = new HttpEntityWrapper(entity) {

              org.apache.http.Header getContentType() {
                // We don't use CONTENT_TYPE field because of issue 253 (no tests for this!)
                return new BasicHeader("Content-Type", definedDefaultParser.getContentType())
              }
            }
          }
        }
      }
      return super.parseResponse(resp, contentType)
    }
  }

  private def addZeroToManyParameters(Map<String, Object> to, String parameterName, Object... parameterValues) {
    if (isEmpty(parameterValues)) {
      appendStandardParameter(to, parameterName)
    } else if (parameterValues.length == 1) {
      appendStandardParameter(to, parameterName, parameterValues[0])
    } else {
      appendCollectionParameter(to, parameterName, asList(parameterValues))
    }
  }

  private boolean isEmpty(Object[] objects) {
    return objects == null || objects.length == 0 || (objects.length == 1 && objects[0] instanceof NoParameterValue)
  }

  // make client aware of JRE proxy settings http://freeside.co/betamax/
  private def allowJreProxySettings(RestAssuredHttpBuilder http) {
    http.client.routePlanner = new ProxySelectorRoutePlanner(http.client.connectionManager.schemeRegistry, ProxySelector.default)
  }

  private def String assembleCompleteTargetPath(requestPath) {
    def targetUri = getTargetURI(path)
    def targetPath = getTargetPath(path)
    if (isFullyQualified(requestPath)) {
      targetUri = ""
      targetPath = ""
    }
    return mergeAndRemoveDoubleSlash(mergeAndRemoveDoubleSlash(targetUri, targetPath), requestPath);
  }

  private def String findEncoderCharsetOrReturnDefault(String contentType) {
    def charset = CharsetExtractor.getCharsetFromContentType(contentType)
    if (charset == null) {
      charset = config == null ? new EncoderConfig().defaultContentCharset() : config.getEncoderConfig().defaultContentCharset()
    }
    charset
  }

  private def ObjectMapperConfig objectMappingConfig() {
    return config == null ? ObjectMapperConfig.objectMapperConfig() : config.getObjectMapperConfig();
  }

  private def HttpClientConfig httpClientConfig() {
    return config == null ? HttpClientConfig.httpClientConfig() : config.getHttpClientConfig();
  }

  private def ConnectionConfig connectionConfig() {
    return config == null ? ConnectionConfig.connectionConfig() : config.getConnectionConfig();
  }

  private def EncoderConfig encoderConfig() {
    return config == null ? EncoderConfig.encoderConfig() : config.getEncoderConfig();
  }

  private def SessionConfig sessionConfig() {
    return config == null ? SessionConfig.sessionConfig() : config.getSessionConfig();
  }

  private def RestAssuredConfig restAssuredConfig() {
    config ?: new RestAssuredConfig()
  }

  private enum EncodingTarget {
    BODY, QUERY
  }

  /**
   * Specifies if the path was specified in the base uri or as an explicit path (for example get("http://www.google-com"))
   */
  private enum PathType {
    BASE_URI, EXPLICIT
  }
}
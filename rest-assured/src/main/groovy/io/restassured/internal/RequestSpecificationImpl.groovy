/*
 * Copyright 2019 the original author or authors.
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
package io.restassured.internal

import io.restassured.RestAssured
import io.restassured.authentication.AuthenticationScheme
import io.restassured.authentication.CertAuthScheme
import io.restassured.authentication.FormAuthScheme
import io.restassured.authentication.NoAuthScheme
import io.restassured.config.*
import io.restassured.filter.Filter
import io.restassured.filter.OrderedFilter
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.filter.time.TimingFilter
import io.restassured.http.*
import io.restassured.internal.MapCreator.CollisionStrategy
import io.restassured.internal.filter.FilterContextImpl
import io.restassured.internal.filter.FormAuthFilter
import io.restassured.internal.filter.SendRequestFilter
import io.restassured.internal.http.*
import io.restassured.internal.log.LogRepository
import io.restassured.internal.mapping.ObjectMapperSerializationContextImpl
import io.restassured.internal.mapping.ObjectMapping
import io.restassured.internal.multipart.MultiPartInternal
import io.restassured.internal.multipart.MultiPartSpecificationImpl
import io.restassured.internal.multipart.RestAssuredMultiPartEntity
import io.restassured.internal.proxy.RestAssuredProxySelector
import io.restassured.internal.proxy.RestAssuredProxySelectorRoutePlanner
import io.restassured.internal.support.ParameterUpdater
import io.restassured.internal.support.PathSupport
import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperType
import io.restassured.parsing.Parser
import io.restassured.response.Response
import io.restassured.specification.*
import io.restassured.spi.AuthFilter
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.HttpEntityWrapper
import org.apache.http.entity.mime.FormBodyPartBuilder
import org.apache.http.impl.client.AbstractHttpClient
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils

import java.security.KeyStore
import java.util.Map.Entry
import java.util.regex.Matcher
import java.util.regex.Pattern

import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE
import static io.restassured.http.ContentType.*
import static io.restassured.http.Method.*
import static io.restassured.internal.common.assertion.AssertParameter.notNull
import static io.restassured.internal.serialization.SerializationSupport.isSerializableCandidate
import static io.restassured.internal.support.PathSupport.isFullyQualified
import static io.restassured.internal.support.PathSupport.mergeAndRemoveDoubleSlash
import static java.lang.String.format
import static java.util.Arrays.asList
import static org.apache.commons.lang3.StringUtils.*
import static org.apache.http.client.params.ClientPNames.*

class RequestSpecificationImpl implements FilterableRequestSpecification, GroovyInterceptable {
  private static final int DEFAULT_HTTP_TEST_PORT = 8080
  private static final String CONTENT_TYPE = "Content-Type"
  private static final String DOUBLE_SLASH = "//"
  private static final String LOCALHOST = "localhost"
  private static final String CHARSET = "charset"
  private static final String ACCEPT_HEADER_NAME = "Accept"
  private static final String SSL = "SSL"
  private static final String MULTIPART = "multipart"
  private static final String MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH = MULTIPART + "/"
  private static final String MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS = MULTIPART + "+"
  private static final String TEMPLATE_START = "{"
  private static final String TEMPLATE_END = "}"

  private String baseUri
  private String path = ""
  private String method
  private String basePath
  // If first argument is null it means that it's a redundant path param that cannot be mapped to a placeholder
  // If second argument is null it means that the parameter has been removed (but we keep it to retain order)
  private List<Tuple2<String, String>> unnamedPathParamsTuples = new ArrayList<>()
  private AuthenticationScheme defaultAuthScheme
  private int port
  private Map<String, String> requestParameters = new LinkedHashMap()
  private Map<String, String> queryParameters = new LinkedHashMap()
  private Map<String, String> formParameters = new LinkedHashMap()
  private Map<String, String> namedPathParameters = [:]
  private Map<String, String> httpClientParams = [:]
  AuthenticationScheme authenticationScheme = new NoAuthScheme()
  private FilterableResponseSpecification responseSpecification;
  private Headers requestHeaders = new Headers([])
  private Cookies cookies = new Cookies([])
  private Object requestBody;
  private List<Filter> filters = [];
  private boolean urlEncodingEnabled
  private RestAssuredConfig restAssuredConfig;
  private List<MultiPartInternal> multiParts = [];
  private ParameterUpdater parameterUpdater = new ParameterUpdater(new ParameterUpdater.Serializer() {
    String serializeIfNeeded(Object value) {
      return RequestSpecificationImpl.this.serializeIfNeeded(value)
    }
  });
  private ProxySpecification proxySpecification = null

  private LogRepository logRepository

  // This field should be removed once http://jira.codehaus.org/browse/GROOVY-4647 is resolved, merge with sha 9619c3b when it's fixed.
  private AbstractHttpClient httpClient

  public RequestSpecificationImpl(String baseURI, int requestPort, String basePath, AuthenticationScheme defaultAuthScheme, List<Filter> filters,
                                  RequestSpecification defaultSpec, boolean urlEncode, RestAssuredConfig restAssuredConfig, LogRepository logRepository,
                                  ProxySpecification proxySpecification) {
    notNull(baseURI, "baseURI");
    notNull(basePath, "basePath");
    notNull(defaultAuthScheme, "defaultAuthScheme");
    notNull(filters, "Filters")
    notNull(urlEncode, "URL Encode query params option")
    this.baseUri = baseURI
    this.basePath = basePath
    this.defaultAuthScheme = defaultAuthScheme
    this.filters.addAll(filters)
    this.urlEncodingEnabled = urlEncode
    port(requestPort)
    this.restAssuredConfig = restAssuredConfig
    if (defaultSpec != null) {
      spec(defaultSpec)
    }
    this.logRepository = logRepository
    this.proxySpecification = proxySpecification
  }

  RequestSpecification when() {
    return this;
  }

  RequestSpecification given() {
    return this;
  }

  RequestSpecification that() {
    return this;
  }

  ResponseSpecification response() {
    return responseSpecification;
  }

  Response get(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(GET, path, pathParams)
  }

  Response post(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(POST, path, pathParams)
  }

  Response put(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(PUT, path, pathParams)
  }

  Response delete(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(DELETE, path, pathParams)
  }

  Response head(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(HEAD, path, pathParams)
  }

  Response patch(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(PATCH, path, pathParams)
  }

  Response options(String path, Object... pathParams) {
    applyPathParamsAndSendRequest(OPTIONS, path, pathParams)
  }

  Response get(URI uri) {
    get(notNull(uri, "URI").toString())
  }

  Response post(URI uri) {
    post(notNull(uri, "URI").toString())
  }

  Response put(URI uri) {
    put(notNull(uri, "URI").toString())
  }

  Response delete(URI uri) {
    delete(notNull(uri, "URI").toString())
  }

  Response head(URI uri) {
    head(notNull(uri, "URI").toString())
  }

  Response patch(URI uri) {
    patch(notNull(uri, "URI").toString())
  }

  Response options(URI uri) {
    options(notNull(uri, "URI").toString())
  }

  Response get(URL url) {
    get(notNull(url, "URL").toString())
  }

  Response post(URL url) {
    post(notNull(url, "URL").toString())
  }

  Response put(URL url) {
    put(notNull(url, "URL").toString())
  }

  Response delete(URL url) {
    delete(notNull(url, "URL").toString())
  }

  Response head(URL url) {
    head(notNull(url, "URL").toString())
  }

  Response patch(URL url) {
    patch(notNull(url, "URL").toString())
  }

  Response options(URL url) {
    options(notNull(url, "URL").toString())
  }

  Response get() {
    get("")
  }

  Response post() {
    post("")
  }

  Response put() {
    put("")
  }

  Response delete() {
    delete("")
  }

  Response head() {
    head("")
  }

  Response patch() {
    patch("")
  }

  Response options() {
    options("")
  }

  Response request(Method method) {
    request(notNull(method, Method.class).name())
  }

  Response request(String method) {
    request(method, "")
  }

  Response request(Method method, String path, Object... pathParams) {
    return request(notNull(method, Method.class).name(), path, pathParams)
  }

  Response request(String method, String path, Object... pathParams) {
    applyPathParamsAndSendRequest(method, path, pathParams)
  }

  Response request(Method method, URI uri) {
    request(method, notNull(uri, URI.class).toString())
  }

  Response request(Method method, URL url) {
    request(method, notNull(url, URL.class).toString())
  }

  Response request(String method, URI uri) {
    request(method, notNull(uri, URI.class).toString())
  }

  Response request(String method, URL url) {
    request(method, notNull(url, URL.class).toString())
  }

  Response get(String path, Map pathParamsMap) {
    pathParams(pathParamsMap)
    applyPathParamsAndSendRequest(GET, path)
  }

  Response post(String path, Map pathParamsMap) {
    pathParams(pathParamsMap)
    applyPathParamsAndSendRequest(POST, path)
  }

  Response put(String path, Map pathParamsMap) {
    pathParams(pathParamsMap)
    applyPathParamsAndSendRequest(PUT, path)
  }

  Response delete(String path, Map pathParamsMap) {
    pathParams(pathParamsMap)
    applyPathParamsAndSendRequest(DELETE, path)
  }

  Response head(String path, Map pathParamsMap) {
    pathParams(pathParamsMap)
    applyPathParamsAndSendRequest(HEAD, path)
  }

  Response patch(String path, Map pathParamsMap) {
    pathParams(pathParamsMap)
    applyPathParamsAndSendRequest(PATCH, path)
  }

  Response options(String path, Map pathParamsMap) {
    pathParams(pathParamsMap)
    applyPathParamsAndSendRequest(OPTIONS, path)
  }

  RequestSpecification params(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return params(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  RequestSpecification params(Map parametersMap) {
    notNull parametersMap, "parametersMap"
    parameterUpdater.updateParameters(restAssuredConfig().paramConfig.requestParamsUpdateStrategy(), parametersMap, requestParameters)
    return this
  }

  RequestSpecification param(String parameterName, Object... parameterValues) {
    notNull parameterName, "parameterName"
    parameterUpdater.updateZeroToManyParameters(restAssuredConfig().paramConfig.requestParamsUpdateStrategy(), requestParameters, parameterName, parameterValues)
    return this
  }

  FilterableRequestSpecification removeParam(String parameterName) {
    notNull parameterName, "parameterName"
    requestParameters.remove(parameterName)
    return this
  }

  RequestSpecification param(String parameterName, Collection<?> parameterValues) {
    notNull parameterValues, "parameterValues"
    return param(parameterName, parameterValues.toArray())
  }

  RequestSpecification queryParam(String parameterName, Collection<?> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    parameterUpdater.updateCollectionParameter(restAssuredConfig().getParamConfig().queryParamsUpdateStrategy(), queryParameters, parameterName, parameterValues)
    return this
  }

  FilterableRequestSpecification removeQueryParam(String parameterName) {
    notNull parameterName, "parameterName"
    queryParameters.remove(parameterName)
    return this
  }

  FilterableRequestSpecification removeHeader(String headerName) {
    notNull headerName, "headerName"
    def headersLeftAfterRemove = headers.findAll { !headerName.equalsIgnoreCase(it.getName()) }
    this.requestHeaders = new Headers(headersLeftAfterRemove)
    this
  }

  FilterableRequestSpecification removeCookie(String cookieName) {
    notNull cookieName, "cookieName"
    def cookiesLeftAfterRemove = cookies.findAll { !cookieName.equalsIgnoreCase(it.getName()) }
    this.cookies = new Cookies(cookiesLeftAfterRemove)
    this
  }

  FilterableRequestSpecification removeCookie(Cookie cookie) {
    notNull cookie, "cookie"
    removeCookie(cookie.getName())
    this
  }

  FilterableRequestSpecification replaceHeader(String headerName, String newValue) {
    notNull headerName, "headerName"
    removeHeader(headerName)
    def headerList = []
    headerList.addAll(this.requestHeaders.list())
    headerList.add(new Header(headerName, newValue))
    this.requestHeaders = new Headers(headerList)
    this
  }

  FilterableRequestSpecification replaceCookie(String cookieName, String value) {
    notNull cookieName, "cookieName"
    removeCookie(cookieName)
    cookie(cookieName, value)
    this
  }

  FilterableRequestSpecification replaceCookie(Cookie cookie) {
    notNull cookie, "cookie"
    removeCookie(cookie.getName())
    this.cookie(cookie)
    this
  }

  FilterableRequestSpecification replaceHeaders(Headers headers) {
    notNull headers, "headers"
    this.requestHeaders = new Headers(headers.asList())
    this
  }

  FilterableRequestSpecification replaceCookies(Cookies cookies) {
    notNull cookies, "cookies"
    this.cookies = new Cookies(cookies.asList())
    this
  }

  FilterableRequestSpecification removeHeaders() {
    this.requestHeaders = new Headers([])
    this
  }

  FilterableRequestSpecification removeCookies() {
    this.cookies = new Cookies([])
    this
  }


  RequestSpecification queryParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return queryParams(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  RequestSpecification queryParams(Map parametersMap) {
    notNull parametersMap, "parametersMap"
    parameterUpdater.updateParameters(restAssuredConfig().paramConfig.queryParamsUpdateStrategy(), parametersMap, queryParameters)
    return this
  }

  RequestSpecification queryParam(String parameterName, Object... parameterValues) {
    notNull parameterName, "parameterName"
    parameterUpdater.updateZeroToManyParameters(restAssuredConfig().paramConfig.queryParamsUpdateStrategy(), queryParameters, parameterName, parameterValues)
    return this
  }

  RequestSpecification formParam(String parameterName, Collection<?> parameterValues) {
    notNull parameterName, "parameterName"
    notNull parameterValues, "parameterValues"
    parameterUpdater.updateCollectionParameter(restAssuredConfig().paramConfig.formParamsUpdateStrategy(), formParameters, parameterName, parameterValues)
    return this
  }

  FilterableRequestSpecification removeFormParam(String parameterName) {
    notNull parameterName, "parameterName"
    formParameters.remove(parameterName)
    return this
  }

  RequestSpecification formParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return formParams(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  RequestSpecification formParams(Map parametersMap) {
    notNull parametersMap, "parametersMap"
    parameterUpdater.updateParameters(restAssuredConfig().paramConfig.formParamsUpdateStrategy(), parametersMap, formParameters)
    return this
  }

  RequestSpecification formParam(String parameterName, Object... additionalParameterValues) {
    notNull parameterName, "parameterName"
    parameterUpdater.updateZeroToManyParameters(restAssuredConfig().paramConfig.formParamsUpdateStrategy(), formParameters, parameterName, additionalParameterValues)
    return this
  }

  RequestSpecification urlEncodingEnabled(boolean isEnabled) {
    this.urlEncodingEnabled = isEnabled
    return this
  }

  RequestSpecification pathParam(String parameterName, Object parameterValue) {
    notNull parameterName, "parameterName"
    notNull parameterValue, "parameterValue"
    parameterUpdater.updateStandardParameter(REPLACE, namedPathParameters, parameterName, parameterValue)
    return this
  }

  RequestSpecification pathParams(String firstParameterName, Object firstParameterValue, Object... parameterNameValuePairs) {
    notNull firstParameterName, "firstParameterName"
    notNull firstParameterValue, "firstParameterValue"
    return pathParams(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstParameterName, firstParameterValue, parameterNameValuePairs))
  }

  RequestSpecification pathParams(Map parameterNameValuePairs) {
    notNull parameterNameValuePairs, "parameterNameValuePairs"
    parameterUpdater.updateParameters(REPLACE, parameterNameValuePairs, namedPathParameters)
    return this
  }

  FilterableRequestSpecification removePathParam(String parameterName) {
    notNull parameterName, "parameterName"
    removeNamedPathParam(parameterName)
    removeUnnamedPathParam(parameterName)
    return this
  }

  FilterableRequestSpecification removeNamedPathParam(String parameterName) {
    notNull parameterName, "parameterName"
    namedPathParameters.remove(parameterName)
    this
  }

  FilterableRequestSpecification removeUnnamedPathParam(String parameterName) {
    notNull parameterName, "parameterName"
    def indexOfParamName = unnamedPathParamsTuples.findIndexOf { it.first == parameterName }
    if (indexOfParamName > -1) {
      removeUnnamedPathParamAtIndex(indexOfParamName)
    }
    this
  }

  FilterableRequestSpecification removeUnnamedPathParamByValue(String parameterValue) {
    notNull parameterValue, "parameterValue"
    def indexOfParamValue = unnamedPathParamsTuples.findIndexOf { it.second == parameterValue }
    if (indexOfParamValue > -1) {
      removeUnnamedPathParamAtIndex(indexOfParamValue)
    }
    return this
  }

  RequestSpecification config(RestAssuredConfig config) {
    this.restAssuredConfig = config
    responseSpecification?.config = config
    this
  }

  RequestSpecification keyStore(String pathToJks, String password) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    // Allow all host names in order to be backward compatible
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(pathToJks, password).allowAllHostnames())
    this
  }

  RequestSpecification keyStore(File pathToJks, String password) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    // Allow all host names in order to be backward compatible
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(pathToJks, password).allowAllHostnames())
    this
  }

  RequestSpecification trustStore(String path, String password) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(path, password).allowAllHostnames())
    this
  }

  RequestSpecification trustStore(File path, String password) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(path, password).allowAllHostnames())
    this
  }

  RequestSpecification trustStore(KeyStore trustStore) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.trustStore(trustStore))
    this
  }

  RequestSpecification keyStore(KeyStore keyStore) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.keyStore(keyStore))
    this
  }

  RequestSpecification relaxedHTTPSValidation() {
    relaxedHTTPSValidation(SSL)
  }

  RequestSpecification relaxedHTTPSValidation(String protocol) {
    def sslConfig = restAssuredConfig().getSSLConfig()
    restAssuredConfig = restAssuredConfig().sslConfig(sslConfig.relaxedHTTPSValidation(protocol))
    this
  }

  RequestSpecification filter(Filter filter) {
    notNull filter, "Filter"
    filters << filter
    return this
  }

  RequestSpecification filters(List<Filter> filters) {
    notNull filters, "Filters"
    this.filters.addAll(filters)
    return this
  }

  RequestSpecification filters(Filter filter, Filter... additionalFilter) {
    notNull filter, "Filter"
    this.filters.add(filter)
    additionalFilter?.each {
      this.filters.add(it)
    }
    return this
  }

  RequestLogSpecification log() {
    return new RequestLogSpecificationImpl(requestSpecification: this, logRepository: logRepository)
  }

  RequestSpecification and() {
    return this;
  }

  RequestSpecification request() {
    return this;
  }

  RequestSpecification with() {
    return this;
  }

  ResponseSpecification then() {
    return responseSpecification;
  }

  ResponseSpecification expect() {
    return responseSpecification;
  }

  AuthenticationSpecification auth() {
    return new AuthenticationSpecificationImpl(this);
  }

  AuthenticationSpecification authentication() {
    return auth();
  }

  RequestSpecification port(int port) {
    if (port < 1 && port != RestAssured.UNDEFINED_PORT) {
      throw new IllegalArgumentException("Port must be greater than 0")
    }
    this.port = port
    return this
  }

  RequestSpecification body(String body) {
    notNull body, "body"
    this.requestBody = body;
    return this;
  }

  RequestSpecification baseUri(String baseUri) {
    notNull baseUri, "Base URI"
    this.baseUri = baseUri;
    return this;
  }

  RequestSpecification basePath(String basePath) {
    notNull basePath, "Base Path"
    this.basePath = basePath;
    return this;
  }

  RequestSpecification proxy(String host, int port) {
    proxy(ProxySpecification.host(host).withPort(port))
  }

  RequestSpecification proxy(String host) {
    if (UriValidator.isUri(host)) {
      proxy(new URI(host))
    } else {
      proxy(ProxySpecification.host(host))
    }
  }

  RequestSpecification proxy(int port) {
    proxy(ProxySpecification.port(port))
  }

  RequestSpecification proxy(String host, int port, String scheme) {
    proxy(new org.apache.http.client.utils.URIBuilder().setHost(host).setPort(port).setScheme(scheme).build())
  }

  RequestSpecification proxy(URI uri) {
    notNull(uri, URI.class)
    proxy(new ProxySpecification(uri.host, uri.port, uri.scheme));
  }

  RequestSpecification proxy(ProxySpecification proxySpecification) {
    notNull(proxySpecification, ProxySpecification.class)
    this.proxySpecification = proxySpecification
    this
  }

  RequestSpecification body(byte[] body) {
    notNull body, "body"
    this.requestBody = body;
    return this;
  }

  RequestSpecification body(File body) {
    notNull body, "body"
    this.requestBody = body;
    return this;
  }

  RequestSpecification body(InputStream body) {
    notNull body, "body"
    this.requestBody = body;
    return this;
  }

  RequestSpecification body(Object object) {
    notNull object, "object"
    if (!isSerializableCandidate(object)) {
      return body(object.toString());
    }

    this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), null, objectMappingConfig(), restAssuredConfig().getEncoderConfig());
    this
  }

  RequestSpecification body(Object object, ObjectMapper mapper) {
    notNull object, "object"
    notNull mapper, "Object mapper"
    def ctx = new ObjectMapperSerializationContextImpl();
    ctx.setObject(object)
    ctx.setCharset(findEncoderCharsetOrReturnDefault(requestContentType))
    ctx.setContentType(requestContentType)
    this.requestBody = mapper.serialize(ctx);
    this
  }

  RequestSpecification body(Object object, ObjectMapperType mapperType) {
    notNull object, "object"
    notNull mapperType, "Object mapper type"
    this.requestBody = ObjectMapping.serialize(object, requestContentType, findEncoderCharsetOrReturnDefault(requestContentType), mapperType, objectMappingConfig(), restAssuredConfig().getEncoderConfig())
    this
  }

  RequestSpecification contentType(ContentType contentType) {
    notNull contentType, ContentType.class
    header(CONTENT_TYPE, contentType)
  }

  RequestSpecification contentType(String contentType) {
    notNull contentType, "Content-Type header cannot be null"
    header(CONTENT_TYPE, contentType)
  }

  RequestSpecification accept(ContentType contentType) {
    notNull contentType, "Accept header"
    accept(contentType.getAcceptHeader())
  }

  RequestSpecification accept(String mediaTypes) {
    notNull mediaTypes, "Accept header media range"
    header(ACCEPT_HEADER_NAME, mediaTypes)
  }

  RequestSpecification headers(Map headers) {
    notNull headers, "headers"
    def headerList = []
    if (this.requestHeaders.exist()) {
      headerList.addAll(this.requestHeaders.list())
    }
    headers.each {
      if (it.value instanceof List) {
        it.value.each { val ->
          headerList << new Header(it.key, serializeIfNeeded(val))
        }
      } else {
        headerList << new Header(it.key, serializeIfNeeded(it.value))
      }
    }
    headerList = removeMergedHeadersIfNeeded(headerList)
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
      headerList = removeMergedHeadersIfNeeded(headerList)
      this.requestHeaders = new Headers(headerList)
    }
    this
  }

  private List removeMergedHeadersIfNeeded(List headerList) {
    def headers = headerList.inject([], { acc, header ->
      def headerConfig = restAssuredConfig().getHeaderConfig()
      String headerName = header.getName()
      if (headerConfig.shouldOverwriteHeaderWithName(headerName)) {
        acc = acc.findAll { !headerName.equalsIgnoreCase(it.getName()) }
      }
      acc.add(header)
      acc
    })
    headers
  }

  RequestSpecification header(String headerName, Object headerValue, Object... additionalHeaderValues) {
    notNull headerName, "Header name"
    notNull headerValue, "Header value"

    def headerList = [new Header(headerName, serializeIfNeeded(headerValue))]
    additionalHeaderValues?.each {
      headerList << new Header(headerName, serializeIfNeeded(it))
    }

    return headers(new Headers(headerList))
  }

  RequestSpecification header(Header header) {
    notNull header, "Header"

    return headers(new Headers(asList(header)));
  }

  RequestSpecification headers(String firstHeaderName, Object firstHeaderValue, Object... headerNameValuePairs) {
    return headers(MapCreator.createMapFromParams(CollisionStrategy.MERGE, firstHeaderName, firstHeaderValue, headerNameValuePairs))
  }

  RequestSpecification cookies(String firstCookieName, Object firstCookieValue, Object... cookieNameValuePairs) {
    return cookies(MapCreator.createMapFromParams(CollisionStrategy.OVERWRITE, firstCookieName, firstCookieValue, cookieNameValuePairs))
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

  RequestSpecification cookies(Cookies cookies) {
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

  RequestSpecification cookie(Cookie cookie) {
    notNull cookie, "Cookie"
    return cookies(new Cookies(asList(cookie)));
  }

  RequestSpecification cookie(String cookieName) {
    cookie(cookieName, null)
  }

  RedirectSpecification redirects() {
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

  RequestSpecification multiPart(MultiPartSpecification multiPartSpec) {
    notNull multiPartSpec, "Multi-part specification"
    def mimeType = multiPartSpec.mimeType
    def content
    if (multiPartSpec.content instanceof File || multiPartSpec.content instanceof InputStream || multiPartSpec.content instanceof byte[]) {
      content = multiPartSpec.content
    } else {
      // Objects ought to be serialized
      if (mimeType == null) {
        mimeType = ANY.matches(requestContentType) ? JSON.toString() : requestContentType
      }
      content = serializeIfNeeded(multiPartSpec.content, mimeType)
    }

    final String controlName;
    if (multiPartSpec instanceof MultiPartSpecificationImpl && !multiPartSpec.isControlNameSpecifiedExplicitly()) {
      // We use the default control name if it was not explicitly specified in the multi-part spec
      controlName = restAssuredConfig().getMultiPartConfig().defaultControlName()
    } else {
      controlName = multiPartSpec.controlName
    }

    final String fileName;
    if (multiPartSpec instanceof MultiPartSpecificationImpl && !multiPartSpec.isFileNameSpecifiedExplicitly()) {
      // We use the default file name if it was not explicitly specified in the multi-part spec
      fileName = restAssuredConfig().getMultiPartConfig().defaultFileName()
    } else {
      fileName = multiPartSpec.fileName
    }

    def headers = multiPartSpec.headers

    multiParts << new MultiPartInternal(controlName: controlName, content: content, fileName: fileName, charset: multiPartSpec.charset, mimeType: mimeType, headers: headers)
    return this
  }

  RequestSpecification multiPart(String controlName, File file) {
    multiParts << new MultiPartInternal(controlName: controlName, content: file, fileName: file.getName())
    this
  }

  RequestSpecification multiPart(File file) {
    multiParts << new MultiPartInternal(controlName: restAssuredConfig().getMultiPartConfig().defaultControlName(), content: file, fileName: file.getName())
    this
  }

  RequestSpecification multiPart(String controlName, File file, String mimeType) {
    multiParts << new MultiPartInternal(controlName: controlName, content: file, mimeType: mimeType, fileName: file.getName())
    this
  }

  RequestSpecification multiPart(String controlName, Object object) {
    def mimeType = ANY.matches(requestContentType) ? JSON.toString() : requestContentType
    return multiPart(controlName, object, mimeType)
  }

  RequestSpecification multiPart(String controlName, Object object, String mimeType) {
    def possiblySerializedObject = serializeIfNeeded(object, mimeType)
    multiParts << new MultiPartInternal(controlName: controlName, content: possiblySerializedObject, mimeType: mimeType, fileName: restAssuredConfig().getMultiPartConfig().defaultFileName())
    this
  }

  RequestSpecification multiPart(String controlName, String filename, Object object, String mimeType) {
    def possiblySerializedObject = serializeIfNeeded(object, mimeType)
    multiParts << new MultiPartInternal(controlName: controlName, content: possiblySerializedObject, mimeType: mimeType, fileName: filename)
    this
  }

  RequestSpecification multiPart(String name, String fileName, byte[] bytes) {
    multiParts << new MultiPartInternal(controlName: name, content: bytes, fileName: fileName)
    this
  }

  RequestSpecification multiPart(String name, String fileName, byte[] bytes, String mimeType) {
    multiParts << new MultiPartInternal(controlName: name, content: bytes, mimeType: mimeType, fileName: fileName)
    this
  }

  RequestSpecification multiPart(String name, String fileName, InputStream stream) {
    multiParts << new MultiPartInternal(controlName: name, content: stream, fileName: fileName)
    this
  }

  RequestSpecification multiPart(String name, String fileName, InputStream stream, String mimeType) {
    multiParts << new MultiPartInternal(controlName: name, content: stream, mimeType: mimeType, fileName: fileName)
    this
  }

  RequestSpecification multiPart(String name, String contentBody) {
    multiParts << new MultiPartInternal(controlName: name, content: contentBody, fileName: restAssuredConfig().getMultiPartConfig().defaultFileName())
    this
  }

  RequestSpecification multiPart(String name, NoParameterValue contentBody) {
    multiParts << new MultiPartInternal(controlName: name, content: contentBody, fileName: restAssuredConfig().getMultiPartConfig().defaultFileName())
    this
  }

  RequestSpecification multiPart(String name, String contentBody, String mimeType) {
    multiParts << new MultiPartInternal(controlName: name, content: contentBody, mimeType: mimeType, fileName: restAssuredConfig().getMultiPartConfig().defaultFileName())
    this
  }

  def newFilterContext(assertionClosure, filters, properties) {
    if (path?.endsWith("?")) {
      throw new IllegalArgumentException("Request URI cannot end with ?");
    }

    // Set default accept header if undefined
    if (!headers.hasHeaderWithName(ACCEPT_HEADER_NAME)) {
      header(ACCEPT_HEADER_NAME, ANY.getAcceptHeader())
    }

    def tempContentType = defineRequestContentTypeAsString(method)
    if (tempContentType != null) {
      header(CONTENT_TYPE, tempContentType)
    }

    def unnamedPathParamValues = unnamedPathParamsTuples.findAll { it.second != null }.collect { it.second }
    def uri = partiallyApplyPathParams(path, true, unnamedPathParamValues)
    String requestUriForLogging = generateRequestUriForLogging(uri, method)

    new FilterContextImpl(requestUriForLogging, getUserDefinedPath(), getDerivedPath(uri), uri, path, unnamedPathParamValues.toArray(), method, assertionClosure, filters, properties);
  }

  private String generateRequestUriForLogging(uri, method) {
    def targetUri
    def allQueryParams = [:]

    if (uri.contains("?")) {
      def uriToUse
      if (isFullyQualified(uri)) {
        uriToUse = uri
      } else {
        uriToUse = getTargetPath(uri)
      }

      targetUri = substringBefore(uriToUse, "?")
      def queryParamsDefinedInPath = substringAfter(uri, "?")

      // Add query parameters defined in path to the allQueryParams map
      if (!isBlank(queryParamsDefinedInPath)) {
        def splittedQueryParams = split(queryParamsDefinedInPath, "&");
        splittedQueryParams.each { queryNameWithPotentialValue ->
          String[] splitted = split(queryNameWithPotentialValue, "=", 2)
          def queryParamHasValueDefined = splitted.size() > 1 || queryNameWithPotentialValue.contains("=")
          if (queryParamHasValueDefined) {
            // Handles the special case where the query param is defined with an empty value
            def value = splitted.size() == 1 ? "" : splitted[1]
            allQueryParams.put(splitted[0], value)
          } else {
            allQueryParams.put(splitted[0], new NoParameterValue());
          }
        }
      }
    } else {
      targetUri = uri
    }

    def actualUri = URIBuilder.convertToURI(assembleCompleteTargetPath(targetUri))
    def uriBuilder = new URIBuilder(actualUri, this.urlEncodingEnabled, encoderConfig())

    if (!POST.name().equalsIgnoreCase(method) && !requestParameters?.isEmpty()) {
      allQueryParams << requestParameters
    }

    if (!queryParameters?.isEmpty()) {
      allQueryParams << queryParameters
    }

    if (GET.name().equalsIgnoreCase(method) && !formParameters?.isEmpty()) {
      allQueryParams << formParameters
    }

    if (!allQueryParams.isEmpty()) {
      uriBuilder.addQueryParams(allQueryParams)
    }

    def requestUriForLogging = uriBuilder.toString()
    requestUriForLogging
  }

  @SuppressWarnings("GroovyUnusedDeclaration")
  private
  Response sendRequest(path, assertionClosure, FilterableRequestSpecification requestSpecification, Map filterContextProperties) {
    notNull path, "Path"
    path = extractRequestParamsIfNeeded(path);
    def method = requestSpecification.getMethod()
    def targetUri = getTargetURI(path);
    def targetPath = getTargetPath(path)

    assertCorrectNumberOfPathParams()

    if (!requestSpecification.getHttpClient() instanceof AbstractHttpClient) {
      throw new IllegalStateException(format("Unfortunately Rest Assured only supports Http Client instances of type %s.", AbstractHttpClient.class.getName()));
    }

    def http = new RestAssuredHttpBuilder(targetUri, assertionClosure, urlEncodingEnabled, config, requestSpecification.getHttpClient() as AbstractHttpClient);
    applyProxySettings(http)
    applyRestAssuredConfig(http)
    registerRestAssuredEncoders(http);
    setRequestHeadersToHttpBuilder(http)

    if (cookies.exist()) {
      http.getHeaders() << [Cookie: cookies.collect { it.name + "=" + it.value }.join("; ")]
    }

    // Allow returning a the response
    def restAssuredResponse = new RestAssuredResponseImpl(logRepository: logRepository)
    RestAssuredConfig cfg = config ?: new RestAssuredConfig();
    restAssuredResponse.setSessionIdName(cfg.getSessionConfig().sessionIdName())
    restAssuredResponse.setDecoderConfig(cfg.getDecoderConfig())
    restAssuredResponse.setConnectionManager(http.client.connectionManager)
    restAssuredResponse.setConfig(cfg)
    restAssuredResponse.setFilterContextProperties(filterContextProperties)
    responseSpecification.restAssuredResponse = restAssuredResponse
    def acceptContentType = assertionClosure.getResponseContentType()

    if (shouldApplySSLConfig(http, cfg)) {
      def sslConfig = cfg.getSSLConfig();
      new CertAuthScheme(pathToKeyStore: sslConfig.getPathToKeyStore(), keyStorePassword: sslConfig.getKeyStorePassword(),
              keystoreType: sslConfig.getKeyStoreType(), keyStore: sslConfig.getKeyStore(),
              pathToTrustStore: sslConfig.getPathToTrustStore(), trustStorePassword: sslConfig.getTrustStorePassword(),
              trustStoreType: sslConfig.getTrustStoreType(), trustStore: sslConfig.getTrustStore(),
              port: sslConfig.getPort(), sslSocketFactory: sslConfig.getSSLSocketFactory(), x509HostnameVerifier: sslConfig.getX509HostnameVerifier())
              .authenticate(http)
    }

    authenticationScheme.authenticate(http)

    if (mayHaveBody(method)) {
      if (hasFormParams() && requestBody != null) {
        throw new IllegalStateException("You can either send form parameters OR body content in $method, not both!");
      }
      def bodyContent = createFormParamBodyContent(assembleBodyContent(method))
      if (POST.name().equalsIgnoreCase(method)) {
        http.post(path: targetPath, body: bodyContent,
                requestContentType: requestHeaders.getValue(CONTENT_TYPE),
                contentType: acceptContentType) { response, content ->
          if (assertionClosure != null) {
            assertionClosure.call(response, content)
          }
        }
      } else if (PATCH.name().equalsIgnoreCase(method)) {
        http.patch(path: targetPath, body: bodyContent,
                requestContentType: requestHeaders.getValue(CONTENT_TYPE),
                contentType: acceptContentType) { response, content ->
          if (assertionClosure != null) {
            assertionClosure.call(response, content)
          }
        }
      } else {
        requestBody = bodyContent
        sendHttpRequest(http, method, acceptContentType, targetPath, assertionClosure)
      }
    } else {
      sendHttpRequest(http, method, acceptContentType, targetPath, assertionClosure)
    }
    return restAssuredResponse
  }

  void assertCorrectNumberOfPathParams() {
    // Path param size is named - (unnamed - named) since named path params may override unnamed if they target the same placeholder
    if (!getRedundantNamedPathParams().isEmpty() || !getRedundantUnnamedPathParamValues().isEmpty() || !getUndefinedPathParamPlaceholders().isEmpty()) {
      def pathParamPlaceholderSize = getPathParamPlaceholders().size()
      def namedPathParams = getNamedPathParams()
      def pathParamSize = namedPathParams.size() + unnamedPathParamsTuples.findAll { it.second != null }.findAll {
        !namedPathParams.containsKey(it.second)
      }.size()

      def redundantNamedPathParams = getRedundantNamedPathParams()
      def redundantUnnamedPathParamValues = getRedundantUnnamedPathParamValues()
      def hasRedundantNamedPathParams = redundantNamedPathParams.size() > 0
      def hasRedundantUnnamedPathParamValues = redundantUnnamedPathParamValues.size() > 0

      final String message
      if (pathParamPlaceholderSize != pathParamSize) {
        message = "Invalid number of path parameters. Expected ${pathParamPlaceholderSize}, was ${pathParamSize}."
      } else {
        message = "Path parameters were not correctly defined."
      }

      String redundantMessage = ""
      if (hasRedundantNamedPathParams || hasRedundantUnnamedPathParamValues) {
        redundantMessage = " Redundant path parameters are: "

        if (hasRedundantNamedPathParams) {
          redundantMessage += "${redundantNamedPathParams.entrySet().join(", ")}"
        }
        if (hasRedundantNamedPathParams && hasRedundantUnnamedPathParamValues) {
          redundantMessage += " and "
        } else if (hasRedundantNamedPathParams && !hasRedundantUnnamedPathParamValues) {
          redundantMessage += "."
        }
        if (hasRedundantUnnamedPathParamValues) {
          redundantMessage += "${redundantUnnamedPathParamValues.join(", ")}."
        }
      }

      String undefinedMessage = ""
      if (!getUndefinedPathParamPlaceholders().isEmpty()) {
        undefinedMessage = " Undefined path parameters are: ${getUndefinedPathParamPlaceholders().join(", ")}."
      }

      throw new IllegalArgumentException("${message}${redundantMessage}${undefinedMessage}")
    }
  }

  boolean shouldApplySSLConfig(http, RestAssuredConfig cfg) {
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
    httpBuilder.encoders.setEncoderConfig(encoderConfig)
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
    if (hasFormParams() && !GET.name().equalsIgnoreCase(httpMethod)) {
      if (POST.name().equalsIgnoreCase(httpMethod)) {
        mergeMapsAndRetainOrder(requestParameters, formParameters)
      } else {
        formParameters
      }
    } else if (multiParts.isEmpty()) {
      requestBody
    } else {
      new byte[0]
    }
  }

  def mergeMapsAndRetainOrder(Map<String, Object> map1, Map<String, Object> map2) {
    def newMap = new LinkedHashMap()
    newMap.putAll(map1)
    newMap.putAll(map2)
    newMap
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

  private def createFormParamBodyContent(bodyContent) {
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

  private def registerRestAssuredEncoders(HTTPBuilder http) {
    // Multipart form-data
    if (multiParts.isEmpty()) {
      return;
    }

    if (hasFormParams()) {
      convertFormParamsToMultiPartParams()
    }

    def contentTypeAsString = headers.getValue(CONTENT_TYPE)
    def ct = ContentTypeExtractor.getContentTypeWithoutCharset(contentTypeAsString)
    def subType;
    if (ct?.toLowerCase()?.startsWith(MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH)) {
      subType = substringAfter(ct, MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH)
    } else if (ct?.toLowerCase()?.contains(MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS)) {
      subType = substringBefore(substringAfter(ct, MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS), "+")
    } else {
      throw new IllegalArgumentException("Content-Type $ct is not valid when using multiparts, it must start with \"$MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH\" or contain \"$MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS\".");
    }

    def charsetFromContentType = CharsetExtractor.getCharsetFromContentType(contentTypeAsString)
    def charsetToUse = isBlank(charsetFromContentType) ? restAssuredConfig().getMultiPartConfig().defaultCharset() : charsetFromContentType
    def boundaryFromContentType = BoundaryExtractor.getBoundaryFromContentType(contentTypeAsString)
    String boundaryToUse = boundaryFromContentType ?: restAssuredConfig().getMultiPartConfig().defaultBoundary()
    boundaryToUse = boundaryToUse ?: generateBoundary()
    if (!boundaryFromContentType) {
      removeHeader(CONTENT_TYPE) // there should only be one
      contentType(contentTypeAsString + "; boundary=\"" + boundaryToUse + "\"")
    }

    def multipartMode = httpClientConfig().httpMultipartMode()
    // For "defaultCharset" to be taken into account we need to 

    http.encoders.putAt ct, { contentType, content ->
      RestAssuredMultiPartEntity entity = new RestAssuredMultiPartEntity(subType, charsetToUse, multipartMode, boundaryToUse);

      multiParts.each {
        def body = it.contentBody
        def controlName = it.controlName
        def headers = it.headers
        if (headers.isEmpty()) {
          entity.addPart(controlName, body);
        } else {
          def builder = FormBodyPartBuilder.create(controlName, body)
          headers.each { name, value ->
            builder.addField(name, value)
          }
          entity.addPart(builder.build())
        }
      }

      entity;
    }
  }

  private static String generateBoundary() {
    def alphabet = (('a'..'z') + ('A'..'Z') + ('0'..'9') + '-' + '_').join()
    def rand = new Random()
    def length = rand.nextInt(11) + 30
    (1..length).collect {
      alphabet[rand.nextInt(alphabet.length())]
    }.join()
  }

  private def convertFormParamsToMultiPartParams() {
    def allFormParams = mergeMapsAndRetainOrder(requestParameters, formParameters)
    allFormParams.each {
      if (it.value instanceof List) {
        it.value.each { val ->
          multiPart(it.key, val)
        }
      } else {
        multiPart(it.key, it.value)
      }
    }
    requestParameters.clear()
    formParameters.clear()
  }

  private def sendHttpRequest(HTTPBuilder http, String method, responseContentType, targetPath, assertionClosure) {
    def allQueryParams = mergeMapsAndRetainOrder(requestParameters, queryParameters)
    if (method.equals(GET.name())) {
      allQueryParams = mergeMapsAndRetainOrder(allQueryParams, formParameters)
    }
    def hasBody = (requestBody != null)
    http.request(method, responseContentType, hasBody) {
      uri.path = targetPath

      setRequestContentType(defineRequestContentTypeAsString(method))

      if (hasBody) {
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
    return POST.name().equals(method) || formParameters.size() > 0 || multiParts.size() > 0
  }

  private String extractRequestParamsIfNeeded(String path) {
    if (path.contains("?")) {
      def indexOfQuestionMark = path.indexOf("?")
      String allParamAsString = path.substring(indexOfQuestionMark + 1);
      def keyValueParams = allParamAsString.split("&");
      keyValueParams.each {
        def keyValue = split(it, "=", 2)
        def theKey;
        def theValue;
        if (keyValue.length < 1 || keyValue.length > 2) {
          throw new IllegalArgumentException("Illegal parameters passed to REST Assured. Parameters was: $keyValueParams")
        } else if (keyValue.length == 1) {
          theKey = keyValue[0]
          theValue = it.contains("=") ? "" : new NoParameterValue();
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

  private def defineRequestContentTypeAsString(String method) {
    return defineRequestContentType(method)?.toString()
  }

  private def defineRequestContentType(String method) {
    def contentType = headers.getValue(CONTENT_TYPE)
    if (contentType == null) {
      if (multiParts.size() > 0) {
        contentType = MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH + restAssuredConfig().getMultiPartConfig().defaultSubtype()
      } else if (GET.name().equals(method) && !formParameters.isEmpty()) {
        contentType = URLENC
      } else if (requestBody == null) {
        contentType = mayHaveBody(method) ? URLENC : null
      } else if (requestBody instanceof byte[]) {
        contentType = BINARY
      } else {
        contentType = TEXT
      }
    }

    if (shouldAppendCharsetToContentType(contentType)) {
      def charset = findEncoderCharsetOrReturnDefault(contentType.toString())
      if (contentType instanceof String) {
        contentType = contentType + "; " + CHARSET + "=" + charset
      } else {
        contentType = contentType.withCharset(charset)
      }
    }
    contentType
  }

  private boolean shouldAppendCharsetToContentType(contentType) {
    contentType != null && !(startsWith(contentType.toString(), MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH) || contains(contentType.toString(), MULTIPART_CONTENT_TYPE_PREFIX_WITH_PLUS)) && restAssuredConfig().encoderConfig.shouldAppendDefaultContentCharsetToContentTypeIfUndefined() && !containsIgnoreCase(contentType.toString(), CHARSET)
  }

  private String getTargetURI(String path) {
    def uri
    def pathHasScheme = isFullyQualified(path)
    if (pathHasScheme) {
      def url = new URL(path)
      uri = getTargetUriFromUrl(url)
    } else if (isFullyQualified(baseUri)) {
      def baseUriAsUrl = new URL(baseUri)
      uri = getTargetUriFromUrl(baseUriAsUrl)
    } else if (port != RestAssured.UNDEFINED_PORT) {
      uri = "$baseUri:$port"
    } else {
      uri = "$baseUri"
    }
    return uri
  }

  private String getTargetUriFromUrl(URL url) {
    def protocol = url.getProtocol()
    boolean useDefaultHttps = false
    if (this.@port == RestAssured.UNDEFINED_PORT && protocol.equalsIgnoreCase("https")) {
      useDefaultHttps = true
    }

    def builder = new StringBuilder(protocol)
      .append("://")
      .append(url.getAuthority())

    def hasSpecifiedPortExplicitly = this.@port != RestAssured.UNDEFINED_PORT
    if (!hasPortDefined(url) && !useDefaultHttps) {
      if (hasSpecifiedPortExplicitly) {
        builder.append(":")
        builder.append(this.@port)
      } else if (!isFullyQualified(url.toString()) || hasAuthorityEqualToLocalhost(url)) {
        builder.append(":")
        builder.append(DEFAULT_HTTP_TEST_PORT)
      }
    }
    return builder.toString()
  }

  private boolean hasAuthorityEqualToLocalhost(uri) {
    uri.getAuthority().trim().equalsIgnoreCase(LOCALHOST)
  }

  private boolean hasPortDefined(uri) {
    return uri.getPort() != -1;
  }


  private def serializeIfNeeded(Object object) {
    serializeIfNeeded(object, requestContentType)
  }

  private def serializeIfNeeded(Object object, contentType) {
    isSerializableCandidate(object) ? ObjectMapping.serialize(object, contentType, findEncoderCharsetOrReturnDefault(contentType), null, objectMappingConfig(), restAssuredConfig().getEncoderConfig()) : object.toString()
  }

  private def applyPathParamsAndSendRequest(String method, String path, Object... unnamedPathParams) {
    notNull path, "path"
    notNull trimToNull(method), "Method"
    notNull unnamedPathParams, "Path params"
    this.method = method.trim().toUpperCase();
    this.path = path;
    if (unnamedPathParams != null) {
      def nullParamIndices = []
      for (int i = 0; i < unnamedPathParams.length; i++) {
        if (unnamedPathParams[i] == null) {
          nullParamIndices << i
        }
      }
      if (!nullParamIndices.isEmpty()) {
        def sizeOne = nullParamIndices.size() == 1
        throw new IllegalArgumentException("Unnamed path parameter cannot be null (path parameter${sizeOne ? "" : "s"} at ${sizeOne ? "index" : "indices"} ${nullParamIndices.join(",")} ${sizeOne ? "is" : "are"} null)");
      }

      buildUnnamedPathParameterTuples(unnamedPathParams)
    }
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
    def logConfig = restAssuredConfig().getLogConfig()
    if (logConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
      if (!filters.any { RequestLoggingFilter.class.isAssignableFrom(it.getClass()) }) {
        log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled())
      }
      if (!filters.any { ResponseLoggingFilter.class.isAssignableFrom(it.getClass()) }) {
        responseSpecification.log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled())
      }
    }
    restAssuredConfig = config ?: new RestAssuredConfig()

    if (!filters.any { ResponseLoggingFilter.class.isAssignableFrom(it.getClass()) } && responseSpecification?.getLogDetail()) {
      filters.add(new ResponseLoggingFilter(responseSpecification.getLogDetail(),
              logConfig.isPrettyPrintingEnabled(), logConfig.defaultStream()))
    }

    // Sort filters by order
    filters = filters.toSorted { f1, f2 -> getFilterOrder(f1) <=> getFilterOrder(f2) }

    // Add timing filter if it has not been added manually
    if (!filters*.getClass().any { TimingFilter.class.isAssignableFrom(it) }) {
      filters << new TimingFilter()
    }

    filters << new SendRequestFilter()
    def ctx = newFilterContext(responseSpecification.assertionClosure, filters.iterator(), [:])
    httpClient = httpClientConfig().httpClientInstance()
    def response = ctx.next(this, responseSpecification)
    responseSpecification.assertionClosure.validate(response)
    return response
  }

  private def applyPathParamsAndSendRequest(Method method, String path, Object... unnamedPathParams) {
    applyPathParamsAndSendRequest(notNull(method, Method.class).name(), path, unnamedPathParams)
  }

  void buildUnnamedPathParameterTuples(Object[] unnamedPathParameterValues) {
    if (unnamedPathParameterValues == null || unnamedPathParameterValues.length == 0) {
      this.unnamedPathParamsTuples = new ArrayList<Tuple2<String, String>>();
    } else {
      // Undefined placeholders since named path params have precedence over unnamed
      def keys = getUndefinedPathParamPlaceholders()
      List<Tuple2<String, String>> list = new ArrayList<>()
      for (int i = 0; i < unnamedPathParameterValues.length; i++) {
        def val = serializeIfNeeded(unnamedPathParameterValues[i])
        def key = i < keys.size() ? keys.get(i) : null
        list.add(new Tuple2<String, String>(key, val))
      }
      this.unnamedPathParamsTuples = list
    }
  }

  String partiallyApplyPathParams(String path, boolean encodePath, List<String> unnamedPathParams) {
    def unnamedPathParamSize = unnamedPathParams?.size() ?: 0

    def host = getTargetURI(path)
    def targetPath = getTargetPath(path)

    def pathWithoutQueryParams = substringBefore(targetPath, "?");
    def shouldAppendSlashAfterEncoding = pathWithoutQueryParams.endsWith("/")
    // The last slash is removed later so we may need to add it again
    def queryParams = substringAfter(path, "?")

    int numberOfUnnamedPathParametersUsed = 0;
    def pathParamNameUsageCount = [:].withDefault { 0 }

    def pathTemplate = ~/.*\{\w+\}.*/
    // If a path fragment contains double slash we need to replace it with something else to not mess up the path
    def hasPathParameterWithDoubleSlash = indexOf(pathWithoutQueryParams, DOUBLE_SLASH) != -1

    def tempParams;
    if (hasPathParameterWithDoubleSlash) {
      tempParams = replace(pathWithoutQueryParams, DOUBLE_SLASH, "RA_double_slash__");
    } else {
      tempParams = pathWithoutQueryParams
    }

    def pathParamFiller = { String separator, boolean performEncode, String acc, String subresource ->
      def indexOfStartBracket
      def indexOfEndBracket = 0
      while ((indexOfStartBracket = subresource.indexOf(TEMPLATE_START, indexOfEndBracket)) >= 0) {
        indexOfEndBracket = subresource.indexOf(TEMPLATE_END, indexOfStartBracket)
        // 3 means "{" and "}" and at least one character
        if (indexOfStartBracket >= 0 && indexOfEndBracket >= 0 && subresource.length() >= 3) {
          def pathParamValue
          def pathParamName = subresource.substring(indexOfStartBracket + 1, indexOfEndBracket)
          // Get path parameter name, what's between the "{" and "}"
          def value = findNamedPathParamValue(pathParamName, pathParamNameUsageCount)
          if (value == null && numberOfUnnamedPathParametersUsed < unnamedPathParamSize && unnamedPathParams[numberOfUnnamedPathParametersUsed].toString() != null) {
            pathParamValue = unnamedPathParams[numberOfUnnamedPathParametersUsed].toString()
            numberOfUnnamedPathParametersUsed += 1
          } else {
            // We return the template again if no match found since we might be interested in partially applied path
            pathParamValue = value == null ? TEMPLATE_START + pathParamName + TEMPLATE_END : value
          }

          def pathToPrepend = ""
          // If declared subresource has values before the first bracket then let's find it.
          if (indexOfStartBracket != 0) {
            pathToPrepend = subresource.substring(0, indexOfStartBracket)
          }

          def pathToAppend = ""
          // If declared subresource has values after the first bracket then let's find it.
          if (subresource.length() > indexOfEndBracket) {
            pathToAppend = subresource.substring(indexOfEndBracket + 1, subresource.length())
          }

          // Since the value of the path parameter might be shorter than the template name we need to
          // adjust the "indexOfEndBracket" index in case this subresource contains more templates after
          // this value.
          def lengthOfTemplate = length(pathParamName) + 2 // 2 because "{" and "}"
          def lengthOfValue = length(pathParamValue)
          if (lengthOfTemplate != lengthOfValue) {
            if (lengthOfTemplate > lengthOfValue) {
              indexOfEndBracket -= (lengthOfTemplate - lengthOfValue)
            } else {
              indexOfEndBracket += (lengthOfValue - lengthOfTemplate)
            }
          }

          subresource = pathToPrepend + pathParamValue + pathToAppend
        }
      }
      format("%s${separator}%s", acc, performEncode ? encode(subresource, EncodingTarget.QUERY) : subresource).toString()
    }

    pathWithoutQueryParams = split(tempParams, "/").inject("", pathParamFiller.curry("/", encodePath))

    if (hasPathParameterWithDoubleSlash) {
      // Now get the double slash replacement back to normal double slashes
      pathWithoutQueryParams = replace(pathWithoutQueryParams, "RA_double_slash__", encode(DOUBLE_SLASH, EncodingTarget.QUERY))
    }

    if (shouldAppendSlashAfterEncoding) {
      pathWithoutQueryParams += "/"
    }

    if (queryParams.matches(pathTemplate)) {
      // Note that we do NOT url encode query params here, that happens by UriBuilder at a later stage.
      queryParams = split(queryParams, "&").inject("", pathParamFiller.curry("&", false)).substring(1)
      // 1 means that we remove first & since query parameters starts with ?

    }
    host + (isEmpty(queryParams) ? pathWithoutQueryParams : pathWithoutQueryParams + "?" + queryParams)
  }


  private String findNamedPathParamValue(String pathParamName, pathParamNameUsageCount) {
    def pathParamValues = this.namedPathParameters.get(pathParamName);
    def pathParamValue
    if (pathParamValues instanceof Collection) {
      def pathParamCount = pathParamNameUsageCount[pathParamName]
      pathParamNameUsageCount[pathParamName] = pathParamCount++
      pathParamValue = pathParamValues.get(pathParamCount)
    } else {
      pathParamValue = pathParamValues
    }
    pathParamValue?.toString()
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
    if (!formParams.isEmpty()) {
      body.deleteCharAt(body.length() - 1); //Delete last &
    }
    return body.toString();
  }


  private String encode(Object string, EncodingTarget encodingType) {
    string = string.toString()
    if (urlEncodingEnabled) {
      def charset
      if (encodingType == EncodingTarget.BODY) {
        charset = encoderConfig().defaultContentCharset()
        def contentType = headers.getValue(CONTENT_TYPE)
        if (contentType instanceof String) {
          def tempCharset = CharsetExtractor.getCharsetFromContentType(contentType as String)
          if (tempCharset != null) {
            charset = tempCharset
          } else if (encoderConfig().hasDefaultCharsetForContentType(contentType as String)) {
            charset = encoderConfig().defaultCharsetForContentType(contentType as String)
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

  void setResponseSpecification(ResponseSpecification responseSpecification) {
    this.responseSpecification = responseSpecification
  }

  String getBaseUri() {
    return baseUri
  }

  String getBasePath() {
    return basePath
  }

  String getDerivedPath() {
    def uri = partiallyApplyPathParams(path, true, unnamedPathParamsTuples.collect { it.second })
    getDerivedPath(uri)
  }

  String getUserDefinedPath() {
    return PathSupport.getPath(path)
  }

  String getMethod() {
    return method
  }

  String getURI() {
    def uri = partiallyApplyPathParams(path, true, unnamedPathParamsTuples.collect { it.second })
    getURI(uri);
  }

  int getPort() {
    def host = new URL(getTargetURI(path))
    return host.getPort()
  }

  Map<String, String> getFormParams() {
    return Collections.unmodifiableMap(formParameters)
  }

  Map<String, String> getPathParams() {
    def namedPathParams = getNamedPathParams()
    def map = new LinkedHashMap<String, String>(namedPathParams)
    map.putAll(getUnnamedPathParams().findAll { !namedPathParams.keySet().contains(it.key) })
    return Collections.unmodifiableMap(map)
  }

  Map<String, String> getNamedPathParams() {
    return Collections.unmodifiableMap(namedPathParameters)
  }

  Map<String, String> getUnnamedPathParams() {
    // If it.first = null means that it's a placeholder
    def map = unnamedPathParamsTuples.findAll { it.first != null }.inject([:], { m, t ->
      m.putAt(t.first, t.second)
      m
    })
    return Collections.unmodifiableMap(map)
  }

  List<String> getUnnamedPathParamValues() {
    return Collections.unmodifiableList(unnamedPathParamsTuples == null ? Collections.emptyList() : unnamedPathParamsTuples.findAll {
      it.second != null
    }.collect {
      it.second
    })
  }

  Map<String, String> getRequestParams() {
    return Collections.unmodifiableMap(requestParameters)
  }

  Map<String, String> getQueryParams() {
    return Collections.unmodifiableMap(queryParameters)
  }

  List<MultiPartSpecification> getMultiPartParams() {
    return multiParts.collect {
      new MultiPartSpecificationImpl(content: it.content, charset: it.charset, fileName: it.fileName, mimeType: it.mimeType, controlName: it.controlName, headers: it.headers)
    }
  }

  Headers getHeaders() {
    return requestHeaders
  }

  Cookies getCookies() {
    return cookies
  }

  def getBody() {
    requestBody
  }

  List<Filter> getDefinedFilters() {
    return Collections.unmodifiableList(filters)
  }

  RestAssuredConfig getConfig() {
    return restAssuredConfig
  }

  HttpClient getHttpClient() {
    return httpClient
    // @Delegate doesn't work because of http://jira.codehaus.org/browse/GROOVY-4647 (when it's fixed 9619c3b should be used instead)
  }

  ProxySpecification getProxySpecification() {
    return proxySpecification
  }

  FilterableRequestSpecification path(String path) {
    notNull path, "Path"
    this.path = trimToEmpty(path)
    return this
  }

  List<String> getUndefinedPathParamPlaceholders() {
    def uri = partiallyApplyPathParams(path, false, unnamedPathParamsTuples.collect { it.second })
    getPlaceholders(uri)
  }

  List<String> getPathParamPlaceholders() {
    def uri = getTargetPath(contains(path, "://") ? substringAfter(path, "://") : path)
    getPlaceholders(uri)
  }

  String getRequestContentType() {
    return getContentType()
  }

  @Override
  String getContentType() {
    return requestHeaders.getValue(CONTENT_TYPE)
  }

  @Override
  RequestSpecification noFilters() {
    this.filters.clear()
    this
  }

  @Override
  RequestSpecification noFiltersOfType(Class filterType) {
    notNull filterType, "Filter type"
    this.filters.removeAll { filterType.isAssignableFrom(it.getClass()) }
    this
  }

  private class RestAssuredHttpBuilder extends HTTPBuilder {
    def assertionClosure

    RestAssuredHttpBuilder(Object defaultURI, assertionClosure, boolean urlEncodingEnabled, RestAssuredConfig config, AbstractHttpClient client) throws URISyntaxException {
      super(defaultURI, urlEncodingEnabled, config?.getEncoderConfig(), config?.getDecoderConfig(), config?.getOAuthConfig(), client)
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
      Object acceptContentType = delegate.getContentType()
      if (!requestHeaders.hasHeaderWithName("Accept")) {
        String acceptContentTypes = acceptContentType.toString()
        if (acceptContentType instanceof ContentType)
          acceptContentTypes = ((ContentType) acceptContentType).getAcceptHeader()
        reqMethod.setHeader("Accept", acceptContentTypes)
      }
      reqMethod.setURI(delegate.getUri().toURI())
      if (shouldApplyContentTypeFromRestAssuredConfigDelegate(delegate, reqMethod)) {
        def contentTypeToUse = trim(delegate.getRequestContentType())
        reqMethod.setHeader(CONTENT_TYPE, contentTypeToUse);
      }
      if (reqMethod.getURI() == null)
        throw new IllegalStateException("Request URI cannot be null")
      Map<?, ?> headers1 = delegate.getHeaders()
      for (Object key : headers1.keySet()) {
        if (key == null) continue;
        Object val = headers1.get(key);
        if (val == null) {
          reqMethod.removeHeaders(key.toString())
        } else if (!key.toString().equalsIgnoreCase(CONTENT_TYPE) || !val.toString().startsWith(MULTIPART_CONTENT_TYPE_PREFIX_WITH_SLASH)) {
          // Don't overwrite multipart header because HTTP Client have added boundary
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

        Object returnVal;
        Object[] closureArgs = null;
        switch (responseClosure.getMaximumNumberOfParameters()) {
          case 1:
            returnVal = responseClosure.call(resp);
            break;
          case 2: // parse the response entity if the response handler expects it:
            HttpEntity entity = resp.getEntity();
            try {
              if (entity == null || entity.getContentLength() == 0) {
                returnVal = responseClosure.call(resp, EMPTY);
              } else {
                returnVal = responseClosure.call(resp, this.parseResponse(resp, acceptContentType));
              }
            } catch (Exception ex) {
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
          if (entity != null) EntityUtils.consumeQuietly(entity);
        }
        // Close idle connections to the server
        def connectionConfig = connectionConfig()
        if (connectionConfig.shouldCloseIdleConnectionsAfterEachResponse()) {
          def closeConnectionConfig = connectionConfig.closeIdleConnectionConfig()
          client.getConnectionManager().closeIdleConnections(closeConnectionConfig.getIdleTime(), closeConnectionConfig.getTimeUnit());
        }
      }
    }

    /*
     * Is is for
     */

    private boolean shouldApplyContentTypeFromRestAssuredConfigDelegate(delegate, HttpRequestBase reqMethod) {
      def requestContentType = delegate.getRequestContentType()
      requestContentType != null && requestContentType != ANY.toString() &&
              (!reqMethod.hasProperty("entity") || reqMethod.entity?.contentType == null) &&
              !reqMethod.getAllHeaders().any { it.getName().equalsIgnoreCase(CONTENT_TYPE) }
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
      Parser definedDefaultParser = responseSpecification.rpr.defaultParser
      if (definedDefaultParser != null && ANY.toString().equals(contentType.toString())) {
        try {
          HttpResponseContentTypeFinder.findContentType(resp);
        } catch (IllegalArgumentException ignored) {
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

  private def applyProxySettings(RestAssuredHttpBuilder http) {
    // make client aware of JRE proxy settings http://freeside.co/betamax/
    http.client.routePlanner = new RestAssuredProxySelectorRoutePlanner(http.client.connectionManager.schemeRegistry,
            new RestAssuredProxySelector(delegatingProxySelector: ProxySelector.default, proxySpecification: proxySpecification), proxySpecification)
    if (proxySpecification?.hasAuth()) {
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      def address = new InetSocketAddress(proxySpecification.host, proxySpecification.port)
      // We need to convert the host to an IP since that's what our proxy selector (RestAssuredProxySelector) expects
      def authScope = new AuthScope(address.getAddress().getHostAddress(), proxySpecification.getPort())
      def credentials = new UsernamePasswordCredentials(proxySpecification.username, proxySpecification.password)
      credsProvider.setCredentials(authScope, credentials);
      http.client.setCredentialsProvider(credsProvider);
    }
  }

  private String assembleCompleteTargetPath(requestPath) {
    def targetUri
    def targetPath
    if (isFullyQualified(requestPath)) {
      targetUri = ""
      targetPath = ""
    } else {
      targetUri = getTargetURI(path)
      targetPath = substringBefore(getTargetPath(path), "?")
    }
    return mergeAndRemoveDoubleSlash(mergeAndRemoveDoubleSlash(targetUri, targetPath), requestPath);
  }

  private String findEncoderCharsetOrReturnDefault(String contentType) {
    def charset = CharsetExtractor.getCharsetFromContentType(contentType)
    if (charset == null) {
      final EncoderConfig cfg
      if (config == null) {
        cfg = new EncoderConfig()
      } else {
        cfg = config.getEncoderConfig()
      }

      if (cfg.hasDefaultCharsetForContentType(contentType)) {
        charset = cfg.defaultCharsetForContentType(contentType)
      } else {
        charset = cfg.defaultContentCharset()
      }
    }
    charset
  }

  private ObjectMapperConfig objectMappingConfig() {
    return config == null ? ObjectMapperConfig.objectMapperConfig() : config.getObjectMapperConfig();
  }

  private HttpClientConfig httpClientConfig() {
    return config == null ? HttpClientConfig.httpClientConfig() : config.getHttpClientConfig();
  }

  private ConnectionConfig connectionConfig() {
    return config == null ? ConnectionConfig.connectionConfig() : config.getConnectionConfig();
  }

  private EncoderConfig encoderConfig() {
    return config == null ? EncoderConfig.encoderConfig() : config.getEncoderConfig();
  }

  private SessionConfig sessionConfig() {
    return config == null ? SessionConfig.sessionConfig() : config.getSessionConfig();
  }

  RestAssuredConfig restAssuredConfig() {
    config ?: new RestAssuredConfig()
  }

  private enum EncodingTarget {
    BODY, QUERY
  }

  static List getPlaceholders(String uri) {
    Pattern p = Pattern.compile(Pattern.quote(TEMPLATE_START) + "(.*?)" + Pattern.quote(TEMPLATE_END))
    Matcher m = p.matcher(uri)
    def placeholders = new LinkedHashSet<String>() // Remove duplicates such as if we have get("/{x}/{x}")
    while (m.find()) {
      placeholders << m.group(1)?.trim()
    }
    return Collections.unmodifiableList(new ArrayList(placeholders))
  }

  static String getDerivedPath(String uri) {
    PathSupport.getPath(uri)
  }

  String getURI(String uri) {
    generateRequestUriForLogging(uri, method)
  }

  // Note that it's not possible to both redundant named and unnamed path parameters
  // as a map since redundant unnamed path parameters doesn't necessarily have a placeholder associated with it.
  // For example if we do get("/{x}", "1", "2") then there's no placeholder name for "2"
  Map<String, String> getRedundantNamedPathParams() {
    def placeholders = getPathParamPlaceholders()
    getNamedPathParams().findAll { !placeholders.contains(it.key) }.asImmutable()
  }

  List<String> getRedundantUnnamedPathParamValues() {
    def allPathParams = getPathParams()
    if (getPathParamPlaceholders().minus(allPathParams.keySet()).size() +
            Math.max(getUnnamedPathParamValues().size() - getPathParamPlaceholders().size(), 0) > 0) {
      return (getUnnamedPathParamValues().minus(allPathParams.values())).asImmutable()
    }
    Collections.unmodifiableList(Collections.emptyList())
  }

  void removeUnnamedPathParamAtIndex(int indexOfParamName) {
    unnamedPathParamsTuples.remove(indexOfParamName)
    // We define the a tuple with "null, null" in order to retain path parameter order
    unnamedPathParamsTuples.add(indexOfParamName, new Tuple2<String, String>(null, null))
  }

  public void setMethod(String method) {
    this.method = method == null ? null : method.toUpperCase()
  }

  private static int getFilterOrder(Filter filter) {
    return (filter instanceof OrderedFilter) ? ((OrderedFilter) filter).getOrder()
            : OrderedFilter.DEFAULT_PRECEDENCE;
  }
}

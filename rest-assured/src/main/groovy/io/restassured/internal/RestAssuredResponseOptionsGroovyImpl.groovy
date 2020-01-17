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

import groovy.xml.StreamingMarkupBuilder
import io.restassured.assertion.CookieMatcher
import io.restassured.common.mapper.DataToDeserialize
import io.restassured.common.mapper.TypeRef
import io.restassured.config.DecoderConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.LogDetail
import io.restassured.filter.time.TimingFilter
import io.restassured.http.Cookie
import io.restassured.http.Cookies
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.internal.http.CharsetExtractor
import io.restassured.internal.mapping.ObjectMapperDeserializationContextImpl
import io.restassured.internal.mapping.ObjectMapping
import io.restassured.internal.print.ResponsePrinter
import io.restassured.internal.support.CloseHTTPClientConnectionInputStreamWrapper
import io.restassured.internal.support.Prettifier
import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperDeserializationContext
import io.restassured.mapper.ObjectMapperType
import io.restassured.path.json.JsonPath
import io.restassured.path.json.config.JsonPathConfig
import io.restassured.path.xml.XmlPath
import io.restassured.path.xml.XmlPath.CompatibilityMode
import io.restassured.path.xml.config.XmlPathConfig
import io.restassured.response.ResponseBody
import io.restassured.response.ResponseBodyData
import io.restassured.response.ResponseOptions
import org.apache.commons.lang3.StringUtils

import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import static io.restassured.internal.common.assertion.AssertParameter.notNull
import static io.restassured.path.json.config.JsonPathConfig.jsonPathConfig
import static io.restassured.path.xml.config.XmlPathConfig.xmlPathConfig
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase
import static org.apache.commons.lang3.StringUtils.isBlank

class RestAssuredResponseOptionsGroovyImpl {
  private static final String CANNOT_PARSE_MSG = "Failed to parse response."
  public static final String BINARY = "binary"
  private static final long NO_RESPONSE_TIME = -1

  def responseHeaders
  Cookies cookies
  def content
  def contentType
  def statusLine
  def statusCode
  def sessionIdName
  Map filterContextProperties
  def connectionManager

  String defaultContentType
  ResponseParserRegistrar rpr

  DecoderConfig decoderConfig

  boolean hasExpectations

  RestAssuredConfig config

  void parseResponse(httpResponse, content, hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
    parseHeaders(httpResponse)
    parseContentType(httpResponse)
    parseCookies()
    parseStatus(httpResponse)
    if (hasBodyAssertions) {
      parseContent(content)
    } else {
      this.content = content
    }
    hasExpectations = hasBodyAssertions
    this.rpr = responseParserRegistrar
    this.defaultContentType = responseParserRegistrar.defaultParser?.getContentType()
  }

  def parseStatus(httpResponse) {
    statusLine = httpResponse.statusLine.toString()
    statusCode = httpResponse.statusLine.statusCode
  }

  def parseContentType(httpResponse) {
    try {
      contentType = httpResponse.contentType?.toString()
    } catch (IllegalArgumentException e) {
      // No content type was found, set it to empty
      contentType = ""
    }
  }

  def parseCookies() {
    if (headers.hasHeaderWithName("Set-Cookie")) {
      cookies = CookieMatcher.getCookies(headers.getValues("Set-Cookie"))
    }
  }

  def parseHeaders(httpResponse) {
    def headerList = []
    httpResponse.headers.each {
      def name = it.getName()
      def value = it.getValue()
      headerList << new Header(name, value)
    }
    this.responseHeaders = new Headers(headerList)
  }

  private def parseContent(content) {
    try {
      if (content instanceof InputStream) {
        this.content = convertToByteArray(content)
      } else if (content instanceof Writable) {
        this.content = toString(content)
      } else if (content instanceof String) {
        this.content = content
      } else {
        this.content = convertToString(content)
      }
    } catch (IllegalStateException e) {
      throw new IllegalStateException(CANNOT_PARSE_MSG, e)
    }
  }

  // TODO: Handle namespaces ??
  def toString(Writable node) {
    def writer = new StringWriter()
    writer << new StreamingMarkupBuilder().bind {
      // mkp.declareNamespace(dc: "http://purl.org/dc/elements/1.1/")
      mkp.yield node
    }
    return writer.toString()
  }

  String print() {
    def string = asString()
    content = string
    println string
    string
  }

  String prettyPrint(ResponseOptions responseOptions, ResponseBody responseBody) {
    def body = new Prettifier().getPrettifiedBodyIfPossible(responseOptions, responseBody)
    content = body
    println body
    body
  }

  def peek(ResponseOptions responseOptions, ResponseBody responseBody) {
    Set<String> blacklistedHeaders = getConfig()?.logConfig?.blacklistedHeaders() ?: Collections.<String> emptySet()
    ResponsePrinter.print(responseOptions, responseBody, System.out, LogDetail.ALL, false, blacklistedHeaders)
  }

  def prettyPeek(ResponseOptions responseOptions, ResponseBody responseBody) {
    Set<String> blacklistedHeaders = getConfig()?.logConfig?.blacklistedHeaders() ?: Collections.<String> emptySet()
    ResponsePrinter.print(responseOptions, responseBody, System.out, LogDetail.ALL, true, blacklistedHeaders)
  }

  String asString() {
    asString(false)
  }

  String asString(boolean forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
    charsetToString(findCharset(forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse))
  }

  boolean isInputStream() {
    content instanceof InputStream
  }

  InputStream asInputStream() {
    if (content == null || content instanceof InputStream) {
      new CloseHTTPClientConnectionInputStreamWrapper(config.getConnectionConfig(), connectionManager, content)
    } else {
      content instanceof String ? new ByteArrayInputStream(convertStringToByteArray(content)) : new ByteArrayInputStream(content)
    }
  }

  byte[] convertStringToByteArray(string) {
    string.getBytes(findCharset())
  }

  byte[] asByteArray() {
    if (content == null) {
      return new byte[0]
    }
    if (hasExpectations) {
      return content instanceof byte[] ? content : content.getBytes(findCharset())
    } else if (content instanceof byte[]) {
      content
    } else if (content instanceof String) {
      convertStringToByteArray(content)
    } else {
      content = convertStreamToByteArray(content)
      content
    }
  }

  def <T> T "as"(Type cls, ResponseBodyData responseBodyData) {
    def charset = findCharset()
    String contentTypeToChose = findContentType {
      throw new IllegalStateException("""Cannot parse content to $cls because no content-type was present in the response and no default parser has been set.\nYou can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n
or you can specify an explicit ObjectMapper using as($cls, <ObjectMapper>);""")
    }
    return ObjectMapping.deserialize(responseBodyData, cls, contentTypeToChose, defaultContentType, charset, null, config.getObjectMapperConfig())
  }

  def <T> T "as"(Type cls, ObjectMapperType mapperType, ResponseBodyData responseBodyData) {
    notNull mapperType, "Object mapper type"
    def charset = findCharset()
    return ObjectMapping.deserialize(responseBodyData, cls, null, defaultContentType, charset, mapperType, config.getObjectMapperConfig())
  }

  def <T> T "as"(Type cls, ObjectMapper mapper) {
    notNull mapper, "Object mapper"
    def ctx = createObjectMapperDeserializationContext(cls)
    return mapper.deserialize(ctx) as T
  }

  def <T> T "as"(TypeRef<T> typeRef, ResponseBodyData responseBodyData) {
    notNull typeRef, "Type ref"
    return "as"(typeRef.getType(), responseBodyData)
  }

  String findCharset() {
    return findCharset(false)
  }

  String findCharset(boolean forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
    String charset = CharsetExtractor.getCharsetFromContentType(isBlank(contentType) ? defaultContentType : contentType)

    if (charset == null || charset.trim().equals("")) {
      if (decoderConfig == null || forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
        return Charset.defaultCharset().toString()
      } else {
        charset = decoderConfig.defaultCharsetForContentType(contentType)
      }
    }

    if (StringUtils.equalsIgnoreCase(charset, BINARY)) {
      charset = decoderConfig.defaultCharsetForContentType(contentType)
    }

    return charset
  }

  Cookies detailedCookies() {
    if (cookies == null) {
      return new Cookies()
    }
    return cookies
  }

  Cookies getDetailedCookies() {
    return detailedCookies()
  }

  Cookie detailedCookie(String name) {
    return detailedCookies().get(name)
  }

  Cookie getDetailedCookie(String name) {
    return detailedCookie(name)
  }

  Headers headers() {
    return responseHeaders ?: new Headers()
  }

  Headers getHeaders() {
    return headers()
  }

  String header(String name) {
    notNull(name, "name")
    return responseHeaders.getValue(name)
  }

  String getHeader(String name) {
    return header(name)
  }

  Map<String, String> cookies() {
    def cookieMap = [:]
    cookies.each { cookie ->
      cookieMap.put(cookie.name, cookie.value)
    }
    return Collections.unmodifiableMap(cookieMap)
  }

  Map<String, String> getCookies() {
    return cookies()
  }

  String cookie(String name) {
    notNull(name, "name")
    return cookies == null ? null : cookies.getValue(name)
  }

  String getCookie(String name) {
    return cookie(name)
  }

  String contentType() {
    return contentType
  }

  ResponseParserRegistrar getRpr() {
    return rpr
  }

  RestAssuredConfig getConfig() {
    return config
  }

  String getContentType() {
    return contentType
  }

  String statusLine() {
    return statusLine
  }

  int statusCode() {
    return statusCode ?: -1
  }

  String getStatusLine() {
    return statusLine()
  }

  String sessionId() {
    return getSessionId()
  }

  String getSessionId() {
    return cookie(sessionIdName)
  }

  int getStatusCode() {
    return statusCode()
  }

  JsonPath jsonPath() {
    jsonPath(jsonPathConfig().charset(findCharset()).
            jackson1ObjectMapperFactory(config.getObjectMapperConfig().jackson1ObjectMapperFactory()).
            jackson2ObjectMapperFactory(config.getObjectMapperConfig().jackson2ObjectMapperFactory()).
            gsonObjectMapperFactory(config.getObjectMapperConfig().gsonObjectMapperFactory()).
            numberReturnType(config.getJsonConfig().numberReturnType()))
  }

  JsonPath jsonPath(JsonPathConfig config) {
    notNull(config, "JsonPathConfig")
    new JsonPath(asString()).using(config)
  }

  XmlPath xmlPath() {
    xmlPath(CompatibilityMode.XML)
  }

  XmlPath xmlPath(XmlPathConfig config) {
    newXmlPath(CompatibilityMode.XML, config)
  }

  XmlPath xmlPath(CompatibilityMode compatibilityMode) {
    notNull(compatibilityMode, "Compatibility mode")
    newXmlPath(compatibilityMode)
  }

  XmlPath htmlPath() {
    return xmlPath(CompatibilityMode.HTML)
  }

  def <T> T path(String path, String... arguments) {
    notNull path, "Path"
    if (arguments?.length > 0) {
      path = String.format(path, arguments)
    }
    def contentType = findContentType {
      throw new IllegalStateException("""Cannot invoke the path method because no content-type was present in the response and no default parser has been set.\n
You can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n""")
    }
    if (containsIgnoreCase(contentType, "xml")) {
      return xmlPath().get(path)
    } else if (containsIgnoreCase(contentType, "json")) {
      return jsonPath().get(path)
    } else if (containsIgnoreCase(contentType, "html")) {
      return newXmlPath(CompatibilityMode.HTML)
    }
    throw new IllegalStateException("Cannot determine which path implementation to use because the content-type $contentType doesn't map to a path implementation.")
  }


  long time() {
    if (filterContextProperties?.containsKey(TimingFilter.RESPONSE_TIME_MILLISECONDS)) {
      filterContextProperties.get(TimingFilter.RESPONSE_TIME_MILLISECONDS)
    } else {
      NO_RESPONSE_TIME
    }
  }

  long timeIn(TimeUnit timeUnit) {
    notNull timeUnit, TimeUnit.class
    def time = time()
    if (time != NO_RESPONSE_TIME && timeUnit != TimeUnit.MILLISECONDS) {
      time = timeUnit.convert(time, TimeUnit.MILLISECONDS)
    }
    time
  }

  private convertToByteArray(InputStream stream) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream()
    int nRead
    byte[] data = new byte[16384]

    try {
      while ((nRead = stream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead)
      }
      buffer.flush()
    } finally {
      stream.close()
    }
    return buffer.toByteArray()
  }

  private String convertToString(Reader reader) {
    if (reader == null) {
      return ""
    }

    Writer writer = new StringWriter()
    char[] buffer = new char[1024]
    try {
      int n
      while ((n = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, n)
      }
    } finally {
      reader?.close()
    }
    return writer.toString()
  }

  private static byte[] convertStreamToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream()
    try {
      int nRead
      byte[] data = new byte[16384]

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead)
      }

      buffer.flush()
    } finally {
      buffer?.close()
      is?.close()
    }

    return buffer.toByteArray()
  }

  private String findContentType(Closure closure) {
    def contentTypeToChose = null
    if (contentType == "") {
      if (defaultContentType != null) {
        contentTypeToChose = defaultContentType
      } else {
        closure.call()
      }
    } else if (rpr.hasCustomParserExcludingDefaultParser(contentType)) {
      contentTypeToChose = rpr.getNonDefaultParser(contentType).contentType
    } else {
      contentTypeToChose = contentType
    }
    return contentTypeToChose
  }

  private def newXmlPath(CompatibilityMode xml) {
    newXmlPath(xml, xmlPathConfig().charset(findCharset()).
            features(config.getXmlConfig().features()).
            properties(config.getXmlConfig().properties()).
            declareNamespaces(config.getXmlConfig().declaredNamespaces()).
            jaxbObjectMapperFactory(config.getObjectMapperConfig().jaxbObjectMapperFactory()))
  }

  private def newXmlPath(CompatibilityMode mode, XmlPathConfig config) {
    notNull(config, "XmlPathConfig")
    new XmlPath(mode, asString()).using(config)
  }

  def charsetToString(charset) {
    if (content == null) {
      return ""
    }

    if (content instanceof String) {
      content
    } else if (content instanceof byte[]) {
      new String(content, charset)
    } else {
      content = convertStreamToByteArray(content)
      new String(content, charset)
    }
  }

  private ObjectMapperDeserializationContext createObjectMapperDeserializationContext(Type cls) {
    def ctx = new ObjectMapperDeserializationContextImpl()
    ctx.type = cls
    ctx.charset = findCharset()
    ctx.contentType = contentType()
    ctx.dataToDeserialize = new DataToDeserialize() {
      @Override
      String asString() {
        return RestAssuredResponseOptionsGroovyImpl.this.asString()
      }

      @Override
      byte[] asByteArray() {
        return RestAssuredResponseOptionsGroovyImpl.this.asByteArray()
      }

      @Override
      InputStream asInputStream() {
        return RestAssuredResponseOptionsGroovyImpl.this.asInputStream()
      }
    }
    ctx
  }
}
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

import com.jayway.restassured.assertion.CookieMatcher
import com.jayway.restassured.config.RestAssuredConfig
import com.jayway.restassured.filter.log.LogDetail
import com.jayway.restassured.internal.http.CharsetExtractor
import com.jayway.restassured.internal.mapper.ObjectMapperType
import com.jayway.restassured.internal.mapping.ObjectMapperDeserializationContextImpl
import com.jayway.restassured.internal.mapping.ObjectMapping
import com.jayway.restassured.internal.print.ResponsePrinter
import com.jayway.restassured.internal.support.CloseHTTPClientConnectionInputStreamWrapper
import com.jayway.restassured.internal.support.Prettifier
import com.jayway.restassured.mapper.DataToDeserialize
import com.jayway.restassured.mapper.ObjectMapper
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext
import com.jayway.restassured.path.json.JsonPath
import com.jayway.restassured.path.json.config.JsonPathConfig
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.XmlPath.CompatibilityMode
import com.jayway.restassured.path.xml.config.XmlPathConfig
import com.jayway.restassured.response.*
import groovy.xml.StreamingMarkupBuilder
import org.apache.commons.lang3.StringUtils

import java.nio.charset.Charset

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull
import static com.jayway.restassured.path.json.config.JsonPathConfig.jsonPathConfig
import static com.jayway.restassured.path.xml.config.XmlPathConfig.xmlPathConfig
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase
import static org.apache.commons.lang3.StringUtils.isBlank

class RestAssuredResponseOptionsGroovyImpl {
  private static final String CANNOT_PARSE_MSG = "Failed to parse response."
  public static final String BINARY = "binary"
  def responseHeaders
  def Cookies cookies
  def content
  def contentType
  def statusLine
  def statusCode
  def sessionIdName
  def connectionManager;

  def String defaultContentType
  def ResponseParserRegistrar rpr

  def String defaultCharset

  def boolean hasExpectations

  def RestAssuredConfig config

  public void parseResponse(httpResponse, content, hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
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
    def headerList = [];
    httpResponse.headers.each {
      def name = it.getName()
      def value = it.getValue();
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
    return writer.toString();
  }

  String print() {
    def string = asString();
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
    ResponsePrinter.print(responseOptions, responseBody, System.out, LogDetail.ALL, false);
  }

  def prettyPeek(ResponseOptions responseOptions, ResponseBody responseBody) {
    ResponsePrinter.print(responseOptions, responseBody, System.out, LogDetail.ALL, true);
  }

  String asString() {
    asString(false)
  }

  def String asString(boolean forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
    charsetToString(findCharset(forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse))
  }

  InputStream asInputStream() {
    if (content == null || content instanceof InputStream) {
      new CloseHTTPClientConnectionInputStreamWrapper(config.getConnectionConfig(), connectionManager, content)
    } else {
      content instanceof String ? new ByteArrayInputStream(convertStringToByteArray(content)) : new ByteArrayInputStream(content)
    }
  }

  def byte[] convertStringToByteArray(string) {
    string.getBytes(findCharset())
  }

  byte[] asByteArray() {
    if (content == null) {
      return new byte[0];
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

  def <T> T "as"(Class<T> cls, ResponseBodyData responseBodyData) {
    def charset = findCharset();
    String contentTypeToChose = findContentType {
      throw new IllegalStateException("""Cannot parse content to $cls because no content-type was present in the response and no default parser has been set.\nYou can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n
or you can specify an explicit ObjectMapper using as($cls, <ObjectMapper>);""")
    }
    return ObjectMapping.deserialize(responseBodyData, cls, contentTypeToChose, defaultContentType, charset, null, config.getObjectMapperConfig())
  }

  def <T> T "as"(Class<T> cls, ObjectMapperType mapperType, ResponseBodyData responseBodyData) {
    notNull mapperType, "Object mapper type"
    def charset = findCharset()
    return ObjectMapping.deserialize(responseBodyData, cls, null, defaultContentType, charset, mapperType, config.getObjectMapperConfig())
  }

  def <T> T "as"(Class<T> cls, ObjectMapper mapper) {
    notNull mapper, "Object mapper"
    def ctx = createObjectMapperDeserializationContext(cls)
    return mapper.deserialize(ctx) as T
  }

  def String findCharset() {
    return findCharset(false)
  }

  def String findCharset(boolean forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
    String charset = CharsetExtractor.getCharsetFromContentType(isBlank(contentType) ? defaultContentType : contentType)

    if (charset == null || charset.trim().equals("")) {
      if (defaultCharset == null || forcePlatformDefaultCharsetIfNoCharsetIsSpecifiedInResponse) {
        return Charset.defaultCharset()
      } else {
        charset = defaultCharset
      }
    }

    if (StringUtils.equalsIgnoreCase(charset, BINARY)) {
      charset = defaultCharset
    }

    return charset;
  }

  def Cookies detailedCookies() {
    if (cookies == null) {
      return new Cookies()
    }
    return cookies
  }

  def Cookies getDetailedCookies() {
    return detailedCookies()
  }

  def Cookie detailedCookie(String name) {
    return detailedCookies().get(name)
  }

  def Cookie getDetailedCookie(String name) {
    return detailedCookie(name)
  }

  Headers headers() {
    return responseHeaders
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
    return statusCode
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
            numberReturnType(config.getJsonConfig().numberReturnType()));
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
      path = String.format(path, arguments);
    }
    def contentType = findContentType {
      throw new IllegalStateException("""Cannot invoke the path method because no content-type was present in the response and no default parser has been set.\n
You can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n""")
    };
    if (containsIgnoreCase(contentType, "xml")) {
      return xmlPath().get(path)
    } else if (containsIgnoreCase(contentType, "json")) {
      return jsonPath().get(path)
    } else if (containsIgnoreCase(contentType, "html")) {
      return newXmlPath(CompatibilityMode.HTML)
    }
    throw new IllegalStateException("Cannot determine which path implementation to use because the content-type $contentType doesn't map to a path implementation.")
  }

  private convertToByteArray(InputStream stream) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];

    try {
      while ((nRead = stream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
    } finally {
      stream.close()
    }
    return buffer.toByteArray();
  }

  private String convertToString(Reader reader) {
    if (reader == null) {
      return "";
    }

    Writer writer = new StringWriter();
    char[] buffer = new char[1024];
    try {
      int n;
      while ((n = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, n);
      }
    } finally {
      reader?.close();
    }
    return writer.toString();
  }

  private static byte[] convertStreamToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
    } finally {
      buffer?.close();
      is?.close();
    }

    return buffer.toByteArray();
  }

  private String findContentType(Closure closure) {
    def contentTypeToChose = null
    if (contentType == "") {
      if (defaultContentType != null) {
        contentTypeToChose = defaultContentType
      } else {
        closure.call()
      }
    } else if (rpr.hasCustomParserExludingDefaultParser(contentType)) {
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

  private ObjectMapperDeserializationContext createObjectMapperDeserializationContext(Class cls) {
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
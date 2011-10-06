/*
 * Copyright 2011 the original author or authors.
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

import com.jayway.restassured.assertion.CookieMatcher
import com.jayway.restassured.internal.mapping.ObjectMapping
import com.jayway.restassured.response.Response
import com.jayway.restassured.response.ResponseBody
import groovy.xml.StreamingMarkupBuilder
import java.nio.charset.Charset
import static com.jayway.restassured.assertion.AssertParameter.notNull
import com.jayway.restassured.mapper.ObjectMapper
import com.jayway.restassured.path.json.JsonPath
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.XmlPath.CompatibilityMode

class RestAssuredResponseImpl implements Response {
  private static final String CANNOT_PARSE_MSG = "Failed to parse response."
  def responseHeaders = [:]
  def cookies = [:]
  def content
  def contentType
  def statusLine
  def statusCode

  def String defaultContentType
  def ResponseParserRegistrar rpr

  def boolean hasExpectations

  public void parseResponse(httpResponse, content, hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
    parseHeaders(httpResponse)
    parseContentType(httpResponse)
    parseCookies()
    parseStatus(httpResponse)
    if(hasBodyAssertions) {
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
      contentType = httpResponse.contentType?.toString()?.toLowerCase()
    } catch(IllegalArgumentException e) {
      // No content type was found, set it to empty
      contentType = ""
    }
  }

  def parseCookies() {
    if(headers.containsKey("Set-Cookie")) {
      cookies = CookieMatcher.getCookieMap(headers.get("Set-Cookie"))
    }
  }

  def parseHeaders(httpResponse) {
    httpResponse.headers.each {
      responseHeaders.put(it.getName(), it.getValue())
    }
  }

  private def parseContent(content) {
    def abstractJsonClass = Class.forName("net.sf.json.AbstractJSON", true, Thread.currentThread().getContextClassLoader());
    try {
      if (content instanceof InputStream) {
        this.content = convertToByteArray(content)
      } else if(abstractJsonClass.isAssignableFrom(content.getClass())) {
        this.content = content.toString(1)
      } else if(content instanceof Writable) {
        this.content = toString(content)
      } else if(content instanceof String) {
        this.content = content
      } else {
        this.content = convertToString(content)
      }
    } catch (IllegalStateException e) {
      throw new IllegalStateException(CANNOT_PARSE_MSG, e)
    }
  }

  // TODO: Handle nachmespaces
  def toString(Writable node) {
    def writer = new StringWriter()
    writer << new StreamingMarkupBuilder().bind {
      // mkp.declareNamespace('':node[0].namespaceURI())
      mkp.yield node
    }
    return writer.toString();
  }

  String print() {
    def string = asString();
    println string
    string
  }

  String asString() {
    if(content == null) {
      return ""
    }
    if(hasExpectations) {
      return content instanceof String ? content : new String(content, findCharset())
    } else {
      return convertStreamToString(content)
    }
  }

  InputStream asInputStream() {
    if(content == null || content instanceof InputStream) {
      return content
    } else {
      content instanceof String ? new ByteArrayInputStream(content.getBytes(findCharset())) : new ByteArrayInputStream(content)
    }
  }

  byte[] asByteArray() {
    if(content == null) {
      return new byte[0];
    }
    if(hasExpectations) {
      return content instanceof byte[] ? content : content.getBytes(findCharset())
    } else {
      return convertStreamToByteArray(content)
    }
  }

  def <T> T "as"(Class<T> cls) {
    String contentTypeToChose = findContentType {
      throw new IllegalStateException("""Cannot parse content to $cls because no content-type was present in the response and no default parser has been set.\nYou can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n
or you can specify an explicit ObjectMapper using as($cls, <ObjectMapper>);""")
    }
    return ObjectMapping.deserialize(asString(), cls, contentTypeToChose, defaultContentType, null)
  }

  def <T> T "as"(Class<T> cls, ObjectMapper mapper) {
    notNull mapper, "Object mapper"
    return ObjectMapping.deserialize(asString(), cls, null, defaultContentType, mapper)
  }

  private Charset findCharset() {
    String charset = headers.get("charset");

    if ( charset == null || charset.trim().equals("") ) {
      return Charset.defaultCharset();
    }

    return Charset.forName(charset);
  }


  Response andReturn() {
    return this
  }

  ResponseBody body() {
    return this
  }

  Response thenReturn() {
    return this
  }

  ResponseBody getBody() {
    return body()
  }

  Map<String, String> headers() {
    return Collections.unmodifiableMap(responseHeaders)
  }

  Map<String, String> getHeaders() {
    return headers()
  }

  String header(String name) {
    notNull(name, "name")
    return responseHeaders.get(name)
  }

  String getHeader(String name) {
    return header(name)
  }

  Map<String, String> cookies() {
    return Collections.unmodifiableMap(cookies)
  }

  Map<String, String> getCookies() {
    return cookies()
  }

  String cookie(String name) {
    notNull(name, "name")
    return cookies.get(name)
  }

  String getCookie(String name) {
    return cookie(name)
  }

  String contentType() {
    return contentType
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

  int getStatusCode() {
    return statusCode()
  }

  JsonPath jsonPath() {
    new JsonPath(asInputStream())
  }

  XmlPath xmlPath() {
    newXmlPath(CompatibilityMode.XML)
  }

  def <T> T path(String path) {
    notNull path, "Path"
    def contentType = findContentType {
      throw new IllegalStateException("""Cannot invoke the path method because no content-type was present in the response and no default parser has been set.\n
You can specify a default parser using e.g.:\nRestAssured.defaultParser = Parser.JSON;\n""")
    }.toLowerCase();
    if(contentType.contains("xml")) {
      return xmlPath().get(path)
    } else if(contentType.contains("json")) {
      return jsonPath().get(path)
    } else if(contentType.contains("html")) {
      return newXmlPath(CompatibilityMode.HTML)
    }
    throw new IllegalStateException("Cannot determine which path implementation to use because the content-type $contentType doesn't map to a path implementation.")
  }

  private convertToByteArray(InputStream stream) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];

    while ((nRead = stream.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }

    buffer.flush();
    return buffer.toByteArray();
  }

  private String convertToString(Reader reader) {
    if(reader == null) {
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
      reader.close();
    }
    return writer.toString();
  }

  private String convertStreamToString(InputStream is) throws IOException {
    Writer writer = new StringWriter();
    char[] buffer = new char[1024];
    Reader reader;
    try {
      reader = new BufferedReader(new InputStreamReader(is, findCharset()));
      int n;
      while ((n = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, n);
      }
    } finally {
      is.close();
      reader.close();
    }
    return writer.toString();
  }

  private byte[] convertStreamToByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();
    } finally {
      buffer.close();
      is.close();
    }

    return buffer.toByteArray();
  }

  private String findContentType(Closure closure) {
    def contentTypeToChose
    if (contentType == "") {
      if (defaultContentType != null) {
        contentTypeToChose = defaultContentType
      } else {
        closure.call()
      }
    } else if(rpr.hasCustomParserExludingDefaultParser(contentType)) {
      contentTypeToChose = rpr.getParser(contentType).contentType
    } else {
      contentTypeToChose = contentType
    }
    return contentTypeToChose
  }

  private def newXmlPath(CompatibilityMode xml) {
    new XmlPath(xml, asInputStream())
  }
}
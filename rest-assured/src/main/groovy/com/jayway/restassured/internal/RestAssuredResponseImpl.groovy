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
import com.jayway.restassured.response.Response
import com.jayway.restassured.response.ResponseBody
import groovy.xml.StreamingMarkupBuilder
import static com.jayway.restassured.assertion.AssertParameter.notNull

class RestAssuredResponseImpl implements Response {
  private static final String CANNOT_PARSE_MSG = "Failed to parse response."
  def responseHeaders = [:]
  def cookies = [:]
  def content
  def contentType
  def statusLine
  def statusCode

  public void parseResponse(httpResponse, content) {
    parseHeaders(httpResponse)
    parseContentType(httpResponse)
    parseCookies()
    parseStatus(httpResponse)
    parseContent(content)
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
    return content instanceof String ? content : new String(content)
  }

  byte[] asByteArray() {
    if(content == null) {
      return new byte[0];
    }
    return content instanceof byte[] ? content : content.getBytes()
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
}
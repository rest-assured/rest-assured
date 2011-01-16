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

import com.jayway.restassured.response.Response

class RestAssuredResponseImpl implements Response {
  private static final String CANNOT_PARSE_MSG = "You cannot use REST Assured expectations and return the response at the same time."
  def resp

  public void parseResponse(httpResponse) {
    try {
      if(httpResponse instanceof InputStream) {
        resp = convertToByteArray(httpResponse)
      } else {
        resp = convertToString(httpResponse)
      }
    } catch(IllegalStateException e) {
      throw new IllegalStateException(CANNOT_PARSE_MSG, e)
    }
  }

  String asString() {
    assertNotNull resp
    return resp instanceof String ? resp : new String(resp)
  }

  byte[] asByteArray() {
    assertNotNull resp
    return resp instanceof byte[] ? resp : resp.getBytes()
  }

  Response andReturn() {
    return this
  }

  Response body() {
    return this
  }

  Response thenReturn() {
    return this
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

  private def assertNotNull(resp) {
    if(!resp) {
      throw new IllegalStateException(CANNOT_PARSE_MSG)
    }
  }

  private def throwException() {

  }
}
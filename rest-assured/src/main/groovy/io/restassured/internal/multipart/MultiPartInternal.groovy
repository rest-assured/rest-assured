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
package io.restassured.internal.multipart

import groovy.transform.Canonical
import io.restassured.internal.NoParameterValue
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody

import java.nio.charset.Charset

@Canonical
class MultiPartInternal {

	public static final String OCTET_STREAM = "application/octet-stream"
  private static final String TEXT_PLAIN = "text/plain"

  def content
  def String controlName
  def String fileName
  def String mimeType
  def String charset
  def Map<String, String> headers = [:]

  def getContentBody() {
    if (content instanceof NoParameterValue) {
      content = "";
    }

    if (content instanceof File) {
      new FileBody(content, fileName, mimeType ?: OCTET_STREAM, charset)
    } else if (content instanceof InputStream) {
      returnInputStreamBody()
    } else if (content instanceof byte[]) {
      content = new ByteArrayInputStream(content)
      returnInputStreamBody()
    } else if (content instanceof String) {
      returnStringBody(content)
    } else if (content != null) {
      returnStringBody(content.toString())
    } else {
      throw new IllegalArgumentException("Illegal content: $content")
    }
  }

  String getMimeType() {
    if (content instanceof File) {
      mimeType ?: OCTET_STREAM
    } else if (content instanceof InputStream) {
      mimeType ?: OCTET_STREAM
    } else if (content instanceof byte[]) {
      mimeType ?: OCTET_STREAM
    } else if (content instanceof String) {
      mimeType ?: TEXT_PLAIN
    } else if (content != null) {
      mimeType ?: TEXT_PLAIN
    } else {
      mimeType
    }
  }

  private def returnStringBody(String content) {
    new StringBody(content, mimeType ?: TEXT_PLAIN, charset == null ? null : Charset.forName(charset))
  }

  private def returnInputStreamBody() {
    new InputStreamBody(content, mimeType ?: OCTET_STREAM, fileName)
  }
}

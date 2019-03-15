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

import io.restassured.specification.MultiPartSpecification
import org.apache.commons.lang3.StringUtils


class MultiPartSpecificationImpl implements MultiPartSpecification {
  private static final String NONE = '<none>'
  private static final String INPUT_STREAM = '<inputstream>'

  def content
  String controlName
  String mimeType
  String charset
  String fileName
  boolean controlNameSpecifiedExplicitly
  boolean fileNameSpecifiedExplicitly
  Map<String, String> headers

  Object getContent() {
    return content
  }

  String getControlName() {
    return controlName
  }

  String getMimeType() {
    return mimeType
  }

  String getCharset() {
    return charset
  }

  String getFileName() {
    return fileName
  }

  Map<String, String> getHeaders() {
    Collections.unmodifiableMap(headers)
  }

  boolean hasFileName() {
    fileName != null
  }

  void setFileName(String fileName) {
    this.fileName = StringUtils.trimToNull(fileName)
  }


  String toString() {
    return """controlName=${controlName ?: NONE}, mimeType=${mimeType ?: NONE}, charset=${charset ?: NONE}, fileName=${
      fileName ?: NONE
    }, content=${content instanceof InputStream ? INPUT_STREAM : content}, headers=${headers}"""
  }
}

/*
 * Copyright 2015 the original author or authors.
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

import com.jayway.restassured.specification.MultiPartSpecification
import org.apache.commons.lang3.StringUtils


class MultiPartSpecificationImpl implements MultiPartSpecification {
  private static final String NONE = '<none>'
  private static final String INPUT_STREAM = '<inputstream>'

  def content
  def String controlName
  def String mimeType
  def String charset
  def String fileName
  def boolean controlNameSpecifiedExplicitly
  def boolean fileNameSpecifiedExplicitly

  def Object getContent() {
    return content
  }

  def String getControlName() {
    return controlName
  }

  def String getMimeType() {
    return mimeType
  }

  def String getCharset() {
    return charset
  }

  def String getFileName() {
    return fileName
  }

  boolean hasFileName() {
    fileName != null
  }

  def void setFileName(String fileName) {
    this.fileName = StringUtils.trimToNull(fileName)
  }


  public String toString() {
    return """controlName=${controlName ?: NONE}, mimeType=${mimeType ?: NONE}, charset=${charset ?: NONE}, fileName=${
      fileName ?: NONE
    }, content=${content instanceof InputStream ? INPUT_STREAM : content}"""
  }
}

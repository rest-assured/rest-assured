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

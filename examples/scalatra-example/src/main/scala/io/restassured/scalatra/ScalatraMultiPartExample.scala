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

package io.restassured.scalatra

import org.apache.commons.fileupload.FileItem
import org.scalatra.ScalatraServlet
import org.scalatra.fileupload.FileUploadSupport

class ScalatraMultiPartExample extends ScalatraServlet with FileUploadSupport {

  post("/file") {
    val controlName = Option(request.getParameter("controlName")).getOrElse("file")
    getFileContent(controlName)
  }

  post("/file-utf8") {
    getFileContent("Cédrìc")
  }

  put("/file") {
    getFileContent()
  }

  get("/file") {
    getFileContent()
  }

  options("/file") {
    getFileContent()
  }

  patch("/file") {
    getFileContent()
  }

  patch("/file400") {
    error400
  }

  put("/file400") {
    error400
  }

  post("/file400") {
    error400
  }

  post("/text") {
    getText
  }

  delete("/text") {
    getText
  }

  post("/textAndReturnHeader") {
    response.setHeader("X-Request-Header", request.getHeader("Content-Type"))
    getText
  }

  post("/fileAndText") {
    getFileContent() + getText
  }

  post("/filename") {
    val option: Option[FileItem] = fileParams.get("file")
    if (option.isDefined) {
      option.get.getName
    } else {
      ""
    }
  }

  post("/string") {
    val control = multiParams.get("other").getOrElse(throw new IllegalArgumentException("Missing argument 'other'"))
    control.mkString(",")
  }


  post("/multiple") {
    multiParams.toString()
  }

  private def getFileContent(controlName: String = "file"): String = {
    val fileItem = fileParams.get(controlName).get
    fileItem.getString
  }

  private def getText: String = {
    val option = multiParams.get("text")
    val seq = option.get
    seq.mkString(",")
  }

  private def error400: String = {
    contentType = "application/json"
    status = 400
    """{ "error" : "message" } """
  }
}
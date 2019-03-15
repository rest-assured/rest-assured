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

import net.liftweb.json.JsonAST
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Printer._
import org.scalatra.ScalatraServlet
import org.scalatra.fileupload.FileUploadSupport

class SecuredScalatraRestExample extends ScalatraServlet with FileUploadSupport {

  before() {
    contentType = "application/json"
  }

  post("/hello") {
    val json = "hello" -> "Hello Secured Scalatra"
    compact(JsonAST.render(json))
  }

  get("/hello") {
    val json = "hello" -> "Hello Secured Scalatra"
    compact(JsonAST.render(json))
  }
  post("/file") {
    val fileItem = fileParams.get("file").get
    fileItem.getString
  }
}
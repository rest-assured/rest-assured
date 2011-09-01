/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.scalatra

/*
 * Copyright 2010 the original author or authors.
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
import org.scalatra.ScalatraServlet
import org.scalatra.fileupload.FileUploadSupport

class ScalatraMultiPartExample extends ScalatraServlet with FileUploadSupport {

  post("""/file""") {
    val fileItem = fileParams.get("file").get
    fileItem.getString

//    multiParams.get("string") foreach { ps: Seq[String] => response.setHeader("string", ps.mkString(";")) }
//    fileParams.get("file") foreach { fi => response.setHeader("file", new String(fi.get)) }
//    fileParams.get("file-none") foreach { fi => response.setHeader("file-none", new String(fi.get)) }
//    fileParams.get("file-multi") foreach { fi => response.setHeader("file-multi", new String(fi.get)) }
//    fileMultiParams.get("file-multi") foreach { fis =>
//      response.setHeader("file-multi-all", fis.foldLeft(""){ (acc, fi) => acc + new String(fi.get) })
//    }
//    params.get("file") foreach { response.setHeader("file-as-param", _) }
//    params("utf8-string")
  }
}
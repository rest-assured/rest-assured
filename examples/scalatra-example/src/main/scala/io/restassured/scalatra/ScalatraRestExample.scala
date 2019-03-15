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

import java.net.URLDecoder
import java.util.{Date, Scanner}

import io.restassured.scalatra.support.Gzip
import javax.servlet.http.Cookie
import net.liftweb.json.Extraction._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Printer._
import net.liftweb.json.{DefaultFormats, JsonParser}
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.scalatra.ScalatraServlet

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.Elem

class ScalatraRestExample extends ScalatraServlet {
  // To allow for json extract
  implicit val formats = DefaultFormats

  case class Winner(id: Long, numbers: List[Int])

  case class Lotto(id: Long, winningNumbers: List[Int], winners: List[Winner], drawDate: Option[java.util.Date])

  val winners = List(Winner(23, List(2, 45, 34, 23, 3, 5)), Winner(54, List(52, 3, 12, 11, 18, 22)))
  val lotto = Lotto(5, List(2, 45, 34, 23, 7, 5, 3), winners, None)

  before() {
    contentType = "application/json"
  }

  post("/hello") {
    val json = ("hello" -> "Hello Scalatra")
    compact(render(json))
  }

  get("/hello") {
    val json = ("hello" -> "Hello Scalatra")
    compact(render(json))
  }

  get("/getWithContent") {
    if (request.body == "hullo") {
      status = 200
    } else {
      status = 400
      "No or incorrect content"
    }
  }

  options("/greetXML") {
    contentType = "text/xml"
    greetXML
  }

  get("/greetXML") {
    greetXML
  }

  get("/xmlWithContentTypeTextXml") {
    contentType = "text/xml; charset=iso-8859-1"
    <greeting>
      <firstName>{params("firstName")}</firstName>
      <lastName>{params("lastName")}</lastName>
    </greeting>
  }

  get("/xmlWithCustomXmlContentType") {
    contentType = "application/something+xml; charset=iso-8859-1"
    <greeting>
      <firstName>{params("firstName")}</firstName>
      <lastName>{params("lastName")}</lastName>
    </greeting>
  }

  get("/greetXMLAttribute") {
    contentType = "application/xml"
    <greeting>
      <name firstName={params("firstName")} lastName={params("lastName")} />
    </greeting>
  }

  get("/i18n") {
     """{ "ön" : "Är ån"}"""
  }

  get("/something.json") {
    """{ "value" : "something" }"""
  }

  get("/utf8-body-json") {
    """{ "value" : "啊 ☆" }"""
  }

  get("/utf8-body-xml") {
    contentType = "application/xml"
    """<value>啊 ☆</value>"""
  }

  get("/jsonStore") {
    "{ \"store\": {\n" +
      "    \"book\": [ \n" +
      "      { \"category\": \"reference\",\n" +
      "        \"author\": \"Nigel Rees\",\n" +
      "        \"title\": \"Sayings of the Century\",\n" +
      "        \"price\": 8.95\n" +
      "      },\n" +
      "      { \"category\": \"fiction\",\n" +
      "        \"author\": \"Evelyn Waugh\",\n" +
      "        \"title\": \"Sword of Honour\",\n" +
      "        \"price\": 12.99\n" +
      "      },\n" +
      "      { \"category\": \"fiction\",\n" +
      "        \"author\": \"Herman Melville\",\n" +
      "        \"title\": \"Moby Dick\",\n" +
      "        \"isbn\": \"0-553-21311-3\",\n" +
      "        \"price\": 8.99\n" +
      "      },\n" +
      "      { \"category\": \"fiction\",\n" +
      "        \"author\": \"J. R. R. Tolkien\",\n" +
      "        \"title\": \"The Lord of the Rings\",\n" +
      "        \"isbn\": \"0-395-19395-8\",\n" +
      "        \"price\": 22.99\n" +
      "      }\n" +
      "    ],\n" +
      "    \"bicycle\": {\n" +
      "      \"color\": \"red\",\n" +
      "      \"price\": 19.95" +
      "    }\n" +
      "  }\n" +
      "}"
  }

  get("/requestUrl") {
    request.getRequestURL + "?" + request.getQueryString
  }

  get("/anonymous_list_with_numbers") {
    contentType = "application/json"
    """[100, 50, 31.0]"""
  }

  get("/russian") {
    contentType = "application/json"
    """{ "title" : "Информационные технологии, интернет, телеком" }"""
  }

  get("/products") {
    contentType = "application/json"
    """[
          {
              "id": 2,
              "name": "An ice sculpture",
              "price": 12.50,
              "tags": ["cold", "ice"],
              "dimensions": {
                  "length": 7.0,
                  "width": 12.0,
                  "height": 9.5
              },
              "warehouseLocation": {
                  "latitude": -78.75,
                  "longitude": 20.4
              }
          },
          {
              "id": 3,
              "name": "A blue mouse",
              "price": 25.50,
                  "dimensions": {
                  "length": 3.1,
                  "width": 1.0,
                  "height": 1.0
              },
              "warehouseLocation": {
                  "latitude": 54.4,
                  "longitude": -32.7
              }
          }
      ]"""
  }

  get("/shopping") {
    contentType = "application/xml"
    <shopping>
      <category type="groceries">
        <item>Chocolate</item>
        <item>Coffee</item>
      </category>
      <category type="supplies">
        <item>Paper</item>
        <item quantity="4">Pens</item>
      </category>
      <category type="present">
        <item when="Aug 10">Kathryn's Birthday</item>
      </category>
    </shopping>
  }

  get("/videos") {
    contentType = "application/xml"
    <videos>
      <music>
        <title>Video Title 1 </title>
        <artist>Artist 1</artist>
      </music>
      <music >
        <title>Video Title 2</title>
        <artist>Artist 2</artist>
        <artist>Artist 3</artist>
      </music>
    </videos>
  }

  get("/videos-not-formatted") {
    contentType = "application/xml"
    <videos><music><title>Video Title 1</title><artist>Artist 1</artist></music><music ><title>Video Title 2</title><artist>Artist 2</artist><artist>Artist 3</artist></music></videos>
  }

  get("/greetJSON") {
    "{ \"greeting\" : { \n" +
      "                \"firstName\" : \""+{params("firstName")}+"\", \n" +
      "                \"lastName\" : \""+{params("lastName")}+"\" \n" +
      "               }\n" +
      "}"
  }

  post("/greetXML") {
    greetXML
  }

  get("/anotherGreetXML") {
    anotherGreetXML
  }

  post("/anotherGreetXML") {
    anotherGreetXML
  }

  post("/threeMultiValueParam") {
    "{ \"list\" : \""+multiParams("list").mkString(",") +"\", " +
            "\"list2\" : \"" + multiParams("list2").mkString(",") + "\", " +
            "\"list3\" : \"" + multiParams("list3").mkString(",") + "\"}"
  }

  get("/multiValueParam") {
    "{ \"list\" : \""+multiParams("list").mkString(",") +"\" }"
  }

  put("/multiValueParam") {
    "{ \"list\" : \""+multiParams("list").mkString(",") +"\" }"
  }

  post("/multiValueParam") {
    "{ \"list\" : \""+multiParams("list").mkString(",") +"\" }"
  }

  patch("/multiValueParam") {
    "{ \"list\" : \""+findMultiParamIn(request.body, "list").mkString(",") +"\" }"
  }

  patch("/jsonGreet") {
      contentType = "application/json"
      val json = JsonParser.parse(request.body)
      "{ \"fullName\" : \"" + (json \ "firstName").extract[String] + " "+ (json \ "lastName").extract[String] + "\" }"
  }

  get("/hello") {
    val json = ("hello" -> "Hello Scalatra")
    compact(render(json))
  }

  get("/text-json") {
    contentType = "text/json"
    """{"test":true}"""
  }

  get("/lotto") {
    val json = ("lotto" -> ("lottoId" -> lotto.id) ~
      ("winning-numbers" -> lotto.winningNumbers) ~
      ("drawDate" -> lotto.drawDate.map(_.toString)) ~
      ("winners" -> lotto.winners.map { w =>
        (("winnerId" -> w.id) ~ ("numbers" -> w.numbers))}))
    compact(render(json))
  }

  get("/reflect") {
    reflect
  }

  put("/reflect") {
    reflect
  }

  put("/reflect") {
    reflect
  }

  patch("/reflect") {
    reflect
  }

  post("/reflect") {
    reflect
  }

  post("/param-reflect") {
    compact(render(decompose(params)))
  }

  post("/:pathParam/manyParams") {
    val queryParam = {params("queryParam")}
    val pathParam = {params("pathParam")}
    val formParam = {params("formParam")}

    queryParam + " " + pathParam + " " + formParam
  }

  post("/charEncoding") {
    contentType = "text/plain"
    request.getCharacterEncoding
  }

  put("/serializedJsonParameter") {
    val something = {params("something")}
    val serialized = {params("serialized")}
    serialized
  }

  patch("/serializedJsonParameter") {
    val something = {params("something")}
    val serialized = {params("serialized")}
    serialized
  }

  get("/contentTypeButNoBody") {
    contentType = "application/json"
  }

  get("/contentTypeAsBody") {
    request.contentType.getOrElse("null")
  }

  post("/contentTypeAsBody") {
    request.contentType.getOrElse("null")
  }

  get("/contentTypeAsContentType") {
    contentType = request.contentType.getOrElse("null")
  }

  post("/textUriList") {
    if (!request.getContentType.contains("text")) {
      status = 400
    } else {
      contentType = "application/json"
      val content = IOUtils.toString(request.getInputStream)
      val uris = content.split("\n")
      val json = "uris" -> decompose(uris)
      compact(render(json))
    }
  }

  get("/:firstName/:lastName") {
    val firstName = {params("firstName")}
    val lastName = {params("lastName")}
    val fullName: String = firstName + " " + lastName
    val json = ("firstName" -> firstName) ~ ("lastName" -> lastName) ~ ("fullName" -> fullName)
    compact(render(json))

  }

  get("/:firstName/:middleName/:lastName") {
    val firstName = {params("firstName")}
    val middleName = {params("middleName")}
    val lastName = {params("lastName")}
    val json = ("firstName" -> firstName) ~ ("lastName" -> lastName) ~ ("middleName" -> middleName)
    compact(render(json))

  }

  get("/409") {
    contentType = "text/plain"
    response.setStatus(409)
    "ERROR"
  }

  get("/user-favorite-xml") {
    contentType = "application/xml"
    <user user-id="24985">
      <date-created>2008-11-17T08:00:00Z</date-created>
      <date-modified>2012-09-27T02:29:43.883Z</date-modified>
      <userFavorite application-id="1" favorite-id="28" userData="someData" sequence-number="1">
        <date-created>2011-01-20T19:59:47.887Z</date-created>
        <date-modified>2012-09-25T23:52:21.167Z</date-modified>
      </userFavorite>
    </user>
  }

  get("/package-db-xml") {
    contentType = "application/xml"
    <package-database xmlns="http://marklogic.com/manage/package/databases">
      <metadata xmlns:db="http://marklogic.com/manage/package/databases">
        <package-version>2.0</package-version>
      </metadata>
    </package-database>
  }

  get("/namespace-example") {
    contentType = "application/xml"
    <foo xmlns:ns="http://localhost/">
      <bar>sudo </bar>
      <ns:bar>make me a sandwich!</ns:bar>
    </foo>
  }

  get("/namespace-example2") {
    contentType = "application/xml"

    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
      <soapenv:Body>
        <ns1:getBankResponse xmlns:ns1="http://thomas-bayer.com/blz/">
          <ns1:details>
            <ns1:bezeichnung>ABK-Kreditbank</ns1:bezeichnung>
            <ns1:bic>ABKBDEB1XXX</ns1:bic>
            <ns1:ort>Berlin</ns1:ort>
            <ns1:plz>10789</ns1:plz>
          </ns1:details>
        </ns1:getBankResponse>
      </soapenv:Body>
    </soapenv:Envelope>
  }

  get("/amount") {
    """{
        "amount": 250.00
    }"""
  }

  get("/game") {
    """{
         "playerOneId": "a084a81a-6bc9-418d-b107-5cb5ce249b77",
         "playerTwoId": "88867e23-0b38-4c43-ad8e-161ba5062c7d",
          "status": "ongoing",
          "rounds": [

          ],
          "_links": {
              "self": {
                  "href": "http://localhost:8080/2dd68f2d-37df-4eed-9fce-5d9ce23a6745"
              },
              "make-move": {
                  "href": "http://localhost:8080/2dd68f2d-37df-4eed-9fce-5d9ce23a6745/make-move"
              }
          },
          "id": "2dd68f2d-37df-4eed-9fce-5d9ce23a6745"
      }"""
  }

  put("/greetPut") {
    // For some reason Scalatra doesn't seem to handle form parameters in PUT requests
    if(request.getParameterNames.exists { _ == "firstName" }) {
      greetJson
    } else {
      val content: String = IOUtils.toString(request.getInputStream)
      val name = "Greetings " + {
        findParamIn(content, "firstName")
      } + " " + {
        findParamIn(content, "lastName")
      }
      val json = ("greeting" -> name)
      compact(render(json))
    }
  }

  patch("/greetPatch") {
    if(request.getParameterNames.exists { _ == "firstName" }) {
      greetJson
    } else {
      val content: String = IOUtils.toString(request.getInputStream)
      val name = "Greetings " + {
        findParamIn(content, "firstName")
      } + " " + {
        findParamIn(content, "lastName")
      }
      val json = ("greeting" -> name)
      compact(render(json))
    }
  }

  delete("/greet") {
    greetJson
  }

  get("/greet") {
    greetJson
  }

  get("/xmlWithMinusInRoot") {
    contentType = "application/xml"
    <a-greeting><firstName>{params("firstName")}</firstName>
      <lastName>{params("lastName")}</lastName>
    </a-greeting>
  }

  get("/xmlWithMinusInChild") {
    contentType = "application/xml"
    <greeting><your-firstName>{params("firstName")}</your-firstName>
      <your-lastName>{params("lastName")}</your-lastName>
    </greeting>
  }

  get("/xmlWithUnderscoreInChild") {
    contentType = "application/xml"
    <greeting><your_firstName>{params("firstName")}</your_firstName>
      <your_lastName>{params("lastName")}</your_lastName>
    </greeting>
  }

  get("/customMimeType") {
    contentType = "application/something-custom"
    <body>
      <message>Custom mime-type</message>
    </body>
  }

  get("/mimeTypeWithPlusXml") {
    contentType = "application/something+xml"
    <body><message>Custom mime-type ending with +xml</message></body>
  }

  get("/mimeTypeWithPlusJson") {
    contentType = "application/something+json"
    """{ "message" : "It works" }"""
  }

  get("/mimeTypeWithPlusHtml") {
    contentType ="application/something+html"
    <html>
      <head>
        <title>my title</title>
      </head>
      <body>
        <p>paragraph 1</p>
        <p>paragraph 2</p>
      </body>
    </html>
  }

  get("/noValueParam") {
    "Params: "+params.foldLeft(new StringBuilder)( (b,t) => b.append(t._1+"="+t._2)).toString()
  }

  put("/noValueParam") {
    val content: String = IOUtils.toString(request.getInputStream)
    if(content.contains("=")) {
      throw new IllegalArgumentException("One of the parameters had a value")
    }
    "OK"
  }

  patch("/noValueParam") {
    val content: String = IOUtils.toString(request.getInputStream)
    if(content.contains("=")) {
      throw new IllegalArgumentException("One of the parameters had a value")
    }
    "OK"
  }

  post("/noValueParam") {
    "Params: "+params.foldLeft(new StringBuilder)( (b,t) => b.append(t._1+"="+t._2)).toString()
  }

  post("/redirect") {
    response.setHeader("Location", "http://localhost:8080/redirect/1")
    response.setStatus(301)
    """{ "id" : 1 } """
  }

  get("/redirect") {
    val url: String = {params("url")}
    redirect(url)
  }

  get("/redirect-and-set-cookie") {
    val url: String = {params("url")}
    val cookie: Cookie = new Cookie("cookieName", "cookieValue")
    response.addCookie(cookie)
    redirect(url)
  }

  get("/customMimeTypeJsonCompatible") {
    contentType = "application/vnd.uoml+json"
    """{ "message" : "It works" }"""
  }

  get("/customMimeTypeJsonCompatible2") {
    contentType = "application/vnd.uoml+something"
    """{ "message" : "It works" }"""
  }

  get("/noContentTypeJsonCompatible") {
    contentType = ""
    """{ "message" : "It works" }"""
  }

  get("/customMimeTypeNonJsonCompatible") {
    contentType = "application/something+json"
    "This is not JSON"
  }

  get("/contentTypeJsonButBodyIsNotJson") {
    contentType = "application/json"
    "This is not JSON"
  }

  get("/cookie_with_no_value") {
    contentType = "text/plain"
    val cookies = request.getCookies
    val name: String = cookies(0).getName
    name
  }

  get("/html_with_cookie") {
    contentType = "text/html"
    response.addHeader("Set-Cookie", "JSESSIONID=B3134D534F40968A3805968207273EF5; Path=/")
    """<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
        <body>body</body>
        </html>"""
  }

  get("/response_cookie_with_no_value") {
    val cookie: Cookie = new Cookie("PLAY_FLASH", "")
    val time: Long = new Date().getTime
    cookie.setMaxAge(time.intValue());
    response.addCookie(cookie)
  }

  get("/key_only_cookie") {
    contentType = "text/plain"
    response.setHeader("Set-Cookie", "some_cookie")
    "OK"
  }

  get("/multiCookie") {
    contentType = "text/plain"
    val cookie1: Cookie = new Cookie("cookie1", "cookieValue1")
    cookie1.setDomain("localhost")
    val cookie2 = new Cookie("cookie1", "cookieValue2")
    cookie2.setPath("/")
    cookie2.setMaxAge(1234567)
    cookie2.setComment("My Purpose")
    cookie2.setDomain("localhost")
    cookie2.setSecure(true)
    cookie2.setVersion(1)
    response.addCookie(cookie1)
    response.addCookie(cookie2)
    "OK"
  }

  get("/multiCookieRequest") {
    val cookies = request.getCookies
            .map(cookie => Map(cookie.getName -> cookie.getValue))
            .foldLeft(mutable.ListBuffer[Map[String, String]]())((list, cookie) => {
      list.add(cookie); list
    })
    compact(render(cookies))
  }

  post("/j_spring_security_check") {
    contentType = "text/plain"
    securityCheck("jsessionid", () => true)
  }

  post("/j_spring_security_check_with_csrf") {
      contentType = "text/plain"
      securityCheck("jsessionid", () => params.get("_csrf").get == "8adf2ea1-b246-40aa-8e13-a85fb7914341")
  }

  post("/j_spring_security_check_with_csrf_header") {
      contentType = "text/plain"
      securityCheck("jsessionid", () => request.getHeader("_csrf") == "8adf2ea1-b246-40aa-8e13-a85fb7914341")
  }

  post("/j_spring_security_check_with_additional_fields") {
    contentType = "text/plain"
    securityCheck("jsessionid", "USER", "PASSWORD", () => {
      params.get("smquerydata").get == ""
      params.get("smauthreason").get == "0"
      params.get("smagentname").get == "OL9V/qlt7/7L+n9klS4+VH5DvC2Gidql5iLqO6CXQTQPU4e4QgjI67sYeeeFAewI"
      params.get("postpreservationdata").get == ""
    })
  }

  post("/j_spring_security_check_phpsessionid") {
    contentType = "text/plain"
    securityCheck("phpsessionid", () => true)
  }

  def securityCheck(sessionIdName: String, usernameParamName : String, passwordParamName :String, additionalChecks: () => Boolean) : Any = {
    val userName = params.get(usernameParamName).get
    val password = params.get(passwordParamName).get
    if (userName == "John" && password == "Doe") {
      if (!additionalChecks.apply()) {
        "NO"
      } else {
        response.setHeader("Set-Cookie", sessionIdName + "=1234")
      }
    } else {
      "NO"
    }
  }

  def securityCheck(sessionIdName: String, additionalChecks: () => Boolean) : Any =
    securityCheck(sessionIdName, "j_username", "j_password", additionalChecks)

  get("/formAuth") {
    formAuth(() => loginPage)
  }

  get("/formAuthCsrf") {
    formAuth(() => loginPageWithCsrf)
  }

  get("/formAuthCsrfInHeader") {
    formAuth(() => loginPageWithCsrfHeader)
  }

  get("/formAuthAdditionalFields") {
    formAuth(() => loginPageWithAdditionalInputFields)
  }

  get("/jsonWithAtSign") {
    """{
        "body" : { "@id" : 10, "content": "some content"  }
     }"""

  }

  get("/malformedJson") {
    """{
      "a": 123456
      "b":"string"
    }"""
  }

  post("/greet") {
    greetJson
  }

  post("/body") {
    getStringBody
  }

  put("/body") {
    getStringBody
  }

  patch("/body") {
    getStringBody
  }

  delete("/body") {
    getStringBody
  }

  put("/binaryBody") {
    getBinaryBodyResponse
  }

  patch("/binaryBody") {
    getBinaryBodyResponse
  }

  post("/binaryBody") {
    getBinaryBodyResponse
  }

  post("/jsonBody") {
    contentType = "text/plain";
    val header: String = request.getHeader("Content-Type")
    if (!header.contains("application/json")) {
      "FAILURE"
    } else {
      val json = JsonParser.parse(request.body)
      (json \ "message").extract[String]
    }
  }

  post("/jsonBodyAcceptHeader") {
    val accept: String = request.getHeader("Accept")
    if (!accept.contains("application/json")) {
      "FAILURE"
    } else {
      val json = JsonParser.parse(request.body)
      (json \ "message").extract[String]
    }
  }

  get("/setCookies") {
    setCookies
  }

   get("/setCommonIdCookies") {
    setCommonIdCookies
  }

  post("/header") {
    getHeaders
  }

  get("/header") {
    getHeaders
  }

  get("/matrix") {
    val matrixParams = StringUtils.substringAfter(URLDecoder.decode(request.getRequestURI, "UTF-8"), ";")
    val nameValueMap = StringUtils.split(matrixParams, "&")
            .map(nameValue => {
              val nameAndValue = StringUtils.split(nameValue, "=")
              (nameAndValue(0), nameAndValue(1))})
            .foldLeft(mutable.HashMap[String, String]())((map, nameAndValue) => {
              map.put(nameAndValue._1, nameAndValue._2)
              map
            }).toMap // Convert map to an immutable map so that JSON gets rendered correctly, see http://stackoverflow.com/questions/6271386/how-do-you-serialize-a-map-to-json-in-scala
    compact(render(decompose(nameValueMap)))
  }

  get("/cookiesWithValues") {
    cookiesWithValues
  }

  get("/headersWithValues") {
    headersWithValues
  }

  post("/headersWithValues") {
    headersWithValues
  }

  get("/multiHeaderReflect") {
    contentType = "text/plain"
    val headerNames = request.getHeaderNames()
    while (headerNames.hasMoreElements()) {
      val name = headerNames.nextElement.toString
      val headerValues = request.getHeaders(name)
      while (headerValues.hasMoreElements) {
        val headerValue: String = headerValues.nextElement().toString
        response.addHeader(name, headerValue)
      }
    }
  }

  get("/multiValueHeader") {
    contentType = "text/plain"
    response.addHeader("MultiHeader", "Value 1")
    response.addHeader("MultiHeader", "Value 2")
    ""
  }

  post("/cookie") {
    getCookies
  }

  get("/cookie") {
    getCookies
  }

  put("/cookie") {
    getCookies
  }

  delete("/cookie") {
    getCookies
  }

  patch("/cookie") {
    getCookies
  }

  get("/jsonList") {
    """[
     { "name" : "Anders",
       "address" : "Spangatan"
     },
     { "name" : "Sven",
       "address" : "Skolgatan"
     }
    ]"""
  }

  get("/emptyBody") {
  }

  get("/textXML") {
    contentType = "text/xml"
    <xml>something</xml>
  }

  get("/textHTML") {
    contentType = "text/html"
    <html>
      <head>
        <title>my title</title>
      </head>
      <body>
        <p>paragraph 1</p>
        <p>paragraph 2</p>
      </body>
    </html>
  }

  get("/response") {
    """{
      |  "response": {
      |    "status": 200,
      |    "startRow": 0,
      |    "endRow": 1,
      |    "totalRows": 1,
      |    "next": "",
      |    "data": {
      |      "id": "workflow-1",
      |      "name": "SampleWorkflow",
      |      "tasks": [
      |        {
      |          "id": "task-0",
      |          "name": "AWX",
      |          "triggered_by": ["task-5"]
      |        },
      |        {
      |          "id": "task-1",
      |          "name": "BrainStorming",
      |          "triggered_by": ["task-2", "task-5"]
      |        },
      |        {
      |          "id": "task-2",
      |          "name": "OnHold",
      |          "triggered_by": ["task-0", "task-4", "task-7", "task-8", "task9"]
      |        },
      |        {
      |          "id": "task-3",
      |          "name": "InvestigateSuggestions",
      |          "triggered_by": ["task-6"]
      |        },
      |        {
      |          "id": "task-4",
      |          "name": "Mistral",
      |          "triggered_by": ["task-3"]
      |        },
      |        {
      |          "id": "task-5",
      |          "name": "Ansible",
      |          "triggered_by": ["task-3"]
      |        },
      |        {
      |          "id": "task-6",
      |          "name": "Integration",
      |          "triggered_by": []
      |        },
      |        {
      |          "id": "task-7",
      |          "name": "Tower",
      |          "triggered_by": ["task-5"]
      |        },
      |        {
      |          "id": "task-8",
      |          "name": "Camunda",
      |          "triggered_by": ["task-3"]
      |        },
      |        {
      |          "id": "task-9",
      |          "name": "HungOnMistral",
      |          "triggered_by": ["task-0", "task-7"]
      |        },
      |        {
      |          "id": "task-10",
      |          "name": "MistralIsChosen",
      |          "triggered_by": ["task-1"]
      |        }
      |      ]
      |    }
      |  }
      |}""".stripMargin('|')
  }

  get("/textHTML-not-formatted") {
    contentType = "text/html"
    <html><head><title>my title</title></head><body><p>paragraph 1</p><p>paragraph 2</p></body></html>
  }

  get("/statusCode500") {
    contentType = "text/plain"
    response.setStatus(500)
    "An expected error occurred"
  }

  get("/rss") {
    contentType = "application/rss+xml"
    <rss>
      <item>
        <title>rss title</title>
      </item>
    </rss>
  }

  get("/gzip-empty-body") {
    contentType = "text/plain"
    response.addHeader("Content-Encoding", "gzip")
    ""
  }

  get("/gzip-json") {
    response.addHeader("Content-Encoding", "gzip")
    val jsonData = ("hello" -> "Hello Scalatra")
    val jsonString = compact(render(jsonData))
    Gzip.compress(jsonString.getBytes("UTF-8"))
  }

  get("/jsonp") {
    contentType = "application/javascript"
    params("callback") + "(" + greetJson + ");"
  }

  get("/statusCode409WithNoBody") {
    contentType = "application/json"
    response.setStatus(409)
  }

  get("/sessionId") {
    def setSessionId {
      response.setHeader("Set-Cookie", "jsessionid=1234")
    }

    val cookies: Array[Cookie] = request.getCookies()
    if (cookies == null) {
      setSessionId
    } else {
      val cookie = cookies.find(_.getName.equalsIgnoreCase("jsessionid"))
      if (cookie == None) {
        setSessionId
      } else if (cookie.get.getValue == "1234") {
        "Success"
      } else {
        response.sendError(409, "Invalid sessionid")
      }
    }
  }

  get("/bigRss") {
    contentType = "application/rss+xml"
    <rss xmlns:dc="http://purl.org/dc/elements/1.1/" version="2.0">
      <channel>
        <title>something</title>
        <link>http://www.someone.com</link>
        <description>something RSS</description>
        <dc:creator>someone</dc:creator>
        <item>
          <title>A title</title>
          <link>http://www.something.com/link/1</link>
          <description>Description 1</description>
          <enclosure url="http://www.someone.com/somejpg.jpg" length="2721" type="image/jpg" />
          <pubDate>Mon, 10 Jan 2011 19:31:46 GMT</pubDate>
          <guid isPermaLink="false">http://www.something.com/link/1</guid>
          <dc:date>2011-01-10T19:31:46Z</dc:date>
        </item>
        <item>
          <title>Title 2</title>
          <link>http://www.something.com/link/2</link>
          <description>Description 2</description>
          <enclosure url="http://www.someone.com/someotherjpg.jpg" length="2721" type="image/jpg" />
          <pubDate>Mon, 10 Jan 2011 19:41:46 GMT</pubDate>
          <guid isPermaLink="false">http://www.something.com/link/2</guid>
          <dc:date>2011-01-10T19:42:46Z</dc:date>
        </item>
      </channel>
    </rss>
  }

  get("/carRecords") {
    contentType = "application/xml"
    <records>
      <car name='HSV Maloo' make='Holden' year='2006'>
        <country>Australia</country>
        <record type='speed'>Production Pickup Truck with speed of 271kph</record>
      </car>
      <car name='P50' make='Peel' year='1962'>
        <country>Isle of Man</country>
        <record type='size'>Smallest Street-Legal Car at 99cm wide and 59 kg in weight</record>
      </car>
      <car name='Royale' make='Bugatti' year='1931'>
        <country>France</country>
        <record type='price'>Most Valuable Car at $15 million</record>
      </car>
    </records>
  }

  post("/validateContentTypeIsDefinedAndReturnBody") {
    if (request.getContentType == null) {
      response.setStatus(304)
    }
    contentType = request.getContentType
    request.body
  }

  post("/file") {
    val content: String = IOUtils.toString(request.getInputStream)
    content
  }

  put("/file") {
    val content: String = IOUtils.toString(request.getInputStream)
    content
  }

  def getBinaryBodyResponse: String = {
    contentType = "text/plain";
    Stream.continually(request.getInputStream().read).takeWhile(_ != -1).map(_.toByte).toList.mkString(", ")
  }

  def getHeaders: String = {
    contentType = "text/plain"
    val headerNames = request.getHeaderNames()
    val names = ListBuffer[String]()
    while (headerNames.hasMoreElements()) {
      val name = headerNames.nextElement.toString
      names.append(name)
    }
    names.mkString(", ")
  }

  def getStringBody: String = {
    contentType = "text/plain";
    request.body
  }

  def getCookies: String = {
    contentType = "text/plain"
    request.getCookies().map(_.getName).mkString(", ")
  }

  def setCookies: String = {
    contentType = "text/plain"
    response.addCookie(new Cookie("key1", "value1"))
    response.addCookie(new Cookie("key2", "value2"))
    response.addCookie(new Cookie("key3", "value3"))
    "ok"
  }

  def setCommonIdCookies: String = {
    contentType = "text/plain"
    response.addCookie(new Cookie("key1", "value1"))
    response.addCookie(new Cookie("key1", "value2"))
    response.addCookie(new Cookie("key1", "value3"))
    "ok"
  }

  notFound {
    response.setStatus(404)
    "Not found"
  }

  def greetJson: String = {
    val name = "Greetings " + {
      params("firstName")
    } + " " + {
      params("lastName")
    }
    val json = ("greeting" -> name)
    compact(render(json))
  }

  def loginPage: String = {
    contentType = "text/html"
    """<html>
      <head>
        <title>Login</title>
      </head>

      <body>
        <form action="j_spring_security_check" method="POST">
          <table>
            <tr><td>User:&nbsp;</td><td><input type='text' name='j_username'></td></tr>
            <tr><td>Password:</td><td><input type='password' name='j_password'></td></tr>
              <tr><td colspan='2'><input name="submit" type="submit"/></td></tr>
           </table>
            </form>
          </body>
     </html>"""
  }

  def loginPageWithCsrf: String = {
    contentType = "text/html"
    """<html>
      <head>
        <title>Login</title>
      </head>

      <body>
        <form action="j_spring_security_check_with_csrf" method="POST">
          <table>
            <tr><td>User:&nbsp;</td><td><input type='text' name='j_username'></td></tr>
            <tr><td>Password:</td><td><input type='password' name='j_password'></td></tr>
              <tr><td colspan='2'><input name="submit" type="submit"/></td></tr>
           </table>
            <input type="hidden" name="_csrf" value="8adf2ea1-b246-40aa-8e13-a85fb7914341"/>
            </form>
          </body>
     </html>"""
  }

  def loginPageWithCsrfHeader: String = {
    contentType = "text/html"
    """<html>
      <head>
        <title>Login</title>
      </head>

      <body>
        <form action="j_spring_security_check_with_csrf_header" method="POST">
          <table>
            <tr><td>User:&nbsp;</td><td><input type='text' name='j_username'></td></tr>
            <tr><td>Password:</td><td><input type='password' name='j_password'></td></tr>
              <tr><td colspan='2'><input name="submit" type="submit"/></td></tr>
           </table>
            <input type="hidden" name="_csrf" value="8adf2ea1-b246-40aa-8e13-a85fb7914341"/>
            </form>
          </body>
     </html>"""
  }

  def loginPageWithAdditionalInputFields : String = {
    contentType = "text/html"
    """
      |<!-- SiteMinder Encoding=ISO-8859-1; -->
      |<html>
      |<head>
      |<meta http-equiv="Content-Type" content="text/html;charset=ISO-8859-1">
      |  <title>PPS Authentication via SiteMinder Password Services</title>
      |</head>
      |<body BGCOLOR="#D2FFFF" TEXT="#000000" onLoad = "resetCredFields();">
      |<!-- Customer Brand -->
      |<form NAME="Login" METHOD="POST" action="j_spring_security_check_with_additional_fields">
      |<INPUT TYPE=HIDDEN NAME="SMENC" VALUE="ISO-8859-1">
      |<INPUT type=HIDDEN name="SMLOCALE" value="US-EN">
      |<center>
      |<!-- outer table with border -->
      |<table width="50%" height=200 border=1 cellpadding=0 cellspacing=0 >
      |<tr>
      |  <td ALIGN="CENTER" VALIGN="CENTER" HEIGHT=40 COLSPAN=4 NOWRAP BGCOLOR="#FFFFCC">
      |       <font size="+2" face="Arial,Helvetica">
      |  <b>PPS</b></font>
      |     </td>
      |  </tr>
      |  <tr>
      |    <td>
      |   <!-- Login table -->
      |      <table WIDTH="100%" HEIGHT=200 BGCOLOR="#FFFFFF" border=0 cellpadding=0 cellspacing=0 >
      |
      | <tr>
      |   <td ALIGN="CENTER" VALIGN="CENTER" HEIGHT=40 COLSPAN=4 NOWRAP BGCOLOR="#FFFFFF">
      |  <font size="+1" face="Arial,Helvetica">
      |  <b>Please Login</b></font>
      |       </td>
      | </tr>
      | <tr> <td colspan=4 height=10> <font size=1>   </font> </td> </tr>
      | <tr>
      |   <td WIDTH=20 >&nbsp;</td>
      |   <td ALIGN="LEFT" >
      |      <b><font size=-1 face="arial,helvetica" > Username: </font></b>
      |    </td>
      |   <td ALIGN="LEFT" >
      |     <input type="text" name="USER" size="30" style="margin-left: 1px">
      |    </td>
      |   <td WIDTH=20 >&nbsp;</td>
      | </tr>
      | <tr> <td colspan=4 height=10> <font size=1>   </font> </td> </tr>
      | <tr>
      |   <td WIDTH=20 >&nbsp;</td>
      |   <td >
      |      <b><font size=-1 face="arial,helvetica" > Password: </font></b>
      |       </td>
      |   <td ALIGN="left" >
      |     <input type="password" name="PASSWORD" size="30" style="margin-left: 1px">
      |   </td>
      |   <td WIDTH=20 >&nbsp;</td>
      | </tr>
      | <tr> <td colspan=4 height=10> <font size=1>   </font> </td> </tr>
      | <tr>
      |   <td colspan=4 NOWRAP WIDTH="50%" HEIGHT="25" align="CENTER">
      |       <input type=hidden name=smquerydata value="">
      |       <input type=hidden name=smauthreason value="0">
      |       <input type=hidden name=smagentname value="OL9V/qlt7/7L+n9klS4+VH5DvC2Gidql5iLqO6CXQTQPU4e4QgjI67sYeeeFAewI">
      |       <input type=hidden name=postpreservationdata value="">
      |       <input type="button" value="Login" onclick="submitForm();">
      |   </td>
      | </tr>
      | <tr> <td colspan=4 height=5> <font size=1>   </font> </td> </tr>
      |      </table>
      |    </td>
      |  </tr>
      |</table>
      |</form></center>
      |</body>
      |</html>
    """.stripMargin
  }

  def greetXML: Elem = {
    contentType = "application/xml"
    <greeting><firstName>{params("firstName")}</firstName>
      <lastName>{params("lastName")}</lastName>
    </greeting>
  }

  def anotherGreetXML: Elem = {
    contentType = "application/xml"
    <greeting>
      <name>
        <firstName>{params("firstName")}</firstName>
        <lastName>{params("lastName")}</lastName>
      </name>
    </greeting>
  }

  get("/demoRequestSpecification") {
    val category = params("category")
    val userName = request.getCookies.filter(_.getName == "user")(0).getValue

    if (category == "books" && userName == "admin") {
      "Catch 22"
    } else {
      "Unknown entity"
    }
  }

  get("/demoResponseSpecification") {
    contentType = "application/json"
    val fullName = params("name")
    val firstAndLastName = fullName.split(" ")
    val firstName = firstAndLastName(0)
    val lastName = firstAndLastName(1)

    "{ \"firstName\" : \""+firstName+"\",\"lastName\" : \""+lastName+"\", \"responseType\" : \"simple\" }"
  }

  get("/contentTypeJsonButContentIsNotJson") {
    contentType = "application/json"
    "This is not a valid JSON document"
  }

  get("/contentTypeHtmlButContentIsJson") {
    contentType = "text/html"
    "{ \"key\" : \"value\", \"42\" : \"23\"}"
  }

  get("/xmlWithBom") {
    contentType = "application/xml"
    IOUtils.toByteArray(getClass.getResourceAsStream("/bom_example.xml"))
  }

  get("/xmlWithHeaderAndFooter") {
    contentType = "application/xml"
    """733
      <?xml version="1.0" encoding="utf-8"?><soapenv:Envelope
      xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
      xmlns:xsd="http://www.w3.org/2001/XMLSchema"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <some>thing</some>
      </soapenv:Envelope>
      0"""
  }

  head("/returnContentTypeAsBody") {
        contentType = "text/plain"
        request.getContentType
  }

  options("/returnBodyAsBody") {
        contentType = "text/plain"
        request.body
  }

  post("/returnContentTypeAsBody") {
      contentType = "text/plain"
      request.getContentType
  }

  get("/returnContentTypeAsBody") {
      contentType = "text/plain"
      request.getContentType
  }

  post("/return204WithContentType") {
    contentType = "application/json"
    status = 204
  }

  get("/cookieWithValidExpiresDate") {
    contentType = "text/plain"
    response.setHeader("Set-Cookie", "name=Nicholas; expires=Sat, 02 May 2009 23:38:25 GMT")
    ""
  }

  get("/cookieWithDoubleQuoteExpiresDate") {
    contentType = "text/plain"
    response.setHeader("Set-Cookie", "name=Nicholas; expires=\"Sat, 02 May 2009 23:38:25 GMT\"")
    ""
  }

  get("/cookieWithInvalidExpiresDate") {
    contentType = "text/plain"
    response.setHeader("Set-Cookie", "name=Nicholas; expires=NO DATE!")
    ""
  }

  def formAuth(loginPage: () => String) = {
    contentType = "text/plain"
    val cookies: Array[Cookie] = request.getCookies
    if(cookies == null) {
      loginPage.apply()
    } else {
      val cookie = cookies.find(sessionName => sessionName.getName.equalsIgnoreCase("jsessionid") || sessionName.getName.equalsIgnoreCase("phpsessionid")).get
      if(cookie == null) {
        loginPageWithCsrf
      } else if (cookie.getValue == "1234") {
        "OK"
      } else {
        "NOT AUTHORIZED"
      }
    }
  }

  def reflect: String = {
    contentType = request.getContentType
    val cookies = request.getCookies
    if (cookies != null) {
      cookies.foreach {
        response.addCookie(_)
      }
    }
    request.body
  }

  def findParamIn(content: String, param: String): String = {
    var value: String = StringUtils.substringBetween(content, param + "=", "&")
    if (value == null) {
      value = StringUtils.substringAfter(content, param + "=")
    }
    return value
  }

  def findMultiParamIn(content: String, param: String): scala.collection.mutable.MutableList[String] = {
    val scanner: Scanner = new Scanner(content).useDelimiter("&")
    val myList = scala.collection.mutable.MutableList[String]()
    while (scanner.hasNext) {
      val next: String = scanner.next
      myList += next.split('=')(1)
    }
    myList
  }

  def headersWithValues: String = {
    contentType = "application/json"
    val headerNames = request.getHeaderNames.map(_.toString)

    val map: Map[String, List[String]] = headerNames.map(headerName => (headerName, request.getHeaders(headerName).map(_.toString).toList)).
            foldLeft(mutable.HashMap[String, List[String]]())((map, header) => {
              map.put(header._1, header._2.toList)
              map
            }).toMap // Convert map to an immutable map so that JSON gets rendered correctly, see http://stackoverflow.com/questions/6271386/how-do-you-serialize-a-map-to-json-in-scala
    compact(render(decompose(map)))
  }

  def cookiesWithValues: String = {
    contentType = "application/json"
    if(request.getCookies == null || request.getCookies.isEmpty) {
      return "[]"
    }

    val cookiesMap = request.getCookies.map(c => {
      val cookieMap = mutable.HashMap[String, Any]()
      cookieMap.put("name", c.getName)
      cookieMap.put("comment", c.getComment)
      cookieMap.put("maxAge", c.getMaxAge)
      cookieMap.put("domain", c.getDomain)
      cookieMap.put("path", c.getPath)
      cookieMap.put("secure", c.getSecure)
      cookieMap.put("value", c.getValue)
      cookieMap.put("version", c.getVersion)
      cookieMap
    }).map(_.toMap) // Convert map to an immutable map so that JSON gets rendered correctly, see http://stackoverflow.com/questions/6271386/how-do-you-serialize-a-map-to-json-in-scala
    compact(render(decompose(cookiesMap)))
  }
}
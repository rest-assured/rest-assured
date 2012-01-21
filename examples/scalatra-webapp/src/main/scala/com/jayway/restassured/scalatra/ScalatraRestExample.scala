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
package com.jayway.restassured.scalatra

import org.scalatra.ScalatraServlet
import net.liftweb.json.JsonDSL._
import java.lang.String
import xml.Elem
import net.liftweb.json.JsonAST._
import net.liftweb.json.Printer._
import scala.collection.JavaConversions._
import net.liftweb.json.{DefaultFormats, JsonParser}
import collection.mutable.ListBuffer
import org.apache.commons.io.IOUtils
import java.util.{Scanner, Date}
import org.apache.commons.lang3.StringUtils
import org.scalatra.util.{MapWithIndifferentAccess, MultiMapHeadView}
import javax.servlet.http.{HttpServletRequest, Cookie}
import collection.immutable.Map

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

  get("/greetXML") {
    greetXML
  }

  get("/greetXMLAttribute") {
    contentType = "application/xml"
    <greeting>
        <name firstName={params("firstName")} lastName={params("lastName")} />
    </greeting>
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
      "      \"price\": 19.95,\n" +
      "    }\n" +
      "  }\n" +
      "}";
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

  get("/multiValueParam") {
    "{ \"list\" : \""+{multiParams("list")}.mkString(",") +"\" }"
  }

  put("/multiValueParam") {
    val content: String = IOUtils.toString(request.getInputStream)
    "{ \"list\" : \""+{findMultiParamIn(content, "list")}.mkString(",") +"\" }"
  }

  post("/multiValueParam") {
    "{ \"list\" : \""+{multiParams("list")}.mkString(",") +"\" }"
  }

  get("/hello") {
    val json = ("hello" -> "Hello Scalatra")
    compact(render(json))
  }

  get("/lotto") {
    val json = ("lotto" -> ("lottoId" -> lotto.id) ~
      ("winning-numbers" -> lotto.winningNumbers) ~
      ("drawDate" -> lotto.drawDate.map(_.toString)) ~
      ("winners" -> lotto.winners.map { w =>
        (("winnerId" -> w.id) ~ ("numbers" -> w.numbers))}))
    compact(render(json))
  }

  post("/reflect") {
    contentType = request.getContentType
    val cookies = request.getCookies
    if(cookies != null) {
      cookies.foreach { response.addCookie(_) }
    }
    request.body
  }

  put("/serializedJsonParameter") {
    val something = {params("something")}
    val serialized = {params("serialized")}
    serialized
  }

  get("/:firstName/:lastName") {
    val firstName = {params("firstName")}
    val lastName = {params("lastName")}
    val fullName: String = firstName + " " + lastName
    val json = ("firstName" -> firstName) ~ ("lastName" -> lastName) ~ ("fullName" -> fullName)
    compact(render(json))

  }

  get("/409") {
    contentType = "text/plain"
    response.setStatus(409)
    "ERROR"
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

  post("/noValueParam") {
    "Params: "+params.foldLeft(new StringBuilder)( (b,t) => b.append(t._1+"="+t._2)).toString()
  }

  get("/redirect") {
    val url: String = {params("url")}
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

  post("/j_spring_security_check") {
    contentType = "text/plain"
    val userName = params.get("j_username").get
    val password = params.get("j_password").get
    if (userName == "John" && password == "Doe") {
      response.setHeader("Set-Cookie", "jsessionid=1234")
    } else {
      "NO"
    }
  }

  get("/formAuth") {
    contentType = "text/plain"
    val cookies: Array[Cookie] = request.getCookies
    if(cookies == null) {
      loginPage
    } else {
      val cookie = cookies.find(_.getName == "jsessionid").get
      if(cookie == null) {
        loginPage
      } else if (cookie.getValue == "1234") {
        "OK"
      } else {
        "NOT AUTHORIZED"
      }
    }
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
  delete("/body") {
    getStringBody
  }

  put("/binaryBody") {
    getBinaryBodyResponse
  }

  post("/binaryBody") {
    getBinaryBodyResponse
  }

  post("/jsonBody") {
    contentType = "text/plain";
    val header: String = request.getHeader("Content-Type")
    if(!header.contains("application/json")) {
      "FAILURE"
    } else {
      val json = JsonParser.parse(request.body)
      (json \  "message").extract[String]
    }
  }

  post("/jsonBodyAcceptHeader") {
    val accept : String = request.getHeader("Accept")
    if(!accept.contains("application/json")) {
      "FAILURE"
    } else {
      val json = JsonParser.parse(request.body)
      (json \  "message").extract[String]
    }
  }

  get("/setCookies") {
    setCookies
  }

  post("/header") {
    getHeaders
  }

  get("/header") {
    getHeaders
  }

  get("/multiHeaderReflect") {
    contentType = "text/plain"
    val headerNames = request.getHeaderNames()
    while(headerNames.hasMoreElements()) {
      val name = headerNames.nextElement.toString
      val headerValues = request.getHeaders(name)
      while(headerValues.hasMoreElements) {
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
    contentType ="text/xml"
    <xml>something</xml>
  }

  get("/textHTML") {
    contentType ="text/html"
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

  get("/textHTML-not-formatted") {
    contentType ="text/html"
    <html><head><title>my title</title></head><body><p>paragraph 1</p><p>paragraph 2</p></body></html>
  }

  get("/statusCode500") {
    contentType = "text/plain"
    response.setStatus(500)
    "An expected error occurred"
  }

  get("/rss") {
    contentType ="application/rss+xml"
    <rss>
      <item>
        <title>rss title</title>
      </item>
    </rss>
  }


  get("/jsonp") {
    contentType ="application/javascript"
    params("callback")+"("+greetJson+");"
  }

  get("/bigRss") {
    contentType ="application/rss+xml"
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


  def getBinaryBodyResponse: String = {
    contentType = "text/plain";
    Stream.continually(request.getInputStream().read).takeWhile(_ != -1).map(_.toByte).toList.mkString(", ")
  }

  def getHeaders: String = {
    contentType = "text/plain"
    val headerNames = request.getHeaderNames()
    val names = ListBuffer[String]()
    while(headerNames.hasMoreElements()) {
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

  def loginPage : String = {
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

    if(category == "books" && userName == "admin") {
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

  def findParamIn(content: String, param: String): String = {
    var value: String = StringUtils.substringBetween(content, param+"=", "&")
    if(value == null) {
      value = StringUtils.substringAfter(content, param+"=")
    }
    return value
  }

  def findMultiParamIn(content: String, param: String): scala.collection.mutable.MutableList[String] = {
    val scanner: Scanner = new Scanner(content).useDelimiter("&")
    val myList = scala.collection.mutable.MutableList[String]()
    while(scanner.hasNext) {
      val next: String = scanner.next
      myList += next.split('=')(1)
    }
    myList
  }
}
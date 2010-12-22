package com.jayway.restassured.scalatra

import org.scalatra.ScalatraServlet
import net.liftweb.json.JsonDSL._
import java.lang.String
import xml.Elem
import net.liftweb.json.JsonAST._
import net.liftweb.json.{DefaultFormats, JsonParser}
import collection.mutable.ListBuffer

class ScalatraRestExample extends ScalatraServlet {
  // To allow for json extract
  implicit val formats = DefaultFormats

  case class Winner(id: Long, numbers: List[Int])
  case class Lotto(id: Long, winningNumbers: List[Int], winners: List[Winner], drawDate: Option[java.util.Date])

  val winners = List(Winner(23, List(2, 45, 34, 23, 3, 5)), Winner(54, List(52, 3, 12, 11, 18, 22)))
  val lotto = Lotto(5, List(2, 45, 34, 23, 7, 5, 3), winners, None)

  before {
    contentType = "application/json"
  }

  post("/hello") {
    val json = ("hello" -> "Hello Scalatra")
    compact(render(json))
  }

  get("/greetXML") {
    greetXML
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


  get("/:firstName/:lastName") {
    val firstName = {params("firstName")}
    val lastName = {params("lastName")}
    val fullName: String = firstName + " " + lastName
    val json = ("firstName" -> firstName) ~ ("lastName" -> lastName) ~ ("fullName" -> fullName)
    compact(render(json))
  }

  put("/greet") {
    greetJson
  }

  delete("/greet") {
    greetJson
  }

  get("/greet") {
    greetJson
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

  post("/binaryBody") {
    getBinaryBodyResponse
  }

  post("/jsonBody") {
    contentType = "text/plain";
    val json = JsonParser.parse(request.body)
    (json \  "message").extract[String]
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
    response.setHeader("Set-Cookie", " key1=value1 ; key2= value2;key3 = value3")
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

  def greetXML: Elem = {
    contentType = "application/xml"
    <greeting>
      <firstName>{params("firstName")}</firstName>
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
}
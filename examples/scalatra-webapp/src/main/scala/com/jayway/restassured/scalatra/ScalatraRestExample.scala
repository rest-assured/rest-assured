package com.jayway.restassured.scalatra

import org.scalatra.ScalatraServlet
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonDSL._
import java.lang.String
import xml.Elem
import text.Document

class ScalatraRestExample extends ScalatraServlet {

  case class Winner(id: Long, numbers: List[Int])
  case class Lotto(id: Long, winningNumbers: List[Int], winners: List[Winner], drawDate: Option[java.util.Date])

  val winners = List(Winner(23, List(2, 45, 34, 23, 3, 5)), Winner(54, List(52, 3, 12, 11, 18, 22)))
  val lotto = Lotto(5, List(2, 45, 34, 23, 7, 5, 3), winners, None)

  before {
    contentType = "application/json"
  }

  post("/hello") {
    val json = ("hello" -> "Hello Scalatra")
    compact(JsonAST.render(json))
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
    compact(JsonAST.render(json))
  }

  get("/lotto") {
    val json = ("lotto" -> ("lottoId" -> lotto.id) ~
            ("winning-numbers" -> lotto.winningNumbers) ~
            ("drawDate" -> lotto.drawDate.map(_.toString)) ~
            ("winners" -> lotto.winners.map { w =>
              (("winnerId" -> w.id) ~ ("numbers" -> w.numbers))}))
    val jsonDocument: Document = JsonAST.render(json)
    compact(jsonDocument)
  }


  get("/:firstName/:lastName") {
    val firstName = {params("firstName")}
    val lastName = {params("lastName")}
    val fullName: String = firstName + " " + lastName
    val json = ("firstName" -> firstName) ~ ("lastName" -> lastName) ~ ("fullName" -> fullName)
    compact(JsonAST.render(json))
  }

  get("/greet") {
    val name = "Greetings " + {params("firstName")} + " " + {params("lastName")}
    val json = ("greeting" -> name)
    compact(JsonAST.render(json))
  }

  post("/greet") {
    val name = "Greetings " + {params("firstName")} + " " + {params("lastName")}
    val json = ("greeting" -> name)
    compact(JsonAST.render(json))
  }

  notFound {
    response.setStatus(404)
    "Not found"
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
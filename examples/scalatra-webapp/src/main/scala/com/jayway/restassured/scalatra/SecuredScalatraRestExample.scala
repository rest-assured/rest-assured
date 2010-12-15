package com.jayway.restassured.scalatra

import org.scalatra.ScalatraServlet
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonDSL._

class SecuredScalatraRestExample extends ScalatraServlet {

  before {
    contentType = "application/json"
  }

  post("/hello") {
    val json = ("hello" -> "Hello Secured Scalatra")
    compact(JsonAST.render(json))
  }

  get("/hello") {
    val json = ("hello" -> "Hello Secured Scalatra")
    compact(JsonAST.render(json))
  }
}
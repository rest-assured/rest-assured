package com.jayway.restassured.scalatra

import org.scalatra.ScalatraServlet
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonDSL._
import java.lang.String


/**
 * Created by IntelliJ IDEA.
 * User: johan
 * Date: Oct 8, 2010
 * Time: 6:42:09 PM
 * To change this template use File | Settings | File Templates.
 */

class ScalatraRestExample extends ScalatraServlet {

  before {
    contentType = "application/json"
  }

  post("/hello") {
    val json = ("hello" -> "Hello Scalatra")
    compact(JsonAST.render(json))
  }

  get("/hello") {
     val json = ("hello" -> "Hello Scalatra")
     compact(JsonAST.render(json))
   }


  get("/:firstName/:lastName") {
    val firstName = {params("firstName")}
    val lastName = {params("lastName")}
    val fullName: String = firstName + " " + lastName
    val json = ("firstName" -> firstName) ~ ("lastName" -> lastName) ~ ("fullName" -> fullName)
    compact(JsonAST.render(json))
  }

  notFound {
    response.setStatus(404)
    "Not found"
  }
}
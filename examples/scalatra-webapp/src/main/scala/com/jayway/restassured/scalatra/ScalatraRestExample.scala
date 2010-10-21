package com.jayway.restassured.scalatra

import org.scalatra.ScalatraServlet
import net.liftweb.json.JsonAST
import net.liftweb.json.JsonDSL._


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


  get("/:key/:value") {
    val key = {params("key")}
    val value = {params("value")}
    System.out.println(key+" "+value)
    val json = ("name" -> "joe") ~ ("age" -> 35)
    compact(JsonAST.render(json))
  }

  notFound {
    response.setStatus(404)
    "Not found"
  }
}
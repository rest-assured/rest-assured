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

import org.scalatra.ScalatraServlet

import scala.collection.mutable
import scala.util.Random

class CustomAuthExample extends ScalatraServlet {

  val authenticatedSessions = new mutable.HashMap[String, Int]()

  before() {
    contentType = "application/json"
  }

  post("/login") {
    val rand = new Random(System.currentTimeMillis())
    val operandA = rand.nextInt(1000)
    val operandB = rand.nextInt(1000)
    val expectedSum = operandA + operandB
    val id = rand.nextLong().toString
    authenticatedSessions += id -> expectedSum
    "{ \"operandA\" : "+operandA + ", \"operandB\" : "+operandB + ", \"id\" : \""+id+"\" }"
  }

  get("/secretMessage") {
    returnIfLoggedIn("I'm secret")
  }

  get("/secretMessage2") {
    returnIfLoggedIn("I'm also secret")
  }

  private def returnIfLoggedIn(message: => String) : String = {
    val actualSum = request.getParameter("sum")
    val id = request.getParameter("id")
    val expectedSum = authenticatedSessions.getOrElse(id, -1)
    if (actualSum == null || id == null || expectedSum == -1 || actualSum.toInt != expectedSum) {
      """{ "message" : "You're not authorized to see the secret message" }"""
    } else {
      "{ \"message\" : \""+message+"\" }"
    }
  }
}
/*
 * Copyright 2024 the original author or authors.
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

package io.restassured.module.scala.extensions

import io.restassured.RestAssured
import io.restassured.builder.ResponseBuilder
import io.restassured.filter.Filter
import io.restassured.http.ContentType.JSON
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test

class RestAssuredScalaExtensionsTest:

  @Before
  def `rest assured is configured`: Unit =
    RestAssured.filters { (_, _, _) =>
      new ResponseBuilder()
        .setStatusCode(200)
        .setContentType(JSON)
        .setBody("""{ "message" : "Hello World"}""")
        .build()
    }

  @Test
  def `rest assured is reset after each test`: Unit =
    RestAssured.reset()

  @Test
  def `basic rest assured scala extensions are compilable`: Unit =
    Given(req =>
      req.port(7000)
      req.header("Header", "Header")
      req.body("hello")
    )
      .When(
        _.put("/the/path")
      )
      .Then(res =>
        res.statusCode(200)
        res.body("message", equalTo("Hello World"))
      )

  @Test
  def `extraction with rest assured scala extensions`: Unit =
    val message: String = Given(req =>
      req.port(7000)
      req.header("Header", "Header")
      req.body("hello")
    )
      .When(
        _.put("/the/path")
      )
      .Extract(
        _.path("message")
      )
    assertThat(message).isEqualTo("Hello World")

  @Test
  def `validation with rest assured scala extensions using ThenAssert returning Unit`: Unit =
    val result = Given(req =>
      req.port(7000)
      req.header("Header", "Header")
      req.body("hello")
    )
      .When(
        _.put("/the/path")
      )
      .ThenAssert(res =>
        res.statusCode(200)
        res.body("message", equalTo("Hello World"))
      )
    assertThat(result).isEqualTo(())

  @Test
  def `extraction after 'then', when path is not used in 'Then',  with rest assured scala extensions`: Unit =
    val message: String = Given(req =>
      req.port(7000)
      req.header("Header", "Header")
      req.body("hello")
      req.filter((_, _, _) =>
        new ResponseBuilder()
          .setStatusCode(200)
          .setContentType(JSON)
          .setBody("""{ "message" : "Hello World"}""")
          .build()
      )
    )
      .When(
        _.put("/the/path")
      )
      .Then(
        _.statusCode(200)
      )
      .Extract(
        _.path("message")
      )
    assertThat(message).isEqualTo("Hello World")

  @Test
  def `extraction after 'then', when path is used in 'Then',  with rest assured scala extensions`: Unit =
    val message: String = Given(req =>
      req.port(7000)
      req.header("Header", "Header")
      req.body("hello")
      req.filter((_, _, _) =>
        new ResponseBuilder()
          .setStatusCode(200)
          .setContentType(JSON)
          .setBody("""{ "message" : "Hello World"}""")
          .build()
      )
    )
      .When(
        _.put("/the/path")
      )
      .Then(res =>
        res.statusCode(200)
        res.body("message", not(emptyOrNullString()))
      )
      .Extract(
        _.path("message")
      )
    assertThat(message).isEqualTo("Hello World")

  @Test
  def `all expectations error messages are included in the error message`: Unit =
    try {
      Given(req =>
        req.port(7000)
        req.header("Header", "Header")
        req.body("hello")
      )
        .When(
          _.put("/the/path")
        )
        .Then(res =>
          res.statusCode(400)
          res.body("message", equalTo("Another World"))
          res.body("message", equalTo("Brave new world"))
        )
    } catch {
      case e: AssertionError =>
        assertThat(e).hasMessage("""3 expectations failed.
                                   |Expected status code <400> but was <200>.
                                   |
                                   |JSON path message doesn't match.
                                   |Expected: Another World
                                   |  Actual: Hello World
                                   |
                                   |JSON path message doesn't match.
                                   |Expected: Brave new world
                                   |  Actual: Hello World
                                   |""".stripMargin)
    }

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

package io.restassured.scala

import io.restassured.module.scala.extensions.*
import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.hamcrest.CoreMatchers.containsString
import org.junit.{After, Before, Test}

class Scala3ITest:

  var webServer: MockWebServer = _

  @Before
  def `Mock web server is initialized`() =
    webServer = new MockWebServer()
    webServer.start()

  @After
  def `Mock web server is shutdown`() =
    webServer.shutdown()

  @Test
  def `trying out rest assured in scala`() =
    val response = new MockResponse
    response.setBody(""" { "key" : "value" } """)
    response.setHeader("content-type", "application/json")
    webServer.enqueue(response)

    Given(_.port(webServer.getPort))
      .When(req => req.get("/greetJSON"))
      .ThenAssert(res => // Use ThenAssert as the last method in the chain for a test with validation
        res.statusCode(200)
        res.body("key", containsString("value"))
      )

  @Test
  def `validating a value and extrating it from the response`() =
    val response = new MockResponse
    response.setBody(""" { "key" : "value" } """)
    response.setHeader("content-type", "application/json")
    webServer.enqueue(response)

    val value: String = Given(_.port(webServer.getPort))
      .When(req => req.get("/greetJSON"))
      .Then(res => // Use Then when you want to chain an Extract method after the validation
        res.statusCode(200)
        res.body("key", containsString("value"))
      )
      .Extract(_.path("key"))

    assert(value == "value")

  @Test
  def `extracting a value from a response`() =
    val response = new MockResponse
    response.setBody(""" { "key" : "value" } """)
    response.setHeader("content-type", "application/json")
    webServer.enqueue(response)

    val value: String = Given(_.port(webServer.getPort))
      .When(req => req.get("/greetJSON"))
      .Extract(_.path("key"))

    assert(value == "value")

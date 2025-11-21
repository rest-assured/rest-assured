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

package io.restassured.scala

import io.restassured.RestAssured.given
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import mockwebserver3.junit5.StartStop
import mockwebserver3.{MockResponse, MockWebServer}
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ScalaITest {

  @StartStop
  var webServer: MockWebServer = new MockWebServer()

  @Test
  def `trying out rest assured in scala`(): Unit = {
    val response = new MockResponse.Builder()
            .body("""{ "key" : "value" }""")
            .addHeader("content-type", "application/json")
            .build()
    webServer.enqueue(response)

    given().
            port(webServer.getPort).
    when().
            get("/greetJSON").
    then().
            statusCode(200).
            body("key", equalTo("value"))
  }

  @Test
  def `trying out rest assured in scala with implicit conversion`(): Unit = {
    val response = new MockResponse.Builder()
            .body("""{ "key" : "value" }""")
            .addHeader("content-type", "application/json")
            .build()
    webServer.enqueue(response)

    given().
            port(webServer.getPort).
    when().
            get("/greetJSON").
    Then().
            statusCode(200).
            body("key", equalTo("value"))
  }
}
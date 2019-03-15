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

import io.restassured.module.mockmvc.RestAssuredMockMvc.given
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers._

class ScalaMockMvcTest {

  @Test
  def implicit_conversion_works_when_using_the_spring_mock_mvc_module() {
    given().
            standaloneSetup(new GreetingController()).
            param("name", "Scala").
    when().
            get("/greeting").
    Then().
            expect(status.isOk).
            body("greetings", equalTo("Scala"))
  }
}

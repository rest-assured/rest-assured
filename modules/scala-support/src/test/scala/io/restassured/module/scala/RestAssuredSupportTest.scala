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

package io.restassured.module.scala

import io.restassured.RestAssured.given
import io.restassured.builder.ResponseBuilder
import io.restassured.filter.{Filter, FilterContext}
import io.restassured.http.ContentType.JSON
import io.restassured.module.scala.RestAssuredSupport._
import io.restassured.specification.{FilterableRequestSpecification, FilterableResponseSpecification}
import org.hamcrest.Matchers.equalTo
import org.junit.Test

class RestAssuredSupportTest {

  @Test
  def implicit_conversion_is_applied_when_importing_rest_assured_support_namespace() {
    given().
            filter(new Filter {
                override def filter(requestSpec: FilterableRequestSpecification,
                                    responseSpec: FilterableResponseSpecification,
                                    ctx: FilterContext) = new ResponseBuilder().setStatusCode(200).setContentType(JSON).setBody("""{ "key" : "value" }""").build()
            }).
    when().
            get("/x").
    Then().
            body("key", equalTo("value"))
  }
}

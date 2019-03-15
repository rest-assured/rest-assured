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

import io.restassured.response.{ResponseBody, ResponseOptions, Validatable, ValidatableResponseOptions}

/**
 * A support class that you may use in Scala to add a "Then" method to the Response object. The reason for doing this
 * is to avoid a warning if you're using "then" in Scala since it might be a reserved keyword in the future.
 */
object RestAssuredSupport {

  implicit class AddThenToResponse[T <: ValidatableResponseOptions[T, R], R <: ResponseBody[R] with ResponseOptions[R]](response: Validatable[T, R]) {
    /**
     * Returns a validatable response that's lets you validate the response. Usage example:
     * <p/>
     * <pre>
     * given().
     *         param("firstName", "John").
     *         param("lastName", "Doe").
     * when().
     *        get("/greet").
     * Then().
     *        body("greeting", equalTo("John Doe"));
     * </pre>
     *
     * The reason for using this method instead of <code>then</code> is to avoid Scala compiler warnings.
     * @return A validatable response
     */
    def Then(): T = {
      response.`then`
    }
  }
}



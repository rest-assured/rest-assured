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

package io.restassured.module.kotlin.extensions

import io.restassured.RestAssured
import io.restassured.builder.ResponseBuilder
import io.restassured.filter.Filter
import io.restassured.http.ContentType.JSON
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test


class RestAssuredKotlinExtensionsTest {

    @Before
    fun `rest assured is configured`() {
        RestAssured.filters(Filter { _, _, _ ->
            ResponseBuilder().setStatusCode(200).setContentType(JSON).setBody("""{ "message" : "Hello World"}""").build()
        })
    }

    @Test
    fun `rest assured is reset after each test`() {
        RestAssured.reset()
    }

    @Test
    fun `basic rest assured kotlin extensions are compilable`() {
        Given {
            port(7000)
            header("Header", "Header")
            body("hello")
        } When {
            put("/the/path")
        } Then {
            statusCode(200)
            body("message", equalTo("Hello World"))
        }
    }

    @Test
    fun `extraction with rest assured kotlin extensions`() {
        val message: String = Given {
            port(7000)
            header("Header", "Header")
            body("hello")
        } When {
            put("/the/path")
        } Extract {
            path("message")
        }

        assertThat(message).isEqualTo("Hello World")
    }

    @Test
    fun `extraction after 'then', when path is not used in 'Then',  with rest assured kotlin extensions`() {
        val message: String = Given {
            port(7000)
            header("Header", "Header")
            body("hello")
            filter { _, _, _ ->
                ResponseBuilder().setStatusCode(200).setContentType(JSON).setBody("""{ "message" : "Hello World"}""").build()
            }
        } When {
            put("/the/path")
        } Then {
            statusCode(200)
        } Extract {
            path("message")
        }

        assertThat(message).isEqualTo("Hello World")
    }

    @Test
    fun `extraction after 'then', when path is used in 'Then',  with rest assured kotlin extensions`() {
        val message: String = Given {
            port(7000)
            header("Header", "Header")
            body("hello")
            filter { _, _, _ ->
                ResponseBuilder().setStatusCode(200).setContentType(JSON).setBody("""{ "message" : "Hello World"}""").build()
            }
        } When {
            put("/the/path")
        } Then {
            statusCode(200)
            body("message", not(emptyOrNullString()))
        } Extract {
            path("message")
        }

        assertThat(message).isEqualTo("Hello World")
    }

    @Test
    fun `all expectations error messages are included in the error message`() {
        val throwable = catchThrowable {
            Given {
                port(7000)
                header("Header", "Header")
                body("hello")
            } When {
                put("/the/path")
            } Then {
                statusCode(400)
                body("message", equalTo("Another World"))
                body("message", equalTo("Brave new world"))
            }
        }

        assertThat(throwable).isExactlyInstanceOf(AssertionError::class.java).hasMessage("""
            3 expectations failed.
            Expected status code <400> but was <200>.

            JSON path message doesn't match.
            Expected: Another World
              Actual: Hello World

            JSON path message doesn't match.
            Expected: Brave new world
              Actual: Hello World
            
        """.trimIndent())
    }
}
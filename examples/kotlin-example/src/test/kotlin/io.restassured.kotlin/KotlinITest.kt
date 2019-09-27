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

package io.restassured.kotlin

import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class KotlinITest {

    lateinit var webServer: MockWebServer

    @Before
    fun `mock web server is started`() {
        webServer = MockWebServer()
        webServer.start()

        RestAssured.port = webServer.port
    }

    @After
    fun `shutdown webserver after each test`() {
        webServer.shutdown()
        RestAssured.reset()
    }

    @Test
    fun `kotlin extension can validate single expecation for json bodies in 'Then' block when no 'Extract' block is defined`() {
        // Given
        givenServerWillReturnJson(""" { "greeting" : "Hello World" } """)

        // When
        When {
            get("/greeting")
        } Then {
            body("greeting", not(emptyOrNullString()))
        }
    }

    @Test
    fun `kotlin extension can validate multiple expectations for json bodies in 'Then' block when no 'Extract' block is defined`() {
        // Given
        givenServerWillReturnJson(""" { "greeting" : "Hello World", "other" : "thing", "something" : "else" } """)

        // When
        When {
            get("/greeting")
        } Then {
            body("greeting", equalTo("Hello World"))
            body("other", equalTo("thing"))
            body("something", equalTo("else"))
        }
    }

    @Test
    fun `kotlin extension can validate json bodies in 'Then' block when 'Extract' block is defined with different path than expectation`() {
        // Given
        givenServerWillReturnJson(""" { "greeting" : "Hello World", "other" : "thing" } """)

        // When
        val greeting : String = When {
            get("/greeting")
        } Then {
            body("greeting", not(emptyOrNullString()))
        } Extract {
            path("greeting")
        }

        // Then
        assertEquals("Hello World", greeting)
    }

    @Test
    fun `kotlin extension can validate json bodies in 'Then' block when 'Extract' block is defined with same path as expectation`() {
        // Given
        givenServerWillReturnJson(""" { "greeting" : "Hello World", "other" : "thing" } """)

        // When
        val other : String = When {
            get("/greeting")
        } Then {
            body("greeting", not(emptyOrNullString()))
        } Extract {
            path("other")
        }

        // Then
        assertEquals("thing", other)
    }

    private fun givenServerWillReturnJson(jsonString: String) {
        val response = MockResponse()
        response.setBody(jsonString)
        response.setHeader("content-type", "application/json")
        webServer.enqueue(response)
    }
}
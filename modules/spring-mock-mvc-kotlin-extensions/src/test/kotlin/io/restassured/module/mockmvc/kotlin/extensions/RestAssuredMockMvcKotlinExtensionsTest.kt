package io.restassured.module.mockmvc.kotlin.extensions

import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.Test
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/*
 * Copyright 2020 the original author or authors.
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

class RestAssuredMockMvcKotlinExtensionsTest {

    @Test
    fun uses_predefined_mock_mvc_instance() {
        val mockMvc =
            MockMvcBuilders.standaloneSetup(GreetingController())
                .build()

        val id: Int =
        Given {
            mockMvc(mockMvc)
            param("name", "Johan")
        } When {
            get("/greeting")
        } Then {
            body(
                "id", Matchers.equalTo(1),
                "content", Matchers.equalTo("Hello, Johan!")
            )
        } Extract {
            path("id")
        }

        assertThat(id).isEqualTo(1)
    }

    @Test
    fun param_with_int() {
        val mockMvc =
            MockMvcBuilders.standaloneSetup(GreetingController())
                .build()

        val id: Int =
        Given {
            mockMvc(mockMvc)
            param("name", 1)
        } When {
            get("/greeting")
        } Then {
            body(
                "id", Matchers.equalTo(1),
                "content", Matchers.equalTo("Hello, 1!")
            )
        } Extract {
            path("id")
        }

        assertThat(id).isEqualTo(1)
    }

    @Test
    fun uses_predefined_standalone() {
        val id: Int =
        Given {
            standaloneSetup(GreetingController())
            param("name", "Johan")
        } When {
            get("/greeting")
        } Then {
            body(
                "id", Matchers.equalTo(1),
                "content", Matchers.equalTo("Hello, Johan!")
            )
        } Extract {
            path("id")
        }

        assertThat(id).isEqualTo(1)
    }

    @Test
    fun uses_static_mock_mvc() {
        RestAssuredMockMvc.mockMvc(MockMvcBuilders.standaloneSetup(GreetingController()).build())
        try {
            val id: Int =
            Given {
                param("name", "Johan")
            } When {
                get("/greeting")
            } Then {
                body(
                    "id", Matchers.equalTo(1),
                    "content", Matchers.equalTo("Hello, Johan!")
                )
            } Extract {
                path("id")
            }

            assertThat(id).isEqualTo(1)

            val id2: Int =
            Given {
                param("name", "Erik")
            } When {
                get("/greeting")
            } Then {
                body(
                    "id", Matchers.equalTo(2),
                    "content", Matchers.equalTo("Hello, Erik!")
                )
            } Extract {
                path("id")
            }

            assertThat(id2).isEqualTo(2)
        } finally {
            RestAssuredMockMvc.reset()
        }
    }
}

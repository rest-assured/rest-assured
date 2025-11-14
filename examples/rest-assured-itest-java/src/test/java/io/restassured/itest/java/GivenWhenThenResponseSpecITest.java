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

package io.restassured.itest.java;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

public class GivenWhenThenResponseSpecITest extends WithJetty {
    @Test
    void simple_given_when_then_works() {
        ResponseSpecification specification = new ResponseSpecBuilder().expectStatusCode(201).expectBody("greeting", equalTo("Greetings John Doo")).build();

        assertThatThrownBy(() ->
            given().
                param("firstName", "John").
                param("lastName", "Doe").
            when().
                get("/greet").
            then().
                spec(specification)
        )
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("2 expectations failed.")
        .hasMessageContaining("Expected status code <201> but was <200>.")
        .hasMessageContaining("JSON path greeting doesn't match.")
        .hasMessageContaining("Expected: Greetings John Doo")
        .hasMessageContaining("Actual: Greetings John Doe");
    }
}

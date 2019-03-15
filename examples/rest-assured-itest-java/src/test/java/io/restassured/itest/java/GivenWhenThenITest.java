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

import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class GivenWhenThenITest extends WithJetty {

    @Test public void
    simple_given_when_then_works() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                statusCode(200).
                body("greeting", equalTo("Greetings John Doe"));
    }

    @Test public void
    given_when_then_works_with_assert_that_and_and() {
        given().
                param("firstName", "John").
                param("lastName", "Doe").
        when().
                get("/greet").
        then().
                assertThat().
                statusCode(200).
                and().
                body("greeting", equalTo("Greetings John Doe"));
    }

    @Test public void
    given_when_then_works_with_xpath_assertions() {
        given().
                params("firstName", "John", "lastName", "Doe").
        when().
                get("/greetXML").
        then().
                body(hasXPath("/greeting/firstName[text()='John']"));
    }

    @Test public void
    given_when_then_works_with_multiple_body_assertions() {
        given().
                params("firstName", "John", "lastName", "Doe").
        when().
                get("/greetXML").
        then().
                body(containsString("greeting")).
                body(containsString("John"));
    }
}

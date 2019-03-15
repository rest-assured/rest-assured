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

package io.restassured.module.mockmvc;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;

public class MockMvcStaticRequestAndResponseSpecTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test public void
    request_and_response_spec_can_be_defined_statically() {
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().addQueryParam("name", "Johan").build();
        RestAssuredMockMvc.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).expectBody("content", equalTo("Hello, Johan!")).build();

        try {
            // When
            RestAssuredMockMvc.given().
                    standaloneSetup(new GreetingController()).
            when().
                    get("/greeting").
            then().
                    body("id", equalTo(1));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    response_validation_fails_if_any_property_in_the_response_is_not_valid() {
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().addQueryParam("name", "Johan").build();
        RestAssuredMockMvc.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).expectBody("content", equalTo("Hello, John!")).build();

        exception.expect(AssertionError.class);
        exception.expectMessage("1 expectation failed.\n" +
                "JSON path content doesn't match.\n" +
                "Expected: Hello, John!\n" +
                "  Actual: Hello, Johan!");

        try {
            // When
            RestAssuredMockMvc.given().
                    standaloneSetup(new GreetingController()).
            when().
                    get("/greeting").
            then().
                    body("id", equalTo(1));
        } finally {
            RestAssuredMockMvc.reset();
        }
    }

    @Test public void
    response_validation_kicks_in_even_when_no_then_clause_is_specified() {
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().addQueryParam("name", "Johan").build();
        RestAssuredMockMvc.responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).expectBody("content", equalTo("Hello, John!")).build();

        exception.expect(AssertionError.class);
        exception.expectMessage("1 expectation failed.\n" +
                "JSON path content doesn't match.\n" +
                "Expected: Hello, John!\n" +
                "  Actual: Hello, Johan!");

        try {
            // When
            RestAssuredMockMvc.given().
                    standaloneSetup(new GreetingController()).
            when().
                    get("/greeting");
        } finally {
            RestAssuredMockMvc.reset();
        }
    }
}

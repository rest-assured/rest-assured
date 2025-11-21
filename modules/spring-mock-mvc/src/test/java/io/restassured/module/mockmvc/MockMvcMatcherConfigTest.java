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

import io.restassured.config.MatcherConfig;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.http.GreetingController;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

public class MockMvcMatcherConfigTest {
    @Test public void
    response_message_is_formatted_with_rest_assured_description_type() {
        RestAssuredMockMvc.config = RestAssuredMockMvcConfig.config().matcherConfig(MatcherConfig.matcherConfig()
                .errorDescriptionType(MatcherConfig.ErrorDescriptionType.REST_ASSURED));

        assertThatThrownBy(() -> {
            try {
                // When
                RestAssuredMockMvc.given().
                        standaloneSetup(new GreetingController()).
                        when().
                        get("/greeting").
                        then().
                        body("content", equalTo("Hello, John!"));
            } finally {
                RestAssuredMockMvc.reset();
            }
        })
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("""
                        1 expectation failed.
                        JSON path content doesn't match.
                        Expected: Hello, John!
                          Actual: Hello, World!""");
    }

    @Test public void
    response_message_is_formatted_with_hamcrest_description_type() {
        RestAssuredMockMvc.config = RestAssuredMockMvcConfig.config().matcherConfig(MatcherConfig.matcherConfig()
                .errorDescriptionType(MatcherConfig.ErrorDescriptionType.HAMCREST));

        assertThatThrownBy(() -> {
            try {
                // When
                RestAssuredMockMvc.given().
                        standaloneSetup(new GreetingController()).
                        when().
                        get("/greeting").
                        then().
                        body("content", equalTo("Hello, John!"));
            } finally {
                RestAssuredMockMvc.reset();
            }
        })
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("""
                        1 expectation failed.
                        JSON path content doesn't match.
                        
                        Expected: "Hello, John!"
                          Actual: was "Hello, World!"""
                );
    }
}

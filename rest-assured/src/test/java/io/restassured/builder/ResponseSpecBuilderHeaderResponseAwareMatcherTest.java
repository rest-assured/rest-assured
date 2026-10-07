/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.builder;

import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;

public class ResponseSpecBuilderHeaderResponseAwareMatcherTest {

    private static final Filter CREATED_RESPONSE = (requestSpec, responseSpec, ctx) -> new ResponseBuilder()
            .setStatusCode(201)
            .setStatusLine("HTTP/1.1 201 Created")
            .setContentType(ContentType.JSON)
            .setHeader("Location", "http://localhost:8080/users/42")
            .setBody("{\"id\":\"42\"}")
            .build();

    @Test
    public void expect_header_with_response_aware_matcher_passes_when_header_matches_value_from_body() {
        ResponseSpecification spec = new ResponseSpecBuilder()
                .expectHeader("Location", response -> endsWith("/users/" + response.path("id")))
                .build();

        given().filter(CREATED_RESPONSE).post("http://localhost:8080/users").then().spec(spec);
    }

    @Test
    public void expect_header_with_response_aware_matcher_fails_when_header_does_not_match_value_from_body() {
        ResponseSpecification spec = new ResponseSpecBuilder()
                .expectHeader("Location", response -> endsWith("/accounts/" + response.path("id")))
                .build();

        assertThatThrownBy(() -> given().filter(CREATED_RESPONSE).post("http://localhost:8080/users").then().spec(spec))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected header \"Location\" was not a string ending with \"/accounts/42\", was \"http://localhost:8080/users/42\"");
    }
}

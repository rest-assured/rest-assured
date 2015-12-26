/*
 * Copyright 2014 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.RestAssured.withArgs;
import static com.jayway.restassured.matcher.ResponseAwareMatcherComposer.and;
import static com.jayway.restassured.matcher.ResponseAwareMatcherComposer.or;
import static com.jayway.restassured.matcher.RestAssuredMatchers.endsWithPath;
import static org.hamcrest.Matchers.*;

public class ResponseAwareMatcherITest extends WithJetty {

    @Test public void
    can_use_response_aware_matcher_to_construct_hamcrest_matcher_with_data_from_response() {
        when().
                get("/game").
        then().
                statusCode(200).
                body("_links.self.href", response -> equalTo("http://localhost:8080/" + response.path("id"))).
                body("status", equalTo("ongoing"));
    }

    @Test public void
    can_use_response_aware_matcher_to_construct_hamcrest_matcher_with_data_from_response_with_predefined_matcher() {
        when().
                get("/game").
        then().
                statusCode(200).
                body("_links.self.href", endsWithPath("id")).
                body("status", equalTo("ongoing"));
    }

    @Test public void
    can_use_response_aware_matcher_to_construct_hamcrest_matcher_with_data_from_response_with_root_path() {
        when().
                get("/game").
        then().
                statusCode(200).
                root("_links.%s.href").
                body(withArgs("self"), endsWithPath("id"));
    }

    @SuppressWarnings("unchecked")
    @Test public void
    response_aware_matchers_are_composable_with_hamcrest_matchers() {
        when().
                get("/game").
        then().
                statusCode(200).
                body("_links.self.href", and(endsWithPath("id"), anyOf(startsWith("http://localhost:8081"), containsString("localhost")), response -> containsString(response.path("id")))).
                body("status", equalTo("ongoing"));
    }

    @SuppressWarnings("unchecked")
    @Test public void
    and_using_hamcrest_matchers_are_composable_with_response_aware_matchers() {
        when().
                get("/game").
        then().
                statusCode(200).
                body("_links.self.href", and(startsWith("http://localhost:8081"), endsWithPath("id"))).
                body("status", equalTo("ongoing"));
    }

    @SuppressWarnings("unchecked")
    @Test public void
    or_using_hamcrest_matchers_are_composable_with_response_aware_matchers() {
        when().
                get("/game").
        then().
                statusCode(200).
                body("_links.self.href", or(startsWith("http://localhost:8081"), endsWithPath("_links.self.href"))).
                body("status", equalTo("ongoing"));
    }

    @Test public void
    response_aware_matchers_are_composable_with_other_response_aware_matchers() {
        when().
                get("/game").
        then().
                statusCode(200).
                body("_links.self.href", and(endsWithPath("id"), response -> containsString("localhost"))).
                body("status", equalTo("ongoing"));
    }

    @Test public void
    response_aware_matchers_are_composable_with_other_response_aware_matchers_and_hamcrest_matchers() {
        when().
                get("/game").
        then().
                statusCode(200).
                body("_links.self.href", or(and(endsWithPath("id"), response -> containsString("localhost2")),
                        response -> startsWith("http://"))).
                body("status", equalTo("ongoing"));
    }
}

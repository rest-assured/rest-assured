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

import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.module.mockmvc.http.ResponseAwareMatcherController;
import io.restassured.module.mockmvc.matcher.RestAssuredMockMvcMatchers;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

public class MockMvcResponseAwareMatcherTest {

    @Test public void
    can_use_predefined_matcher_for_response_aware_matching() {
        RestAssuredMockMvc.given().
                standaloneSetup(new ResponseAwareMatcherController()).
        when().
                get("/responseAware").
        then().
                statusCode(200).
                body("_links.self.href", RestAssuredMockMvcMatchers.endsWithPath("id")).
                body("status", equalTo("ongoing"));
    }

    @Test public void
    can_use_custom_matcher_for_response_aware_matching() {
        RestAssuredMockMvc.given().
                standaloneSetup(new ResponseAwareMatcherController()).
        when().
                get("/responseAware").
        then().
                statusCode(200).
                body("_links.self.href", new ResponseAwareMatcher<MockMvcResponse>() {
                    public Matcher<?> matcher(MockMvcResponse response) throws Exception {
                        String contentAsString = response.getMockHttpServletResponse().getContentAsString();
                        return endsWith(new JsonPath(contentAsString).getString("id"));
                    }
                }).
                body("status", equalTo("ongoing"));
    }
}

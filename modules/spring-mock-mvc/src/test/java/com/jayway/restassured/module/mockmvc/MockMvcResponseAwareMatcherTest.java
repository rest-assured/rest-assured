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

package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.matcher.ResponseAwareMatcher;
import com.jayway.restassured.module.mockmvc.http.ResponseAwareMatcherController;
import com.jayway.restassured.module.mockmvc.response.MockMvcResponse;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.Test;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.jayway.restassured.module.mockmvc.matcher.RestAssuredMockMvcMatchers.endsWithPath;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

public class MockMvcResponseAwareMatcherTest {

    @Test public void
    can_use_predefined_matcher_for_response_aware_matching() {
        given().
                standaloneSetup(new ResponseAwareMatcherController()).
        when().
                get("/responseAware").
        then().
                statusCode(200).
                body("_links.self.href", endsWithPath("id")).
                body("status", equalTo("ongoing"));
    }

    @Test public void
    can_use_custom_matcher_for_response_aware_matching() {
        given().
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

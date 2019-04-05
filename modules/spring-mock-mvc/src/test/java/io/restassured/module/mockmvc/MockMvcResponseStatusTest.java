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

import io.restassured.module.mockmvc.http.ResponseAwareMatcherController;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.is;

public class MockMvcResponseStatusTest {

    @Test
    public void
    can_use_integer_value_for_status_code_matching() {
        RestAssuredMockMvc.given().
                standaloneSetup(new ResponseAwareMatcherController()).
                when().
                get("/responseAware").
                then().
                statusCode(200);
    }

    @Test
    public void
    can_use_hamcrest_matcher_for_status_code_matching() {
        RestAssuredMockMvc.given().
                standaloneSetup(new ResponseAwareMatcherController()).
                when().
                get("/responseAware").
                then().
                statusCode(is(200));
    }

    @Test
    public void
    can_use_spring_http_status_for_status_matching() {
        RestAssuredMockMvc.given().
                standaloneSetup(new ResponseAwareMatcherController()).
                when().
                get("/responseAware").
                then().
                status(HttpStatus.OK);
    }
}

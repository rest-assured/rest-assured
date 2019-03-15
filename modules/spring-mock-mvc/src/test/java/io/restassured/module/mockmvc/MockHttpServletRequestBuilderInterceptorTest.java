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

import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.intercept.MockHttpServletRequestBuilderInterceptor;
import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.equalTo;

public class MockHttpServletRequestBuilderInterceptorTest {

    @Test public void
    mock_http_servlet_request_builder_interceptor_works() {
        RestAssuredMockMvc.given().
                standaloneSetup(new GreetingController()).
                interceptor(new MockHttpServletRequestBuilderInterceptor() {
                    public void intercept(MockHttpServletRequestBuilder requestBuilder) {
                        requestBuilder.param("name", "John");
                    }
                }).
        when().
                get("/greeting").
        then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, John!"));
    }
}

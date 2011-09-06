/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.itest.java.presentation;

import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.post;
import static org.hamcrest.Matchers.equalTo;

public class CustomAuthDemoITest extends WithJetty {

    @Test
    public void customAuthDemo() throws Exception {
        given().
                filter(new CustomAuthFilter()).
        expect().
                body("message", equalTo("I'm secret")).
        when().
                get("/custom-auth/secretMessage");

    }

    @Test
    public void customAuthDemo2() throws Exception {
        given().
                filter(new CustomAuthFilter()).
        expect().
                body("message", equalTo("I'm also secret")).
        when().
                get("/custom-auth/secretMessage2");
    }


    private class CustomAuthFilter implements Filter {
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            final String loginResponse = post("/custom-auth/login").asString();
            final JsonPath jsonPath = new JsonPath(loginResponse);
            final int operandA = jsonPath.getInt("operandA");
            final int operandB = jsonPath.getInt("operandB");
            final String sessionId = jsonPath.getString("id");
            requestSpec.param("sum", String.valueOf(operandA+operandB));
            requestSpec.param("id", sessionId);
            return ctx.next(requestSpec, responseSpec);
        }
    }
}

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

package io.restassured.itest.java.presentation.filter;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.junit.Ignore;

import static io.restassured.RestAssured.post;

@Ignore("Not a test")
public class CustomAuthFilter implements Filter {
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        final String loginResponse = post("/custom-auth/login").asString();
        final JsonPath jsonPath = new JsonPath(loginResponse);
        final int operandA = jsonPath.getInt("operandA");
        final int operandB = jsonPath.getInt("operandB");
        final String sessionId = jsonPath.getString("id");
        requestSpec.param("sum", operandA+operandB);
        requestSpec.param("id", sessionId);
        return ctx.next(requestSpec, responseSpec);
    }
}

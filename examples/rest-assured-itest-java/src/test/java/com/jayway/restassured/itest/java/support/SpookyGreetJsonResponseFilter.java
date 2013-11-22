/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.itest.java.support;

import com.jayway.restassured.builder.ResponseBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.junit.Ignore;

@Ignore("Not a test")
public class SpookyGreetJsonResponseFilter implements Filter {

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        final Response response = ctx.next(requestSpec, responseSpec);
        final String responseAsString = response.asString();
        final JsonPath jsonPath = new JsonPath(responseAsString);
        final String firstName = jsonPath.getString("greeting.firstName");
        final String updatedResponse = responseAsString.replace(firstName, "Spooky");
        return new ResponseBuilder().clone(response).setBody(updatedResponse).build();
    }
}

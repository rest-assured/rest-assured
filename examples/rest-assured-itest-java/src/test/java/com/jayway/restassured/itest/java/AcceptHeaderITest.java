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

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class AcceptHeaderITest extends WithJetty {

    @Test public void
    accept_method_with_string_parameter_is_just_an_alias_for_header_accept() {
        given().
                accept("application/json").
                body("{ \"message\" : \"hello world\"}").
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));
    }

    @Test public void
    accept_method_with_content_type_parameter_is_just_an_alias_for_header_accept() {
        given().
                accept(JSON).
                body("{ \"message\" : \"hello world\"}").
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));
    }

    @Test public void
    accept_method_from_spec_is_set_to_request_when_specified_as_content_type() {
        RequestSpecification spec = new RequestSpecBuilder().setAccept(JSON).build();

        given().
                spec(spec).
                body("{ \"message\" : \"hello world\"}").
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));
    }

    @Test public void
    accept_method_from_spec_is_set_to_request_when_specified_as_string() {
        RequestSpecification spec = new RequestSpecBuilder().setAccept("application/json").build();

        given().
                spec(spec).
                body("{ \"message\" : \"hello world\"}").
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));
    }


    @Test public void
    accept_headers_are_merged_from_request_spec_and_request() {
        RequestSpecification spec = new RequestSpecBuilder().setAccept("text/jux").build();

        final MutableObject<List<String>> headers = new MutableObject<List<String>>();

        given().
                accept(JSON).
                spec(spec).
                body("{ \"message\" : \"hello world\"}").
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        headers.setValue(requestSpec.getHeaders().getValues("Accept"));
                        return ctx.next(requestSpec, responseSpec);
                    }
                }).
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));

        assertThat(headers.getValue(), contains("application/json, application/javascript, text/javascript", "text/jux"));
    }
}

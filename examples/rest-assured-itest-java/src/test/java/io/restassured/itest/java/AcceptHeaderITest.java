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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HeaderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class AcceptHeaderITest extends WithJetty {

    @Test public void
    accept_method_with_string_parameter_is_just_an_alias_for_header_accept() {
        RestAssured.given().
                accept("application/json").
                body("{ \"message\" : \"hello world\"}").
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));
    }

    @Test public void
    accept_method_with_content_type_parameter_is_just_an_alias_for_header_accept() {
        RestAssured.given().
                accept(ContentType.JSON).
                body("{ \"message\" : \"hello world\"}").
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));
    }

    @Test public void
    accept_method_from_spec_is_set_to_request_when_specified_as_content_type() {
        RequestSpecification spec = new RequestSpecBuilder().setAccept(ContentType.JSON).build();

        RestAssured.given().
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

        RestAssured.given().
                spec(spec).
                body("{ \"message\" : \"hello world\"}").
        when().
                post("/jsonBodyAcceptHeader").
        then().
                body(equalTo("hello world"));
    }

    @Test public void
    accept_headers_are_overwritten_from_request_spec_by_default() {
        RequestSpecification spec = new RequestSpecBuilder().setAccept(ContentType.JSON).build();

        final MutableObject<List<String>> headers = new MutableObject<List<String>>();

        RestAssured.given().
                accept("text/jux").
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

        assertThat(headers.getValue(), contains("application/json, application/javascript, text/javascript, text/json"));
    }

    @Test public void
    accept_headers_are_merged_from_request_spec_and_request_when_configured_to() {
        RequestSpecification spec = new RequestSpecBuilder().setAccept("text/jux").build();

        final MutableObject<List<String>> headers = new MutableObject<List<String>>();

        RestAssured.given().
                config(RestAssuredConfig.config().headerConfig(HeaderConfig.headerConfig().mergeHeadersWithName("Accept"))).
                accept(ContentType.JSON).
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

        assertThat(headers.getValue(), contains("application/json, application/javascript, text/javascript, text/json", "text/jux"));
    }
}

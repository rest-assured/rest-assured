/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal;

import io.restassured.RestAssured;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.specification.SpecificationQuerier.query;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * Sending and validating work on copies of the request and response specification, so neither changes (#945).
 */
class CopyOnSendTest {
    private final List<String> sent = new ArrayList<>();

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    @Test
    void sending_doesnt_change_the_request_specification() {
        RestAssured.authentication = RestAssured.basic("user", "password");
        Filter editing = (req, res, ctx) -> {
            req.header("X-Filter", "1").cookie("filter", "1").queryParam("filter", "1");
            return ctx.next(req, res);
        };
        Filter respond = respond();
        RequestSpecification spec = given().formParam("f", "1").filter(editing).filter(respond);

        spec.put("/x/{id}?a=1", 1);

        QueryableRequestSpecification queryable = query(spec);
        assertThat(queryable.getDefinedFilters()).containsExactly(editing, respond);
        assertThat(queryable.getMethod()).isNull();
        assertThat(queryable.getUserDefinedPath()).isEmpty();
        assertThat(queryable.getUnnamedPathParams()).isEmpty();
        assertThat(queryable.getQueryParams()).isEmpty();
        assertThat(queryable.getFormParams()).containsOnlyKeys("f");
        assertThat(queryable.getHeaders().exist()).isFalse();
        assertThat(queryable.getCookies().exist()).isFalse();
        assertThat((Object) queryable.getBody()).isNull();
        assertThat(queryable.getAuthenticationScheme()).isInstanceOf(NoAuthScheme.class);
        assertThat(queryable.getHttpClient()).isNull();
    }

    @Test
    void sending_with_named_path_params_doesnt_change_the_request_specification() {
        RequestSpecification spec = given().filter(record());

        spec.get("/x/{id}", Collections.singletonMap("id", 1));

        assertThat(sent).containsExactly("GET http://localhost:8080/x/1");
        assertThat(query(spec).getPathParams()).isEmpty();
    }

    @Test
    void a_specification_sent_twice_sends_the_same_request() {
        RequestSpecification spec = given().filter((req, res, ctx) -> {
            req.cookie("filter", "1");
            return ctx.next(req, res);
        }).filter(record());

        spec.get("/x?a=1");
        spec.get("/x?a=1");

        assertThat(sent).containsExactly("GET http://localhost:8080/x?a=1 [filter=1]", "GET http://localhost:8080/x?a=1 [filter=1]");
    }

    @Test
    void merging_a_specification_doesnt_change_its_parameters() {
        RequestSpecification built = new RequestSpecBuilder().addQueryParam("q", "1", "2").addFormParam("f", "1", "2").addParam("p", "1", "2").build();

        given().spec(built).queryParam("q", "3").formParam("f", "3").param("p", "3");

        assertParametersOf(built);
    }

    @Test
    void given_doesnt_change_the_parameters_of_the_static_request_specification() {
        RestAssured.requestSpecification = new RequestSpecBuilder().addQueryParam("q", "1", "2").addFormParam("f", "1", "2").addParam("p", "1", "2").build();

        given().queryParam("q", "3").formParam("f", "3").param("p", "3");

        assertParametersOf(RestAssured.requestSpecification);
    }

    @Test
    void one_response_specification_can_validate_responses_from_many_threads() throws Exception {
        ResponseSpecification specification = new ResponseSpecBuilder().expectBody("n", lessThan(1000)).build();
        List<String> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4000; i++) {
                int n = i % 2000;
                futures.add(executor.submit(() -> {
                    Response response = new ResponseBuilder().setStatusCode(200).setContentType("application/json")
                            .setBody("{\"n\":" + n + ",\"padding\":\"" + "x".repeat(n) + "\"}").build();
                    boolean passed;
                    try {
                        specification.validate(response);
                        passed = true;
                    } catch (AssertionError e) {
                        passed = false;
                    }
                    if (passed != n < 1000) {
                        failures.add(n + (passed ? " passed" : " failed"));
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(failures).isEmpty();
    }

    private static void assertParametersOf(RequestSpecification specification) {
        QueryableRequestSpecification queryable = query(specification);
        assertThat((Object) queryable.getQueryParams().get("q")).isEqualTo(asList("1", "2"));
        assertThat((Object) queryable.getFormParams().get("f")).isEqualTo(asList("1", "2"));
        assertThat((Object) queryable.getRequestParams().get("p")).isEqualTo(asList("1", "2"));
    }

    // Records the method, URI and cookies, and answers the request without sending it
    private Filter record() {
        return (req, res, ctx) -> {
            sent.add(req.getMethod() + " " + req.getURI() + (req.getCookies().exist() ? " " + req.getCookies().asList() : ""));
            return response();
        };
    }

    private static Filter respond() {
        return (req, res, ctx) -> response();
    }

    private static Response response() {
        return new ResponseBuilder().setStatusCode(200).setBody("").build();
    }
}

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

package io.restassured.itest.java;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Sending works on copies of the request and response specification, so one specification can be sent many times and
 * from many threads (#945).
 */
public class CopyOnSendITest extends WithJetty {
    private static final int THREADS = 8;
    private static final int REQUESTS = 400;

    @Test
    void a_filter_added_after_the_specification_was_sent_with_a_response_specification_runs() {
        RequestSpecification built = new RequestSpecBuilder().build();
        given(built, new ResponseSpecBuilder().expectStatusCode(200).build()).get("/greetJSON?firstName=John&lastName=Doe");
        AtomicBoolean filterRan = new AtomicBoolean();

        given().spec(built).filter((req, res, ctx) -> {
            filterRan.set(true);
            return ctx.next(req, res);
        }).get("/greetJSON");

        assertThat(filterRan).isTrue();
    }

    @Test
    void a_parser_registered_on_a_response_doesnt_reach_the_specification_it_was_sent_with() {
        RequestSpecification specification = given();
        specification.get("/contentTypeHtmlButContentIsJson").then().parser("text/html", Parser.JSON).body("42", equalTo("23"));

        assertThatThrownBy(() -> specification.expect().body("42", equalTo("23")).when().get("/contentTypeHtmlButContentIsJson"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void one_request_specification_can_be_sent_from_many_threads() throws Exception {
        RequestSpecification specification = given();

        assertEachResponseIsItsOwn(i -> specification.get("/{firstName}/{lastName}", "first" + i, "last" + i));
    }

    @Test
    void one_built_request_and_response_specification_can_be_sent_from_many_threads() throws Exception {
        RequestSpecification request = new RequestSpecBuilder().build();
        ResponseSpecification response = new ResponseSpecBuilder().expectStatusCode(200).build();

        assertEachResponseIsItsOwn(i -> given(request, response).get("/{firstName}/{lastName}", "first" + i, "last" + i));
    }

    private static void assertEachResponseIsItsOwn(IntFunction<Response> send) throws Exception {
        List<String> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                int request = i;
                futures.add(executor.submit(() -> {
                    try {
                        String fullName = send.apply(request).path("fullName");
                        if (!("first" + request + " last" + request).equals(fullName)) {
                            failures.add(request + ": got " + fullName);
                        }
                    } catch (Throwable e) {
                        failures.add(request + ": " + e);
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
}

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

package io.restassured.module.mockmvc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RestAssuredMockMvcClient}s over different controllers used from many threads at once, named with
 * <code>given(client)</code> or bound to the threads with <code>useClient(..)</code>, while the static settings have
 * another controller (#534).
 */
class RestAssuredMockMvcClientConcurrencyTest {
    private static final int THREADS = 8;
    private static final int REQUESTS = 400;

    @AfterEach
    void reset() {
        RestAssuredMockMvc.reset();
    }

    @Test
    void each_request_reaches_the_controller_of_its_client() throws Exception {
        RestAssuredMockMvcClient one = RestAssuredMockMvcClient.builder().standaloneSetup(new NamedController("one")).build();
        RestAssuredMockMvcClient two = RestAssuredMockMvcClient.builder().standaloneSetup(new NamedController("two")).build();
        RestAssuredMockMvc.standaloneSetup(new NamedController("static"));

        assertEachRequest(i -> {
            String expected;
            String answer;
            if (i % 3 == 0) {
                expected = "one";
                answer = given(one).queryParam("n", i).get("/name").asString();
            } else if (i % 3 == 1) {
                expected = "two";
                RestAssuredMockMvc.useClient(two);
                try {
                    answer = given().queryParam("n", i).get("/name").asString();
                } finally {
                    RestAssuredMockMvc.resetClient();
                }
            } else {
                expected = "static";
                answer = given().queryParam("n", i).get("/name").asString();
            }
            return answer.equals(expected + " " + i) ? null : "expected the controller " + expected + " but got " + answer;
        });
    }

    // Sends the requests from many threads. The function returns null, or what went wrong.
    private static void assertEachRequest(IntFunction<String> sendAndCheck) throws Exception {
        List<String> failures = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                int request = i;
                futures.add(executor.submit(() -> {
                    try {
                        String failure = sendAndCheck.apply(request);
                        if (failure != null) {
                            failures.add(request + ": " + failure);
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

    @RestController
    static class NamedController {
        private final String name;

        NamedController(String name) {
            this.name = name;
        }

        @GetMapping("/name")
        public String name(@RequestParam("n") int n) {
            return name + " " + n;
        }
    }
}

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

package io.restassured;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress tests for the static defaults of {@link RestAssured} (issue #1967): creating request specifications from many
 * threads while other threads change the default filters or parsers must never fail or see a half-modified default.
 * The tests run a fixed number of iterations and assert no timing, so they pass deterministically once the races are
 * gone. Before the fix they failed with ConcurrentModificationException, ArrayIndexOutOfBoundsException or null and
 * partially applied filters.
 */
class RestAssuredConcurrencyTest {

    private static final int READERS = 4;
    private static final int ITERATIONS_PER_READER = 20_000;

    @AfterEach
    void reset() {
        RestAssured.reset();
    }

    @Test
    @Timeout(30)
    void given_never_sees_a_half_modified_default_filter_list_while_filters_are_changed() throws Exception {
        Filter a = new NamedFilter("a");
        Filter b = new NamedFilter("b");
        Filter c = new NamedFilter("c");
        List<Filter> onlyA = Collections.singletonList(a);
        List<Filter> all = Arrays.asList(a, b, c);
        RestAssured.filters(a);

        // One writer, so that the default filters are always either [a] or [a, b, c] between two writes
        List<Throwable> failures = runConcurrently(1,
                () -> {
                    RestAssured.replaceFiltersWith(a);
                    RestAssured.filters(b, c);
                },
                () -> {
                    List<Filter> copied = ((FilterableRequestSpecification) given()).getDefinedFilters();
                    if (!copied.equals(onlyA) && !copied.equals(all)) {
                        throw new AssertionError("given() copied an inconsistent default filter list: " + copied);
                    }
                });

        assertThat(failures).isEmpty();
    }

    @Test
    @Timeout(30)
    void given_and_expect_never_fail_while_parsers_are_registered() throws Exception {
        RestAssured.registerParser("application/stable", Parser.XML);

        AtomicInteger counter = new AtomicInteger();

        List<Throwable> failures = runConcurrently(2,
                () -> {
                    String contentType = "application/vnd.test" + (counter.incrementAndGet() % 64);
                    RestAssured.registerParser(contentType, Parser.JSON);
                    RestAssured.unregisterParser(contentType);
                },
                () -> {
                    ResponseSpecificationImpl responseSpecification = (ResponseSpecificationImpl) expect();
                    Parser parser = responseSpecification.getRpr().getParser("application/stable");
                    if (parser != Parser.XML) {
                        throw new AssertionError("Registered parser was lost: " + parser);
                    }
                    given();
                    new ResponseSpecBuilder().build();
                });

        assertThat(failures).isEmpty();
    }

    @Test
    void given_does_not_reassign_the_static_config_when_a_session_id_is_set() {
        RestAssuredConfig config = RestAssuredConfig.config();
        RestAssured.config = config;
        RestAssured.sessionId = "1234";

        RequestSpecificationImpl requestSpecification = (RequestSpecificationImpl) given();

        assertThat(RestAssured.config).isSameAs(config);
        assertThat(config.getSessionConfig().isSessionIdValueDefined()).isFalse();
        assertThat(requestSpecification.getConfig().getSessionConfig().sessionIdValue()).isEqualTo("1234");
    }

    @Test
    void given_does_not_reassign_the_static_config_when_it_is_null_and_a_session_id_is_set() {
        RestAssured.config = null;
        RestAssured.sessionId = "1234";

        RequestSpecificationImpl requestSpecification = (RequestSpecificationImpl) given();

        assertThat(RestAssured.config).isNull();
        assertThat(requestSpecification.getConfig().getSessionConfig().sessionIdValue()).isEqualTo("1234");
    }

    @Test
    void given_does_not_store_the_default_parser_in_the_static_parser_registry() {
        RestAssured.defaultParser = Parser.JSON;
        assertThat(((ResponseSpecificationImpl) expect()).getRpr().getDefaultParser()).isEqualTo(Parser.JSON);

        RestAssured.defaultParser = null;

        assertThat(((ResponseSpecificationImpl) expect()).getRpr().getDefaultParser()).isNull();
        assertThat(((ResponseSpecificationImpl) new ResponseSpecBuilder().build()).getRpr().getDefaultParser()).isNull();
    }

    @Test
    void response_spec_builder_uses_the_default_parser() {
        RestAssured.defaultParser = Parser.JSON;

        assertThat(((ResponseSpecificationImpl) new ResponseSpecBuilder().build()).getRpr().getDefaultParser()).isEqualTo(Parser.JSON);
    }

    @Test
    void filters_returns_a_read_only_view_of_the_default_filters() {
        Filter a = new NamedFilter("a");
        Filter b = new NamedFilter("b");
        List<Filter> view = RestAssured.filters();

        RestAssured.filters(a);
        RestAssured.filters(Collections.singletonList(b));

        assertThat(view).containsExactly(a, b);
        assertThat(RestAssured.filters()).containsExactly(a, b);
        RestAssured.replaceFiltersWith(b);
        assertThat(RestAssured.filters()).containsExactly(b);
        assertThat(catchUnsupported(() -> RestAssured.filters().add(a))).isTrue();
    }

    private static boolean catchUnsupported(Runnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    /**
     * Runs <code>numberOfWriters</code> writer threads in a loop until {@value #READERS} reader threads have each run
     * {@value #ITERATIONS_PER_READER} iterations, and returns everything that was thrown.
     */
    private static List<Throwable> runConcurrently(int numberOfWriters, Runnable writer, Runnable reader) throws InterruptedException {
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean done = new AtomicBoolean();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> writers = new ArrayList<>();
        List<Thread> readers = new ArrayList<>();
        for (int i = 0; i < numberOfWriters; i++) {
            writers.add(new Thread(() -> {
                await(start);
                while (!done.get() && failures.isEmpty()) {
                    try {
                        writer.run();
                    } catch (Throwable t) {
                        failures.add(t);
                    }
                }
            }));
        }
        for (int i = 0; i < READERS; i++) {
            readers.add(new Thread(() -> {
                await(start);
                for (int j = 0; j < ITERATIONS_PER_READER && failures.isEmpty(); j++) {
                    try {
                        reader.run();
                    } catch (Throwable t) {
                        failures.add(t);
                    }
                }
            }));
        }
        writers.forEach(Thread::start);
        readers.forEach(Thread::start);
        start.countDown();
        for (Thread thread : readers) {
            thread.join();
        }
        done.set(true);
        for (Thread thread : writers) {
            thread.join();
        }
        return failures;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class NamedFilter implements Filter {
        private final String name;

        NamedFilter(String name) {
            this.name = name;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                               FilterContext ctx) {
            return ctx.next(requestSpec, responseSpec);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

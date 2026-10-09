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

import io.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress test for the static defaults of {@link RestAssuredMockMvc} (issue #1967): creating request specifications
 * from many threads while another thread changes the default result handlers and request post processors must never
 * fail or see a half-modified list. It runs a fixed number of iterations and asserts no timing.
 */
class RestAssuredMockMvcConcurrencyTest {

    private static final int READERS = 4;
    private static final int ITERATIONS_PER_READER = 20_000;

    @AfterEach
    void reset() {
        RestAssuredMockMvc.reset();
    }

    @Test
    @Timeout(30)
    void given_never_sees_half_modified_default_result_handlers_or_post_processors() throws Exception {
        ResultHandler handler1 = result -> {
        };
        ResultHandler handler2 = result -> {
        };
        RequestPostProcessor processor1 = request -> request;
        RequestPostProcessor processor2 = request -> request;
        List<ResultHandler> handlers = Arrays.asList(handler1, handler2);
        List<RequestPostProcessor> processors = Arrays.asList(processor1, processor2);

        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean done = new AtomicBoolean();
        CountDownLatch start = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            await(start);
            while (!done.get() && failures.isEmpty()) {
                try {
                    RestAssuredMockMvc.reset();
                    RestAssuredMockMvc.resultHandlers(handler1, handler2);
                    RestAssuredMockMvc.postProcessors(processor1, processor2);
                } catch (Throwable t) {
                    failures.add(t);
                }
            }
        });
        List<Thread> readers = new ArrayList<>();
        for (int i = 0; i < READERS; i++) {
            readers.add(new Thread(() -> {
                await(start);
                for (int j = 0; j < ITERATIONS_PER_READER && failures.isEmpty(); j++) {
                    try {
                        MockMvcRequestSpecificationImpl spec = (MockMvcRequestSpecificationImpl) RestAssuredMockMvc.given();
                        List<ResultHandler> copiedHandlers = spec.getResultHandlers();
                        List<RequestPostProcessor> copiedProcessors = spec.getRequestPostProcessors();
                        if (!copiedHandlers.isEmpty() && !copiedHandlers.equals(handlers)) {
                            throw new AssertionError("given() copied inconsistent result handlers: " + copiedHandlers);
                        }
                        if (!copiedProcessors.isEmpty() && !copiedProcessors.equals(processors)) {
                            throw new AssertionError("given() copied inconsistent post processors: " + copiedProcessors);
                        }
                    } catch (Throwable t) {
                        failures.add(t);
                    }
                }
            }));
        }
        writer.start();
        readers.forEach(Thread::start);
        start.countDown();
        for (Thread reader : readers) {
            reader.join();
        }
        done.set(true);
        writer.join();

        assertThat(failures).isEmpty();
    }

    @Test
    void result_handlers_and_post_processors_return_read_only_views() {
        ResultHandler handler = result -> {
        };
        RequestPostProcessor processor = request -> request;
        List<ResultHandler> handlersView = RestAssuredMockMvc.resultHandlers();
        List<RequestPostProcessor> processorsView = RestAssuredMockMvc.postProcessors();

        RestAssuredMockMvc.resultHandlers(handler);
        RestAssuredMockMvc.postProcessors(processor);

        assertThat(handlersView).containsExactly(handler);
        assertThat(processorsView).containsExactly(processor);
        assertThat(isUnsupported(() -> handlersView.add(handler))).isTrue();
        assertThat(isUnsupported(() -> processorsView.add(processor))).isTrue();
    }

    private static boolean isUnsupported(Runnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

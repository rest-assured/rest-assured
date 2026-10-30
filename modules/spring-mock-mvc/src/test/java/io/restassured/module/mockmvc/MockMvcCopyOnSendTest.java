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

import io.restassured.config.LogConfig;
import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import io.restassured.module.mockmvc.specification.MockMvcRequestAsyncSender;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.equalTo;

/**
 * Sending a request specification doesn't change it, so it can be sent again, and from many threads.
 */
class MockMvcCopyOnSendTest {
    private final StringWriter writer = new StringWriter();
    private final PrintStream captor = new PrintStream(new WriterOutputStream(writer, Charset.defaultCharset()), true);

    @AfterEach
    void reset() {
        RestAssuredMockMvc.reset();
    }

    @Test
    void a_specification_sent_twice_logs_only_its_own_request_when_validation_fails() {
        MockMvcRequestSpecification specification = failingSpecificationThatLogsIfValidationFails();

        catchThrowable(() -> specification.get("/greeting").then().body("content", equalTo("Hello, Ringo!")));
        catchThrowable(() -> specification.get("/greeting").then().body("content", equalTo("Hello, Ringo!")));

        assertThat(StringUtils.countMatches(writer.toString(), "Request method:")).isEqualTo(2);
    }

    @Test
    void a_specification_sent_from_many_threads_logs_only_the_request_of_each_send_when_validation_fails() throws Exception {
        MockMvcRequestSpecification specification = failingSpecificationThatLogsIfValidationFails();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Throwable>> sends = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                sends.add(executor.submit(() -> catchThrowable(() -> specification.get("/greeting").then().body("content", equalTo("Hello, Ringo!")))));
            }
            for (Future<Throwable> send : sends) {
                assertThat(send.get()).isInstanceOf(AssertionError.class);
            }
        } finally {
            executor.shutdown();
        }

        assertThat(StringUtils.countMatches(writer.toString(), "Request method:")).isEqualTo(20);
    }

    @Test
    void sending_a_specification_doesnt_change_it() {
        MockMvcRequestSpecificationImpl specification = (MockMvcRequestSpecificationImpl) failingSpecificationThatLogsIfValidationFails().queryParam("a", "1");
        MockMvcRequestAsyncSender sender = specification.when();
        specification.queryParam("a", "2").header("X", "1");

        sender.get("/greeting");

        assertThat(specification.getRequestLoggingFilter()).isNull();
        assertThat(specification.getLogRepository().getRequestLog()).isEmpty();
        assertThat(writer.toString()).isEmpty();
        assertThat(specification.getQueryParams()).containsEntry("a", List.of("1", "2"));
        assertThat(sender.get("/greeting").mvcResult().getRequest().getParameterValues("a")).containsExactly("1");
    }

    private MockMvcRequestSpecification failingSpecificationThatLogsIfValidationFails() {
        return given().
                standaloneSetup(new GreetingController()).
                config(config().logConfig(new LogConfig(captor, true).enableLoggingOfRequestAndResponseIfValidationFails())).
                param("name", "John");
    }
}

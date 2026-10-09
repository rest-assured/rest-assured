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

import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.log.LogRepository;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LogRequestAndResponseOnFailListenerTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final RestAssuredConfig config = RestAssuredConfig.config().logConfig(LogConfig.logConfig().defaultStream(new PrintStream(output, true)));
    private final LogRepository logRepository = new LogRepository();
    private final LogRequestAndResponseOnFailListener listener = new LogRequestAndResponseOnFailListener();

    @Test
    void prints_the_request_log_and_the_response_log_separated_by_a_line_separator() {
        logRepository.registerRequestLog(stream("request"));
        logRepository.registerResponseLog(stream("response"));

        listener.onFailure(given(), responseSpecification(logRepository), null);

        assertThat(output()).isEqualTo("request" + SystemUtils.LINE_SEPARATOR + "response");
    }

    @Test
    void prints_only_the_request_log_when_there_is_no_response_log() {
        logRepository.registerRequestLog(stream("request"));
        logRepository.registerResponseLog(stream(""));

        listener.onFailure(given(), responseSpecification(logRepository), null);

        assertThat(output()).isEqualTo("request");
    }

    @Test
    void prints_only_the_response_log_when_there_is_no_request_log() {
        logRepository.registerResponseLog(stream("response"));

        listener.onFailure(given(), responseSpecification(logRepository), null);

        assertThat(output()).isEqualTo("response");
    }

    @Test
    void prints_nothing_without_logs_or_log_repository() {
        listener.onFailure(given(), responseSpecification(logRepository), null);
        listener.onFailure(given(), responseSpecification(null), null);

        assertThat(output()).isEmpty();
    }

    /**
     * The Groovy listener guarded with {@code !responseSpecification instanceof ResponseSpecificationImpl}, which Groovy
     * parses as {@code (!responseSpecification) instanceof ResponseSpecificationImpl} and which is never true. So any other
     * response specification reaches the cast and fails.
     */
    @Test
    void fails_for_a_response_specification_that_is_not_a_response_specification_impl() {
        ResponseSpecification other = mock(ResponseSpecification.class);

        assertThatThrownBy(() -> listener.onFailure(given(), other, null)).isInstanceOf(ClassCastException.class);
        assertThatThrownBy(() -> listener.onFailure(given(), null, null)).isInstanceOf(NullPointerException.class);
    }

    private ResponseSpecificationImpl responseSpecification(LogRepository logRepository) {
        return new ResponseSpecificationImpl("", null, new ResponseParserRegistrar(), config, logRepository);
    }

    private static ByteArrayOutputStream stream(String content) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.writeBytes(content.getBytes(StandardCharsets.UTF_8));
        return stream;
    }

    private String output() {
        return output.toString(StandardCharsets.UTF_8);
    }
}

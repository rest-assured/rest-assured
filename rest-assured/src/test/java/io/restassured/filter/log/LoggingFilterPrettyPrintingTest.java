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

package io.restassured.filter.log;

import io.restassured.RestAssured;
import io.restassured.RestAssuredClient;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.internal.http.client.FakeHttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A logging filter created without a pretty-print argument pretty prints as the log config of the request says when it
 * logs, not as {@link RestAssured#config} said when it was created.
 */
class LoggingFilterPrettyPrintingTest {
    private static final RestAssuredConfig FAKE = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class));
    private static final RestAssuredConfig NOT_PRETTY = FAKE.logConfig(logConfig().enablePrettyPrinting(false));
    private static final String PRETTY = "\"a\": 1";
    private static final String RAW = "{\"a\":1}";

    private final ByteArrayOutputStream log = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(log, true);

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
        RestAssured.reset();
    }

    @Test
    void response_and_error_logging_filters_follow_the_log_config_of_the_request() {
        assertThat(logOf(NOT_PRETTY, new ResponseLoggingFilter(stream), 200)).contains(RAW);
        assertThat(logOf(NOT_PRETTY, new ErrorLoggingFilter(stream), 400)).contains(RAW);
        assertThat(logOf(FAKE, new ResponseLoggingFilter(stream), 200)).contains(PRETTY);
        assertThat(logOf(FAKE, new ErrorLoggingFilter(stream), 400)).contains(PRETTY);
    }

    @Test
    void request_logging_filter_follows_the_log_config_of_the_request() {
        assertThat(logOf(NOT_PRETTY, new RequestLoggingFilter(stream), 200)).contains(RAW);
        assertThat(logOf(NOT_PRETTY, new RequestLoggingFilter(LogDetail.BODY, stream), 200)).contains(RAW);
        assertThat(logOf(FAKE, new RequestLoggingFilter(stream), 200)).contains(PRETTY);
    }

    @Test
    void a_pretty_print_argument_wins_over_the_log_config_of_the_request() {
        assertThat(logOf(NOT_PRETTY, new ResponseLoggingFilter(LogDetail.BODY, true, stream), 200)).contains(PRETTY);
        assertThat(logOf(NOT_PRETTY, new RequestLoggingFilter(LogDetail.BODY, true, stream), 200)).contains(PRETTY);
        assertThat(logOf(FAKE, new ResponseLoggingFilter(LogDetail.BODY, false, stream), 200)).contains(RAW);
    }

    @Test
    void the_static_config_at_the_time_the_filter_was_created_doesnt_matter() {
        RestAssured.config = NOT_PRETTY;
        Filter filter = new ResponseLoggingFilter(stream);
        RestAssured.reset();

        assertThat(logOf(FAKE, filter, 200)).contains(PRETTY);
    }

    @Test
    void a_filter_of_a_client_follows_the_log_config_of_the_client() {
        RestAssuredClient client = RestAssuredClient.builder().config(NOT_PRETTY).filters(new ResponseLoggingFilter(stream)).build();
        FakeHttpClients.response = request -> json(200);

        given(client).get("http://localhost/path");

        assertThat(log.toString(UTF_8)).contains(RAW);
    }

    private String logOf(RestAssuredConfig config, Filter filter, int statusCode) {
        log.reset();
        FakeHttpClients.response = request -> json(statusCode);
        given().config(config).filter(filter).contentType(ContentType.JSON).body(RAW).post("http://localhost/path");
        return log.toString(UTF_8);
    }

    private static HttpResponse json(int statusCode) {
        byte[] bytes = RAW.getBytes(UTF_8);
        return new HttpResponse("HTTP/1.1", statusCode, "", List.of(new HttpHeader("Content-Type", "application/json")), bytes.length,
                new ByteArrayInputStream(bytes), null);
    }
}

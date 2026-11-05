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
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.internal.http.client.FakeHttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.any;

/**
 * Every response log hides the headers blacklisted in the log config of the request, not only <code>then().log()</code>.
 */
class ResponseLogBlacklistedHeadersTest {
    private static final String URL = "http://localhost/path";

    private final ByteArrayOutputStream log = new ByteArrayOutputStream();

    @BeforeEach
    void blacklistSecretAndAnswerWithIt() {
        RestAssured.config = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class))
                .logConfig(logConfig().defaultStream(new PrintStream(log, true)).blacklistHeader("X-Secret"));
        answerWith(200, "X-Secret");
    }

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
        RestAssured.reset();
    }

    @Test
    void then_log() {
        given().get(URL).then().log().headers();

        assertSecretHidden();
    }

    @Test
    void expect_log() {
        expect().log().headers().when().get(URL);

        assertSecretHidden();
    }

    @Test
    void log_of_a_response_specification_builder() {
        expect().spec(new ResponseSpecBuilder().log(LogDetail.HEADERS).build()).when().get(URL);

        assertSecretHidden();
    }

    @Test
    void logging_if_validation_fails_with_then() {
        RestAssured.config = RestAssured.config.logConfig(RestAssured.config.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails());

        assertThatThrownBy(() -> given().get(URL).then().statusCode(201)).isInstanceOf(AssertionError.class);

        assertSecretHidden();
    }

    @Test
    void logging_if_validation_fails_with_expect() {
        RestAssured.config = RestAssured.config.logConfig(RestAssured.config.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails());

        assertThatThrownBy(() -> expect().statusCode(201).when().get(URL)).isInstanceOf(AssertionError.class);

        assertSecretHidden();
    }

    @Test
    void response_and_error_logging_filters_take_the_blacklist_from_the_log_config() {
        answerWith(404, "X-Secret");
        PrintStream stream = new PrintStream(log, true);

        given().filter(new ResponseLoggingFilter(stream)).filter(new ErrorLoggingFilter(stream)).get(URL);

        assertThat(logged()).contains("X-Secret: [ BLACKLISTED ]").doesNotContain("s3cr3t");
    }

    @Test
    void blacklisted_headers_given_to_the_filter_are_matched_ignoring_case() {
        answerWith(200, "x-secret");

        given().filter(new StatusCodeBasedLoggingFilter(LogDetail.HEADERS, false, new PrintStream(log, true), any(Integer.class), Set.of("X-Secret")))
                .get(URL);

        assertThat(logged()).contains("x-secret: [ BLACKLISTED ]").doesNotContain("s3cr3t");
    }

    private static void answerWith(int statusCode, String secretHeaderName) {
        byte[] body = "{\"a\":1}".getBytes(UTF_8);
        FakeHttpClients.response = request -> new HttpResponse("HTTP/1.1", statusCode, "Status", List.of(
                new HttpHeader("Content-Type", "application/json"), new HttpHeader(secretHeaderName, "s3cr3t")), body.length,
                new ByteArrayInputStream(body), null);
    }

    private void assertSecretHidden() {
        assertThat(logged()).contains("X-Secret: [ BLACKLISTED ]").doesNotContain("s3cr3t");
    }

    private String logged() {
        return log.toString(UTF_8);
    }
}

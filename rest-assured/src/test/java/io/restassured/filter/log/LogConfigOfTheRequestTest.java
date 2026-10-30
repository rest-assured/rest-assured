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
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.http.client.FakeHttpClients;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The filter of <code>log()</code>, and of <code>log(..)</code> on a {@link RequestSpecBuilder}, takes the print stream,
 * the blacklisted headers and the URL encoding of the request URI from the log config of the request when it logs.
 */
class LogConfigOfTheRequestTest {
    private static final RestAssuredConfig FAKE = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class));
    private static final String URL = "http://localhost/path";

    private final ByteArrayOutputStream log = new ByteArrayOutputStream();
    private final LogConfig blacklistingSecret = logConfig().defaultStream(new PrintStream(log, true)).blacklistHeader("Secret");

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
        RestAssured.reset();
    }

    @Test
    void a_built_specification_logs_with_the_static_log_config() {
        RestAssured.config = FAKE.logConfig(blacklistingSecret);
        RequestSpecification specification = new RequestSpecBuilder().log(LogDetail.HEADERS).build();

        given().spec(specification).header("Secret", "password").get(URL);

        assertThat(logged()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void a_built_specification_logs_with_the_log_config_of_the_client() {
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE.logConfig(blacklistingSecret)).build();
        RequestSpecification specification = new RequestSpecBuilder().log(LogDetail.HEADERS).build();

        given(client).spec(specification).header("Secret", "password").get(URL);

        assertThat(logged()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void log_takes_a_log_config_set_after_it() {
        given().log().headers().config(FAKE.logConfig(blacklistingSecret)).header("Secret", "password").get(URL);

        assertThat(logged()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void log_takes_the_url_encoding_of_a_log_config_set_after_it() {
        given().log().uri().config(FAKE.logConfig(blacklistingSecret.urlEncodeRequestUri(false))).queryParam("q", "a b").get(URL);

        assertThat(logged()).contains("q=a b");
    }

    @Test
    void a_stream_or_pretty_printing_given_by_the_user_wins_over_the_log_config() {
        ByteArrayOutputStream own = new ByteArrayOutputStream();

        given().config(FAKE.logConfig(blacklistingSecret)).filter(new RequestLoggingFilter(LogDetail.HEADERS, new PrintStream(own, true)))
                .header("Secret", "password").get(URL);
        given().config(FAKE.logConfig(blacklistingSecret)).log().body(false).contentType("application/json").body("{\"a\":1}").post(URL);

        assertThat(own.toString(UTF_8)).contains("Secret=password");
        assertThat(logged()).contains("{\"a\":1}");
    }

    private String logged() {
        return log.toString(UTF_8);
    }
}

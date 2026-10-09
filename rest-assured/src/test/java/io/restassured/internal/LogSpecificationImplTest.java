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

import io.restassured.authentication.NoAuthScheme;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;

import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogSpecificationImplTest {

    private final LogSpecificationImpl logSpecification = new LogSpecificationImpl();

    @Test
    void the_log_config_of_the_request_specification_is_used() {
        PrintStream stream = new PrintStream(new ByteArrayOutputStream());
        RequestSpecificationImpl spec = spec(config().logConfig(logConfig().defaultStream(stream).urlEncodeRequestUri(false).enablePrettyPrinting(false)));

        assertThat(logSpecification.getPrintStream(spec)).isSameAs(stream);
        assertThat(logSpecification.shouldUrlEncodeRequestUri(spec)).isFalse();
        assertThat(logSpecification.shouldPrettyPrint(spec)).isFalse();
    }

    @Test
    void the_default_log_config() {
        RequestSpecificationImpl spec = spec(new RestAssuredConfig());

        assertThat(logSpecification.getPrintStream(spec)).isSameAs(System.out);
        assertThat(logSpecification.shouldUrlEncodeRequestUri(spec)).isTrue();
        assertThat(logSpecification.shouldPrettyPrint(spec)).isTrue();
    }

    @Test
    void without_config_the_stream_is_system_out_the_uri_is_not_url_encoded_and_pretty_printing_is_enabled() {
        RequestSpecificationImpl spec = spec(null);

        assertThat(logSpecification.getPrintStream(spec)).isSameAs(System.out);
        assertThat(logSpecification.shouldUrlEncodeRequestUri(spec)).isFalse();
        assertThat(logSpecification.shouldPrettyPrint(spec)).isTrue();
    }

    @Test
    void a_request_specification_is_required() {
        String message = "Cannot configure logging since request specification is not defined. You may be misusing the API.";
        assertThatThrownBy(() -> logSpecification.getPrintStream(null)).isInstanceOf(IllegalStateException.class).hasMessage(message);
        assertThatThrownBy(() -> logSpecification.shouldUrlEncodeRequestUri(null)).isInstanceOf(IllegalStateException.class).hasMessage(message);
        assertThatThrownBy(() -> logSpecification.shouldPrettyPrint(null)).isInstanceOf(IllegalStateException.class).hasMessage(message);
    }

    private static RequestSpecificationImpl spec(RestAssuredConfig config) {
        return new RequestSpecificationImpl("http://localhost", 8080, "", new NoAuthScheme(), Collections.emptyList(), null, true,
                config, null, null, true, true);
    }
}

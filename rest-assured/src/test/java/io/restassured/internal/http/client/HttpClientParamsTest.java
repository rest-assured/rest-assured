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

package io.restassured.internal.http.client;

import io.restassured.config.HttpClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpClientParamsTest {

    @Test
    void the_default_config_has_no_user_agent_and_no_parameter_that_fails() {
        assertThat(HttpClientParams.validate(new HttpClientConfig())).isNull();
    }

    @Test
    void a_user_agent_parameter_without_a_value_sends_no_user_agent() {
        assertThat(HttpClientParams.validate(httpClientConfig().setParam(HttpClientParams.USER_AGENT, null))).isNull();
    }

    @Test
    void the_user_agent_parameter_is_the_default_user_agent() {
        assertThat(HttpClientParams.validate(httpClientConfig().setParam(HttpClientParams.USER_AGENT, "mine/1.0"))).isEqualTo("mine/1.0");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "http.connection.timeout",
            "http.socket.timeout",
            "http.conn-manager.timeout",
            "http.protocol.cookie-policy",
            "http.protocol.cookie-datepatterns",
            "http.protocol.expect-continue",
            "http.useragent",
            "http.protocol.handle-redirects",
            "http.protocol.max-redirects",
            "http.protocol.allow-circular-redirects",
            "http.protocol.reject-relative-redirect"})
    void a_parameter_that_rest_assured_still_reads_is_accepted(String name) {
        HttpClientParams.validate(httpClientConfig().setParam(name, "1"));
    }

    @Test
    void a_parameter_that_rest_assured_doesnt_read_any_more_fails() {
        assertThatThrownBy(() -> HttpClientParams.validate(httpClientConfig().setParam("http.default-headers", List.of("x"))))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The Apache HttpClient 4 parameter \"http.default-headers\" of the HttpClientConfig isn't used any more")
                .hasMessageContaining("httpClientConfig().customizeClient(..)");
    }

    @Test
    void a_value_rest_assured_cant_read_fails_with_the_replacement() {
        assertThatThrownBy(() -> HttpClientParams.validate(httpClientConfig().setParam("http.socket.timeout", new Object())))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a String, a number, a boolean or a list of Strings, but was java.lang.Object")
                .hasMessageContaining("Use httpClientConfig().readTimeout(..) instead.");
        assertThatThrownBy(() -> HttpClientParams.validate(httpClientConfig().setParam("http.protocol.cookie-datepatterns", List.of(1))))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Use httpClientConfig().cookieDatePatterns(..) instead.");
    }

    @Test
    void a_string_a_number_a_boolean_and_a_list_of_strings_are_read() {
        HttpClientParams.validate(httpClientConfig().setParam("http.socket.timeout", 1000)
                .setParam("http.protocol.expect-continue", true)
                .setParam("http.protocol.cookie-policy", "default")
                .setParam("http.protocol.cookie-datepatterns", List.of("EEE, dd MMM yyyy HH:mm:ss zzz")));
    }
}

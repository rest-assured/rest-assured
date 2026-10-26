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

package io.restassured.internal.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OriginTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "http://Example.com/path, http, example.com, 80",
            "https://example.com/path, https, example.com, 443",
            "http://example.com:8080/path, http, example.com, 8080",
            "http://user:pass@example.com:8080/path, http, example.com, 8080",
            "http://[::1]:8080/path, http, [::1], 8080",
            "HTTP://My_Svc:8080/path, http, my_svc, 8080",
            "http://my_svc/path, http, my_svc, 80",
            "https://user@my_svc/path, https, my_svc, 443",
            "http://user@my_svc:9090/path, http, my_svc, 9090"})
    void the_scheme_host_and_port_of_a_uri(String uri, String scheme, String host, int port) {
        assertThat(Origin.of(URI.create(uri))).isEqualTo(new Origin(scheme, host, port));
    }

    @Test
    void a_host_whose_name_isnt_a_domain_name_is_only_the_same_as_itself() {
        Origin origin = Origin.of(URI.create("http://a_b:8080/start"));

        assertThat(origin.isSameAs(Origin.of(URI.create("http://A_B:8080/other")))).isTrue();
        assertThat(origin.isSameAs(Origin.of(URI.create("http://evil_c.attacker.example:8080/other")))).isFalse();
        assertThat(origin.isSameAs(Origin.of(URI.create("http://a_b:9090/start")))).isFalse();
        assertThat(origin.isSameAs(Origin.of(URI.create("https://a_b:8080/start")))).isFalse();
    }

    @Test
    void a_uri_without_a_host_is_the_same_as_no_other() {
        Origin origin = Origin.of(URI.create("http:/path"));

        assertThat(origin.host()).isNull();
        assertThat(origin.isSameAs(Origin.of(URI.create("http:/path")))).isFalse();
    }
}

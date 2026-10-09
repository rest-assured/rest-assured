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

import io.restassured.specification.RedirectSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.client.params.ClientPNames.ALLOW_CIRCULAR_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.MAX_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.REJECT_RELATIVE_REDIRECT;
import static org.assertj.core.api.Assertions.assertThat;

class RedirectSpecificationImplTest {

    @Test
    void each_method_puts_its_http_client_param_and_returns_the_request_specification() {
        RequestSpecification spec = given();
        Map<String, Object> params = new LinkedHashMap<>();
        RedirectSpecification redirects = new RedirectSpecificationImpl(spec, params);

        assertThat(redirects.max(3)).isSameAs(spec);
        assertThat(redirects.follow(false)).isSameAs(spec);
        assertThat(redirects.allowCircular(true)).isSameAs(spec);
        assertThat(redirects.rejectRelative(true)).isSameAs(spec);

        assertThat(params).containsExactly(
                Map.entry(MAX_REDIRECTS, 3),
                Map.entry(HANDLE_REDIRECTS, false),
                Map.entry(ALLOW_CIRCULAR_REDIRECTS, true),
                Map.entry(REJECT_RELATIVE_REDIRECT, true));
    }

    @Test
    void later_calls_replace_earlier_values() {
        Map<String, Object> params = new LinkedHashMap<>();
        RedirectSpecification redirects = new RedirectSpecificationImpl(given(), params);

        redirects.max(3);
        redirects.max(5);

        assertThat(params).containsExactly(Map.entry(MAX_REDIRECTS, 5));
    }

    @Test
    void redirects_writes_to_the_http_client_params_of_the_request_specification() throws Exception {
        RequestSpecification spec = given();

        RequestSpecification returned = spec.redirects().max(2).redirects().follow(true);

        assertThat(returned).isSameAs(spec);
        Field field = RequestSpecificationImpl.class.getDeclaredField("httpClientParams");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) field.get(spec);
        assertThat(params).containsEntry(MAX_REDIRECTS, 2).containsEntry(HANDLE_REDIRECTS, true);
    }
}

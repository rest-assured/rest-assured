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

package io.restassured.internal.support;

import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.specification.FilterableRequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class UrlInQueryParameterRequestTest {

    @Test
    public void request_url_without_path_slash_can_have_a_url_in_its_query_string() {
        FilterableRequestSpecification request = send("https://example.com?redirect=https://example.com/callback");

        assertThat(request.getURI()).isEqualTo("https://example.com?redirect=https://example.com/callback");
    }

    @Test
    public void relative_path_without_leading_slash_can_have_a_url_in_its_query_string() {
        FilterableRequestSpecification request = send("x?r=http://y/z");

        assertThat(request.getURI()).isEqualTo("http://localhost:8080/x?r=http://y/z");
    }

    private static FilterableRequestSpecification send(String url) {
        AtomicReference<FilterableRequestSpecification> captured = new AtomicReference<>();
        Filter capture = (requestSpec, responseSpec, ctx) -> {
            captured.set(requestSpec);
            return new ResponseBuilder().setStatusCode(200).setStatusLine("HTTP/1.1 200 OK").setBody("").build();
        };
        given().filter(capture).urlEncodingEnabled(false).get(url);
        return captured.get();
    }
}

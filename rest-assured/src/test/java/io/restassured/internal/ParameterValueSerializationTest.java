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

import com.fasterxml.jackson.annotation.JsonValue;
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.specification.FilterableRequestSpecification;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ParameterValueSerializationTest {

    record UserId(UUID uuid) {
        @JsonValue
        @Override
        public String toString() {
            return uuid.toString();
        }
    }

    record Filters(String name, int limit) {
    }

    @Test
    public void java_time_values_are_sent_as_iso_8601_in_path_and_query_params() {
        FilterableRequestSpecification request = send(given()
                .pathParam("date", LocalDate.of(2024, 4, 10))
                .queryParam("since", Instant.parse("2024-04-10T10:15:30Z")), "/reports/{date}");

        assertThat(request.getURI()).isEqualTo("http://localhost:8080/reports/2024-04-10?since=2024-04-10T10%3A15%3A30Z");
    }

    @Test
    public void value_objects_serialized_to_a_json_string_are_sent_without_quotes() {
        UserId userId = new UserId(UUID.fromString("c2a5a7c4-587a-4f13-94f9-084ebfac5302"));

        FilterableRequestSpecification request = send(given()
                .pathParam("userId", userId)
                .queryParam("user_id", userId)
                .header("X-User", userId), "/users/{userId}");

        assertThat(request.getURI()).isEqualTo("http://localhost:8080/users/c2a5a7c4-587a-4f13-94f9-084ebfac5302?user_id=c2a5a7c4-587a-4f13-94f9-084ebfac5302");
        assertThat(request.getHeaders().getValue("X-User")).isEqualTo("c2a5a7c4-587a-4f13-94f9-084ebfac5302");
    }

    @Test
    public void value_objects_serialized_to_json_objects_are_still_sent_as_json() {
        FilterableRequestSpecification request = send(given().queryParam("filters", new Filters("john", 10)), "/users");

        assertThat(request.getQueryParams().get("filters")).isEqualTo("{\"name\":\"john\",\"limit\":10}");
    }

    @Test
    public void quoted_strings_are_sent_as_is() {
        FilterableRequestSpecification request = send(given().header("If-None-Match", "\"etag-value\""), "/users");

        assertThat(request.getHeaders().getValue("If-None-Match")).isEqualTo("\"etag-value\"");
    }

    private static FilterableRequestSpecification send(io.restassured.specification.RequestSpecification spec, String path) {
        AtomicReference<FilterableRequestSpecification> captured = new AtomicReference<>();
        Filter capture = (requestSpec, responseSpec, ctx) -> {
            captured.set(requestSpec);
            return new ResponseBuilder().setStatusCode(200).setStatusLine("HTTP/1.1 200 OK").setBody("").build();
        };
        spec.filter(capture).get("http://localhost:8080" + path);
        return captured.get();
    }
}

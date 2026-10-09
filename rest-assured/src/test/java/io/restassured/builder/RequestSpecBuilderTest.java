/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.builder;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.http.Method;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RequestSpecBuilderTest {
    private static final Filter OK_RESPONSE = (requestSpec, responseSpec, ctx) -> new ResponseBuilder().setStatusCode(200).build();

    private static void assertSendingDirectlyIsRejected(ThrowingCallable send) {
        assertThatThrownBy(send)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A request specification built by RequestSpecBuilder can't send a request or hold response expectations itself. " +
                        "Use it with given().spec(specification) or given(specification), e.g. given().spec(specification).when().get(\"/path\").");
    }

    @Test public void
    request_spec_doesnt_throw_NPE_when_logging_after_creation() {
        new RequestSpecBuilder().build().log().all(true);
    }

    @Test public void
    request_spec_throws_illegal_state_exception_when_sending_a_request_directly() {
        RequestSpecification spec = new RequestSpecBuilder().addHeader("name", "value").addFilter(OK_RESPONSE).build();

        assertSendingDirectlyIsRejected(() -> spec.when().get("http://localhost:8080/x"));
        assertSendingDirectlyIsRejected(() -> spec.get("http://localhost:8080/x"));
        assertSendingDirectlyIsRejected(spec::post);
        assertSendingDirectlyIsRejected(() -> spec.request(Method.PUT, "http://localhost:8080/x"));
        assertSendingDirectlyIsRejected(() -> spec.delete("http://localhost:8080/{id}", Collections.singletonMap("id", "1")));
    }

    @Test public void
    request_spec_is_unchanged_after_sending_a_request_directly_is_rejected() {
        RequestSpecificationImpl spec = (RequestSpecificationImpl) new RequestSpecBuilder().addFilter(OK_RESPONSE).build();

        assertSendingDirectlyIsRejected(() -> spec.get("http://localhost:8080/{id}", Collections.singletonMap("id", "1")));
        assertSendingDirectlyIsRejected(() -> spec.post("http://localhost:8080/{id}", "1"));

        assertThat(spec.getPathParams()).isEmpty();
        assertThat(spec.getMethod()).isNull();
        assertThat(spec.getDefinedFilters()).containsExactly(OK_RESPONSE);
    }

    @Test public void
    request_spec_throws_illegal_state_exception_when_defining_response_expectations_directly() {
        RequestSpecification spec = new RequestSpecBuilder().build();

        assertSendingDirectlyIsRejected(spec::then);
        assertSendingDirectlyIsRejected(spec::expect);
        assertSendingDirectlyIsRejected(spec::response);
    }

    @Test public void
    request_spec_sends_request_when_added_to_given() {
        AtomicReference<String> sentHeaders = new AtomicReference<>();
        RequestSpecification spec = new RequestSpecBuilder().addHeader("name", "value").addFilter((requestSpec, responseSpec, ctx) -> {
            sentHeaders.set(requestSpec.getHeaders().getValues("name").toString());
            return OK_RESPONSE.filter(requestSpec, responseSpec, ctx);
        }).build();

        given().spec(spec).when().get("http://localhost:8080/x").then().statusCode(200);
        assertThat(sentHeaders.get()).isEqualTo("[value]");
        given(spec).when().get("http://localhost:8080/x").then().statusCode(200);
        assertThat(sentHeaders.get()).isEqualTo("[value]");
    }

    @Test public void
    request_spec_picks_up_filters_from_static_config() {
        RestAssured.filters(new RequestLoggingFilter());
        try {
            RequestSpecBuilder builder = new RequestSpecBuilder();
            RequestSpecificationImpl spec = (RequestSpecificationImpl) builder.build();
            assertThat(spec.getDefinedFilters()).hasSize(1);
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    request_spec_picks_up_headers_from_static_request_spec() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("hello", "world")
                .build();
        try {
            RequestSpecBuilder builder = new RequestSpecBuilder();
            RequestSpecificationImpl spec = (RequestSpecificationImpl) builder.build();
            assertThat(spec.getHeaders().getValue("hello")).isEqualTo("world");
        } finally {
            RestAssured.reset();
        }
    }
}

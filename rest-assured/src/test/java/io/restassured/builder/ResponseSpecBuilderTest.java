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
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResponseSpecBuilderTest {
    private static final Filter OK_RESPONSE = (requestSpec, responseSpec, ctx) -> new ResponseBuilder().setStatusCode(200).build();

    private static void assertDefiningOrSendingARequestDirectlyIsRejected(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A response specification built by ResponseSpecBuilder can't define or send a request itself. " +
                        "Use it with given().spec(requestSpec).expect().spec(responseSpec), then().spec(responseSpec) or given(requestSpec, responseSpec), " +
                        "e.g. given().spec(requestSpec).when().get(\"/path\").then().spec(responseSpec).");
    }

    @Test
    @DisplayName("response_spec_doesnt_throw_NPE_when_logging_all_after_creation")
    void response_spec_doesnt_throw_NPE_when_logging_all_after_creation() {
        assertThatThrownBy(() -> new ResponseSpecBuilder().build().log().all(true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot configure logging since request specification is not defined. You may be misusing the API.");
    }

    @Test
    void response_spec_throws_illegal_state_exception_when_defining_or_sending_a_request_directly() {
        ResponseSpecification spec = new ResponseSpecBuilder().expectStatusCode(200).build();

        assertDefiningOrSendingARequestDirectlyIsRejected(() -> spec.when().get("http://localhost:8080/x"));
        assertDefiningOrSendingARequestDirectlyIsRejected(spec::when);
        assertDefiningOrSendingARequestDirectlyIsRejected(spec::given);
        assertDefiningOrSendingARequestDirectlyIsRejected(spec::with);
        assertDefiningOrSendingARequestDirectlyIsRejected(spec::request);
        assertDefiningOrSendingARequestDirectlyIsRejected(() -> spec.expect().given());
    }

    @Test
    void response_spec_validates_the_response_when_used_with_a_request() {
        ResponseSpecification spec = new ResponseSpecBuilder().expectStatusCode(200).build();
        RequestSpecification requestSpec = new RequestSpecBuilder().addFilter(OK_RESPONSE).build();

        given().spec(requestSpec).expect().spec(spec).when().get("http://localhost:8080/x");
        given().filter(OK_RESPONSE).when().get("http://localhost:8080/x").then().spec(spec);
        given(requestSpec, spec).get("http://localhost:8080/x");

        ResponseSpecification failingSpec = new ResponseSpecBuilder().expectStatusCode(201).build();
        assertThatThrownBy(() -> given().spec(requestSpec).expect().spec(failingSpec).when().get("http://localhost:8080/x"))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> given().filter(OK_RESPONSE).when().get("http://localhost:8080/x").then().spec(failingSpec))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void response_spec_set_as_the_default_response_specification_validates_the_response() {
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(201).build();
        try {
            assertThatThrownBy(() -> given().filter(OK_RESPONSE).when().get("http://localhost:8080/x"))
                    .isInstanceOf(AssertionError.class);
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    @DisplayName("responseSpecShouldContainMergedExpectations")
    void responseSpecShouldContainMergedExpectations() {
        ResponseSpecification originalSpec = new ResponseSpecBuilder()
                .expectBody(equalTo("goodTestBody"))
                .build();
        ResponseSpecification mergedSpec = new ResponseSpecBuilder()
                .addResponseSpecification(originalSpec)
                .build();

        Response goodResponse = mock(Response.class);
        when(goodResponse.asString()).thenReturn("goodTestBody");

        Response badResponse = mock(Response.class);
        when(badResponse.asString()).thenReturn("badTestBody");

        // Should not throw for good response
        mergedSpec.validate(goodResponse);
        // Should throw AssertionError for bad response
        assertThatThrownBy(() -> mergedSpec.validate(badResponse))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("responseParserShouldHandleConfiguredContentType")
    void responseParserShouldHandleConfiguredContentType() {
        ResponseSpecificationImpl responseSpec = (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .registerParser("dummyContentType", Parser.HTML)
                .build();

        assertThat(responseSpec.getRpr().getParser("dummyContentType")).isEqualTo(Parser.HTML);
    }

    @Test
    @DisplayName("defaultResponseParserShouldBeConfiguredToHandleUnrecognizedContentTypes")
    void defaultResponseParserShouldBeConfiguredToHandleUnrecognizedContentTypes() {
        ResponseSpecificationImpl responseSpec = (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .setDefaultParser(Parser.HTML)
                .build();

        assertThat(responseSpec.getRpr().getParser("nonExistentContentType")).isEqualTo(Parser.HTML);
    }
}

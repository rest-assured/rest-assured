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

import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResponseSpecBuilderTest {

    @Test
    @DisplayName("response_spec_doesnt_throw_NPE_when_logging_all_after_creation")
    void response_spec_doesnt_throw_NPE_when_logging_all_after_creation() {
        assertThatThrownBy(() -> new ResponseSpecBuilder().build().log().all(true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot configure logging since request specification is not defined. You may be misusing the API.");
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

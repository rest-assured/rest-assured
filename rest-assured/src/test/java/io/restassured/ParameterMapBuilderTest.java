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

package io.restassured;

import io.restassured.authentication.NoAuthScheme;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.log.LogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.withArgs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.equalTo;

class ParameterMapBuilderTest {
    private RequestSpecificationImpl requestBuilder;

    @BeforeEach
    void setup() {
        requestBuilder = new RequestSpecificationImpl("baseURI", 20, "", new NoAuthScheme(), Collections.emptyList(), null, true, null, new LogRepository(), null, true, true);
    }

    @Test
    void mapThrowIAEWhenOddNumberOfStringsAreSupplied() {
        Throwable throwable = catchThrowable(() -> requestBuilder.params("key1", "value1", "key2"));
        assertThat(throwable).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply the same number of keys as values.");
    }

    // Does RA really handle conversion from map with multi-value parameters correctly?
    @Test
    void doesntThrowWhenSecondParameterIsAMap() {
        Map<String, String> map = ((RequestSpecificationImpl) requestBuilder.params("key1", Collections.singletonList("value1"))).getRequestParams();
        assertThat(map).hasSize(1);
    }

    @Test
    void mapThrowIAEWhenMixingArgumentsAndNoArguments() {
        Throwable throwable = catchThrowable(() -> requestBuilder.params("key1", withArgs("hello"), "key2", "key", equalTo("2")));
        assertThat(throwable).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("You must supply the same number of keys as values.");
    }

    @Test
    void mapBuildsAMapBasedOnTheSuppliedKeysAndValues() {
        Map<String, String> map = ((RequestSpecificationImpl) requestBuilder.params("key1", "value1", "key2", "value2")).getRequestParams();
        assertThat(map).hasSize(2);
        assertThat(map.get("key1")).isEqualTo("value1");
        assertThat(map.get("key2")).isEqualTo("value2");
    }

    @Test
    void removesParamOnRemoveParamMethod() {
        requestBuilder.params("key1", "value1");
        Map<String, String> map = ((RequestSpecificationImpl) requestBuilder.removeParam("key1")).getRequestParams();

        assertThat(map).hasSize(0);
    }

    @Test
    void removesQueryParamOnRemoveQueryParamMethod() {
        requestBuilder.queryParams("key1", "value1");
        Map<String, String> map = ((RequestSpecificationImpl) requestBuilder.removeQueryParam("key1")).getQueryParams();

        assertThat(map).hasSize(0);
    }

    @Test
    void removesFormParamOnRemoveFormParamMethod() {
        requestBuilder.queryParams("key1", "value1");
        Map<String, String> map = ((RequestSpecificationImpl) requestBuilder.removeFormParam("key1")).getFormParams();

        assertThat(map).hasSize(0);
    }

    @Test
    void removesPathParamOnRemoveFormPathMethod() {
        requestBuilder.pathParams("key1", "value1");
        Map<String, String> map = ((RequestSpecificationImpl) requestBuilder.removePathParam("key1")).getNamedPathParams();

        assertThat(map).hasSize(0);
    }
}

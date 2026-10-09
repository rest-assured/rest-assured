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

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class RequestSpecificationTest {
    private static final String CONTENT_TYPE = "content-type";
    private static final String CONTENT_TYPE_TEST_VALUE = "something";

    @Test
    void allowsRemovingAllFilters() {
        RequestSpecification requestSpec = given().filter(new ExampleFilter1()).filter(new ExampleFilter2()).noFilters();

        assertThat(filterable(requestSpec).getDefinedFilters()).isEmpty();
    }

    @Test
    void allowsRemovingFiltersOfASpecificType() {
        RequestSpecification requestSpec = given().filters(asList(new ExampleFilter1(), new ExampleFilter2(), new ExampleFilter3())).noFiltersOfType(ExampleFilter1.class);

        assertThat(filterable(requestSpec).getDefinedFilters()).hasSize(1);
        assertThat(filterable(requestSpec).getDefinedFilters().get(0)).isInstanceOf(ExampleFilter2.class);
    }

    @Test
    void contentTypeAsHeaderParameter() {
        RequestSpecification requestSpec = given().header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE);

        assertThat(filterable(requestSpec).getHeaders().get(CONTENT_TYPE).getValue()).isEqualTo(CONTENT_TYPE_TEST_VALUE);
    }

    @Test
    void contentTypeAsHeaderObject() {
        Header header = new Header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE);
        RequestSpecification requestSpec = given().header(header);

        assertThat(filterable(requestSpec).getHeaders().get(CONTENT_TYPE).getValue()).isEqualTo(header.getValue());
    }

    @Test
    void contentTypeInHeaderObject() {
        Headers header = new Headers(new Header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE));

        RequestSpecification requestSpec = given().headers(header);

        assertThat(filterable(requestSpec).getHeaders().get(CONTENT_TYPE).getValue()).isEqualTo(CONTENT_TYPE_TEST_VALUE);
    }

    @Test
    void contentTypeInHeaderMap() {
        Map<String, String> headerMap = new TreeMap<>();
        headerMap.put(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE);

        RequestSpecification requestSpec = given().headers(headerMap);

        assertThat(filterable(requestSpec).getHeaders().get(CONTENT_TYPE).getValue()).isEqualTo(CONTENT_TYPE_TEST_VALUE);
    }

    private static FilterableRequestSpecification filterable(RequestSpecification requestSpec) {
        return (FilterableRequestSpecification) requestSpec;
    }

    private static class ExampleFilter1 implements Filter {
        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            return null;
        }
    }

    private static class ExampleFilter2 implements Filter {
        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            return null;
        }
    }

    private static class ExampleFilter3 extends ExampleFilter1 {
        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            return null;
        }
    }
}

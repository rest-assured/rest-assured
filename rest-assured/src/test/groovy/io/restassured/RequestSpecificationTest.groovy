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



package io.restassured

import io.restassured.filter.Filter
import io.restassured.filter.FilterContext
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.response.Response
import io.restassured.specification.FilterableRequestSpecification
import io.restassured.specification.FilterableResponseSpecification
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import static RestAssured.given
import static java.util.Arrays.asList
import static org.assertj.core.api.Assertions.assertThat

class RequestSpecificationTest {
    private static final def CONTENT_TYPE = "content-type"
    private static final def CONTENT_TYPE_TEST_VALUE = "something"

  @Test
  void allowsRemovingAllFilters() {
    def requestSpec = given().filter(new ExampleFilter1()).filter(new ExampleFilter2()).noFilters()

    assertThat(requestSpec.filters).isEmpty()
  }

  @Test
  void allowsRemovingFiltersOfASpecificType() {
    def requestSpec = given().filters(asList(new ExampleFilter1(), new ExampleFilter2(), new ExampleFilter3())).noFiltersOfType(ExampleFilter1.class)

    assertThat(requestSpec.filters).hasSize(1)
    assertThat(requestSpec.filters.get(0)).isInstanceOf(ExampleFilter2.class)
  }

  @Test
  void contentTypeAsHeaderParameter() {
    def requestSpec = given().header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE)

    assertThat(requestSpec.requestHeaders.get(CONTENT_TYPE).getValue()).isEqualTo(CONTENT_TYPE_TEST_VALUE)
  }

  @Test
  void contentTypeAsHeaderObject() {
    def header = new Header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE)
    def requestSpec = given().header(header)

    assertThat(requestSpec.requestHeaders.get(CONTENT_TYPE).getValue()).isEqualTo(header.value)
  }

  @Test
  void contentTypeInHeaderObject() {
    def header = new Headers(new Header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE))

    def requestSpec = given().headers(header)

    assertThat(requestSpec.requestHeaders.get(CONTENT_TYPE).getValue()).isEqualTo(CONTENT_TYPE_TEST_VALUE)
  }

  @Test
  void contentTypeInHeaderMap() {
      def headerMap = new TreeMap<String, String>()
      headerMap.put(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE)

      def requestSpec = given().headers(headerMap)

      assertThat(requestSpec.requestHeaders.get(CONTENT_TYPE).getValue()).isEqualTo(CONTENT_TYPE_TEST_VALUE)
  }

  @Disabled
  private class ExampleFilter1 implements Filter {
    @Override
    Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
      return null
    }
  }

  @Disabled
  private class ExampleFilter2 implements Filter {
    @Override
    Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
      return null
    }
  }

  @Disabled
  private class ExampleFilter3 extends ExampleFilter1 {
    @Override
    Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
      return null
    }
  }
}

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
import org.junit.Ignore
import org.junit.Test

import static RestAssured.given
import static java.util.Arrays.asList
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class RequestSpecificationTest {
    private static final def CONTENT_TYPE = "content-type"
    private static final def CONTENT_TYPE_TEST_VALUE = "something"

  @Test
  public void allowsRemovingAllFilters() throws Exception {
    def requestSpec = given().filter(new ExampleFilter1()).filter(new ExampleFilter2()).noFilters();

    assertThat(requestSpec.filters.isEmpty(), equalTo(true))
  }

  @Test
  public void allowsRemovingFiltersOfASpecificType() throws Exception {
    def requestSpec = given().filters(asList(new ExampleFilter1(), new ExampleFilter2(), new ExampleFilter3())).noFiltersOfType(ExampleFilter1.class);

    assertThat(requestSpec.filters[0], instanceOf(ExampleFilter2.class))
    assertThat(requestSpec.filters.size(), equalTo(1))
  }

  @Test
  public void contentTypeAsHeaderParameter() {
    def requestSpec = given().header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE)

    assertEquals(CONTENT_TYPE_TEST_VALUE, requestSpec.requestHeaders.get(CONTENT_TYPE).getValue())
  }

  @Test
  public void contentTypeAsHeaderObject() {
    def header = new Header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE)
    def requestSpec = given().header(header);

    assertEquals(header.value, requestSpec.requestHeaders.get(CONTENT_TYPE).getValue())
  }

  @Test
  public void contentTypeInHeaderObject() {
    def header = new Headers(new Header(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE))

    def requestSpec = given().headers(header)

    assertEquals(CONTENT_TYPE_TEST_VALUE, requestSpec.requestHeaders.get(CONTENT_TYPE).getValue())
  }

  @Test
  public void contentTypeInHeaderMap() {
      def headerMap = new TreeMap<String, String>()
      headerMap.put(CONTENT_TYPE, CONTENT_TYPE_TEST_VALUE)

      def requestSpec = given().headers(headerMap)

      assertEquals(CONTENT_TYPE_TEST_VALUE, requestSpec.requestHeaders.get(CONTENT_TYPE).getValue())
  }

  @Ignore
  private class ExampleFilter1 implements Filter {
    @Override
    Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
      return null
    }
  }

  @Ignore
  private class ExampleFilter2 implements Filter {
    @Override
    Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
      return null
    }
  }

  @Ignore
  private class ExampleFilter3 extends ExampleFilter1 {
    @Override
    Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
      return null
    }
  }
}

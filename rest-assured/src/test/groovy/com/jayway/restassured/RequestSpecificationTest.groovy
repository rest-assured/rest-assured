/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured

import com.jayway.restassured.filter.Filter
import com.jayway.restassured.filter.FilterContext
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.FilterableRequestSpecification
import com.jayway.restassured.specification.FilterableResponseSpecification
import org.junit.Ignore
import org.junit.Test
import static com.jayway.restassured.RestAssured.given
import static java.util.Arrays.asList
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertEquals
import com.jayway.restassured.response.Header

class RequestSpecificationTest {

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
  public void contentTypeAsHeaderParameters() {
    def requestSpec = given().header("content-type", "something");

    assertEquals(requestSpec.contentType, "something");
  }

  public void contentTypeAsHeaderObject() {
      def header = new Header("content-type", "something");
    def requestSpec = given().header(header);

    assertEquals(requestSpec.contentType(), header);
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

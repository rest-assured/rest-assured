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

import com.jayway.restassured.builder.RequestSpecBuilder
import com.jayway.restassured.builder.ResponseSpecBuilder
import com.jayway.restassured.filter.Filter
import com.jayway.restassured.filter.FilterContext
import com.jayway.restassured.internal.SpecificationMerger
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.FilterableRequestSpecification
import com.jayway.restassured.specification.FilterableResponseSpecification
import groovyx.net.http.ContentType
import org.junit.Test
import static groovy.util.GroovyTestCase.assertEquals
import static java.util.Arrays.asList
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertTrue

class SpecificationMergerTest {

  @Test
  def void mergesCookies() throws Exception {
    def merge = new ResponseSpecBuilder().expectCookie("first", "value1").build();
    def with = new ResponseSpecBuilder().expectCookie("second", "value2").build();

    SpecificationMerger.merge(merge, with)

    assertEquals merge.cookieAssertions.size(), 2
  }

  @Test
  def void mergesHeaders() throws Exception {
    def merge = new ResponseSpecBuilder().expectHeader("first", "value1").build();
    def with = new ResponseSpecBuilder().expectHeader("second", "value2").build();

    SpecificationMerger.merge(merge, with)

    assertEquals merge.headerAssertions.size(), 2
  }

  @Test
  def void mergesBodyMatchers() throws Exception {
    def merge = new ResponseSpecBuilder().expectBody("first", equalTo("value1")).build();
    def with = new ResponseSpecBuilder().expectBody("second", equalTo("value2")).build();

    SpecificationMerger.merge(merge, with)

    assertEquals merge.bodyMatchers.size(), 2
  }

  @Test
  def void mergesFilters() throws Exception {
    def merge = new RequestSpecBuilder().addFilter(newFilter()).addFilter(newFilter()).build();
    def with = new RequestSpecBuilder().addFilter(newFilter()).addFilters(asList(newFilter(), newFilter())).build();

    SpecificationMerger.merge(merge, with)

    assertEquals merge.filters.size(), 5
  }

  @Test
  public void overwritesContentType() throws Exception {
    def merge = new ResponseSpecBuilder().expectContentType(ContentType.ANY).build();
    def with = new ResponseSpecBuilder().expectContentType(ContentType.BINARY).build();

    SpecificationMerger.merge(merge, with)

    assertEquals merge.contentType, ContentType.BINARY
  }

  @Test
  public void overwritesRootPath() throws Exception {
    def merge = new ResponseSpecBuilder().rootPath("rootPath").build();
    def with = new ResponseSpecBuilder().rootPath("new.").build();

    SpecificationMerger.merge(merge, with)

    assertEquals merge.bodyRootPath, "new."
  }

  @Test
  public void overwritesStatusCode() throws Exception {
    def merge = new ResponseSpecBuilder().expectStatusCode(200).build();
    def with = new ResponseSpecBuilder().expectStatusCode(400).build();

    SpecificationMerger.merge(merge, with)

    assertTrue merge.expectedStatusCode.matches(400)
  }

  @Test
  public void overwritesStatusLine() throws Exception {
    def merge = new ResponseSpecBuilder().expectStatusLine("something").build();
    def with = new ResponseSpecBuilder().expectStatusLine("something else").build();

    SpecificationMerger.merge(merge, with)

    assertTrue merge.expectedStatusLine.matches("something else")
  }

  private Filter newFilter() {
    return new Filter() {
      @Override
      Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        return ctx.next(requestSpec, responseSpec);
      }
    };
  }
}

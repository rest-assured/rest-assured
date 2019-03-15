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

import io.restassured.authentication.NoAuthScheme
import io.restassured.internal.RequestSpecificationImpl
import io.restassured.internal.log.LogRepository
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class ParameterMapBuilderTest {
  private RequestSpecificationImpl requestBuilder;

  @Before
  public void setup() throws Exception {
    requestBuilder = new RequestSpecificationImpl("baseURI", 20, "", new NoAuthScheme(), [], null, true, null, new LogRepository(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  void mapThrowIAEWhenOddNumberOfStringsAreSupplied() throws Exception {
    requestBuilder.params("key1", "value1", "key2");
  }

  @Test
  void mapBuildsAMapBasedOnTheSuppliedKeysAndValues() throws Exception {
    def map = requestBuilder.params("key1", "value1", "key2", "value2").requestParameters;

    assertEquals 2, map.size()
    assertEquals "value1", map.get("key1")
    assertEquals "value2", map.get("key2")
  }

  @Test
  void removesParamOnRemoveParamMethod() throws Exception {
    requestBuilder.params("key1", "value1");
    def map = requestBuilder.removeParam("key1").requestParameters

    assertEquals 0, map.size()
  }

  @Test
  void removesQueryParamOnRemoveQueryParamMethod() throws Exception {
    requestBuilder.queryParams("key1", "value1");
    def map = requestBuilder.removeQueryParam("key1").queryParameters

    assertEquals 0, map.size()
  }

  @Test
  void removesFormParamOnRemoveFormParamMethod() throws Exception {
    requestBuilder.queryParams("key1", "value1");
    def map = requestBuilder.removeFormParam("key1").formParameters

    assertEquals 0, map.size()
  }

  @Test
  void removesPathParamOnRemoveFormPathMethod() throws Exception {
    requestBuilder.pathParams("key1", "value1");
    def map = requestBuilder.removePathParam("key1").namedPathParameters

    assertEquals 0, map.size()
  }
}

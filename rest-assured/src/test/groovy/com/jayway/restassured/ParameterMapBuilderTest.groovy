/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured

import com.jayway.restassured.authentication.NoAuthScheme
import com.jayway.restassured.internal.NoKeystoreSpecImpl
import com.jayway.restassured.internal.RequestSpecificationImpl
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class ParameterMapBuilderTest {
  private RequestSpecificationImpl requestBuilder;

  @Before
  public void setup() throws Exception {
    requestBuilder = new RequestSpecificationImpl("baseURI", 20, "", new NoAuthScheme(), [], new NoKeystoreSpecImpl(), null, null, true, null);
  }

  @Test(expected = IllegalArgumentException.class)
  def void mapThrowIAEWhenOddNumberOfStringsAreSupplied() throws Exception {
    requestBuilder.parameters("key1", "value1", "key2");
  }

  @Test
  def void mapBuildsAMapBasedOnTheSuppliedKeysAndValues() throws Exception {
    def map = requestBuilder.parameters("key1", "value1", "key2", "value2").requestParameters;

    assertEquals 2, map.size()
    assertEquals "value1", map.get("key1")
    assertEquals "value2", map.get("key2")
  }
}

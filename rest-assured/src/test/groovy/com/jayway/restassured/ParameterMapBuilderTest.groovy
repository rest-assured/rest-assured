package com.jayway.restassured

import org.junit.Before
import org.junit.Test
import static groovy.util.GroovyTestCase.assertEquals
import com.jayway.restassured.internal.RequestSpecificationImpl

class ParameterMapBuilderTest {
  private RequestSpecificationImpl requestBuilder;

  @Before
  public void setup() throws Exception {
    requestBuilder = new RequestSpecificationImpl();
  }

  @Test(expected = IllegalArgumentException.class)
  def void mapThrowIAEWhenNoValuesAreSupplied() throws Exception {
    requestBuilder.parameters("key");
  }

  @Test(expected = IllegalArgumentException.class)
  def void mapThrowIAEWhenOddNumberOfStringsAreSupplied() throws Exception {
    requestBuilder.parameters("key1", "value1", "key2");
  }

  @Test
  def void mapBuildsAMapBasedOnTheSuppliedKeysAndValues() throws Exception {
    def map = requestBuilder.parameters("key1", "value1", "key2", "3").parameters;

    assertEquals 2, map.size()
    assertEquals "value1", map.get("key1")
    assertEquals "3", map.get("key2")
  }
}

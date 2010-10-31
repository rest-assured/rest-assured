package com.jayway.restassured


import org.junit.Test;
import static com.jayway.restassured.RestAssured.map
import static groovy.util.GroovyTestCase.assertEquals
import static org.junit.Assert.assertTrue
import org.junit.Before

class ParameterMapBuilderTest {
  private RequestBuilder requestBuilder;

  @Before
  public void setup() throws Exception {
    requestBuilder = new RequestBuilder();
  }

  @Test(expected = IllegalArgumentException.class)
  def void mapThrowIAEWhenNoKeyValuePairsAreSupplied() throws Exception {
    requestBuilder.parameters();
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
    def map = requestBuilder.parameters("key1", "value1", "key2", 3).query;

    assertEquals 2, map.size()
    assertEquals "value1", map.get("key1")
    assertEquals 3, map.get("key2")
  }
}
